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
package com.artipie.rpm.files;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.cactoos.Scalar;

/**
 * Unpacks given gzip to the temp dir.
 * @since 0.8
 */
public final class UnpackGzip implements Scalar<Path> {

    /**
     * Path to gzip.
     */
    private final Path gzip;

    /**
     * Ctor.
     * @param gzip Path
     */
    public UnpackGzip(final Path gzip) {
        this.gzip = gzip;
    }

    @Override
    @SuppressWarnings("PMD.AssignmentInOperand")
    public Path value() throws IOException {
        final Path res = Files.createTempDirectory("unpack");
        res.toFile().mkdir();
        final GzipCompressorInputStream input =
            new GzipCompressorInputStream(Files.newInputStream(this.gzip));
        try (TarArchiveInputStream tar = new TarArchiveInputStream(input)) {
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tar.getNextEntry()) != null) {
                final File next = res.resolve(entry.getName()).toFile();
                if (entry.isDirectory()) {
                    next.mkdirs();
                } else {
                    try (OutputStream out = Files.newOutputStream(next.toPath())) {
                        IOUtils.copy(tar, out);
                    }
                }
            }
        }
        return res;
    }
}
