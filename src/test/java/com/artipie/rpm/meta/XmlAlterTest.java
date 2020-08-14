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
import com.jcabi.matchers.XhtmlMatchers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test {@link XmlAlter}.
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class XmlAlterTest {

    @Test
    public void writesCorrectPackageCount(@TempDir final Path temp) throws Exception {
        final Path file = temp.resolve("primary.xml");
        Files.copy(new TestResource("repodata/primary.xml.example").asPath(), file);
        final int expected = 10;
        new XmlAlter(file).pkgAttr("metadata", String.valueOf(expected));
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(file), StandardCharsets.UTF_8),
            XhtmlMatchers.hasXPath(String.format("/*[@packages='%s']", expected))
        );
    }

    @Test
    public void writesPackageCountToNotProperlyFormattedXml(@TempDir final Path temp)
        throws Exception {
        final Path file = temp.resolve("test.xml");
        Files.write(
            file,
            String.join(
                "\n",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<tag packages=\"2\" name=\"abc\"><a>2</a></tag>"
            ).getBytes()
        );
        final int expected = 10;
        new XmlAlter(file).pkgAttr("tag", String.valueOf(expected));
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(file), StandardCharsets.UTF_8),
            XhtmlMatchers.hasXPath(String.format("/*[@packages='%s']", expected))
        );
    }

    @Test
    public void doesNothingIfTagNotFound(@TempDir final Path temp) throws Exception {
        final Path file = temp.resolve("one.xml");
        final byte[] xml = String.join(
            "\n",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<one packages=\"2\" name=\"abc\"><a>2</a></one>"
        ).getBytes();
        Files.write(file, xml);
        new XmlAlter(file).pkgAttr("two", "10");
        MatcherAssert.assertThat(
            file,
            new IsXmlEqual(xml)
        );
    }

    @Test
    public void doesNothingIfAttrNotFound(@TempDir final Path temp) throws Exception {
        final Path file = temp.resolve("one.xml");
        final byte[] xml = String.join(
            "\n",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<tag name=\"abc\"><a>2</a></tag>"
        ).getBytes();
        Files.write(file, xml);
        new XmlAlter(file).pkgAttr("tag", "23");
        MatcherAssert.assertThat(
            file,
            new IsXmlEqual(xml)
        );
    }

}
