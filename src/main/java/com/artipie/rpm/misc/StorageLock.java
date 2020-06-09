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
 * @todo #230:30min Use this class in RPM update to prohibit parallel update of the same repository:
 *  add lock before starting the update and release it on terminate.
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
    private static final String PTRN = "%s/lock-[a-z0-9-]{36}";

    /**
     * Error message.
     */
    private static final String ERROR = "Repository %s is already being updated!";

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
    public CompletableFuture<Void> lock() {
        return this.countLocks().thenCompose(
            count -> {
                if (count == 0) {
                    return this.storage.save(this.file, new Content.From(new byte[]{}))
                        .<Void>thenCompose(
                            ignored -> this.countLocks().<Void>thenCompose(
                                cnt -> {
                                    final CompletableFuture<Void> res;
                                    if (cnt > 1) {
                                        res = this.release().<Void>thenApply(
                                            nothing -> {
                                                throw new IllegalStateException(
                                                    String.format(
                                                        StorageLock.ERROR, this.repo.string()
                                                    )
                                                );
                                            }
                                        );
                                    } else {
                                        res = CompletableFuture.completedFuture(null);
                                    }
                                    return res;
                                }
                            )
                        );
                } else {
                    throw new IllegalStateException(
                        String.format(StorageLock.ERROR, this.repo.string())
                    );
                }
            }
        );
    }

    /**
     * Releases lock by removing the lock file.
     * @return CompletableFuture
     */
    public CompletableFuture<Void> release() {
        return this.storage.delete(this.file);
    }

    /**
     * Counts locks files.
     * @return Count on completion
     */
    private CompletableFuture<Long> countLocks() {
        return this.storage.list(this.repo).thenApply(
            keys -> keys.stream().filter(
                key -> key.string().matches(String.format(StorageLock.PTRN, this.repo.string()))
            ).count()
        );
    }

}
