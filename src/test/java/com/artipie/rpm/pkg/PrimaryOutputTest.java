/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.rpm.Digest;
import com.artipie.rpm.TestRpm;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.meta.XmlPrimaryMaid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.matchers.CompareMatcher;

/**
 * Test for {@link PrimaryOutput}.
 * @since 0.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class PrimaryOutputTest {

    @Test
    void createsPrimaryForAbc(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("primary.xml");
        try (PackageOutput.FileOutput primary = new PrimaryOutput(res)) {
            primary.start();
            final Path rpm = new TestRpm.Abc().path();
            primary.accept(
                new FilePackage.Headers(
                    new FilePackageHeader(rpm).header(), rpm,
                    Digest.SHA256, rpm.getFileName().toString()
                )
            );
        }
        MatcherAssert.assertThat(
            Files.readAllBytes(res),
            CompareMatcher.isIdenticalTo(
                Files.readAllBytes(new TestRpm.Abc().metadata(XmlPackage.PRIMARY))
            ).ignoreWhitespace()
            .ignoreElementContentWhitespace()
            .normalizeWhitespace()
            .withNodeFilter(
                node -> !"file".equals(node.getLocalName())
                    && !"provides".equals(node.getLocalName())
                    && !"requires".equals(node.getLocalName())
            ).withAttributeFilter(
                attr -> !"file".equals(attr.getName()) && !"archive".equals(attr.getName())
            )
        );
    }

    @Test
    void checkFile(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("primary.xml");
        try (PackageOutput.FileOutput primary = new PrimaryOutput(res)) {
            primary.start();
            MatcherAssert.assertThat(
                primary.file(),
                new IsEqual<>(res)
            );
        }
    }

    @Test
    void checkTag(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("primary.xml");
        try (PackageOutput.FileOutput primary = new PrimaryOutput(res)) {
            primary.start();
            MatcherAssert.assertThat(
                primary.tag(),
                new IsEqual<>("metadata")
            );
        }
    }

    @Test
    void createsPrimaryForLibdeflt(@TempDir final Path temp) throws IOException {
        final Path res = temp.resolve("primary.xml");
        try (PackageOutput.FileOutput primary = new PrimaryOutput(res)) {
            primary.start();
            final Path rpm = new TestRpm.Libdeflt().path();
            primary.accept(
                new FilePackage.Headers(
                    new FilePackageHeader(rpm).header(), rpm,
                    Digest.SHA256, rpm.getFileName().toString()
                )
            );
        }
        MatcherAssert.assertThat(
            Files.readAllBytes(res),
            CompareMatcher.isIdenticalTo(
                Files.readAllBytes(new TestRpm.Libdeflt().metadata(XmlPackage.PRIMARY))
            ).ignoreWhitespace()
            .ignoreElementContentWhitespace()
            .normalizeWhitespace()
            .withNodeFilter(
                node -> !"file".equals(node.getLocalName())
                    && !"provides".equals(node.getLocalName())
                    && !"requires".equals(node.getLocalName())
            ).withAttributeFilter(
                attr -> !"file".equals(attr.getName()) && !"archive".equals(attr.getName())
            )
        );
    }

    @Test
    void createsCorrectMaidInstance(@TempDir final Path temp) throws IOException {
        try (PrimaryOutput output = new PrimaryOutput(temp.resolve("fake.xml"))) {
            output.start();
            MatcherAssert.assertThat(
                output.maid(),
                new IsInstanceOf(XmlPrimaryMaid.class)
            );
        }
    }
}
