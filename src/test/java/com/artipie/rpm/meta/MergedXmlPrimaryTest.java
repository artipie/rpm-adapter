/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
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
 * Test for {@link MergedXmlPrimary}.
 * @since 1.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class MergedXmlPrimaryTest {

    @Test
    void addsRecords() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TestRpm.Libdeflt libdeflt = new TestRpm.Libdeflt();
        try (InputStream input = new TestResource("repodata/primary.xml.example").asInputStream()) {
            final MergedXmlPrimary.Result res =
                new MergedXmlPrimary(
                    input, out
                ).merge(
                    new MapOf<Path, String>(
                        new MapEntry<>(libdeflt.path(), libdeflt.path().getFileName().toString())
                    ),
                    Digest.SHA256, new XmlEvent.Primary()
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
            new TestResource("repodata/MergedXmlTest/libdeflt-primary.xml.example")
                .asInputStream()
        ) {
            final MergedXmlPrimary.Result res =
                new MergedXmlPrimary(input, out).merge(
                    new MapOf<Path, String>(
                        new MapEntry<>(time.path(), time.path().getFileName().toString()),
                        new MapEntry<>(libdeflt.path(), libdeflt.path().getFileName().toString())
                    ),
                    Digest.SHA256, new XmlEvent.Primary()
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
            new TestResource("repodata/MergedXmlTest/libdeflt-nginx-promary.xml.example")
                .asInputStream()
        ) {
            final MergedXmlPrimary.Result res =
                new MergedXmlPrimary(input, out).merge(
                    new MapOf<Path, String>(
                        new MapEntry<>(time.path(), time.path().getFileName().toString()),
                        new MapEntry<>(abc.path(), abc.path().getFileName().toString()),
                        new MapEntry<>(libdeflt.path(), libdeflt.path().getFileName().toString())
                    ),
                    Digest.SHA256, new XmlEvent.Primary()
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
