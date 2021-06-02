/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.asto.test.TestResource;
import com.artipie.rpm.meta.XmlPackage;
import com.jcabi.matchers.XhtmlMatchers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RpmMetadata.Append}.
 * @since 1.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class RpmMetadataAppendTest {

    @Test
    void appendsRecords() throws IOException {
        final ByteArrayOutputStream primary = new ByteArrayOutputStream();
        final ByteArrayOutputStream other = new ByteArrayOutputStream();
        new RpmMetadata.Append(
            Digest.SHA256,
            new RpmMetadata.MetadataItem(
                XmlPackage.PRIMARY,
                new ByteArrayInputStream(
                    new TestResource("repodata/primary.xml.example").asBytes()
                ),
                primary
            ),
            new RpmMetadata.MetadataItem(
                XmlPackage.OTHER,
                new ByteArrayInputStream(
                    new TestResource("repodata/other.xml.example").asBytes()
                ),
                other
            )
        ).perform(
            new MapOf<Path, String>(
                new MapEntry<>(
                    new TestRpm.Libdeflt().path(),
                    new TestRpm.Libdeflt().path().getFileName().toString()
                ),
                new MapEntry<>(
                    new TestRpm.Abc().path(),
                    new TestRpm.Abc().path().getFileName().toString()
                )
            )
        );
        MatcherAssert.assertThat(
            "Records were not added to primary xml",
            primary.toString(),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='metadata' and @packages='4']",
                //@checkstyle LineLengthCheck (4 lines)
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='nginx']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='aom']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='abc']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='libdeflt1_0']"
            )
        );
        MatcherAssert.assertThat(
            "Records were not added to others xml",
            other.toString(),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='otherdata' and @packages='4']",
                "/*[local-name()='otherdata']/*[local-name()='package' and @name='aom']",
                "/*[local-name()='otherdata']/*[local-name()='package' and @name='nginx']",
                "/*[local-name()='otherdata']/*[local-name()='package' and @name='abc']",
                "/*[local-name()='otherdata']/*[local-name()='package' and @name='libdeflt1_0']"
            )
        );
    }

    @Test
    void createsIndexesWhenInputsAreAbsent() {
        final ByteArrayOutputStream primary = new ByteArrayOutputStream();
        final ByteArrayOutputStream other = new ByteArrayOutputStream();
        new RpmMetadata.Append(
            Digest.SHA256,
            new RpmMetadata.MetadataItem(
                XmlPackage.PRIMARY,
                primary
            ),
            new RpmMetadata.MetadataItem(
                XmlPackage.OTHER,
                other
            )
        ).perform(
            new MapOf<Path, String>(
                new MapEntry<>(
                    new TestRpm.Time().path(),
                    new TestRpm.Time().path().getFileName().toString()
                ),
                new MapEntry<>(
                    new TestRpm.Abc().path(),
                    new TestRpm.Abc().path().getFileName().toString()
                )
            )
        );
        MatcherAssert.assertThat(
            "Records were not added to primary xml",
            primary.toString(),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='metadata' and @packages='2']",
                //@checkstyle LineLengthCheck (2 lines)
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='time']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='abc']"
            )
        );
        MatcherAssert.assertThat(
            "Records were not added to others xml",
            other.toString(),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='otherdata' and @packages='2']",
                "/*[local-name()='otherdata']/*[local-name()='package' and @name='time']",
                "/*[local-name()='otherdata']/*[local-name()='package' and @name='abc']"
            )
        );
    }
}
