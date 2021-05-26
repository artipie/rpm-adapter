/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.jcabi.matchers.XhtmlMatchers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link XmlPackagesFile}.
 *
 * @since 0.6.3
 */
public final class XmlPackagesFileTest {

    @Test
    public void writesCorrectTag(@TempDir final Path temp) throws Exception {
        final Path file = temp.resolve("tag.xml");
        try (XmlPackagesFile packs = new XmlPackagesFile(new XmlFile(file), XmlPackage.OTHER)) {
            packs.startPackages();
        }
        XmlPackagesFileTest.assertion(
            file, String.format("/*[name()='%s']", XmlPackage.OTHER.tag())
        );
    }

    @Test
    public void writesCorrectNamespace(@TempDir final Path temp) throws Exception {
        final Path file = temp.resolve("name.xml");
        try (XmlPackagesFile packs = new XmlPackagesFile(new XmlFile(file), XmlPackage.FILELISTS)) {
            packs.startPackages();
        }
        XmlPackagesFileTest.assertion(
            file,
            String.format("/*[namespace-uri(.)='%s']", XmlPackage.FILELISTS.xmlNamespaces().get(""))
        );
    }

    private static void assertion(final Path file, final String expected) throws IOException {
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(file), StandardCharsets.UTF_8),
            XhtmlMatchers.hasXPath(expected)
        );
    }
}
