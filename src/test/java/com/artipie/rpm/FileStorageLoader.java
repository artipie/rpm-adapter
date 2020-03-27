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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.IOUtils;

/**
 * Utility class to load resource from classpath to Storage.
 *
 * @since 0.4
 */
public final class FileStorageLoader {
    /**
     * Ctor.
     */
    private FileStorageLoader() {
    }

    /**
     * Uploads resource from classpath to Storage.
     * @param storage The storage
     * @param key Resource name and key to upload
     * @throws IOException when resource could not read
     * @throws ExecutionException when upload failed
     * @throws InterruptedException when upload interrupted
     */
    public static void uploadResource(
        final Storage storage,
        final String key) throws IOException, ExecutionException, InterruptedException {
        final byte[] data = IOUtils.toByteArray(
            FileStorageLoader.class.getResourceAsStream(
                String.format("/%s", key)
            )
        );
        storage.save(
            new Key.From(key),
            new Content.From(data)
        ).get();
    }
}
