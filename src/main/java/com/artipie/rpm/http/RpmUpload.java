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

/**
 * Slice for rpm packages upload.
 *
 * @since 0.8.3
 * @todo #162:30min Finish implementation of RpmUpload
 *  RpmUpload should behave like the defined in
 *  https://github.com/artipie/rpm-adapter/issues/162:
 *  Upload HTTP request
 *      Method: PUT
 *      URI: /package.rpm - the name of RPM package
 *      Query params:
 *      override (optional) - if true, override existing package with same name
 *      Body: RPM package data
 *  Upload process
 *      User sends RPM package as PUT HTTP request with RPM data in body.
 *      RPM adapter Slice implementation should process this request, store
 *      the package in repository without changing the name.
 *      If package with same name already exist and override query param flag
 *      is not true, then return 409 error.
 *      Artipie Slice returns 202 status on success and trigger metadata update
 *      asynchronously, it should not run multiple metadata updates
 *      simultaneously. Finish the implementation and enable tests in RpmUploadTest.
 */
public class RpmUpload extends Slice.Wrap {

    /**
     * New RPM repository HTTP API.
     *
     * @param storage Storage
     */
    public RpmUpload(final Storage storage) {
        super(new RpmSlice(storage));
    }
}
