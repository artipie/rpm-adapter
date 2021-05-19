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
import com.artipie.rpm.hm.IsXmlEqual;
import com.artipie.rpm.pkg.FilePackage;
import com.artipie.rpm.pkg.FilePackageHeader;
import com.fasterxml.aalto.stax.OutputFactoryImpl;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link XmlEvent.Other}.
 * @since 1.5
 */
class XmlEventFilelistsTest {

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    @Test
    void writesPackageInfo() throws XMLStreamException, IOException {
        final Path res = Files.createTempFile(this.tmp, "filelists", ".xml");
        final Path file = new TestResource("libdeflt1_0-2020.03.27-25.1.armv7hl.rpm").asPath();
        try (OutputStream out = Files.newOutputStream(res)) {
            final XMLEventWriter writer = new OutputFactoryImpl().createXMLEventWriter(out);
            new XmlEvent.Filelists().add(
                writer,
                new FilePackage.Headers(new FilePackageHeader(file).header(), file, Digest.SHA256)
            );
            writer.close();
        }
        MatcherAssert.assertThat(
            res,
            new IsXmlEqual(
                String.join(
                    "\n",
                    //@checkstyle LineLengthCheck (1 line)
                    "<package pkgid=\"47bbb8b2401e8853812e6340f4197252b92463c132f64a257e18c0c8c83ae462\" name=\"libdeflt1_0\" arch=\"armv7hl\">",
                    "<version epoch=\"0\" ver=\"2020.03.27\" rel=\"25.1\"/>",
                    "<file>/usr/lib/libdeflt.so.1.0</file>",
                    "<file type=\"dir\">/usr/share/licenses/libdeflt1_0</file>",
                    "<file>/usr/share/licenses/libdeflt1_0/CDDL.Schily.txt</file>",
                    "</package>"
                )
            )
        );
    }

}
