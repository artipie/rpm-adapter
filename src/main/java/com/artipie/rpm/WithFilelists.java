/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Yegor Bugayenko
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
package com.artipie.rpm;

import com.artipie.asto.Key;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;

/**
 * Rpm decorator which provide filelist creation on update.
 *
 * @since 0.3.0
 * @todo #17:30min Implement WithFilelists decorator.
 *  WithFilelists decorator create filelists.xml and filelists.xml.gz files on
 *  upload. Implement this behavior and then enable the test in
 *  WithFilelistsTest.
 */
public final class WithFilelists implements RpmAbstraction {

    /**
     * Original Rpm.
     */
    private final RpmAbstraction origin;

    /**
     * Constructor.
     *
     * @param rpm Rpm to be wrapped.
     */
    public WithFilelists(final RpmAbstraction rpm) {
        this.origin = rpm;
    }

    @Override
    public Completable update(final Key key) {
        return this.origin.update(key);
    }

    @Override
    public Completable batchUpdate(final Key prefix) {
        return this.origin.batchUpdate(prefix);
    }

    @Override
    public CompletableSource doUpdate(RepoUpdater updater, Key key) {
        return this.origin.doUpdate(updater, key);
    }
}
