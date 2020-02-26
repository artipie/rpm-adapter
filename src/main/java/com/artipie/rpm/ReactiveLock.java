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

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reactive sync primitive.
 *
 * @since 0.1
 */
class ReactiveLock {

    /**
     * Unlock and emit sync object.
     */
    private final Object sync = new Object();

    /**
     * Current state of the lock. {@link AtomicBoolean} is used here for the purpose of readability.
     */
    private final AtomicBoolean locked = new AtomicBoolean(false);

    /**
     * Queue of lock acquires.
     */
    private final Queue<CompletableEmitter> acquires = new ConcurrentLinkedQueue<>();

    /**
     * Acquire the lock in a reactive way.
     *
     * @return Completion when lock is acquired.
     */
    public Completable lock() {
        return Completable.create(
            emitter -> {
                this.acquires.add(emitter);
                this.tryToEmitNext();
            }
        );
    }

    /**
     * Unlock the lock.
     */
    public void unlock() {
        synchronized (this.sync) {
            if (this.locked.compareAndSet(true, false)) {
                this.tryToEmitNext();
            } else {
                throw new IllegalStateException("Attempt to unlock non-locked lock");
            }
        }
    }

    /**
     * Give a lock to another awaiter.
     */
    private void tryToEmitNext() {
        synchronized (this.sync) {
            if (!this.locked.get()) {
                final CompletableEmitter emitter = this.acquires.poll();
                if (emitter != null) {
                    this.locked.set(true);
                    emitter.onComplete();
                }
            }
        }
    }
}
