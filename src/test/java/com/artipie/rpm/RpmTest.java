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
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.rpm.meta.XmlPackage;
import com.jcabi.xml.XMLDocument;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.cactoos.Scalar;
import org.cactoos.list.ListOf;
import org.cactoos.list.Mapped;
import org.cactoos.scalar.AndInThreads;
import org.cactoos.scalar.Unchecked;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Rpm}.
 *
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @todo #110:30min Meaningful error on broken package.
 *  Rpm should throw an exception when trying to add an invalid package.
 *  The type of exception must be IllegalArgumentException and its message
 *  "Reading of RPM package 'package' failed, data corrupt or malformed.",
 *  like described in showMeaningfulErrorWhenInvalidPackageSent. Implement it
 *  and then enable the test.
 */
final class RpmTest {

    /**
     * Path of repomd.xml file.
     */
    private static final String REPOMD = "repodata/repomd.xml";

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

    @Test
    void updatesDifferentReposSimultaneouslyTwice() throws Exception {
        final Storage storage = new InMemoryStorage();
        final Rpm repo =  new Rpm(
            storage, StandardNamingPolicy.SHA1, Digest.SHA256, true
        );
        final List<String> keys = new ListOf<>("one", "two", "three");
        final CountDownLatch latch = new CountDownLatch(keys.size());
        final List<Scalar<Boolean>> tasks = new Mapped<>(
            key -> new Unchecked<>(
                () -> {
                    RpmTest.putFilesInStorage(storage, new Key.From(key));
                    latch.countDown();
                    latch.await();
                    repo.batchUpdate(new Key.From(key)).blockingAwait();
                    return true;
                }
            ),
            keys
        );
        new AndInThreads(tasks).value();
        new AndInThreads(tasks).value();
        keys.forEach(
            key -> {
                final Key res = new Key.From(key, "repodata");
                RpmTest.metadataArePresent(storage, res);
                RpmTest.repomdIsPresent(storage, res);
            }
        );
    }

    @Test
    void doesntBrakeMetadataWhenInvalidPackageSent()
        throws Exception {
        final Storage storage = new InMemoryStorage();
        final Rpm repo =  new Rpm(
            storage, StandardNamingPolicy.SHA1, Digest.SHA256, true
        );
        storage.save(
            new Key.From("oldfile.rpm"),
            new Content.From(
                Files.readAllBytes(RpmTest.ABC)
            )
        );
        repo.batchUpdate(Key.ROOT).blockingAwait();
        final String repomd = new String(
            new Concatenation(
                storage.value(new Key.From(RpmTest.REPOMD)).get()
            ).single().blockingGet().array(),
            Charset.defaultCharset()
        );
        final byte[] broken = {0x00, 0x01, 0x02 };
        storage.save(
            new Key.From("broken.rpm"),
            new Content.From(
                broken
            )
        );
        repo.batchUpdate(Key.ROOT).blockingAwait();
        RpmTest.metadataArePresent(storage, Key.ROOT);
        MatcherAssert.assertThat(
            countData(
                new String(
                    new Concatenation(
                        storage.value(new Key.From(RpmTest.REPOMD)).get()
                    ).single().blockingGet().array(),
                    Charset.defaultCharset()
                )
            ),
            new IsEqual<>(countData(repomd))
        );
    }

    @Test
    @Disabled
    void showMeaningfulErrorWhenInvalidPackageSent() throws Exception {
        final Storage storage = new InMemoryStorage();
        final Rpm repo =  new Rpm(
            storage, StandardNamingPolicy.SHA1, Digest.SHA256, true
        );
        putFilesInStorage(
            storage,
            new Key.From("stored.rpm")
        );
        repo.batchUpdate(Key.ROOT).blockingAwait();
        final byte[] broken = {0x00, 0x01, 0x02 };
        storage.save(
            new Key.From("brokentwo.rpm"),
            new Content.From(
                broken
            )
        ).join();
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> repo.batchUpdate(Key.ROOT).blockingAwait(),
            "Reading of RPM package \"brokentwo.rpm\" failed, data corrupt or malformed."
        );
    }

    private static int countData(final String xml) {
        return Integer.parseInt(
            new XMLDocument(xml)
                .xpath("count(/*[local-name()='repomd']/*[local-name()='data'])")
                .get(0)
        );
    }

    /**
     * Checks that metadata are present.
     * @param storage Storage
     * @param repo Repodata key
     */
    private static void metadataArePresent(final Storage storage, final Key repo) {
        new XmlPackage.Stream(true).get().forEach(
            item ->
                MatcherAssert.assertThat(
                    String.format("Metadata %s is present", item.filename()),
                    storage.list(repo).join().stream()
                        .map(Key::string).filter(str -> str.contains(item.filename())).count(),
                    new IsEqual<>(1L)
                )
        );
    }

    /**
     * Checks that repomd is present.
     * @param storage Storage
     * @param key Key
     */
    private static void repomdIsPresent(final Storage storage, final Key key) {
        MatcherAssert.assertThat(
            "Repomd is present",
            storage.list(key).join().stream()
                .map(Key::string).filter(str -> str.contains("repomd")).count(),
            new IsEqual<>(1L)
        );
    }

    /**
     * Puts files into storage.
     * @param storage Where to put
     * @param key Repo key
     * @throws IOException On error
     */
    private static void putFilesInStorage(final Storage storage, final Key key) throws IOException {
        storage.save(
            new Key.From(key, RpmTest.ABC.getFileName().toString()),
            new Content.From(Files.readAllBytes(RpmTest.ABC))
        ).join();
        storage.save(
            new Key.From(key, RpmTest.LIBDEFLT.getFileName().toString()),
            new Content.From(Files.readAllBytes(RpmTest.LIBDEFLT))
        ).join();
    }
}
