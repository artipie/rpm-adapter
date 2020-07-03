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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.rpm.files.Gzip;
import com.artipie.rpm.meta.XmlPackage;
import com.jcabi.xml.XMLDocument;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.cactoos.text.FormattedText;
import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.llorllale.cactoos.matchers.MatcherOf;

/**
 * Storage has metadata matcher.
 * @since 0.11
 * @todo #311:30min Create proper unit test for this class (use metadata examples from test
 *  resources), do not forget to test mismatches descriptions.
 * @todo #311:30min Add one more check (here or create another matcher) for repomd.xml: we need to
 *  verify that exactly one repomd is present in storage and has info about metadatas, as example
 *  check RpmITCase#assertion(). Repomd should be verified at least in RpmTest and RpmITCase.
 *  @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class StorageHasMetadata extends AllOf<Storage> {

    /**
     * Ctor.
     * @param expected Amount of expected items in metadata
     * @param filelists Need filelist
     * @param temp Temp dir to unpack xml
     */
    public StorageHasMetadata(final int expected, final boolean filelists, final Path temp) {
        super(StorageHasMetadata.matchers(expected, filelists, temp));
    }

    /**
     * List of matchers.
     * @param expected Amount of expected items in metadata
     * @param filelists Need filelist
     * @param temp Temp dir to unpack xml
     * @return Matchers list
     */
    private static List<Matcher<? super Storage>> matchers(
        final int expected, final boolean filelists, final Path temp
    ) {
        return new XmlPackage.Stream(filelists).get().map(
            pckg -> new MatcherOf<Storage>(
                storage -> hasMetadata(storage, temp, pckg, expected),
                new FormattedText("Metadata %s has %d rpm packages", pckg.name(), expected)
            )
        ).collect(Collectors.toList());
    }

    /**
     * Obtains metadata xml from storage and checks that
     * this metadata has correct amount of packages.
     * @param storage Storage
     * @param temp Temp dir to unpack xml
     * @param pckg Package type
     * @param expected Amount of expected items in metadata
     * @return True if metadata is correct
     * @throws Exception On error
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    private static boolean hasMetadata(
        final Storage storage, final Path temp, final XmlPackage pckg, final int expected
    ) throws Exception {
        final BlockingStorage bsto = new BlockingStorage(storage);
        final List<Key> repodata = bsto.list(new Key.From("repodata")).stream()
            .filter(key -> key.string().contains(pckg.filename())).collect(Collectors.toList());
        final boolean res;
        if (repodata.size() == 1) {
            final Key meta = repodata.get(0);
            final Path gzip = Files.createTempFile(temp, pckg.name(), "xml.gz");
            Files.write(gzip, bsto.value(meta));
            final Path xml = Files.createTempFile(temp, pckg.name(), "xml");
            new Gzip(gzip).unpack(xml);
            res = new NodeHasPkgCount(expected, pckg.tag()).matches(new XMLDocument(xml));
        } else {
            res = false;
        }
        return res;
    }

}
