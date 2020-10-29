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

import com.artipie.asto.Copy;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.rpm.files.Gzip;
import com.artipie.rpm.hm.StorageHasMetadata;
import com.artipie.rpm.hm.StorageHasRepoMd;
import io.reactivex.Completable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.cactoos.Scalar;
import org.cactoos.list.ListOf;
import org.cactoos.list.Mapped;
import org.cactoos.scalar.AndInThreads;
import org.cactoos.scalar.Unchecked;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.llorllale.cactoos.matchers.IsTrue;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

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
 * @checkstyle ClassFanOutComplexityCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
final class RpmTest {

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    static Path tmp;

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Test config.
     */
    private RepoConfig config;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.config = new RepoConfig.Simple(Digest.SHA256, StandardNamingPolicy.SHA1, true);
    }

    @ParameterizedTest
    @EnumSource(UpdateType.class)
    void updatesDifferentReposSimultaneouslyTwice(final UpdateType update) throws Exception {
        final Rpm repo =  new Rpm(this.storage, this.config);
        final List<String> keys = new ListOf<>("one", "two", "three");
        final CountDownLatch latch = new CountDownLatch(keys.size());
        final List<Scalar<Boolean>> tasks = new Mapped<>(
            key -> new Unchecked<>(
                () -> {
                    new TestRpm.Multiple(
                        new TestRpm.Abc(),
                        new TestRpm.Libdeflt()
                    ).put(new SubStorage(new Key.From(key), this.storage));
                    latch.countDown();
                    latch.await();
                    update.action.apply(repo, new Key.From(key)).blockingAwait();
                    return true;
                }
            ),
            keys
        );
        new AndInThreads(tasks).value();
        new AndInThreads(tasks).value();
        keys.forEach(
            key -> MatcherAssert.assertThat(
                new SubStorage(new Key.From(key), this.storage),
                Matchers.allOf(
                    new StorageHasRepoMd(this.config),
                    new StorageHasMetadata(2, this.config.filelists(), RpmTest.tmp)
                )
            )
        );
    }

    @ParameterizedTest
    @EnumSource(UpdateType.class)
    void updateWorksOnNewRepo(final UpdateType update) throws IOException {
        new TestRpm.Multiple(
            new TestRpm.Abc(), new TestRpm.Libdeflt(), new TestRpm.Time()
        ).put(this.storage);
        update.action.apply(new Rpm(this.storage, this.config), Key.ROOT).blockingAwait();
        MatcherAssert.assertThat(
            this.storage,
            Matchers.allOf(
                new StorageHasMetadata(3, this.config.filelists(), RpmTest.tmp),
                new StorageHasRepoMd(this.config)
            )
        );
    }

    @ParameterizedTest
    @EnumSource(UpdateType.class)
    @DisabledOnOs(OS.WINDOWS)
    void doesNotTouchMetadataIfInvalidRpmIsSent(final UpdateType update) throws Exception {
        final RepoConfig cnfg =
            new RepoConfig.Simple(Digest.SHA256, StandardNamingPolicy.PLAIN, true);
        final Rpm repo = new Rpm(this.storage, cnfg);
        new TestRpm.Multiple(new TestRpm.Abc(), new TestRpm.Libdeflt()).put(this.storage);
        update.action.apply(repo, Key.ROOT).blockingAwait();
        final Storage stash = new InMemoryStorage();
        new Copy(this.storage, new ListOf<>(this.storage.list(new Key.From("repodata")).join()))
            .copy(stash).join();
        new TestRpm.Invalid().put(this.storage);
        update.action.apply(repo, Key.ROOT).blockingAwait();
        for (
            final Key key : stash.list(Key.ROOT).join().stream()
            .filter(key -> key.string().endsWith("gz")).collect(Collectors.toList())
        ) {
            final Path first = Files.createTempFile(
                RpmTest.tmp, new KeyLastPart(key).get(), ".first"
            );
            final Path second = Files.createTempFile(
                RpmTest.tmp, new KeyLastPart(key).get(), ".second"
            );
            final Path gzip = Files.createTempFile(
                RpmTest.tmp, new KeyLastPart(key).get(), ".gzip"
            );
            Files.write(gzip, new BlockingStorage(this.storage).value(key));
            new Gzip(gzip).unpack(first);
            Files.write(gzip, new BlockingStorage(stash).value(key));
            new Gzip(gzip).unpack(second);
            MatcherAssert.assertThat(
                String.format("%s xmls are equal", key.string()),
                Files.readAllBytes(first),
                CompareMatcher.isIdenticalTo(second.toFile())
                    .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
                    .ignoreWhitespace().normalizeWhitespace()
            );
        }
        MatcherAssert.assertThat(
            "Repomd is valid",
            this.storage,
            new StorageHasRepoMd(cnfg)
        );
    }

    @ParameterizedTest
    @EnumSource(UpdateType.class)
    void skipsInvalidPackageOnUpdate(final UpdateType update) throws Exception {
        final Rpm repo =  new Rpm(this.storage, this.config);
        new TestRpm.Abc().put(this.storage);
        update.action.apply(repo, Key.ROOT).blockingAwait();
        new TestRpm.Multiple(new TestRpm.Invalid(), new TestRpm.Libdeflt()).put(this.storage);
        update.action.apply(repo, Key.ROOT).blockingAwait();
        MatcherAssert.assertThat(
            this.storage,
            Matchers.allOf(
                new StorageHasMetadata(2, true, RpmTest.tmp),
                new StorageHasRepoMd(this.config)
            )
        );
    }

    @Test
    @Disabled
    void showMeaningfulErrorWhenInvalidPackageSent() throws Exception {
        final Rpm repo = new Rpm(
            this.storage, StandardNamingPolicy.SHA1, Digest.SHA256, true
        );
        new TestRpm.Multiple(
            new TestRpm.Abc(),
            new TestRpm.Libdeflt()
        ).put(this.storage);
        repo.batchUpdate(Key.ROOT).blockingAwait();
        new TestRpm.Invalid().put(this.storage);
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> repo.batchUpdate(Key.ROOT).blockingAwait(),
            "Reading of RPM package \"brokentwo.rpm\" failed, data corrupt or malformed."
        );
    }

    @ParameterizedTest
    @EnumSource(UpdateType.class)
    void throwsExceptionWhenFullUpdatesDoneSimultaneously(final UpdateType type)
        throws IOException {
        final Rpm repo =  new Rpm(
            this.storage, StandardNamingPolicy.SHA1, Digest.SHA256, true
        );
        final List<Key> keys = Collections.nCopies(3, Key.ROOT);
        final CountDownLatch latch = new CountDownLatch(keys.size());
        new TestRpm.Multiple(
            new TestRpm.Abc(),
            new TestRpm.Libdeflt()
        ).put(this.storage);
        final List<CompletableFuture<Void>> tasks = new ArrayList<>(keys.size());
        for (final Key key : keys) {
            final CompletableFuture<Void> future = new CompletableFuture<>();
            tasks.add(future);
            new Thread(
                () -> {
                    try {
                        latch.countDown();
                        latch.await();
                        type.action.apply(repo, key).blockingAwait();
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
            this.storage.list(Key.ROOT).join().stream()
                .noneMatch(key -> key.string().contains("lock")),
            new IsEqual<>(true)
        );
    }

    /**
     * Update types.
     * @since 1.3
     */
    enum UpdateType {

        /**
         * Incremental update.
         */
        INCREMENTAL(Rpm::batchUpdateIncrementally),

        /**
         * Non incremental update.
         */
        NON_INCREMENTAL(Rpm::batchUpdate);

        /**
         * Update action.
         */
        private final BiFunction<Rpm, Key, Completable> action;

        /**
         * Ctor.
         * @param action Action
         */
        UpdateType(final BiFunction<Rpm, Key, Completable> action) {
            this.action = action;
        }
    }
}
