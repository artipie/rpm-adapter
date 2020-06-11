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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.rpm.Rpm;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Slice for rpm packages upload.
 *
 * @since 0.8.3
 * @todo #253:30min Finish implementation of RpmUpload by implementing parsing of optional
 *  false by default `override` request parameter: if package with same name already exist and
 *  `override` query param flag is not true, then return 409 error. Parameter parsing should be
 *  implemented in `Request` class and tested, in `response` method we need to check parameter
 *  value and if the file exists in storage. Do not forget about tests.
 *  For more information please refer to https://github.com/artipie/rpm-adapter/issues/162:
 */
final class RpmUpload implements Slice {

    /**
     * Asto storage.
     */
    private final Storage asto;

    /**
     * Rpm instance.
     */
    private final Rpm rpm;

    /**
     * RPM repository HTTP API.
     *
     * @param storage Storage
     */
    RpmUpload(final Storage storage) {
        this.asto = storage;
        this.rpm = new Rpm(storage);
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Request request = new Request(line);
        return new AsyncResponse(
            SingleInterop.fromFuture(
                this.asto.save(
                    request.file(),
                    new Content.From(body)
                ).thenApply(ignored -> true)
            )
                .flatMapCompletable(ignored -> this.rpm.batchUpdate(Key.ROOT))
                .<Response>andThen(Single.just(new RsWithStatus(RsStatus.ACCEPTED)))
        );
    }

    /**
     * Request line.
     * @since 0.9
     */
    static final class Request {

        /**
         * RegEx pattern for path.
         */
        public static final Pattern PTRN = Pattern.compile("^/(?<rpm>.*\\.rpm)");

        /**
         * Request line.
         */
        private final String line;

        /**
         * Ctor.
         * @param line Line from request
         */
        Request(final String line) {
            this.line = line;
        }

        /**
         * Returns file key.
         * @return File key
         */
        public Key file() {
            return new Key.From(this.path().group("rpm"));
        }

        /**
         * Matches request path by RegEx pattern.
         *
         * @return Path matcher.
         */
        private Matcher path() {
            final String path = new RequestLineFrom(this.line).uri().getPath();
            final Matcher matcher = PTRN.matcher(path);
            if (!matcher.matches()) {
                throw new IllegalStateException(String.format("Unexpected path: %s", path));
            }
            return matcher;
        }
    }
}
