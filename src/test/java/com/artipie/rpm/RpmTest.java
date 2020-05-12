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
package com.artipie.rpm;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import java.nio.charset.Charset;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link Rpm}.
 *
 * @since 0.9
 * @todo #63:30min Don't change metadata when invalid package is sent.
 *  Currently Rpm is recalculating metadata when an invalid package is sent.
 *  It should not. Correct that and enable the test below.
 */
final class RpmTest {

    /**
     * Path of repomd.xml fil.
     */
    private static final String REPOMD = "repodata/repomd.xml";

    @Test
    @Disabled
    void doesntBrakeMetadataWhenInvalidPackageSent(@TempDir final Path tmp)
        throws Exception {
        final Storage storage = new InMemoryStorage();
        final Rpm repo =  new Rpm(
            storage, StandardNamingPolicy.SHA1, Digest.SHA256, true
        );
        storage.save(
            new Key.From("oldfile.rpm"),
            new Content.From("anything".getBytes())
        );
        repo.batchUpdate(Key.ROOT).blockingAwait();
        final String repomd = new String(
            new Concatenation(
                storage.value(new Key.From(RpmTest.REPOMD)).get()
            ).single().blockingGet().array(),
            Charset.defaultCharset()
        );
        final byte[] broken = {0x00, 0x01, 0x02 };
        storage.save(
            new Key.From("broken.rpm"),
            new Content.From(
                broken
            )
        );
        repo.batchUpdate(Key.ROOT).blockingAwait();
        MatcherAssert.assertThat(
            new String(
                new Concatenation(
                    storage.value(new Key.From(RpmTest.REPOMD)).get()
                ).single().blockingGet().array(),
                Charset.defaultCharset()
            ),
            new IsEqual<>(repomd)
        );
    }
}
