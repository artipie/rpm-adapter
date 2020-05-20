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

import com.jcabi.matchers.XhtmlMatchers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link XmlPackagesFile}.
 *
 * @since 0.6.3
 */
public final class XmlPackagesFileTest {

    @Test
    public void writesCorrectTag(@TempDir final Path temp) throws Exception {
        final Path file = temp.resolve("tag.xml");
        try (XmlPackagesFile packs = new XmlPackagesFile(new XmlFile(file), XmlPackage.OTHERS)) {
            packs.startPackages();
        }
        XmlPackagesFileTest.assertion(
            file, String.format("/*[name()='%s']", XmlPackage.OTHERS.tag())
        );
    }

    @Test
    public void writesCorrectNamespace(@TempDir final Path temp) throws Exception {
        final Path file = temp.resolve("name.xml");
        try (XmlPackagesFile packs = new XmlPackagesFile(new XmlFile(file), XmlPackage.FILELISTS)) {
            packs.startPackages();
        }
        XmlPackagesFileTest.assertion(
            file,
            String.format("/*[namespace-uri(.)='%s']", XmlPackage.FILELISTS.xmlNamespaces().get(""))
        );
    }

    private static void assertion(final Path file, final String expected) throws IOException {
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(file), StandardCharsets.UTF_8),
            XhtmlMatchers.hasXPath(expected)
        );
    }
}
