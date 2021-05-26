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
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RpmMetadata.Remove}.
 * @since 1.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class RpmMetadataRemoveTest {

    @Test
    void removesRecord() throws IOException {
        final ByteArrayOutputStream primary = new ByteArrayOutputStream();
        final ByteArrayOutputStream filelist = new ByteArrayOutputStream();
        final String checksum = "7eaefd1cb4f9740558da7f12f9cb5a6141a47f5d064a98d46c29959869af1a44";
        new RpmMetadata.Remove(
            new RpmMetadata.MetadataItem(
                XmlPackage.PRIMARY,
                new ByteArrayInputStream(
                    new TestResource("repodata/primary.xml.example").asBytes()
                ),
                primary
            ),
            new RpmMetadata.MetadataItem(
                XmlPackage.FILELISTS,
                new ByteArrayInputStream(
                    new TestResource("repodata/filelists.xml.example").asBytes()
                ),
                filelist
            )
        ).perform(new ListOf<>(checksum));
        MatcherAssert.assertThat(
            "Record was not removed from primary xml",
            primary.toString(),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    XhtmlMatchers.hasXPaths(
                        "/*[local-name()='metadata' and @packages='1']",
                        //@checkstyle LineLengthCheck (1 line)
                        "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='nginx']"
                    ),
                    new IsNot<>(new StringContains(checksum))
                )
            )
        );
        MatcherAssert.assertThat(
            "Record was not removed from filelist xml",
            filelist.toString(),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    XhtmlMatchers.hasXPaths(
                        "/*[local-name()='filelists' and @packages='1']",
                        "/*[local-name()='filelists']/*[local-name()='package' and @name='nginx']"
                    ),
                    new IsNot<>(new StringContains(checksum))
                )
            )
        );
    }
}
