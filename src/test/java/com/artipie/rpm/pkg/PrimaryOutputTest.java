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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.matchers.CompareMatcher;

/**
 * Test for {@link PrimaryOutput}.
 * @since 0.10
 * @todo #277:30min Extend this test: add test methods to check `maid()` method of
 *  PrimaryOutput`
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class PrimaryOutputTest {

    @Test
    void createsPrimaryForAbc(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("primary.xml");
        try (PackageOutput.FileOutput primary = new PrimaryOutput(res)) {
            primary.start();
            final Path rpm =
                Paths.get("src/test/resources-binary/abc-1.01-26.git20200127.fc32.ppc64le.rpm");
            primary.accept(
                new FilePackage.Headers(new FilePackageHeader(rpm).header(), rpm, Digest.SHA256)
            );
        }
        MatcherAssert.assertThat(
            Files.readAllBytes(res),
            CompareMatcher.isIdenticalTo(
                Files.readAllBytes(
                    Paths.get("src/test/resources-binary/repodata/abc-primary.xml")
                )
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
        final Path res = temp.resolve("primary.xml");
        try (PackageOutput.FileOutput primary = new PrimaryOutput(res)) {
            primary.start();
            MatcherAssert.assertThat(
                primary.file(),
                new IsEqual<>(res)
            );
        }
    }

    @Test
    void checkTag(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("primary.xml");
        try (PackageOutput.FileOutput primary = new PrimaryOutput(res)) {
            primary.start();
            MatcherAssert.assertThat(
                primary.tag(),
                new IsEqual<>("metadata")
            );
        }
    }

    @Test
    void createsPrimaryForLibdeflt(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("primary.xml");
        try (PackageOutput.FileOutput primary = new PrimaryOutput(res)) {
            primary.start();
            final Path rpm =
                Paths.get("src/test/resources-binary/libdeflt1_0-2020.03.27-25.1.armv7hl.rpm");
            primary.accept(
                new FilePackage.Headers(new FilePackageHeader(rpm).header(), rpm, Digest.SHA256)
            );
        }
        MatcherAssert.assertThat(
            Files.readAllBytes(res),
            CompareMatcher.isIdenticalTo(
                Files.readAllBytes(
                    Paths.get("src/test/resources-binary/repodata/libdeflt-primary.xml")
                )
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
