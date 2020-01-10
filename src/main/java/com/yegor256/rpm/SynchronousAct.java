package com.yegor256.rpm;

import io.reactivex.rxjava3.core.Completable;
import java.nio.file.Path;

/**
 * Synchronous act wrapper.
 *
 * @since 0.1
 */
public class SynchronousAct implements Repomd.Act {

    /**
     * The lock to synchronize on.
     */
    private final ReactiveLock lock;

    /**
     * Wrapped act.
     */
    private final Repomd.Act act;

    /**
     * Create an act with synchronization on a lock.
     *
     * @param act      Act to synchronize.
     * @param lock The lock to sync on.
     */
    public SynchronousAct(final Repomd.Act act, final ReactiveLock lock) {
        this.act = act;
        this.lock = lock;
    }

    @Override
    public Completable update(final Path file) {
        return this.lock.lock()
            .andThen(this.act.update(file))
            .doOnTerminate(this.lock::unlock);
    }
}
