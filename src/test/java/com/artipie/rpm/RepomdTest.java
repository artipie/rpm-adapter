/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Yegor Bugayenko
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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import io.reactivex.Completable;
import io.vertx.reactivex.core.Vertx;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link Repomd}.
 *
 * @since 1.0
 * @todo #63:30min Add a test that verifies that the gziped "type" file
 *  referenced in the repomd.xml exists in the storage and is well formed.
 *  Use an existing xsd if possible to make this test. Do not test
 *  that the values are correct, there will be other tests for that.
 * @todo #63:30min Add a test that verifies that the Repomd can update many
 *  files one by one. A file must be created before calling update so that it
 *  can be updated.
 * @todo #63:30min Add a test that verifies that the Act object passed to
 *  the Repomd is actually used as expected and updates the referenced
 *  gzip file.
 * @todo #63:30min Add a test per available digest and NamingPolicy
 *  to validate Repomd is producing the expected content for the
 *  repomd.xml file.
 * @todo #63:30min Add a test verify that it doesn't brake existing metadata
 *  in case of invalid package (e.g. generate metadata for first package,
 *  then try to generate for broken package).
 */
public final class RepomdTest {

    // @todo #63:30min Improve this test to verify that the content
    //  of the repomd.xml is well formed in terms of xml elements.
    //  Use an existing xsd if possible to make this test. Do not test
    //  that the values are correct, there will be other tests for that.
    @Test
    public void updateCreateRepomd() throws Exception {
        final Storage stg = new InMemoryStorage();
        final Repomd rmd = new Repomd(
            stg,
            Vertx.vertx(),
            new NamingPolicy.HashPrefixed(Digest.SHA1),
            Digest.SHA1
        );
        rmd.update("type", file -> Completable.complete()).blockingAwait();
        MatcherAssert.assertThat(
            stg.exists(new Key.From("repodata/repomd.xml")).get(),
            Matchers.is(true)
        );
    }
}
