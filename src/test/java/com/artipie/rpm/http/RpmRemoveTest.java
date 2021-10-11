/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.rpm.RepoConfig;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RpmRemove}.
 * @since 1.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RpmRemoveTest {

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void returnsBadRequestIfFileDoesNotExist() {
        MatcherAssert.assertThat(
            "Response status is not `BAD_REQUEST`",
            new RpmRemove(this.asto, new RepoConfig.Simple()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.DELETE, "/any.rpm")
            )
        );
        MatcherAssert.assertThat(
            "Storage should be empty",
            this.asto.list(Key.ROOT).join(),
            Matchers.emptyIterable()
        );
    }

    @Test
    void returnsBadRequestIfChecksumIsIncorrect() {
        final String pckg = "my_package.rpm";
        this.asto.save(new Key.From(pckg), Content.EMPTY).join();
        MatcherAssert.assertThat(
            "Response status is not `BAD_REQUEST`",
            new RpmRemove(this.asto, new RepoConfig.Simple()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.DELETE, "/my_package.rpm"),
                Headers.EMPTY,
                new Content.From("abc123".getBytes())
            )
        );
        MatcherAssert.assertThat(
            "Storage should have 1 value",
            this.asto.list(Key.ROOT).join(),
            Matchers.iterableWithSize(1)
        );
    }

}
