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
import com.artipie.asto.memory.InMemoryStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test {@link StorageLocker}.
 * @since 0.9
 */
class StorageLockerTest {

    /**
     * Lock file name.
     */
    private static final String LOCK = "lock";

    @Test
    void addsLockFile() {
        final Storage sto = new InMemoryStorage();
        final StorageLocker lock = new StorageLocker(sto, Key.ROOT);
        lock.lock();
        MatcherAssert.assertThat(
            sto.list(Key.ROOT).join().stream()
                .allMatch(key -> key.string().startsWith(StorageLockerTest.LOCK)),
            new IsEqual<>(true)
        );
    }

    @Test
    void deletesLockFile() {
        final Storage sto = new InMemoryStorage();
        final Key.From key = new Key.From("one");
        final StorageLocker lock = new StorageLocker(sto, key);
        lock.lock();
        lock.release();
        MatcherAssert.assertThat(
            sto.list(key).join().size(),
            new IsEqual<>(0)
        );
    }

    @Test
    void addsLockAndThrowsExceptionOnSecondLock() {
        final Storage sto = new InMemoryStorage();
        final Key.From key = new Key.From("two");
        final StorageLocker locker = new StorageLocker(sto, key);
        locker.lock();
        Assertions.assertThrows(
            IllegalStateException.class,
            locker::lock
        );
        MatcherAssert.assertThat(
            "One lock file added",
            sto.list(key).join().size(),
            new IsEqual<>(1)
        );
    }

}
