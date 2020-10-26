/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.rpm.hm;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.rpm.Digest;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.meta.XmlPackage;
import com.jcabi.xml.XMLDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.cactoos.text.FormattedText;
import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.llorllale.cactoos.matchers.MatcherOf;

/**
 * Matcher for checking rempomd.xml file presence and information in the storage.
 *
 * @since 1.1
 * @todo #317:30min Implement StorageHasRepoMd matcher. StorageHasRepoMd
 *  should verify that repomd.xml has valid information about primary,
 *  filelists and other metadata files. After implementing this, enable tests
 *  in StorageHasRepoMdTest.
 */
public final class StorageHasRepoMd extends AllOf<Storage> {

    /**
     * Repodata key.
     */
    private static final Key BASE = new Key.From("repodata");

    /**
     * Repomd rey.
     */
    private static final Key.From REPOMD = new Key.From(StorageHasRepoMd.BASE, "repomd.xml");

    /**
     * Ctor.
     * @param config Rmp repo config
     */
    public StorageHasRepoMd(final RepoConfig config) {
        super(matchers(config));
    }

    /**
     * List of matchers.
     * @param config Rmp repo config
     * @return Matchers list
     */
    private static List<Matcher<? super Storage>> matchers(final RepoConfig config) {
        final List<Matcher<? super Storage>> res = new ArrayList<>(4);
        res.add(
            new MatcherOf<Storage>(
                storage -> storage.exists(StorageHasRepoMd.REPOMD).join(),
                new FormattedText("Repomd is present")
            )
        );
        new XmlPackage.Stream(config.filelists()).get().forEach(
            pckg -> res.add(
                new MatcherOf<Storage>(
                    storage -> hasRecord(storage, pckg, config.digest()),
                    new FormattedText("Repomd has record for %s xml", pckg.name())
                )
            )
        );
        return res;
    }

    /**
     * Has repomd record for xml metadata package?
     * @param storage Storage
     * @param pckg Metadata package
     * @param digest Digest algorithm
     * @return True if record is present
     */
    private static boolean hasRecord(final Storage storage, final XmlPackage pckg,
        final Digest digest) {
        final Optional<Content> repomd = storage.list(StorageHasRepoMd.BASE).join().stream()
            .filter(item -> item.string().contains(pckg.filename())).findFirst()
            .map(item -> storage.value(new Key.From(item)).join());
        boolean res = false;
        if (repomd.isPresent()) {
            final String checksum = new ContentDigest(
                repomd.get(),
                digest::messageDigest
            ).hex().toCompletableFuture().join();
            res = !new XMLDocument(
                new PublisherAs(storage.value(StorageHasRepoMd.REPOMD).join())
                    .asciiString().toCompletableFuture().join()
            ).nodes(
                String.format(
                    //@checkstyle LineLengthCheck (1 line)
                    "/*[name()='repomd']/*[name()='data' and @type='%s']/*[name()='checksum' and @type='%s' and text()='%s']",
                    pckg.name().toLowerCase(Locale.US),
                    digest.type(),
                    checksum
                )
            ).isEmpty();
        }
        return res;
    }
}
