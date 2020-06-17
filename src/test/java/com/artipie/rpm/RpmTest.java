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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.rpm.files.Gzip;
import com.artipie.rpm.meta.XmlPackage;
import com.jcabi.xml.XMLDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;
import org.cactoos.Scalar;
import org.cactoos.list.ListOf;
import org.cactoos.list.Mapped;
import org.cactoos.scalar.AndInThreads;
import org.cactoos.scalar.Unchecked;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link Rpm}.
 *
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class RpmTest {

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
                    new TestRpm.Multiple(
                        new TestRpm.Abc(),
                        new TestRpm.Libdeflt()
                    ).put(storage);
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
    void doesntBrakeMetadataWhenInvalidPackageSent(@TempDir final Path tmp) throws Exception {
        final Storage storage = new FileStorage(tmp);
        final Rpm repo =  new Rpm(storage, StandardNamingPolicy.SHA1, Digest.SHA256, true);
        new TestRpm.Abc().put(storage);
        repo.batchUpdate(Key.ROOT).blockingAwait();
        final byte[] broken = {0x00, 0x01, 0x02 };
        storage.save(new Key.From("broken.rpm"), new Content.From(broken)).get();
        new TestRpm.Libdeflt().put(storage);
        repo.batchUpdate(Key.ROOT).blockingAwait();
        MatcherAssert.assertThat(
            countData(tmp),
            new IsEqual<>(2)
        );
    }

    @Test
    void doesntBrakeMetadataWhenInvalidPackageSentOnIncrementalUpdate(@TempDir final Path tmp)
        throws Exception {
        final Storage storage = new FileStorage(tmp);
        final Rpm repo =  new Rpm(storage, StandardNamingPolicy.SHA1, Digest.SHA256, true);
        new TestRpm.Libdeflt().put(storage);
        repo.batchUpdate(Key.ROOT).blockingAwait();
        final byte[] broken = {0x00, 0x01, 0x02 };
        storage.save(new Key.From("broken-file.rpm"), new Content.From(broken)).get();
        new TestRpm.Abc().put(storage);
        repo.batchUpdateIncrementally(Key.ROOT).blockingAwait();
        MatcherAssert.assertThat(
            countData(tmp),
            new IsEqual<>(2)
        );
    }

    private static int countData(final Path path) throws IOException {
        final Path primary = path.resolve("primary.xml");
        new Gzip(path.resolve(meta(path))).unpack(primary);
        return Integer.parseInt(
            new XMLDocument(new String(Files.readAllBytes(primary), StandardCharsets.UTF_8))
                .xpath("count(/*[local-name()='metadata']/*[local-name()='package'])")
                .get(0)
        );
    }

    /**
     * Searches for the meta file by substring in folder.
     * @param dir Where to look for the file
     * @return Path to find
     * @throws IOException On error
     */
    private static Path meta(final Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            final Optional<Path> res = walk
                .filter(
                    path -> path.getFileName().toString().endsWith("primary.xml.gz")
                ).findFirst();
            if (res.isPresent()) {
                return res.get();
            } else {
                throw new IllegalStateException(
                    String.format("Metafile %s does not exists in %s", "primary", dir.toString())
                );
            }
        }
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
}
