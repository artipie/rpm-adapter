/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.jcabi.matchers.XhtmlMatchers;
import com.jcabi.xml.XMLDocument;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testcase for {@link XmlOthers} class.
 * @since 0.6
 */
public class XmlOthersTest {

    /**
     * Test package added successfully.
     * @param tmp Temp path
     * @throws Exception
     * @checkstyle
     */
    @Test
    void canAddPackage(@TempDir final Path tmp) throws Exception {
        final Path xml = tmp.resolve("others.xml");
        try (XmlOthers others = new XmlOthers(xml)) {
            others.startPackages();
            others.addPackage(
                "aom", "aarch64",
                "7ae"
            ).close();
        }
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(xml), StandardCharsets.UTF_8),
            // @checkstyle LineLengthCheck (1 line)
            XhtmlMatchers.hasXPath("/*[local-name()='otherdata']/*[local-name()='package' and @name='aom' and @pkgid='7ae' and @arch='aarch64']")
        );
    }

    /**
     * Test adding a version for the package.
     * @param tmp Temp path
     * @throws Exception
     */
    @Test
    void canAddVersion(@TempDir final Path tmp) throws Exception {
        final Path xml = tmp.resolve("other.xml");
        try (XmlOthers others = new XmlOthers(xml)) {
            others.startPackages();
            others.addPackage(
                "aaa", "bbb",
                "ccc"
            ).version(0, "1.0.0", "8.20190810git9666276.el8").close();
        }
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(xml), StandardCharsets.UTF_8),
            // @checkstyle LineLengthCheck (1 line)
            XhtmlMatchers.hasXPath("/*[local-name()='otherdata']/*[local-name()='package']/*[local-name()='version' and @epoch='0' and @ver='1.0.0' and @rel='8.20190810git9666276.el8']")
        );
    }

    /**
     * Test adding a changelog for the package.
     * @param tmp Temp path
     * @throws Exception
     */
    @Test
    void canAddChangelog(@TempDir final Path tmp) throws Exception {
        final Path xml = tmp.resolve("changelog.xml");
        try (XmlOthers others = new XmlOthers(xml)) {
            others.startPackages();
            others.addPackage(
                "ddd", "eee",
                "fff"
            ).version(0, "2.0.0", "8.20190810git9666276.el9")
            .changelog("Paulo Lobo", 0, "Test for changelog generation")
            .close();
        }
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(xml), StandardCharsets.UTF_8),
            // @checkstyle LineLengthCheck (1 line)
            XhtmlMatchers.hasXPath("/*[local-name()='otherdata']/*[local-name()='package']/*[local-name()='changelog' and @date='0' and @author='Paulo Lobo' and text()='Test for changelog generation']")
        );
    }

    /**
     * Test adding a changelog list for the package.
     * @param tmp Temp path
     * @throws Exception
     */
    @Test
    void canAddMultipleChangelogs(@TempDir final Path tmp) throws Exception {
        final Path xml = tmp.resolve("multiple-changelog.xml");
        try (XmlOthers others = new XmlOthers(xml)) {
            others.startPackages();
            others.addPackage(
                "ggg", "hhh",
                "iii"
            ).version(0, "3.0.0", "9.20190810git9666276.el10")
            .changelog(
                // @checkstyle LineLengthCheck (5 lines)
                new ListOf<>(
                    "* Wed May 13 2020 John Doe <johndoe@artipie.org> - 0.1-2\n- Second artipie package",
                    "* Tue May 31 2016 Jane Doe <janedoe@artipie.org> - 0.1-1\n- First artipie package\n- Example second item in the changelog for version-release 0.1-1"
                )
            )
            .close();
        }
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(xml), StandardCharsets.UTF_8),
            // @checkstyle LineLengthCheck (5 lines)
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='otherdata']/*[local-name()='package']/*[local-name()='changelog' and @date='1589328000' and @author='John Doe <johndoe@artipie.org>' and text()='- 0.1-2\n- Second artipie package']",
                "/*[local-name()='otherdata']/*[local-name()='package']/*[local-name()='changelog' and @date='1464652800' and @author='Jane Doe <janedoe@artipie.org>' and text()='- 0.1-1\n- First artipie package\n- Example second item in the changelog for version-release 0.1-1']"
            )
        );
        MatcherAssert.assertThat(
            // @checkstyle LineLengthCheck (2 lines)
            new XMLDocument(xml)
                .xpath("count(/*[local-name()='otherdata']/*[local-name()='package']/*[local-name()='changelog'])")
                .get(0),
            new IsEqual<>("2")
        );
    }

}
