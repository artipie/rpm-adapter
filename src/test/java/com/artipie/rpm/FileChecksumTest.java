package com.artipie.rpm;

import java.nio.file.Files;
import java.nio.file.Path;
import org.cactoos.io.InputOf;
import org.cactoos.io.Sha256DigestOf;
import org.cactoos.text.HexOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test case for {@link FileChecksum}.
 *
 * @since 0.8
 */
final class FileChecksumTest {

    @Test
    void generatesValidChecksum(@TempDir final Path tmp) throws Exception {
        final Path target = tmp.resolve("test.bin");
        Files.write(target, new byte[]{0x00, 0x01, 0x02, 0x03});
        MatcherAssert.assertThat(
            new FileChecksum(target, Digest.SHA256).hex(),
            new IsEqual<>(new HexOf(new Sha256DigestOf(new InputOf(target))).asString())
        );
    }
}
