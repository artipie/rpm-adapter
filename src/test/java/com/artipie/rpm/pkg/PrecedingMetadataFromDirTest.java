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

import com.artipie.asto.test.TestResource;
import com.artipie.rpm.meta.XmlPackage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.llorllale.cactoos.matchers.MatcherOf;

/**
 * Test for {@link PrecedingMetadata.FromDir}.
 * @since 0.11
 * @checkstyle LeftCurlyCheck (500 lines)
 */
class PrecedingMetadataFromDirTest {

    @Test
    void returnsTrueIfFileExists(@TempDir final Path temp) throws IOException {
        final XmlPackage pckg = XmlPackage.PRIMARY;
        this.copyExampleToTemp(temp, pckg);
        MatcherAssert.assertThat(
            new PrecedingMetadata.FromDir(pckg, temp).exists(),
            new IsEqual<>(true)
        );
    }

    @Test
    void unzipIfFileExists(@TempDir final Path temp) throws IOException {
        final XmlPackage pckg = XmlPackage.PRIMARY;
        this.copyExampleToTemp(temp, pckg);
        final Optional<Path> unziped = new PrecedingMetadata.FromDir(pckg, temp).findAndUnzip();
        MatcherAssert.assertThat(
            "Metadata found",
            unziped.isPresent(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Metadata unzipped to parent directory",
            unziped.get().getParent(),
            new IsEqual<>(temp)
        );
    }

    @Test
    void doesNotFindIfNoMetadataExists(@TempDir final Path temp) throws IOException {
        temp.resolve("some.txt").toFile().createNewFile();
        MatcherAssert.assertThat(
            new PrecedingMetadata.FromDir(XmlPackage.FILELISTS, temp),
            new AllOf<PrecedingMetadata.FromDir>(
                new ListOf<Matcher<? super PrecedingMetadata.FromDir>>(
                    new MatcherOf<PrecedingMetadata.FromDir>(
                        fromDir -> !fromDir.exists()
                    ),
                    new MatcherOf<PrecedingMetadata.FromDir>(
                        meta -> !meta.findAndUnzip().isPresent()
                    )
                )
            )
        );
    }

    private void copyExampleToTemp(final Path temp, final XmlPackage pckg) throws IOException {
        Files.copy(
            new TestResource("repodata/primary.xml.gz.example").asPath(),
            temp.resolve(String.format("%s.xml.gz", pckg.filename()))
        );
    }

}
