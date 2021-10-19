/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.asto.ArtipieIOException;
import com.artipie.rpm.Digest;
import com.artipie.rpm.TestRpm;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link FilePackage}.
 * @since 0.11
 */
class FilePackageTest {

    @Test
    void returnsPath() {
        final Path path = Paths.get("some/path/package.rpm");
        MatcherAssert.assertThat(
            new FilePackage(path, path.getFileName().toString()).path(),
            new IsEqual<>(path)
        );
    }

    @Test
    void returnsParsedFile() throws IOException {
        final Path path = new TestRpm.Abc().path();
        MatcherAssert.assertThat(
            new FilePackage(path, path.getFileName().toString()).parsed(),
            new IsInstanceOf(ParsedFilePackage.class)
        );
    }

    @Test
    void throwsExceptionIfFileDoesNotExists() {
        Assertions.assertThrows(
            ArtipieIOException.class,
            () -> new FilePackage(Paths.get("some/file"), "file").parsed()
        );
    }

    @Test
    void throwsExceptionIfFileIsInvalid(@TempDir final Path temp) throws IOException {
        final Path rpm = temp.resolve("bad.rpm");
        Files.write(rpm, new TestRpm.Invalid().bytes());
        Assertions.assertThrows(
            InvalidPackageException.class,
            () -> new FilePackage(rpm, rpm.getFileName().toString()).parsed()
        );
    }

    @Test
    void canPresentItselfAsString() {
        final String name = "package.rpm";
        MatcherAssert.assertThat(
            new FilePackage(Paths.get("urs/some_dir/", name), name).toString(),
            new IsEqual<>(String.format("FilePackage[%s]", name))
        );
    }

    @Test
    void onSaveCallsAcceptAndDeletesFile(@TempDir final Path temp) throws IOException {
        final Path rpm = temp.resolve("test.rpm");
        Files.copy(new TestRpm.Libdeflt().path(), rpm);
        final PackageOutput.FileOutput.Fake out =
            new PackageOutput.FileOutput.Fake(temp.resolve("any"));
        new FilePackage(rpm, rpm.getFileName().toString()).save(out, Digest.SHA256);
        MatcherAssert.assertThat(
            "PackageOutput was accepted",
            out.isAccepted(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Rpm package was removed",
            rpm.toFile().exists(),
            new IsEqual<>(false)
        );
    }

}
