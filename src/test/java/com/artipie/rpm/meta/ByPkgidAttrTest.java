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
import com.artipie.rpm.hm.IsXmlEqual;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link XmlMaid.ByPkgidAttr}.
 * @since 0.3
 */
public final class ByPkgidAttrTest {

    @Test
    void clearsFirstItem(@TempDir final Path temp) throws IOException {
        final Path file = Files.copy(
            new TestResource("repodata/other.xml.example").asPath(),
            temp.resolve("other.xml")
        );
        new XmlMaid.ByPkgidAttr(file).clean(
            new ListOf<>("7eaefd1cb4f9740558da7f12f9cb5a6141a47f5d064a98d46c29959869af1a44")
        );
        MatcherAssert.assertThat(
            file,
            new IsXmlEqual(new TestResource("repodata/other.xml.example.second").asPath())
        );
    }

    @Test
    void clearsLastItem(@TempDir final Path temp) throws IOException {
        final Path file = Files.copy(
            new TestResource("repodata/filelists.xml.example").asPath(),
            temp.resolve("filelist.xml")
        );
        new XmlMaid.ByPkgidAttr(file).clean(
            new ListOf<>("54f1d9a1114fa85cd748174c57986004857b800fe9545fbf23af53f4791b31e2")
        );
        MatcherAssert.assertThat(
            file,
            new IsXmlEqual(new TestResource("repodata/filelists.xml.example.first").asPath())
        );
    }
}
