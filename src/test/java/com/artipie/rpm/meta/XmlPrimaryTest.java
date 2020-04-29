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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link XmlPrimary}.
 *
 * @since 0.6
 */
@DisabledOnOs(OS.WINDOWS)
public final class XmlPrimaryTest {

    /**
     * Name for the xml file.
     */
    private static final String FILE_NAME = "primary.xml";

    // @todo #117:30min These tests the creation of a package and addition of license,
    //  now add some more assertion to verify that this class
    //  can write `primary.xml` file correctly. The example of primary can
    //  be found at test resources. If #86 is fixed then remove the DisabledOnOs
    //  annotation.
    @Test
    public void writesFile(@TempDir final Path temp) throws Exception {
        final Path file = temp.resolve(XmlPrimaryTest.FILE_NAME);
        try (XmlPrimary prim = new XmlPrimary(file)) {
            prim.startPackages()
                .startPackage()
                .files(
                    new String[] {"file" },
                    new String[] {"dir" },
                    new int[] {0 }
                ).startFormat()
                .license("license")
                .close()
                .close();
        }
    }

    @Test
    public void writesPackageWithFiles(@TempDir final Path temp) throws Exception {
        final Path file = temp.resolve(XmlPrimaryTest.FILE_NAME);
        try (XmlPrimary prim = new XmlPrimary(file)) {
            prim.startPackages()
                .startPackage()
                .files(
                    new String[] {"afile" },
                    new String[] {"another" },
                    new int[] {0 }
                ).startFormat()
                .close()
                .close();
        }
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(file), StandardCharsets.UTF_8),
            // @checkstyle LineLengthCheck (1 line)
            XhtmlMatchers.hasXPath("/*[local-name()='metadata']/*[local-name()='package' and @type='rpm']/*[local-name()='file']")
        );
    }

    @Test
    public void writesPackageWithLicence(@TempDir final Path temp) throws Exception {
        final Path file = temp.resolve(XmlPrimaryTest.FILE_NAME);
        try (XmlPrimary prim = new XmlPrimary(file)) {
            prim.startPackages()
                .startPackage()
                .startFormat()
                .license("alicense")
                .close()
                .close();
        }
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(file), StandardCharsets.UTF_8),
            // @checkstyle LineLengthCheck (1 line)
            XhtmlMatchers.hasXPath("/*[local-name()='metadata']/*[local-name()='package' and @type='rpm']/*[local-name()='format']/*[local-name()='license' and text()='alicense']")
        );
    }
}
