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
 * Test for {@link XmlEvent.Others}.
 * @since 1.5
 */
class XmlEventOthersTest {

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    @Test
    void writesPackageInfo() throws XMLStreamException, IOException {
        final Path res = Files.createTempFile(this.tmp, "others", ".xml");
        final Path file = new TestResource("abc-1.01-26.git20200127.fc32.ppc64le.rpm").asPath();
        try (OutputStream out = Files.newOutputStream(res)) {
            final XMLEventWriter writer = new OutputFactoryImpl().createXMLEventWriter(out);
            new XmlEvent.Others(writer).add(
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
                    "<package pkgid=\"b9d10ae3485a5c5f71f0afb1eaf682bfbea4ea667cc3c3975057d6e3d8f2e905\" name=\"abc\" arch=\"ppc64le\">",
                    "<version epoch=\"0\" ver=\"1.01\" rel=\"26.git20200127.fc32\"/>",
                    "</package>"
                )
            )
        );
    }

}
