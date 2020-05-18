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
package com.artipie.rpm;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.rpm.files.Gzip;
import com.artipie.rpm.files.TestBundle;
import com.jcabi.matchers.XhtmlMatchers;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for {@link Rpm}.
 * @since 0.6
 * @todo #85:30min Continue the automation of batchUpdate tests.
 *  We still need to check the files to check primary.xml, others.xml and
 *  filelists.xml. These files are stored in storage at path:
 *  `repomd/SHA1-TYPE.xml.gz`, where SHA1 is a HEX from SHA1 of file content
 *  and TYPE is a type of file (primary, others, filelists). Don't forget to
 *  uncompress it first.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledIfSystemProperty(named = "it.longtests.enabled", matches = "true")
final class RpmITCase {

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    static Path tmp;

    /**
     * Gzipped bundle of RPMs.
     */
    private static Path bundle;

    /**
     * Repository storage with RPM packages.
     */
    private Storage storage;

    @BeforeAll
    static void setUpClass() throws Exception {
        RpmITCase.bundle = new TestBundle(TestBundle.Size.THOUSAND).unpack(RpmITCase.tmp);
    }

    @BeforeEach
    void setUp() throws Exception {
        final Path repo = Files.createDirectory(RpmITCase.tmp.resolve("repo"));
        new Gzip(RpmITCase.bundle).unpack(repo);
        this.storage = new FileStorage(repo);
    }

    @AfterEach
    void tearDown() throws Exception {
        FileUtils.deleteDirectory(RpmITCase.tmp.resolve("repo").toFile());
    }

    @Test
    void generatesMetadata() {
        new Rpm(this.storage, StandardNamingPolicy.SHA1, Digest.SHA256, true)
            .batchUpdate(Key.ROOT)
            .blockingAwait();
    }

    @Test
    void dontKeepOldMetadata() throws Exception {
        new Rpm(this.storage, StandardNamingPolicy.SHA1, Digest.SHA256, true)
            .batchUpdate(Key.ROOT)
            .blockingAwait();
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        MatcherAssert.assertThat(
            "got 4 metadata files after first update",
            bsto.list(new Key.From("repodata")).size(),
            Matchers.equalTo(4)
        );
        for (int cnt = 0; cnt < 5; ++cnt) {
            final Key first = bsto.list(Key.ROOT).stream()
                .filter(name -> name.string().endsWith(".rpm"))
                .findFirst().orElseThrow(() -> new IllegalStateException("not key found"));
            bsto.delete(first);
            new Rpm(this.storage, StandardNamingPolicy.SHA1, Digest.SHA256, true)
                .batchUpdate(Key.ROOT)
                .blockingAwait();
        }
        MatcherAssert.assertThat(
            "got 4 metadata files after second update",
            bsto.list(new Key.From("repodata")).size(),
            Matchers.equalTo(4)
        );
    }

    @Test
    void generatesRepomdMetadata() throws Exception {
        new Rpm(this.storage, StandardNamingPolicy.SHA1, Digest.SHA256, true)
            .batchUpdate(Key.ROOT)
            .blockingAwait();
        MatcherAssert.assertThat(
            new String(
                new Concatenation(
                    this.storage.value(new Key.From("repodata/repomd.xml")).get()
                ).single().blockingGet().array(),
                Charset.defaultCharset()
            ),
            XhtmlMatchers.hasXPaths(
                //@checkstyle LineLengthCheck (1 line)
                "/*[namespace-uri()='http://linux.duke.edu/metadata/repo' and local-name()='repomd']",
                "/*[name()='repomd']/*[name()='revision']",
                "/*[name()='repomd']/*[name()='data' and @type='primary']",
                "/*[name()='repomd']/*[name()='data' and @type='others']",
                "/*[name()='repomd']/*[name()='data' and @type='filelists']"
            )
        );
    }
}
