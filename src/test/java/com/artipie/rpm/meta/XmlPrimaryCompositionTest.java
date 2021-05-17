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
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link XmlPrimaryComposition}.
 * @since 1.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
class XmlPrimaryCompositionTest {

    @Test
    void addsRecords() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TestRpm.Libdeflt libdeflt = new TestRpm.Libdeflt();
        try (InputStream input = new TestResource("repodata/primary.xml.example").asInputStream()) {
            final XmlPrimaryComposition.Result res =
                new XmlPrimaryComposition(
                    input,
                    out, Digest.SHA256
                ).append(
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
}
