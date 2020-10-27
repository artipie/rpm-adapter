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
package com.artipie.rpm.hm;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.rpm.Digest;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.StandardNamingPolicy;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.llorllale.cactoos.matchers.Assertion;

/**
 * Tests for {@link StorageHasRepoMd} matcher.
 *
 * @since 1.1
 */
public final class StorageHasRepoMdTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void matchPositive(final boolean filelists) {
        final Storage storage = new InMemoryStorage();
        new TestResource(
            "repodata/StorageHasRepoMdTest/repomd.xml"
        ).saveTo(storage, new Key.From("repodata/repomd.xml"));
        new TestResource(
            "repodata/StorageHasRepoMdTest/primary.xml.gz"
        ).saveTo(storage, new Key.From("repodata/primary.xml.gz"));
        new TestResource(
            "repodata/StorageHasRepoMdTest/other.xml.gz"
        ).saveTo(storage, new Key.From("repodata/other.xml.gz"));
        if (filelists) {
            new TestResource(
                "repodata/StorageHasRepoMdTest/filelists.xml.gz"
            ).saveTo(storage, new Key.From("repodata/filelists.xml.gz"));
        }
        new Assertion<>(
            "The matcher gives positive result for a valid repomd.xml configuration",
            storage,
            new StorageHasRepoMd(
                new RepoConfig.Simple(Digest.SHA256, StandardNamingPolicy.PLAIN, filelists)
            )
        ).affirm();
    }

    @Test
    public void doNotMatchesWhenRepomdAbsent() {
        new Assertion<>(
            "The matcher gives a negative result when storage does not have repomd.xml",
            new InMemoryStorage(),
            new IsNot<>(new StorageHasRepoMd(new RepoConfig.Simple()))
        ).affirm();
    }
}
