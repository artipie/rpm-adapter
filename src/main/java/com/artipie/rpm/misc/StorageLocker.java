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
package com.artipie.rpm.misc;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import java.util.UUID;

/**
 * Locker for the storage.
 * @since 0.9
 * @todo #230:30min Use this class in RPM update to prohibit parallel update of the same repository:
 *  add lock before starting the update and release it on terminate.
 */
public class StorageLocker {

    /**
     * Lock directory name.
     */
    private static final String FNAME = "lock-%s";

    /**
     * Lock directory name.
     */
    private static final String PTRN = "%s/lock-[a-z0-9-]{36}";

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Repository key.
     */
    private final Key repo;

    /**
     * Lock file name.
     */
    private final String guid;

    /**
     * Ctor.
     * @param storage Storage
     * @param repo Repo key
     */
    public StorageLocker(final Storage storage, final Key repo) {
        this.storage = storage;
        this.repo = repo;
        this.guid = UUID.randomUUID().toString();
    }

    /**
     * Locks storage by adding random file to it.
     * @throws IllegalStateException On error and if storage is already locked
     */
    void lock() {
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        try {
            if (this.noLocks(bsto)) {
                synchronized (this.storage) {
                    if (this.noLocks(bsto)) {
                        bsto.save(
                            new Key.From(this.repo, String.format(StorageLocker.FNAME, this.guid)),
                            new byte[]{}
                        );
                    } else {
                        throw this.error();
                    }
                }
            } else {
                throw this.error();
            }
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Releases lock by removing the lock file.
     */
    void release() {
        synchronized (this.storage) {
            try {
                new BlockingStorage(this.storage)
                    .delete(new Key.From(this.repo, String.format(StorageLocker.FNAME, this.guid)));
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        }
    }

    /**
     * Checks is storage already has lock.
     * @param bsto Blocking storage
     * @return True is there is no lock
     * @throws InterruptedException Or error
     */
    private boolean noLocks(final BlockingStorage bsto) throws InterruptedException {
        return bsto.list(this.repo).stream().noneMatch(
            key -> key.string().matches(String.format(StorageLocker.PTRN, this.repo.string()))
        );
    }

    /**
     * Returns error.
     * @return Exception
     */
    private IllegalStateException error() {
        return new IllegalStateException(
            String.format("Repository %s is already being updated!", this.repo.string())
        );
    }

}
