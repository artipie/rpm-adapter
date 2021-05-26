/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
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
