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
package com.artipie.rpm.pkg;

import com.artipie.rpm.Digest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
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
            new FilePackage(path).path(),
            new IsEqual<>(path)
        );
    }

    @Test
    void returnsParsedFile() throws IOException {
        MatcherAssert.assertThat(
            new FilePackage(
                Paths.get("src/test/resources-binary/abc-1.01-26.git20200127.fc32.ppc64le.rpm")
            ).parsed(),
            new IsInstanceOf(ParsedFilePackage.class)
        );
    }

    @Test
    void canPresentItselfAsString() {
        final String name = "package.rpm";
        MatcherAssert.assertThat(
            new FilePackage(Paths.get("urs/some_dir/", name)).toString(),
            new IsEqual<>(String.format("FilePackage[%s]", name))
        );
    }

    @Test
    void onSaveCallsAcceptAndDeletesFile(@TempDir final Path temp) throws IOException {
        final Path rpm = temp.resolve("test.rpm");
        Files.copy(
            Paths.get("src/test/resources-binary/libdeflt1_0-2020.03.27-25.1.armv7hl.rpm"),
            rpm
        );
        final PackageOutput.FileOutput.Fake out =
            new PackageOutput.FileOutput.Fake(temp.resolve("any"));
        new FilePackage(rpm).save(out, Digest.SHA256);
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
