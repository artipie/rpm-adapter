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
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceDownload;

/**
 * Artipie {@link Slice} for RPM repository HTTP API.
 * @since 0.7
 */
public final class RpmSlice extends Slice.Wrap {

    /**
     * New RPM repository HTTP API.
     * @param storage Storage
     */
    public RpmSlice(final Storage storage) {
        super(
            new SliceRoute(
                new SliceRoute.Path(new RtRule.ByMethod(RqMethod.GET), new SliceDownload(storage))
            )
        );
    }
}
