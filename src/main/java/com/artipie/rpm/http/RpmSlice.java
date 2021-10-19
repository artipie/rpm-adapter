/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.http;

import com.artipie.asto.Storage;
import com.artipie.http.Slice;
import com.artipie.http.auth.Action;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthSlice;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;
import com.artipie.rpm.RepoConfig;

/**
 * Artipie {@link Slice} for RPM repository HTTP API.
 * @since 0.7
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class RpmSlice extends Slice.Wrap {

    /**
     * Ctor.
     * @param storage The storage.
     */
    public RpmSlice(final Storage storage) {
        this(storage, Permissions.FREE, Authentication.ANONYMOUS, new RepoConfig.Simple());
    }

    /**
     * Ctor.
     * @param storage Storage
     * @param perms Access permissions.
     * @param auth Auth details.
     * @param config Repository configuration.
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    public RpmSlice(
        final Storage storage,
        final Permissions perms,
        final Authentication auth,
        final RepoConfig config
    ) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new ByMethodsRule(RqMethod.GET),
                    new BasicAuthSlice(
                        new SliceDownload(storage),
                        auth,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new ByMethodsRule(RqMethod.PUT),
                    new BasicAuthSlice(
                        new RpmUpload(storage, config),
                        auth,
                        new Permission.ByName(perms, Action.Standard.WRITE)
                    )
                ),
                new RtRulePath(
                    new ByMethodsRule(RqMethod.DELETE),
                    new BasicAuthSlice(
                        new RpmRemove(storage, config),
                        auth,
                        new Permission.ByName(perms, Action.Standard.WRITE)
                    )
                ),
                new RtRulePath(RtRule.FALLBACK, new SliceSimple(StandardRs.NOT_FOUND))
            )
        );
    }
}
