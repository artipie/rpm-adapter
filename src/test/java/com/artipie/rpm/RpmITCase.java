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
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import io.reactivex.Observable;
import io.vertx.reactivex.core.Vertx;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for {@link Rpm}.
 * @since 0.6
 * @todo #69:30min I've checked that this test generates metadata correctly,
 *  but we need to automate it. Let's check all metadata files after
 *  `Rpm.batchUpdate()` using xpath matchers. The files to check:
 *  primary.xml, others.xml, filelists.xml, repomd.xml.
 *  These files are stored in storage at path: `repomd/SHA1-TYPE.xml.gz`,
 *  where SHA1 is a HEX from SHA1 of file content and TYPE is a type of file
 *  (primary, others, filelists). Don't forget to uncompress it first.
 *  repomd.xml is not compressed and stored at `repodata/repomd.xml`.
 * @todo #69:30min This test is failing on Windows build. It's failing because of error
 *  FileAlreadyExistException in PrimaryXml.close in Files.move, but it's using
 *  REPLACE_EXISTING options, so it should not fail and it's not failing on Linux.
 *  Let's discover the problem, fix it, and remove DisabledOnOs(OS.WINDOWS) annotation.
 */
@DisabledOnOs(OS.WINDOWS)
final class RpmITCase {

    /**
     * VertX closeable instance.
     */
    private Vertx vertx;

    @BeforeEach
    void setUp() {
        this.vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() {
        this.vertx.close();
    }

    @Test
    void generatesMetadata(@TempDir final Path tmp) {
        final Storage storage = new FileStorage(tmp, this.vertx.fileSystem());
        Observable.fromArray(
            "aom-1.0.0-8.20190810git9666276.el8.aarch64.rpm",
            "nginx-1.16.1-1.el8.ngx.x86_64.rpm"
        ).flatMapCompletable(
            rpm -> new RxStorageWrapper(storage).save(new Key.From(rpm), new TestContent(rpm))
        ).blockingAwait();
        new Rpm(storage, StandardNamingPolicy.SHA1, Digest.SHA256)
            .batchUpdate(Key.ROOT)
            .blockingAwait();
    }
}
