/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.jcabi.matchers.XhtmlMatchers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link XmlFilelists}.
 *
 * @since 0.6.3
 */
public final class XmlFilelistsTest {

    @Test
    public void writesFile(@TempDir final Path temp) throws Exception {
        final Path file = temp.resolve("filelists.xml");
        try (XmlFilelists list = new XmlFilelists(file)) {
            list.startPackages()
                .startPackage("packagename", "packagearch", "packagechecksun")
                .version(1, "packageversion", "packagerel")
                .files(
                    new String[] {"file" },
                    new String[] {"dir" },
                    new int[] {0 }
                ).close();
        }
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(file), StandardCharsets.UTF_8),
            XhtmlMatchers.hasXPaths(
                // @checkstyle LineLengthCheck (2 line)
                "/*[local-name()='filelists']/*[local-name()='package' and @pkgid='packagechecksun' and @name='packagename' and @arch='packagearch']/*[local-name()='version' and @epoch='1' and @ver='packageversion' and @rel='packagerel']",
                "/*[local-name()='filelists']/*[local-name()='package' and @pkgid='packagechecksun' and @name='packagename' and @arch='packagearch']/*[local-name()='file' and text()='dirfile']"
            )
        );
    }
}
