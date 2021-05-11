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
