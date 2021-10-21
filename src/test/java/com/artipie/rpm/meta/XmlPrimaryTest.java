/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.rpm.pkg.HeaderTags;
import com.jcabi.matchers.XhtmlMatchers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link XmlPrimary}.
 *
 * @since 0.6
 */
public final class XmlPrimaryTest {
    @Test
    public void writesFile(@TempDir final Path temp) throws Exception {
        final Path file = temp.resolve("primary.xml");
        try (XmlPrimary prim = new XmlPrimary(file)) {
            // @checkstyle MagicNumberCheck (30 lines)
            prim.startPackages()
                .startPackage()
                .name("primary")
                .arch("arch")
                .version(0, "0.0.1", "8.20190810git9666276.el8")
                .summary("Package test for XmlPrimary")
                .description("This is a package to test XmlPrimary behavior")
                .packager("artipie")
                .url("http://giuthub.com/artipie/rpm-adapter")
                .time(5, 7)
                .size(15L, 9, 2)
                .location("http://github.com/artipie/rpm-adapter/primary.rpm")
                .files(
                    new String[] {"file" },
                    new String[] {"dir" },
                    new int[] {0 }
                ).startFormat()
                .license("license")
                .vendor("vendor")
                .group("Unspecified")
                .buildHost("http://giuthub.com/artipie/rpm-adapter/buildhost")
                .sourceRpm("primary.src.rpm")
                .headerRange(3, 8)
                .provides(
                    new ListOf<String>("abs"),
                    new ListOf<HeaderTags.Version>(new HeaderTags.Version("1.0.0"))
                )
                .requires(
                    new ListOf<>(
                        "ld-linux-aarch64.so.1()(64bit)"
                    ),
                    new ListOf<HeaderTags.Version>(new HeaderTags.Version("1.2.0"))
                )
                .close()
                .close();
        }
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(file), StandardCharsets.UTF_8),
            // @checkstyle LineLengthCheck (25 lines)
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='metadata']/*[local-name()='package' and @type='rpm']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='primary']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='arch' and text()='arch']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='version' and @epoch='0' and @ver='0.0.1' and @rel='8.20190810git9666276.el8']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='summary' and text()='Package test for XmlPrimary']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='description' and text()='This is a package to test XmlPrimary behavior']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='packager' and text()='artipie']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='url' and text()='http://giuthub.com/artipie/rpm-adapter']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='time' and @file='5' and @build='7']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='size' and @package='15' and @installed='9' and @archive='2']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='location' and @href='http://github.com/artipie/rpm-adapter/primary.rpm']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='file' and text()='dirfile']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='format']/*[name()='rpm:license' and text()='license']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='format']/*[name()='rpm:vendor' and text()='vendor']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='format']/*[name()='rpm:group' and text()='Unspecified']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='format']/*[name()='rpm:buildhost' and text()='http://giuthub.com/artipie/rpm-adapter/buildhost']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='format']/*[name()='rpm:sourcerpm' and text()='primary.src.rpm']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='format']/*[name()='rpm:header-range' and @start='3' and @end='8']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='format']/*[name()='rpm:requires']/*[name()='rpm:entry' and @name='ld-linux-aarch64.so.1()(64bit)' and @ver='1.2.0' and @epoch='0']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='format']/*[name()='rpm:provides']/*[name()='rpm:entry' and @name='abs' and @ver='1.0.0' and @epoch='0']"
            )
        );
    }
}
