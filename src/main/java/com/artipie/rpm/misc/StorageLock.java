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
import java.util.function.Predicate;

/**
 * Locker for the storage.
 * @since 0.9
 * @todo #230:30min Use this class in RPM update to prohibit parallel update of the same repository:
 *  add lock before starting the update and release it on terminate.
 */
public final class StorageLock {

    /**
     * Lock file name.
     */
    private static final String FNAME = "lock-%s";

    /**
     * Lock file name pattern.
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
     * Lock file key.
     */
    private final Key file;

    /**
     * Ctor.
     * @param storage Storage
     * @param repo Repo key
     */
    public StorageLock(final Storage storage, final Key repo) {
        this.storage = storage;
        this.repo = repo;
        this.file = new Key.From(
            this.repo, String.format(StorageLock.FNAME, UUID.randomUUID().toString())
        );
    }

    /**
     * Locks storage by adding random file to it.
     * @throws IllegalStateException On error and if storage is already locked
     */
    void lock() {
        final BlockingStorage bsto = new BlockingStorage(this.storage);
        try {
            if (this.noLocks(bsto)) {
                bsto.save(this.file, new byte[]{});
                if (bsto.list(this.repo).stream().filter(this.lockKeyPredicate()).count() > 1) {
                    this.release();
                    throw this.error();
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
        try {
            new BlockingStorage(this.storage).delete(this.file);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Checks is storage already has a lock.
     * @param bsto Blocking storage
     * @return True is there is no lock
     * @throws InterruptedException Or error
     */
    private boolean noLocks(final BlockingStorage bsto) throws InterruptedException {
        return bsto.list(this.repo).stream().noneMatch(this.lockKeyPredicate());
    }

    /**
     * Lock key predicate.
     * @return Predicate for key
     */
    private Predicate<Key> lockKeyPredicate() {
        return key -> key.string().matches(String.format(StorageLock.PTRN, this.repo.string()));
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
