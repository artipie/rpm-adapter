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

import com.artipie.asto.Storage;
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicIdentities;
import com.artipie.http.auth.Identities;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.auth.SliceAuth;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceDownload;

/**
 * Artipie {@link Slice} for RPM repository HTTP API.
 * @since 0.7
 * @todo #259:30min Add integration test for rpm repository: we need to verify that
 *  original rpm client (yum and dnf) can understand and work with rpm repository. Create rpm repo
 *  with our Rpm#batchUpdate first, then configure yum/dnf repository accordingly and
 *  list/install packages from this repository. To set authorisation see
 *  https://stackoverflow.com/a/26852734/1723695
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class RpmSlice extends Slice.Wrap {

    /**
     * Ctor.
     * @param storage The storage.
     */
    public RpmSlice(final Storage storage) {
        this(storage, Permissions.FREE, Identities.ANONYMOUS);
    }

    /**
     * Ctor.
     * @param storage The storage.
     * @param perms Access permissions.
     * @param auth Auth details.
     */
    public RpmSlice(final Storage storage, final Permissions perms, final Authentication auth) {
        this(storage, perms, new BasicIdentities(auth));
    }

    /**
     * Ctor.
     * @param storage Storage
     * @param perms Access permissions.
     * @param users Concrete identities.
     */
    public RpmSlice(final Storage storage, final Permissions perms, final Identities users) {
        super(
            new SliceRoute(
                new SliceRoute.Path(
                    new RtRule.ByMethod(RqMethod.GET),
                    new SliceAuth(
                        new SliceDownload(storage),
                        new Permission.ByName("download", perms),
                        users
                    )
                ),
                new SliceRoute.Path(
                    new RtRule.ByMethod(RqMethod.PUT),
                    new SliceAuth(
                        new RpmUpload(storage),
                        new Permission.ByName("upload", perms),
                        users
                    )
                )
            )
        );
    }
}
