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

import com.artipie.http.rq.RequestLine;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link RpmUpload}.
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class RpmUploadRequestTest {

    @Test
    void returnsFileNameKey() {
        MatcherAssert.assertThat(
            new RpmUpload.Request(
                new RequestLine("PUT", "/file.rpm", "HTTP/1.1").toString()
            ).file().string(),
            new IsEqual<>("file.rpm")
        );
    }

    @ParameterizedTest
    @CsvSource({
        "/file.rpm?override=true,true",
        "/file.rpm?some_param=true&override=true,true",
        "/file.rpm?some_param=false&override=true,true",
        ",false",
        "/file.rpm,false",
        "/file.rpm?some_param=true,false",
        "/file.rpm?override=false,false",
        "/file.rpm?override=whatever,false",
        "/file.rpm?not_override=true,false"
    })
    void readsOverrideFlag(final String uri, final boolean expected) {
        MatcherAssert.assertThat(
            new RpmUpload.Request(
                new RequestLine("PUT", uri, "HTTP/1.1").toString()
            ).override(),
            new IsEqual<>(expected)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "/file.rpm?skip_update=true,true",
        "/file.rpm?some_param=true&skip_update=true,true",
        "/file.rpm?some_param=false&skip_update=true,true",
        ",false",
        "/file.rpm,false",
        "/file.rpm?some_param=true,false",
        "/file.rpm?skip_update=false,false",
        "/file.rpm?skip_update=whatever,false",
        "/file.rpm?not_skip_update=true,false"
    })
    void readsSkipUpdateFlag(final String uri, final boolean expected) {
        MatcherAssert.assertThat(
            new RpmUpload.Request(
                new RequestLine("PUT", uri, "HTTP/1.1").toString()
            ).skipUpdate(),
            new IsEqual<>(expected)
        );
    }
}
