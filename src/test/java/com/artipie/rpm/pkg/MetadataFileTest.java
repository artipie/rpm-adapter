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
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link MetadataFile}.
 *
 * @since 0.6.3
 */
public final class MetadataFileTest {

    // @todo #78:30min This test has everything setup so that the file
    //  corresponding to the fake wrapped FileOutput is actually included in the
    //  repomd.xml file. This test now should verify that it updates `repomd.xml`
    //  file correctly after save. If #101 is solved, remove the DisabledOnOs
    //  annotation.
    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void updatesRepomdOnSave(@TempDir final Path tmp) throws Exception {
        final Path fake = tmp.resolve("fake.xml");
        final XmlRepomd repomd = new XmlRepomd(
            tmp.resolve("repomd.xml")
        );
        try (MetadataFile meta = new MetadataFile(
            "type",
            new PackageOutput.FileOutput.Fake(fake).start(),
            repomd
        )) {
            final Path gzip = meta.save(new NamingPolicy.HashPrefixed(Digest.SHA1), Digest.SHA1);
            Files.deleteIfExists(gzip);
        }
    }
}
