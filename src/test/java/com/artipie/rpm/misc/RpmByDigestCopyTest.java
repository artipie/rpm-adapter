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
package com.artipie.rpm.misc;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.rpm.TestRpm;
import io.reactivex.Flowable;
import java.io.IOException;
import java.util.Collections;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RpmByDigestCopy}.
 * @since 0.11
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class RpmByDigestCopyTest {

    /**
     * Storage to copy from.
     */
    private Storage from;

    /**
     * Destination storage.
     */
    private Storage dest;

    @BeforeEach
    void init() {
        this.from = new InMemoryStorage();
        this.dest = new InMemoryStorage();
    }

    @Test
    void filtersFilesByDigests() throws IOException {
        final TestRpm rpm = new TestRpm.Abc();
        new TestRpm.Multiple(rpm, new TestRpm.Libdeflt()).put(this.from);
        new RpmByDigestCopy(
            this.from, Key.ROOT,
            new ListOf<String>("47bbb8b2401e8853812e6340f4197252b92463c132f64a257e18c0c8c83ae462")
        ).copy(this.dest).blockingAwait();
        MatcherAssert.assertThat(
            this.dest.list(Key.ROOT).join().size() == 1
                && this.dest.exists(new Key.From(rpm.path().getFileName().toString())).join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void filtersFilesByExtension() throws IOException {
        final TestRpm rpm = new TestRpm.Abc();
        rpm.put(this.from);
        this.from.save(new Key.From("some/content"), new Content.From(Flowable.empty())).join();
        new RpmByDigestCopy(this.from, Key.ROOT, Collections.emptyList())
            .copy(this.dest).blockingAwait();
        MatcherAssert.assertThat(
            this.dest.list(Key.ROOT).join().size() == 1
                && this.dest.exists(new Key.From(rpm.path().getFileName().toString())).join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void copiesAllWhenDigestsAreEmpty() throws IOException {
        new TestRpm.Multiple(new TestRpm.Abc(), new TestRpm.Libdeflt()).put(this.from);
        new RpmByDigestCopy(this.from, Key.ROOT, Collections.emptyList())
            .copy(this.dest).blockingAwait();
        MatcherAssert.assertThat(
            this.dest.exists(new Key.From(new TestRpm.Abc().path().getFileName().toString())).join()
                && this.dest.exists(
                    new Key.From(new TestRpm.Libdeflt().path().getFileName().toString())
                ).join(),
            new IsEqual<>(true)
        );
    }

}
