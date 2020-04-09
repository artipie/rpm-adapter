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
package com.artipie.rpm.meta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Testcase for {@link XmlOthers} class.
 * @since 0.1
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class XmlOthersTest {

    /**
     * Path of the file.
     */
    private static String path = "src/test/resources/other.xml";

    /**
     * The XmlOthers object.
     */
    private static XmlOthers xmlOthers = new XmlOthers(Paths.get(XmlOthersTest.path));

    /**
     * File object for the created file.
     */
    private static File file = new File(XmlOthersTest.path);

    /**
     * Expected content of the file.
     * @checkstyle StringLiteralsConcatenationCheck (20 lines)
     */
    private static String expectedOut =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
        + "<otherdata xmlns=\"http://linux.duke.edu/metadata/other\" packages=\"1\">\n"
        + "    <package name=\"aom\" "
        + "pkgid=\"7eaefd1cb4f9740558da7f12f9cb5a6141a47f5d064a98d46c29959869af1a44\""
        + " arch=\"aarch64\">\n"
        + "        <version ver=\"1.0.0\" rel=\"8.20190810git9666276.el8\" epoch=\"0\"/>\n"
        + "        <changelog>NOT_IMPLEMENTED</changelog>\n"
        + "    </package>\n"
        + "</otherdata>\n";

    /**
     * Setting up the file and insert some test packages.
     * @throws Exception
     * @todo #80:30min after implementing the changelog, refactor this to add actual changelog
     *  this should also include changing the {@code expectedOut} to match the added changelog
     */
    @BeforeAll
    public void setUp() throws Exception {
        XmlOthersTest.xmlOthers.startPackages();
        final XmlOthers.Package pack = XmlOthersTest.xmlOthers.addPackage(
            "aom",
            "aarch64",
            "7eaefd1cb4f9740558da7f12f9cb5a6141a47f5d064a98d46c29959869af1a44"
        );
        pack.version(0, "1.0.0", "8.20190810git9666276.el8");
        pack.changelog();
        pack.close();
        XmlOthersTest.xmlOthers.close();
    }

    /**
     * Delete the created file after test is done.
     * @throws Exception
     */
    @AfterAll
    public void tearDown() throws Exception {
        XmlOthersTest.file.delete();
    }

    /**
     * Test the created file content is the same as expected.
     */
    @Test
    public void addingAPackage() {
        try {
            final String content = new String(Files.readAllBytes(Paths.get(XmlOthersTest.path)));
            MatcherAssert.assertThat(
                "file created",
                content,
                IsEqual.equalTo(XmlOthersTest.expectedOut)
            );
        } catch (final IOException exception) {
            MatcherAssert.assertThat(
                String.format("IOException occurred: %s", exception.getMessage()),
                false
            );
        }
    }
}
