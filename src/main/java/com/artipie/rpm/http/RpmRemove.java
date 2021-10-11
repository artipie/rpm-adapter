/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.rpm.RepoConfig;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Rpm endpoint to remove packages accepts file checksum of the package to remove
 * in the request body. The slice validates request data, saves it to temp location and,
 * if update mode is {@link RepoConfig.UpdateMode#UPLOAD} and `skip_update` parameter is false
 * (or absent), initiates removing files process.
 * If request is not valid (see {@link RpmRemove#validate(Key)}), `BAD_REQUEST` status is returned.
 * @since 1.9
 */
public final class RpmRemove implements Slice {

    /**
     * Temp key for the packages to remove.
     */
    private static final Key TO_RM = new Key.From(".remove");

    /**
     * Asto storage.
     */
    private final Storage asto;

    /**
     * Repository config.
     */
    private final RepoConfig cnfg;

    /**
     * Ctor.
     * @param asto Asto storage
     * @param cnfg Repo config
     */
    public RpmRemove(final Storage asto, final RepoConfig cnfg) {
        this.asto = asto;
        this.cnfg = cnfg;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final RpmUpload.Request request = new RpmUpload.Request(line);
        final Key temp = new Key.From(RpmRemove.TO_RM, request.file());
        return new AsyncResponse(
            this.asto.save(temp, new Content.From(body)).thenCompose(
                nothing -> this.validate(request.file())
            ).thenCompose(
                valid -> {
                    CompletionStage<RsStatus> res = CompletableFuture.completedFuture(RsStatus.OK);
                    if (valid && this.cnfg.mode() == RepoConfig.UpdateMode.UPLOAD
                        && !request.skipUpdate()) {
                        // @checkstyle MethodBodyCommentsCheck (2 lines)
                        // initiate removing file process
                        res = CompletableFuture.completedFuture(RsStatus.OK);
                    } else if (!valid) {
                        res = this.asto.delete(temp).thenApply(ignored -> RsStatus.BAD_REQUEST);
                    }
                    return res.thenApply(RsWithStatus::new);
                }
            )
        );
    }

    /**
     * Validate rpm package to remove. Valid if:
     * a) package exists,
     * b) checksums (checksum of the existing package = checksum from request body) are equal.
     * @param file File key
     * @return True is package is valid
     */
    private CompletionStage<Boolean> validate(final Key file) {
        return this.asto.exists(file).thenCompose(
            exists -> {
                CompletionStage<Boolean> res = CompletableFuture.completedFuture(false);
                if (exists) {
                    res = this.asto.value(file).thenCompose(
                        val -> new ContentDigest(val, () -> this.cnfg.digest().messageDigest())
                            .hex().thenCompose(
                                pkg -> this.asto.value(new Key.From(RpmRemove.TO_RM, file))
                                    .thenCompose(
                                        temp -> new PublisherAs(temp).asciiString().thenApply(
                                            sent -> sent.equals(pkg)
                                        )
                                    )
                            )
                    );
                }
                return res;
            }
        );
    }

}
