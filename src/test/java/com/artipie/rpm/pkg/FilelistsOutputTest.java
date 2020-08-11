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
import com.artipie.rpm.TestRpm;
import com.artipie.rpm.meta.XmlPackage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.matchers.CompareMatcher;

/**
 * Test for {@link FilelistsOutput}.
 *
 * @since 0.11
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class FilelistsOutputTest {
    @Test
    void createsFileslistsForAbc(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("filelists.xml");
        try (PackageOutput.FileOutput fileslists = new FilelistsOutput(res)) {
            fileslists.start();
            final Path rpm = new TestRpm.Abc().path();
            fileslists.accept(
                new FilePackage.Headers(new FilePackageHeader(rpm).header(), rpm, Digest.SHA256)
            );
        }
        MatcherAssert.assertThat(
            Files.readAllBytes(res),
            CompareMatcher.isIdenticalTo(
                Files.readAllBytes(new TestRpm.Abc().metadata(XmlPackage.FILELISTS))
            ).ignoreWhitespace()
                .ignoreElementContentWhitespace()
                .normalizeWhitespace()
                .withNodeFilter(
                    node -> !"file".equals(node.getLocalName())
                        && !"provides".equals(node.getLocalName())
                        && !"requires".equals(node.getLocalName())
                ).withAttributeFilter(
                    attr -> !"file".equals(attr.getName()) && !"archive".equals(attr.getName())
            )
        );
    }

    @Test
    void checkFile(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("filelists.xml");
        try (PackageOutput.FileOutput fileslists = new FilelistsOutput(res)) {
            fileslists.start();
            MatcherAssert.assertThat(
                fileslists.file(),
                new IsEqual<>(res)
            );
        }
    }

    @Test
    void checkTag(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("fileslists.xml");
        try (PackageOutput.FileOutput fileslists = new FilelistsOutput(res)) {
            fileslists.start();
            MatcherAssert.assertThat(
                fileslists.tag(),
                new IsEqual<>("filelists")
            );
        }
    }

    @Test
    void createsFilelistForLibdeflt(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("fileslists.xml");
        try (PackageOutput.FileOutput fileslists = new FilelistsOutput(res)) {
            fileslists.start();
            final Path rpm = new TestRpm.Libdeflt().path();
            fileslists.accept(
                new FilePackage.Headers(new FilePackageHeader(rpm).header(), rpm, Digest.SHA256)
            );
        }
        MatcherAssert.assertThat(
            Files.readAllBytes(res),
            CompareMatcher.isIdenticalTo(
                Files.readAllBytes(new TestRpm.Libdeflt().metadata(XmlPackage.FILELISTS))
            ).ignoreWhitespace()
                .ignoreElementContentWhitespace()
                .normalizeWhitespace()
                .withNodeFilter(
                    node -> !"file".equals(node.getLocalName())
                        && !"provides".equals(node.getLocalName())
                        && !"requires".equals(node.getLocalName())
                ).withAttributeFilter(
                    attr -> !"file".equals(attr.getName()) && !"archive".equals(attr.getName())
            )
        );
    }
}
