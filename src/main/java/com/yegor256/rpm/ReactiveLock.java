package com.yegor256.rpm;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableEmitter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reactive sync primitive.
 *
 * @since 0.1
 */
public class ReactiveLock {

    /**
     * Current state of the lock.
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
        return Completable.create(emitter -> {
            this.acquires.add(emitter);
            this.tryToEmitNext();
        });
    }

    /**
     * Unlock the lock.
     */
    public synchronized void unlock() {
        if (this.locked.compareAndSet(true, false)) {
            this.tryToEmitNext();
        }
    }

    /**
     * Give a lock to another awaiter.
     */
    private synchronized void tryToEmitNext() {
        if (!this.locked.get()) {
            final CompletableEmitter emitter = this.acquires.poll();
            if (emitter != null) {
                this.locked.set(true);
                emitter.onComplete();
            }
        }
    }
}
