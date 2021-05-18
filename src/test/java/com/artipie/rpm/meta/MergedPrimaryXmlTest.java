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
package com.artipie.rpm.meta;

import com.artipie.asto.test.TestResource;
import com.artipie.rpm.Digest;
import com.artipie.rpm.TestRpm;
import com.jcabi.matchers.XhtmlMatchers;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link MergedPrimaryXml}.
 * @since 1.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class MergedPrimaryXmlTest {

    @Test
    void addsRecords() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TestRpm.Libdeflt libdeflt = new TestRpm.Libdeflt();
        try (InputStream input = new TestResource("repodata/primary.xml.example").asInputStream()) {
            final MergedPrimaryXml.Result res =
                new MergedPrimaryXml(
                    input,
                    out, Digest.SHA256
                ).merge(
                    new MapOf<Path, String>(
                        new MapEntry<>(libdeflt.path(), libdeflt.path().getFileName().toString())
                    )
                );
            MatcherAssert.assertThat(
                "Packages count is incorrect",
                res.count(),
                new IsEqual<>(3L)
            );
            MatcherAssert.assertThat(
                "Duplicated packages checksum should be empty",
                res.checksums(),
                new IsEmptyCollection<>()
            );
            MatcherAssert.assertThat(
                "Primary does not have expected packages",
                out.toString(StandardCharsets.UTF_8.name()),
                XhtmlMatchers.hasXPaths(
                    // @checkstyle LineLengthCheck (3 lines)
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='aom']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='nginx']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='libdeflt1_0']"
                )
            );
        }
    }

    @Test
    void addsReplacesRecords() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TestRpm.Libdeflt libdeflt = new TestRpm.Libdeflt();
        final TestRpm.Time time = new TestRpm.Time();
        try (InputStream input =
            new TestResource("repodata/XmlPrimaryCompTest/libdeflt-primary.xml.example")
                .asInputStream()
        ) {
            final MergedPrimaryXml.Result res =
                new MergedPrimaryXml(input, out, Digest.SHA256).merge(
                    new MapOf<Path, String>(
                        new MapEntry<>(time.path(), time.path().getFileName().toString()),
                        new MapEntry<>(libdeflt.path(), libdeflt.path().getFileName().toString())
                    )
                );
            MatcherAssert.assertThat(
                "Packages count is incorrect",
                res.count(),
                new IsEqual<>(2L)
            );
            MatcherAssert.assertThat(
                "Duplicated packages checksum should contain one checksum",
                res.checksums(),
                Matchers.contains("abc123")
            );
            final String actual = out.toString(StandardCharsets.UTF_8.name());
            MatcherAssert.assertThat(
                "Primary does not have expected packages",
                actual,
                XhtmlMatchers.hasXPaths(
                    // @checkstyle LineLengthCheck (3 lines)
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='time']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='libdeflt1_0']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='checksum' and text()='47bbb8b2401e8853812e6340f4197252b92463c132f64a257e18c0c8c83ae462']"
                )
            );
        }
    }

    @Test
    void appendsSeveralPackages() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TestRpm libdeflt = new TestRpm.Libdeflt();
        final TestRpm time = new TestRpm.Time();
        final TestRpm abc = new TestRpm.Abc();
        try (InputStream input =
            new TestResource("repodata/XmlPrimaryCompTest/libdeflt-nginx-promary.xml.example")
                .asInputStream()
        ) {
            final MergedPrimaryXml.Result res =
                new MergedPrimaryXml(input, out, Digest.SHA256).merge(
                    new MapOf<Path, String>(
                        new MapEntry<>(time.path(), time.path().getFileName().toString()),
                        new MapEntry<>(abc.path(), abc.path().getFileName().toString()),
                        new MapEntry<>(libdeflt.path(), libdeflt.path().getFileName().toString())
                    )
                );
            MatcherAssert.assertThat(
                "Packages count is incorrect",
                res.count(),
                new IsEqual<>(4L)
            );
            MatcherAssert.assertThat(
                "Duplicated packages checksum should contain one checksum",
                res.checksums(),
                Matchers.contains("abc123")
            );
            final String actual = out.toString(StandardCharsets.UTF_8.name());
            MatcherAssert.assertThat(
                "Primary does not have expected packages",
                actual,
                XhtmlMatchers.hasXPaths(
                    // @checkstyle LineLengthCheck (5 lines)
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='time']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='abc']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='nginx']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='libdeflt1_0']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='checksum' and text()='47bbb8b2401e8853812e6340f4197252b92463c132f64a257e18c0c8c83ae462']"
                )
            );
        }
    }
}
