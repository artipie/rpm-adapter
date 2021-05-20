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
}
