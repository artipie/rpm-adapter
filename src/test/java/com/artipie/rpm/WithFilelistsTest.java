/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Yegor Bugayenko
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
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.fs.RxFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link WithFilelists}.
 *
 * @since 0.3.3
 * @checkstyle ClassDataAbstractionCouplingCheck (100 lines)
 */
public class WithFilelistsTest {
    @Test
    @Disabled
    public void createFileListOnRpmUpdate(
        @TempDir final Path folder, @TempDir final Path store
    ) throws Exception {
        final String key = "with-fileList.aarch64.rpm";
        final Storage storage = WithFilelistsTest.save(folder, store, key);
        new WithFilelists(new Rpm.Base(storage)).update(key).blockingAwait();
        MatcherAssert.assertThat(
            storage.list(new Key.From("repodata"))
                .get().stream().map(Key::string).collect(Collectors.toList()),
            new IsIterableContainingInAnyOrder<>(
                new ListOf<Matcher<? super String>>(
                    new IsEqual<String>("repodata/filelists.xml"),
                    new IsEqual<String>("repodata/filelists.xml.gz")
                )
            )
        );
    }

    private static Storage save(final Path folder, final Path store, final String key)
        throws IOException, InterruptedException, ExecutionException {
        final Storage storage = new FileStorage(store);
        final Path bin = folder.resolve("x.rpm");
        Files.copy(
            RpmITCase.class.getResourceAsStream(
                String.format("/%s", key)
            ),
            bin,
            StandardCopyOption.REPLACE_EXISTING
        );
        storage.save(new Key.From(key), new RxFile(bin).flow()).get();
        return storage;
    }
}
