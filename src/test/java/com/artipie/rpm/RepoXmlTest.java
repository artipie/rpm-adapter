/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Yegor Bugayenko
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

import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * RepoXmlTest.
 *
 * @since 0.3
 */
public class RepoXmlTest {
    /**
     * RPM xml works.
     */
    @Test
    public void directivesConstructedProperly() throws IOException {
        final long size = 10;
        MatcherAssert.assertThat(
            new RepoXml()
                .base("type", "key")
                .openChecksum("open")
                .checksum("sum")
                .size(size)
                .openSize(size).toString(),
            Matchers.equalTo(
                String.join(
                    "",
                    "XPATH \"/repomd\";ADDIF \"revision\";SET \"1\";XPATH \"",
                    "/repomd/data[type=&apos;type&apos;]\";\n",
                    "4:REMOVE;XPATH \"/repomd\";ADD \"data\";ATTR \"type\", ",
                    "\"type\";ADD \"location\";ATTR \"href\", \"key.gz\";\n",
                    "10:UP;ADD \"open-checksum\";ATTR \"type\", \"sha256\";SET \"open\"",
                    ";UP;ADD \"checksum\";ATTR \"type\", \"sha256\";\n",
                    "17:SET \"sum\";UP;ADD \"size\";SET \"10\";UP;ADD \"open-size\"",
                    ";SET \"10\";UP;"
                )
            )
        );
    }
}
