/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.rpm.Digest;
import com.artipie.rpm.TestRpm;
import com.artipie.rpm.meta.XmlPackage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

/**
 * Test for {@link FilelistsOutput}.
 *
 * @since 0.11
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class FilelistsOutputTest {
    @Test
    void createsFileslistsForAbc(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("filelists.xml");
        final TestRpm.Abc abc = new TestRpm.Abc();
        try (PackageOutput.FileOutput fileslists = new FilelistsOutput(res)) {
            fileslists.start();
            final Path rpm = abc.path();
            fileslists.accept(
                new FilePackage.Headers(new FilePackageHeader(rpm).header(), rpm, Digest.SHA256)
            );
        }
        MatcherAssert.assertThat(
            Files.readAllBytes(res),
            CompareMatcher.isSimilarTo(Files.readAllBytes(abc.metadata(XmlPackage.FILELISTS)))
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeFilter(
                    node -> !("file".equals(node.getLocalName())
                        && "/usr/lib/.build-id".equals(node.getTextContent()))
                ).withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
        );
    }

    @Test
    void checkFile(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("filelists.xml");
        try (PackageOutput.FileOutput fileslists = new FilelistsOutput(res)) {
            fileslists.start();
            MatcherAssert.assertThat(
                fileslists.file(),
                new IsEqual<>(res)
            );
        }
    }

    @Test
    void checkTag(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("fileslists.xml");
        try (PackageOutput.FileOutput fileslists = new FilelistsOutput(res)) {
            fileslists.start();
            MatcherAssert.assertThat(
                fileslists.tag(),
                new IsEqual<>("filelists")
            );
        }
    }

    @Test
    void createsFilelistForLibdeflt(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("fileslists.xml");
        final TestRpm.Libdeflt libdeflt = new TestRpm.Libdeflt();
        try (PackageOutput.FileOutput fileslists = new FilelistsOutput(res)) {
            fileslists.start();
            final Path rpm = libdeflt.path();
            fileslists.accept(
                new FilePackage.Headers(new FilePackageHeader(rpm).header(), rpm, Digest.SHA256)
            );
        }
        MatcherAssert.assertThat(
            Files.readAllBytes(res),
            CompareMatcher.isSimilarTo(Files.readAllBytes(libdeflt.metadata(XmlPackage.FILELISTS)))
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
        );
    }
}
