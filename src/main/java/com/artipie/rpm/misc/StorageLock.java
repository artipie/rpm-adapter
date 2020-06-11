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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Locker for the storage.
 * @since 0.9
 * @todo #230:30min Consider waiting for the lock to be released instead of throwing an exception.
 *  This feature implementation should be properly discussed and planed first.
 */
public final class StorageLock {

    /**
     * Lock file name.
     */
    private static final String FNAME = "lock-%s";

    /**
     * Lock file name pattern.
     */
    private static final String PTRN = "lock-[a-z0-9-]{36}";

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
     * @return CompletableFuture
     * @throws IllegalStateException If storage is already locked
     */
    public CompletableFuture<Boolean> lock() {
        return this.countLocks().thenCompose(
            count -> {
                if (count == 0) {
                    return this.storage.save(this.file, new Content.From(new byte[]{}))
                        .<Boolean>thenCompose(
                            ignored -> this.countLocks().<Boolean>thenCompose(
                                cnt -> {
                                    final CompletableFuture<Boolean> res;
                                    if (cnt > 1) {
                                        res = this.release().<Boolean>thenApply(
                                            nothing -> {
                                                throw new StorageIsLockedException(
                                                    this.repo.string(), cnt
                                                );
                                            }
                                        );
                                    } else {
                                        res = CompletableFuture.completedFuture(true);
                                    }
                                    return res;
                                }
                            )
                        );
                } else {
                    throw new StorageIsLockedException(
                        this.repo.string(), count
                    );
                }
            }
        );
    }

    /**
     * Releases lock by removing the lock file.
     * @return CompletableFuture
     */
    public CompletableFuture<Boolean> release() {
        return this.storage.exists(this.file).thenCompose(
            exists -> {
                final CompletableFuture<Boolean> res;
                if (exists) {
                    res = this.storage.delete(this.file).thenApply(ignored -> true);
                } else {
                    res = CompletableFuture.completedFuture(true);
                }
                return res;
            }
        );
    }

    /**
     * Counts locks files.
     * @return Count on completion
     */
    private CompletableFuture<Long> countLocks() {
        final String match;
        if (this.repo.string().isEmpty()) {
            match = StorageLock.PTRN;
        } else {
            match = String.join("/", this.repo.string(), StorageLock.PTRN);
        }
        return this.storage.list(this.repo).thenApply(
            keys -> keys.stream().filter(
                key -> key.string().matches(match)
            ).count()
        );
    }

}
