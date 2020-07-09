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
import com.artipie.rpm.TestResource;
import com.artipie.rpm.TestRpm;
import com.artipie.rpm.hm.NodeHasPkgCount;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.meta.XmlRepomd;
import com.jcabi.aspects.Tv;
import com.jcabi.matchers.XhtmlMatchers;
import com.jcabi.xml.XMLDocument;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link ModifiableMetadata}.
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ModifiableMetadataTest {

    @Test
    void generatesMetadataFileWhenRpmsWereRemovedAndAdded(@TempDir final Path temp)
        throws IOException {
        final Path res = temp.resolve("primary.xml");
        res.toFile().createNewFile();
        final Path part = temp.resolve("part.primary.xml");
        Files.copy(new TestResource("repodata/primary.xml.example").file(), part);
        final ModifiableMetadata mtd = new ModifiableMetadata(
            new MetadataFile(XmlPackage.PRIMARY, new PrimaryOutput(res).start()),
            this.preceding(Optional.of(part))
        );
        final Path rpm = new TestRpm.Abc().path();
        mtd.accept(
            new FilePackage.Headers(new FilePackageHeader(rpm).header(), rpm, Digest.SHA256)
        );
        mtd.close();
        mtd.brush(
            new ListOf<String>("7eaefd1cb4f9740558da7f12f9cb5a6141a47f5d064a98d46c29959869af1a44")
        );
        MatcherAssert.assertThat(
            "Has 'abc' and 'nginx' packages, writes `packages` attribute correctly",
            new XMLDocument(res),
            Matchers.allOf(
                XhtmlMatchers.hasXPath(
                    //@checkstyle LineLengthCheck (3 lines)
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='abc']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='nginx']"
                ),
                new NodeHasPkgCount(2, XmlPackage.PRIMARY.tag())
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
    void generatesMetadataFileWhenRpmsWereRemoved(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("other.xml");
        res.toFile().createNewFile();
        final Path part = temp.resolve("part.other.xml");
        Files.copy(new TestResource("repodata/other.xml.example").file(), part);
        final ModifiableMetadata mtd = new ModifiableMetadata(
            new MetadataFile(XmlPackage.OTHER, new OthersOutput(res).start()),
            this.preceding(Optional.of(part))
        );
        mtd.close();
        mtd.brush(
            new ListOf<String>("54f1d9a1114fa85cd748174c57986004857b800fe9545fbf23af53f4791b31e2")
        );
        MatcherAssert.assertThat(
            "Has 'aom' package, writes `packages` attribute correctly",
            new XMLDocument(res),
            Matchers.allOf(
                XhtmlMatchers.hasXPath(
                    "/*[local-name()='otherdata']/*[local-name()='package' and @name='aom']"
                ),
                new NodeHasPkgCount(1, XmlPackage.OTHER.tag())
            )
        );
        MatcherAssert.assertThat(
            "Does not have 'nginx' package",
            new String(Files.readAllBytes(res), StandardCharsets.UTF_8),
            new IsNot<>(
                XhtmlMatchers.hasXPath(
                    "/*[local-name()='otherdata']/*[local-name()='package' and @name='nginx']"
                )
            )
        );
    }

    @Test
    void generatesMetadataFileWhenRpmsWereAdded(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("filelists.xml");
        res.toFile().createNewFile();
        final Path part = temp.resolve("part.filelists.xml");
        Files.copy(new TestResource("repodata/filelists.xml.example").file(), part);
        final ModifiableMetadata mtd = new ModifiableMetadata(
            new MetadataFile(XmlPackage.FILELISTS, new FilelistsOutput(res).start()),
            this.preceding(Optional.of(part))
        );
        final Path rpm = new TestRpm.Abc().path();
        mtd.accept(
            new FilePackage.Headers(new FilePackageHeader(rpm).header(), rpm, Digest.SHA256)
        );
        mtd.close();
        mtd.brush(Collections.emptyList());
        MatcherAssert.assertThat(
            "Has 'aom', 'nginx' and 'abc' packages, writes `packages` attribute correctly",
            new XMLDocument(res),
            Matchers.allOf(
                XhtmlMatchers.hasXPath(
                    "/*[local-name()='filelists']/*[local-name()='package' and @name='aom']",
                    "/*[local-name()='filelists']/*[local-name()='package' and @name='nginx']",
                    "/*[local-name()='filelists']/*[local-name()='package' and @name='abc']"
                ),
                new NodeHasPkgCount(3, XmlPackage.FILELISTS.tag())
            )
        );
    }

    @Test
    void generatesMetadataFileForNewRepo(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("primary.xml");
        res.toFile().createNewFile();
        final ModifiableMetadata mtd = new ModifiableMetadata(
            new MetadataFile(XmlPackage.PRIMARY, new PrimaryOutput(res).start()),
            this.preceding(Optional.empty())
        );
        final Path abc = new TestRpm.Abc().path();
        mtd.accept(
            new FilePackage.Headers(new FilePackageHeader(abc).header(), abc, Digest.SHA256)
        );
        final Path libdeflt = new TestRpm.Libdeflt().path();
        mtd.accept(
            new FilePackage.Headers(
                new FilePackageHeader(libdeflt).header(), libdeflt, Digest.SHA256
            )
        );
        mtd.close();
        mtd.brush(Collections.emptyList());
        MatcherAssert.assertThat(
            "Has 'libdeflt1_0' and 'abc' packages, writes `packages` attribute correctly",
            new XMLDocument(res),
            Matchers.allOf(
                XhtmlMatchers.hasXPath(
                    //@checkstyle LineLengthCheck (2 lines)
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='abc']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='libdeflt1_0']"
                ),
                new NodeHasPkgCount(2, XmlPackage.PRIMARY.tag())
            )
        );
    }

    @Test
    void savesItselfToRepomd(@TempDir final Path temp) throws IOException {
        final Path filelists = temp.resolve("test.filelists.xml");
        final Path part = temp.resolve("part.filelists.xml");
        Files.copy(new TestResource("repodata/filelists.xml.example").file(), part);
        filelists.toFile().createNewFile();
        final ModifiableMetadata mtd = new ModifiableMetadata(
            new MetadataFile(XmlPackage.FILELISTS, new FilelistsOutput(filelists).start()),
            this.preceding(Optional.of(part))
        );
        mtd.close();
        final Path repomd = temp.resolve("repomd.xml");
        try (XmlRepomd xml = new XmlRepomd(repomd)) {
            xml.begin(System.currentTimeMillis() / Tv.THOUSAND);
            mtd.save(new Repodata.Temp(StandardNamingPolicy.PLAIN, temp), Digest.SHA256, xml);
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

    private PrecedingMetadata preceding(final Optional<Path> part) {
        return new PrecedingMetadata() {
            @Override
            public boolean exists() {
                return true;
            }

            @Override
            public Optional<Path> findAndUnzip() {
                return part;
            }
        };
    }
}
