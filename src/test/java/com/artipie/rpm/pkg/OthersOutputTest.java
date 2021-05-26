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
import org.xmlunit.matchers.CompareMatcher;

/**
 * Test for {@link OthersOutput}.
 *
 * @since 0.11
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class OthersOutputTest {
    @Test
    void createsOthersForAbc(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("others.xml");
        final TestRpm.Abc abc = new TestRpm.Abc();
        try (PackageOutput.FileOutput others = new OthersOutput(res)) {
            others.start();
            final Path rpm = abc.path();
            others.accept(
                new FilePackage.Headers(
                    new FilePackageHeader(rpm).header(), rpm,
                    Digest.SHA256, rpm.getFileName().toString()
                )
            );
        }
        MatcherAssert.assertThat(
            Files.readAllBytes(res),
            CompareMatcher.isSimilarTo(Files.readAllBytes(abc.metadata(XmlPackage.OTHER)))
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeFilter(
                    node -> !"changelog".equals(node.getLocalName())
                )
        );
    }

    @Test
    void checkFile(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("others.xml");
        try (PackageOutput.FileOutput others = new OthersOutput(res)) {
            others.start();
            MatcherAssert.assertThat(
                others.file(),
                new IsEqual<>(res)
            );
        }
    }

    @Test
    void checkTag(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("others.xml");
        try (PackageOutput.FileOutput others = new OthersOutput(res)) {
            others.start();
            MatcherAssert.assertThat(
                others.tag(),
                new IsEqual<>("otherdata")
            );
        }
    }

    @Test
    void createsOthersForLibdeflt(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("others.xml");
        final TestRpm.Libdeflt libdeflt = new TestRpm.Libdeflt();
        try (PackageOutput.FileOutput others = new OthersOutput(res)) {
            others.start();
            final Path rpm = libdeflt.path();
            others.accept(
                new FilePackage.Headers(
                    new FilePackageHeader(rpm).header(), rpm,
                    Digest.SHA256, rpm.getFileName().toString()
                )
            );
        }
        MatcherAssert.assertThat(
            Files.readAllBytes(res),
            CompareMatcher.isSimilarTo(Files.readAllBytes(libdeflt.metadata(XmlPackage.OTHER)))
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeFilter(
                    node -> !"changelog".equals(node.getLocalName())
                )
        );
    }
}
