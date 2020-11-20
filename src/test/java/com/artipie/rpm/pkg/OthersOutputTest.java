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
 * Test for {@link OthersOutput}.
 *
 * @since 0.11
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class OthersOutputTest {
    @Test
    void createsOthersForAbc(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("others.xml");
        final TestRpm.Abc abc = new TestRpm.Abc();
        try (PackageOutput.FileOutput others = new OthersOutput(res)) {
            others.start();
            final Path rpm = abc.path();
            others.accept(
                new FilePackage.Headers(
                    new FilePackageHeader(rpm).header(), rpm,
                    Digest.SHA256, rpm.getFileName().toString()
                )
            );
        }
        MatcherAssert.assertThat(
            Files.readAllBytes(res),
            CompareMatcher.isSimilarTo(Files.readAllBytes(abc.metadata(XmlPackage.OTHER)))
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeFilter(
                    node -> !"changelog".equals(node.getLocalName())
                )
        );
    }

    @Test
    void checkFile(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("others.xml");
        try (PackageOutput.FileOutput others = new OthersOutput(res)) {
            others.start();
            MatcherAssert.assertThat(
                others.file(),
                new IsEqual<>(res)
            );
        }
    }

    @Test
    void checkTag(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("others.xml");
        try (PackageOutput.FileOutput others = new OthersOutput(res)) {
            others.start();
            MatcherAssert.assertThat(
                others.tag(),
                new IsEqual<>("otherdata")
            );
        }
    }

    @Test
    void createsOthersForLibdeflt(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("others.xml");
        final TestRpm.Libdeflt libdeflt = new TestRpm.Libdeflt();
        try (PackageOutput.FileOutput others = new OthersOutput(res)) {
            others.start();
            final Path rpm = libdeflt.path();
            others.accept(
                new FilePackage.Headers(
                    new FilePackageHeader(rpm).header(), rpm,
                    Digest.SHA256, rpm.getFileName().toString()
                )
            );
        }
        MatcherAssert.assertThat(
            Files.readAllBytes(res),
            CompareMatcher.isSimilarTo(Files.readAllBytes(libdeflt.metadata(XmlPackage.OTHER)))
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeFilter(
                    node -> !"changelog".equals(node.getLocalName())
                )
        );
    }
}
