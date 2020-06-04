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
import com.artipie.rpm.files.Gzip;
import com.jcabi.xml.XMLDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link Rpm}.
 *
 * @since 0.9
 */
final class RpmTest {

    /**
     * Abc test rmp file.
     */
    private static final String ABC =
        "src/test/resources-binary/abc-1.01-26.git20200127.fc32.ppc64le.rpm";

    /**
     * Libdeflt test rmp file.
     */
    private static final String LIBDEFLT =
        "src/test/resources-binary/libdeflt1_0-2020.03.27-25.1.armv7hl.rpm";

    @Test
    void doesntBrakeMetadataWhenInvalidPackageSent(@TempDir final Path tmp) throws Exception {
        final Storage storage = new FileStorage(tmp);
        final Rpm repo =  new Rpm(storage, StandardNamingPolicy.SHA1, Digest.SHA256, true);
        RpmTest.addRpm(storage, "oldfile.rpm", RpmTest.ABC);
        repo.batchUpdate(Key.ROOT).blockingAwait();
        final byte[] broken = {0x00, 0x01, 0x02 };
        storage.save(new Key.From("broken.rpm"), new Content.From(broken)).get();
        RpmTest.addRpm(storage, "new.rpm", RpmTest.LIBDEFLT);
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
        RpmTest.addRpm(storage, "first.rpm", RpmTest.LIBDEFLT);
        repo.batchUpdate(Key.ROOT).blockingAwait();
        final byte[] broken = {0x00, 0x01, 0x02 };
        storage.save(new Key.From("broken-file.rpm"), new Content.From(broken)).get();
        RpmTest.addRpm(storage, "second.rpm", RpmTest.ABC);
        repo.updateBatchIncrementally(Key.ROOT).blockingAwait();
        MatcherAssert.assertThat(
            countData(tmp),
            new IsEqual<>(2)
        );
    }

    /**
     * Adds rpm into storage.
     * @param storage Where to add
     * @param key Key
     * @param rpm Rpm
     * @throws Exception On error
     */
    private static void addRpm(final Storage storage, final String key, final String rpm)
        throws Exception {
        storage.save(
            new Key.From(key),
            new Content.From(Files.readAllBytes(Paths.get(rpm)))
        ).get();
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
}
