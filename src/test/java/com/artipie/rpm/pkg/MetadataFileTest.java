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
            final String hex = new FileChecksum(fake, Digest.SHA1).hex();
            final long size = Files.size(fake);
            final Path gzip = meta.save(
                new NamingPolicy.HashPrefixed(Digest.SHA1), Digest.SHA1, repomd
            );
            repomd.close();
            MatcherAssert.assertThat(
                new String(Files.readAllBytes(repomd.file()), StandardCharsets.UTF_8),
                // @checkstyle LineLengthCheck (10 lines)
                XhtmlMatchers.hasXPaths(
                    "/*[local-name()='repomd']/*[local-name()='revision']",
                    String.format("/*[local-name()='repomd']/*[local-name()='data' and @type='primary']/*[local-name()='checksum' and @type='sha' and text()='%s']", new FileChecksum(gzip, Digest.SHA1).hex()),
                    String.format("/*[local-name()='repomd']/*[local-name()='data' and @type='primary']/*[local-name()='open-checksum' and @type='sha' and text()='%s']", hex),
                    String.format("/*[local-name()='repomd']/*[local-name()='data' and @type='primary']/*[local-name()='location' and @href='repodata/%s']", gzip.toFile().getName()),
                    String.format("/*[local-name()='repomd']/*[local-name()='data' and @type='primary']/*[local-name()='size' and text()='%d']", Files.size(gzip)),
                    String.format("/*[local-name()='repomd']/*[local-name()='data' and @type='primary']/*[local-name()='open-size' and text()='%d']", size)
                )
            );
            Files.deleteIfExists(gzip);
        }
    }
}
