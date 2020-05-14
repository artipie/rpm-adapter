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
import com.artipie.asto.fs.FileStorage;
import com.artipie.rpm.files.Gzip;
import com.artipie.rpm.files.TestBundle;
import com.jcabi.log.Logger;
import com.jcabi.matchers.XhtmlMatchers;
import io.vertx.reactivex.core.Vertx;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
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
 */
@EnabledIfSystemProperty(named = "it.longtests.enabled", matches = "true")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class RpmITCase {

    /**
     * Abc test rmp file.
     */
    private static final Path ABC =
        Paths.get("src/test/resources-binary/abc-1.01-26.git20200127.fc32.ppc64le.rpm");

    /**
     * Libdeflt test rmp file.
     */
    private static final Path LIBDEFLT =
        Paths.get("src/test/resources-binary/libdeflt1_0-2020.03.27-25.1.armv7hl.rpm");

    /**
     * VertX closeable instance.
     */
    private Vertx vertx;

    @BeforeEach
    void setUp() {
        this.vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() {
        this.vertx.close();
    }

    @Test
    void generatesMetadata(@TempDir final Path tmp) throws Exception {
        final Path bundle = new TestBundle(TestBundle.Size.THOUSAND).unpack(tmp);
        final Path repo = Files.createDirectory(tmp.resolve("repo"));
        new Gzip(bundle).unpackTar(repo);
        Files.delete(bundle);
        final Storage storage = new FileStorage(repo, this.vertx.fileSystem());
        final long start = System.currentTimeMillis();
        new Rpm(storage, StandardNamingPolicy.SHA1, Digest.SHA256, true)
            .batchUpdate(Key.ROOT)
            .blockingAwait();
        if (Logger.isInfoEnabled(this)) {
            Logger.info(
                this,
                "Batch update completed in %[ms]s", System.currentTimeMillis() - start
            );
        }
    }

    @Test
    void generatesMetadataWhenUpdatingIncrementally(@TempDir final Path tmp) throws Exception {
        final Path bundle = new TestBundle(TestBundle.Size.THOUSAND).unpack(tmp);
        final Path repo = Files.createDirectory(tmp.resolve("repo"));
        new Gzip(bundle).unpackTar(repo);
        Files.walk(repo).filter(file -> file.toString().contains("oxygen"))
            .forEach(file -> FileUtils.deleteQuietly(file.toFile()));
        Files.copy(RpmITCase.ABC, repo.resolve(RpmITCase.ABC.getFileName()));
        Files.copy(RpmITCase.LIBDEFLT, repo.resolve(RpmITCase.LIBDEFLT.getFileName()));
        Files.delete(bundle);
        final Storage storage = new FileStorage(repo, this.vertx.fileSystem());
        final long start = System.currentTimeMillis();
        new Rpm(storage, StandardNamingPolicy.SHA1, Digest.SHA256, true)
            .updateBatchIncrementally(Key.ROOT)
            .blockingAwait();
        if (Logger.isInfoEnabled(this)) {
            Logger.info(
                this,
                "Incremental batch update completed in %[ms]s", System.currentTimeMillis() - start
            );
        }
    }

    @Test
    void generatesRepomdMetadata(@TempDir final Path tmp) throws Exception {
        final Path bundle = new TestBundle(TestBundle.Size.HUNDRED).unpack(tmp);
        final Path repo = Files.createDirectory(
            tmp.resolve("generatesRepomdMetadata")
        );
        new Gzip(bundle).unpackTar(repo);
        Files.delete(bundle);
        final Storage storage = new FileStorage(repo, this.vertx.fileSystem());
        new Rpm(storage, StandardNamingPolicy.SHA1, Digest.SHA256, true)
            .batchUpdate(Key.ROOT)
            .blockingAwait();
        RpmITCase.assertion(storage);
    }

    @Test
    void generatesRepomdMetadataWhenUpdatingIncrementally(@TempDir final Path tmp)
        throws Exception {
        final Path bundle = new TestBundle(TestBundle.Size.HUNDRED).unpack(tmp);
        final Path repo = Files.createDirectory(tmp.resolve("generatesRepomdMetadata"));
        new Gzip(bundle).unpackTar(repo);
        Files.walk(repo).filter(file -> file.toString().contains("oxygen"))
            .forEach(file -> FileUtils.deleteQuietly(file.toFile()));
        Files.copy(RpmITCase.ABC, repo.resolve(RpmITCase.ABC.getFileName()));
        Files.copy(RpmITCase.LIBDEFLT, repo.resolve(RpmITCase.LIBDEFLT.getFileName()));
        Files.delete(bundle);
        final Storage storage = new FileStorage(repo, this.vertx.fileSystem());
        new Rpm(storage, StandardNamingPolicy.SHA1, Digest.SHA256, true)
            .updateBatchIncrementally(Key.ROOT)
            .blockingAwait();
        RpmITCase.assertion(storage);
    }

    /**
     * Assertion.
     * @param storage Storage
     * @throws InterruptedException on Error
     * @throws java.util.concurrent.ExecutionException on Error
     */
    private static void assertion(final Storage storage)
        throws InterruptedException, java.util.concurrent.ExecutionException {
        MatcherAssert.assertThat(
            new String(
                new Concatenation(
                    storage.value(new Key.From("repodata/repomd.xml")).get()
                )
                    .single().blockingGet().array(),
                Charset.defaultCharset()
            ),
            XhtmlMatchers.hasXPaths(
                //@checkstyle LineLengthCheck (1 line)
                "/*[namespace-uri()='http://linux.duke.edu/metadata/repo' and local-name()='repomd']",
                "/*[name()='repomd']/*[name()='revision']",
                "/*[name()='repomd']/*[name()='data' and @type='primary']",
                "/*[name()='repomd']/*[name()='data' and @type='other']",
                "/*[name()='repomd']/*[name()='data' and @type='filelists']"
            )
        );
    }
}
