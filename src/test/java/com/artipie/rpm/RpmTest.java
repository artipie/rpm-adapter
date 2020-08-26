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
package com.artipie.rpm;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.rpm.hm.StorageHasMetadata;
import io.reactivex.Completable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiFunction;
import org.cactoos.Scalar;
import org.cactoos.list.ListOf;
import org.cactoos.list.Mapped;
import org.cactoos.scalar.AndInThreads;
import org.cactoos.scalar.Unchecked;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.llorllale.cactoos.matchers.IsTrue;

/**
 * Unit tests for {@link Rpm}.
 *
 * @since 0.9
 * @todo #110:30min Meaningful error on broken package.
 *  Rpm should throw an exception when trying to add an invalid package.
 *  The type of exception must be IllegalArgumentException and its message
 *  "Reading of RPM package 'package' failed, data corrupt or malformed.",
 *  like described in showMeaningfulErrorWhenInvalidPackageSent. Implement it
 *  and then enable the test.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle IllegalCatchCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
final class RpmTest {

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    static Path tmp;

    @Test
    void updatesDifferentReposSimultaneouslyTwice() throws Exception {
        final Storage storage = new InMemoryStorage();
        final boolean filelists = true;
        final Rpm repo =  new Rpm(
            storage, StandardNamingPolicy.SHA1, Digest.SHA256, filelists
        );
        final List<String> keys = new ListOf<>("one", "two", "three");
        final CountDownLatch latch = new CountDownLatch(keys.size());
        final List<Scalar<Boolean>> tasks = new Mapped<>(
            key -> new Unchecked<>(
                () -> {
                    new TestRpm.Multiple(
                        new TestRpm.Abc(),
                        new TestRpm.Libdeflt()
                    ).put(new SubStorage(new Key.From(key), storage));
                    latch.countDown();
                    latch.await();
                    repo.batchUpdate(new Key.From(key)).blockingAwait();
                    return true;
                }
            ),
            keys
        );
        new AndInThreads(tasks).value();
        new AndInThreads(tasks).value();
        keys.forEach(
            key -> {
                final Key res = new Key.From(key, "repodata");
                RpmTest.repomdIsPresent(storage, res);
                MatcherAssert.assertThat(
                    new SubStorage(new Key.From(key), storage),
                    new StorageHasMetadata(2, filelists, RpmTest.tmp)
                );
            }
        );
    }

    @Test
    void incrementalUpdateWorksOnNewRepo() throws IOException {
        final Storage storage = new InMemoryStorage();
        final boolean filelists = true;
        new TestRpm.Multiple(
            new TestRpm.Abc(), new TestRpm.Libdeflt(), new TestRpm.Time()
        ).put(storage);
        new Rpm(storage, StandardNamingPolicy.SHA1, Digest.SHA256, filelists)
            .batchUpdateIncrementally(Key.ROOT).blockingAwait();
        MatcherAssert.assertThat(
            storage,
            new StorageHasMetadata(3, filelists, RpmTest.tmp)
        );
    }

    @Test
    void doesntBrakeMetadataWhenInvalidPackageSent() throws Exception {
        final Storage storage = new InMemoryStorage();
        final boolean filelists = true;
        final Rpm repo =  new Rpm(storage, StandardNamingPolicy.SHA1, Digest.SHA256, filelists);
        new TestRpm.Abc().put(storage);
        repo.batchUpdate(Key.ROOT).blockingAwait();
        new TestRpm.Multiple(new TestRpm.Invalid(), new TestRpm.Libdeflt()).put(storage);
        repo.batchUpdate(Key.ROOT).blockingAwait();
        MatcherAssert.assertThat(
            storage,
            new StorageHasMetadata(2, filelists, RpmTest.tmp)
        );
    }

    @Test
    void doesntBrakeMetadataWhenInvalidPackageSentOnIncrementalUpdate() throws Exception {
        final Storage storage = new InMemoryStorage();
        final boolean filelists = true;
        final Rpm repo =  new Rpm(storage, StandardNamingPolicy.SHA1, Digest.SHA256, filelists);
        new TestRpm.Libdeflt().put(storage);
        repo.batchUpdate(Key.ROOT).blockingAwait();
        new TestRpm.Multiple(new TestRpm.Abc(), new TestRpm.Invalid()).put(storage);
        repo.batchUpdateIncrementally(Key.ROOT).blockingAwait();
        MatcherAssert.assertThat(
            storage,
            new StorageHasMetadata(2, filelists, RpmTest.tmp)
        );
    }

    @Test
    @Disabled
    void showMeaningfulErrorWhenInvalidPackageSent() throws Exception {
        final Storage storage = new InMemoryStorage();
        final Rpm repo =  new Rpm(
            storage, StandardNamingPolicy.SHA1, Digest.SHA256, true
        );
        new TestRpm.Multiple(
            new TestRpm.Abc(),
            new TestRpm.Libdeflt()
        ).put(storage);
        repo.batchUpdate(Key.ROOT).blockingAwait();
        new TestRpm.Invalid().put(storage);
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> repo.batchUpdate(Key.ROOT).blockingAwait(),
            "Reading of RPM package \"brokentwo.rpm\" failed, data corrupt or malformed."
        );
    }

    @RepeatedTest(10)
    void throwsExceptionWhenFullUpdatesDoneSimultaneously() throws Exception {
        this.testSimultaneousActions(Rpm::batchUpdate);
    }

    @RepeatedTest(10)
    void throwsExceptionWhenIncrementalUpdatesDoneSimultaneously() throws Exception {
        this.testSimultaneousActions(Rpm::batchUpdateIncrementally);
    }

    private void testSimultaneousActions(
        final BiFunction<Rpm, Key, Completable> action
    ) throws IOException {
        final Storage storage = new InMemoryStorage();
        final Rpm repo =  new Rpm(
            storage, StandardNamingPolicy.SHA1, Digest.SHA256, true
        );
        final List<Key> keys = Collections.nCopies(3, Key.ROOT);
        final CountDownLatch latch = new CountDownLatch(keys.size());
        new TestRpm.Multiple(
            new TestRpm.Abc(),
            new TestRpm.Libdeflt()
        ).put(storage);
        final List<CompletableFuture<Void>> tasks = new ArrayList<>(keys.size());
        for (final Key key : keys) {
            final CompletableFuture<Void> future = new CompletableFuture<>();
            tasks.add(future);
            new Thread(
                () -> {
                    try {
                        latch.countDown();
                        latch.await();
                        action.apply(repo, key).blockingAwait();
                        future.complete(null);
                    } catch (final Exception exception) {
                        future.completeExceptionally(exception);
                    }
                }
            ).start();
        }
        for (final CompletableFuture<Void> task : tasks) {
            try {
                task.join();
            } catch (final Exception ignored) {
            }
        }
        MatcherAssert.assertThat(
            "Some updates failed",
            tasks.stream().anyMatch(CompletableFuture::isCompletedExceptionally),
            new IsTrue()
        );
        MatcherAssert.assertThat(
            "Storage has no locks",
            storage.list(Key.ROOT).join().stream().noneMatch(key -> key.string().contains("lock")),
            new IsEqual<>(true)
        );
    }

    /**
     * Checks that repomd is present.
     * @param storage Storage
     * @param key Key
     */
    private static void repomdIsPresent(final Storage storage, final Key key) {
        MatcherAssert.assertThat(
            "Repomd is present",
            storage.list(key).join().stream()
                .map(Key::string).filter(str -> str.contains("repomd")).count(),
            new IsEqual<>(1L)
        );
    }
}
