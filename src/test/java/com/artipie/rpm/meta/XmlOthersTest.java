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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testcase for {@link XmlOthers} class.
 * @since 0.6
 */
public class XmlOthersTest {

    /**
     * Test package added successfully.
     * @param tmp Temp path
     * @throws Exception
     * @checkstyle
     */
    @Test
    void canAddPackage(@TempDir final Path tmp) throws Exception {
        final Path xml = tmp.resolve("others.xml");
        try (XmlOthers others = new XmlOthers(xml)) {
            others.startPackages();
            others.addPackage(
                "aom", "aarch64",
                "7ae"
            ).close();
        }
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(xml), StandardCharsets.UTF_8),
            // @checkstyle LineLengthCheck (1 line)
            XhtmlMatchers.hasXPath("/*[local-name()='otherdata']/*[local-name()='package' and @name='aom' and @pkgid='7ae' and @arch='aarch64']")
        );
    }

    /**
     * Test adding a version for the package.
     * @param tmp Temp path
     * @throws Exception
     */
    @Test
    void canAddVersion(@TempDir final Path tmp) throws Exception {
        final Path xml = tmp.resolve("other.xml");
        try (XmlOthers others = new XmlOthers(xml)) {
            others.startPackages();
            others.addPackage(
                "aaa", "bbb",
                "ccc"
            ).version(0, "1.0.0", "8.20190810git9666276.el8").close();
        }
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(xml), StandardCharsets.UTF_8),
            // @checkstyle LineLengthCheck (1 line)
            XhtmlMatchers.hasXPath("/*[local-name()='otherdata']/*[local-name()='package']/*[local-name()='version' and @epoch='0' and @ver='1.0.0' and @rel='8.20190810git9666276.el8']")
        );
    }

    /**
     * Test adding a changelog for the package.
     * @param tmp Temp path
     * @throws Exception
     */
    @Test
    @Disabled
    void canAddChangelog(@TempDir final Path tmp) throws Exception {
        final Path xml = tmp.resolve("changelog.xml");
        try (XmlOthers others = new XmlOthers(xml)) {
            others.startPackages();
            others.addPackage(
                "ddd", "eee",
                "fff"
            ).version(0, "2.0.0", "8.20190810git9666276.el9")
            .changelog("Paulo Lobo", 0, "Test for changelog generation")
            .close();
        }
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(xml), StandardCharsets.UTF_8),
            // @checkstyle LineLengthCheck (1 line)
            XhtmlMatchers.hasXPath("/*[local-name()='otherdata']/*[local-name()='package']/*[local-name()='changelog' and @epoch='0' and @author='Paulo Lobo' and text()='Test for changelog generation']")
        );
    }

}
