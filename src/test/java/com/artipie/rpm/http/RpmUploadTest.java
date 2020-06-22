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
package com.artipie.rpm.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RpmUpload}.
 *
 * @since 0.8.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class RpmUploadTest {

    @Test
    void canUploadArtifact() throws Exception {
        final Storage storage = new InMemoryStorage();
        final byte[] content = Files.readAllBytes(
            Paths.get("src/test/resources-binary/abc-1.01-26.git20200127.fc32.ppc64le.rpm")
        );
        MatcherAssert.assertThat(
            "ACCEPTED 202 returned",
            new RpmUpload(storage).response(
                new RequestLine("PUT", "/uploaded.rpm", "HTTP/1.1").toString(),
                new ListOf<Map.Entry<String, String>>(),
                Flowable.fromArray(ByteBuffer.wrap(content))
            ),
            new RsHasStatus(RsStatus.ACCEPTED)
        );
        MatcherAssert.assertThat(
            "Content saved to storage",
            new BlockingStorage(storage).value(new Key.From("uploaded.rpm")),
            new IsEqual<>(content)
        );
    }

    @Test
    void canReplaceArtifact() throws Exception {
        final Storage storage = new InMemoryStorage();
        final byte[] content =
            "replaced package bytes".getBytes(StandardCharsets.UTF_8);
        final Key key = new Key.From("replaced.rpm");
        new BlockingStorage(storage).save(key, "uploaded package".getBytes());
        MatcherAssert.assertThat(
            new RpmUpload(storage).response(
                new RequestLine("PUT", "/replaced.rpm?override=true", "HTTP/1.1").toString(),
                Headers.EMPTY,
                Flowable.fromArray(ByteBuffer.wrap(content))
            ),
            new RsHasStatus(RsStatus.ACCEPTED)
        );
        MatcherAssert.assertThat(
            new BlockingStorage(storage).value(key),
            new IsEqual<>(content)
        );
    }

    @Test
    void dontReplaceArtifact() throws Exception {
        final Storage storage = new InMemoryStorage();
        final byte[] content =
            "first package content".getBytes(StandardCharsets.UTF_8);
        final Key key = new Key.From("not-replaced.rpm");
        new BlockingStorage(storage).save(key, content);
        MatcherAssert.assertThat(
            new RpmUpload(storage).response(
                new RequestLine("PUT", "/not-replaced.rpm", "HTTP/1.1").toString(),
                Headers.EMPTY,
                Flowable.fromArray(ByteBuffer.wrap("second package content".getBytes()))
            ),
            new RsHasStatus(RsStatus.CONFLICT)
        );
        MatcherAssert.assertThat(
            new BlockingStorage(storage).value(key),
            new IsEqual<>(content)
        );
    }
}
