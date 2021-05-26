/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.rpm.Digest;
import com.artipie.rpm.FileChecksum;
import com.artipie.rpm.NamingPolicy;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.meta.XmlRepomd;
import com.jcabi.matchers.XhtmlMatchers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link MetadataFile}.
 *
 * @since 0.6.3
 */
public final class MetadataFileTest {

    @Test
    public void updatesRepomdOnSave(@TempDir final Path tmp) throws Exception {
        final Path fake = tmp.resolve("fake.xml");
        try (
            MetadataFile meta = new MetadataFile(
                XmlPackage.PRIMARY, new PackageOutput.FileOutput.Fake(fake).start()
            )
        ) {
            final XmlRepomd repomd = new XmlRepomd(tmp.resolve("repomd.xml"));
            repomd.begin(System.currentTimeMillis());
            final String openhex = new FileChecksum(fake, Digest.SHA1).hex();
            final long size = Files.size(fake);
            final Path gzip = meta.save(
                new Repodata.Temp(new NamingPolicy.HashPrefixed(Digest.SHA1), tmp),
                Digest.SHA1, repomd
            );
            repomd.close();
            final String hex = new FileChecksum(gzip, Digest.SHA1).hex();
            MatcherAssert.assertThat(
                new String(Files.readAllBytes(repomd.file()), StandardCharsets.UTF_8),
                // @checkstyle LineLengthCheck (10 lines)
                XhtmlMatchers.hasXPaths(
                    "/*[local-name()='repomd']/*[local-name()='revision']",
                    String.format("/*[local-name()='repomd']/*[local-name()='data' and @type='primary']/*[local-name()='checksum' and @type='sha' and text()='%s']", hex),
                    String.format("/*[local-name()='repomd']/*[local-name()='data' and @type='primary']/*[local-name()='open-checksum' and @type='sha' and text()='%s']", openhex),
                    String.format("/*[local-name()='repomd']/*[local-name()='data' and @type='primary']/*[local-name()='location' and @href='repodata/%s']", String.format("%s-%s.xml.gz", hex, XmlPackage.PRIMARY.filename())),
                    String.format("/*[local-name()='repomd']/*[local-name()='data' and @type='primary']/*[local-name()='size' and text()='%d']", Files.size(gzip)),
                    String.format("/*[local-name()='repomd']/*[local-name()='data' and @type='primary']/*[local-name()='open-size' and text()='%d']", size)
                )
            );
            Files.deleteIfExists(gzip);
        }
    }
}
