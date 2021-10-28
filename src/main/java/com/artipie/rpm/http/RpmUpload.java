/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
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
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.Rpm;
import com.google.common.base.Splitter;
import com.google.common.collect.Streams;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.reactivestreams.Publisher;

/**
 * Slice for rpm packages upload.
 *
 * @since 0.8.3
 */
public final class RpmUpload implements Slice {

    /**
     * Temp key for the packages to remove.
     */
    public static final Key TO_ADD = new Key.From(".add");

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
     * @param config Repository configuration
     */
    RpmUpload(final Storage storage, final RepoConfig config) {
        this.asto = storage;
        this.rpm = new Rpm(storage, config);
    }

    @Override
    public Response response(
        final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Request request = new Request(line);
        final Key key = request.file();
        final CompletionStage<Boolean> conflict;
        if (request.override()) {
            conflict = CompletableFuture.completedFuture(false);
        } else {
            conflict = this.asto.exists(key);
        }
        return new AsyncResponse(
            conflict.thenApply(
                conflicts -> {
                    final Response response;
                    if (conflicts) {
                        response = new RsWithStatus(RsStatus.CONFLICT);
                    } else {
                        response = new AsyncResponse(
                            CompletableInterop.fromFuture(
                                this.asto.save(key, new Content.From(body))
                            ).andThen(
                                Completable.defer(
                                    () -> {
                                        final Completable result;
                                        if (request.skipUpdate()) {
                                            result = Completable.complete();
                                        } else {
                                            result = this.rpm.batchUpdate(Key.ROOT);
                                        }
                                        return result;
                                    }
                                )
                            ).andThen(
                                Single.just(new RsWithStatus(RsStatus.ACCEPTED))
                            )
                        );
                    }
                    return response;
                }
            )
        );
    }

    /**
     * Request line.
     *
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
         *
         * @param line Line from request
         */
        Request(final String line) {
            this.line = line;
        }

        /**
         * Returns file key.
         *
         * @return File key
         */
        public Key file() {
            return new Key.From(this.path().group("rpm"));
        }

        /**
         * Returns override param.
         *
         * @return Override param value, <code>false</code> - if absent
         */
        public boolean override() {
            return this.hasParamValue("override=true");
        }

        /**
         * Returns `skip_update` param.
         *
         * @return Skip update param value, <code>false</code> - if absent
         */
        public boolean skipUpdate() {
            return this.hasParamValue("skip_update=true");
        }

        /**
         * Returns `force` param.
         *
         * @return Force param value, <code>false</code> - if absent
         */
        public boolean force() {
            return this.hasParamValue("force=true");
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

        /**
         * Checks that request query contains param with value.
         *
         * @param param Param with value string.
         * @return Result is <code>true</code> if there is param with value,
         *  <code>false</code> - otherwise.
         */
        private boolean hasParamValue(final String param) {
            return Optional.ofNullable(new RequestLineFrom(this.line).uri().getQuery())
                .map(query -> Streams.stream(Splitter.on("&").split(query)))
                .orElse(Stream.empty())
                .anyMatch(part -> part.equals(param));
        }
    }
}
