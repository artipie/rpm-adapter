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
package com.artipie.rpm.pkg;

import com.artipie.rpm.Digest;
import com.artipie.rpm.StandardNamingPolicy;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.meta.XmlRepomd;
import com.jcabi.aspects.Tv;
import com.jcabi.log.Logger;
import com.jcabi.matchers.XhtmlMatchers;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import javax.xml.stream.XMLStreamException;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.redline_rpm.ReadableChannelWrapper;
import org.redline_rpm.Scanner;
import org.redline_rpm.header.Format;
import org.redline_rpm.header.Header;

/**
 * Test for {@link ModifiableMetadata}.
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class ModifiableMetadataTest {

    @Test
    void generatesMetadataFile(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("primary.xml");
        res.toFile().createNewFile();
        final Path part = temp.resolve("part.primary.xml");
        Files.copy(Paths.get("src/test/resources-binary/repodata/primary.xml.example"), part);
        final ModifiableMetadata mtd = new ModifiableMetadata(
            new MetadataFile(XmlPackage.PRIMARY, new PrimaryOutput(res).start()),
            part
        );
        final Path rpm =
            Paths.get("src/test/resources-binary/abc-1.01-26.git20200127.fc32.ppc64le.rpm");
        mtd.accept(new FilePackage.Headers(this.header(rpm), rpm, Digest.SHA256));
        mtd.close();
        mtd.brush(
            new ListOf<String>("7eaefd1cb4f9740558da7f12f9cb5a6141a47f5d064a98d46c29959869af1a44")
        );
        MatcherAssert.assertThat(
            "Has 'abc' and 'nginx' packages, writes `packages` attribute correctly",
            new String(Files.readAllBytes(res), StandardCharsets.UTF_8),
            XhtmlMatchers.hasXPath(
                //@checkstyle LineLengthCheck (2 lines)
                "/*[local-name()='metadata' and @packages='2']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='abc']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='nginx']"
            )
        );
        MatcherAssert.assertThat(
            "Does not have 'aom' package",
            new String(Files.readAllBytes(res), StandardCharsets.UTF_8),
            new IsNot<>(
                XhtmlMatchers.hasXPath(
                    //@checkstyle LineLengthCheck (1 line)
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='aom']"
                )
            )
        );
    }

    @Test
    void savesItselfToRepomd(@TempDir final Path temp) throws IOException, XMLStreamException {
        final Path filelists = temp.resolve("test.filelists.xml");
        final Path part = temp.resolve("part.filelists.xml");
        Files.copy(Paths.get("src/test/resources-binary/repodata/filelists.xml.example"), part);
        filelists.toFile().createNewFile();
        final ModifiableMetadata mtd = new ModifiableMetadata(
            new MetadataFile(XmlPackage.FILELISTS, new FilelistsOutput(filelists).start()),
            part
        );
        mtd.close();
        final Path repomd = temp.resolve("repomd.xml");
        try (XmlRepomd xml = new XmlRepomd(repomd)) {
            xml.begin(System.currentTimeMillis() / Tv.THOUSAND);
            mtd.save(StandardNamingPolicy.PLAIN, Digest.SHA256, xml);
        }
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(repomd), Charset.defaultCharset()),
            XhtmlMatchers.hasXPaths(
                //@checkstyle LineLengthCheck (1 line)
                "/*[namespace-uri()='http://linux.duke.edu/metadata/repo' and local-name()='repomd']",
                "/*[name()='repomd']/*[name()='revision']",
                "/*[name()='repomd']/*[name()='data' and @type='filelists']"
            )
        );
    }

    /**
     * Creates header from rpm file.
     * @param file File
     * @return Header
     * @throws IOException On error
     */
    private Header header(final Path file) throws IOException {
        try (FileChannel chan = FileChannel.open(file, StandardOpenOption.READ)) {
            final Format format = new Scanner(
                new PrintStream(Logger.stream(Level.FINE, this))
            ).run(new ReadableChannelWrapper(chan));
            return format.getHeader();
        }
    }

}
