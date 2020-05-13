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
import com.artipie.rpm.NamingPolicy;
import com.artipie.rpm.meta.XmlRepomd;
import com.jcabi.matchers.XhtmlMatchers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link MetadataFile}.
 *
 * @since 0.6.3
 */
public final class MetadataFileTest {

    // @todo #106:30min MetadataFile is saving an invalid xml to repomd.xml.
    //  This test verifies that MetadataFile updates `repomd.xml`
    //  file correctly after save, but it doesn't. Fix it then enable the test,
    //  restoring the coverage levels to:
    //  instructions: 0.36
    //  lines: 0.42
    //  complexity: 0.38
    //  methods: 0.35
    //  missed classes: 16.
    @Test
    @Disabled
    public void updatesRepomdOnSave(@TempDir final Path tmp) throws Exception {
        final Path fake = tmp.resolve("fake.xml");
        try (
            XmlRepomd repomd = new XmlRepomd(tmp.resolve("repomd.xml"));
            MetadataFile meta = new MetadataFile(
                "type", new PackageOutput.FileOutput.Fake(fake).start()
            )
        ) {
            repomd.begin(System.currentTimeMillis());
            final Path gzip = meta.save(
                new NamingPolicy.HashPrefixed(Digest.SHA1), Digest.SHA1, repomd
            );
            MatcherAssert.assertThat(
                new String(Files.readAllBytes(repomd.file()), StandardCharsets.UTF_8),
                // @checkstyle LineLengthCheck (10 lines)
                XhtmlMatchers.hasXPaths(
                    "/*[local-name()='repomd']/*[local-name()='revision']",
                    "/*[local-name()='repomd']/*[local-name()='data' and @type='type']/*[local-name()='checksum' and @type='sha' and text()='1d29a2e759c6d25f1a0762bb6e47ec7cee8f9a97']",
                    "/*[local-name()='repomd']/*[local-name()='data' and @type='type']/*[local-name()='open-checksum' and @type='sha' and text()='3ac201172677076a818a18eb1e8feecf1a04722a']",
                    "/*[local-name()='repomd']/*[local-name()='data' and @type='type']/*[local-name()='location' and @href='repodata/dfd213cf4bf5ac9d11f33a51b820c943b48c9a99-type.xml.gz']",
                    "/*[local-name()='repomd']/*[local-name()='data' and @type='type']/*[local-name()='size' and text()='29']",
                    "/*[local-name()='repomd']/*[local-name()='data' and @type='type']/*[local-name()='open-size' and text()='9']"
                    )
            );
            Files.deleteIfExists(gzip);
        }
    }
}
