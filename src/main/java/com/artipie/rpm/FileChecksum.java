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

import com.artipie.rpm.pkg.Checksum;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Locale;
import javax.xml.bind.DatatypeConverter;

/**
 * Hashing sum of a file.
 *
 * @since 0.1
 */
public final class FileChecksum implements Checksum {

    /**
     * Default file buffer is 8K.
     */
    private static final int BUF_SIZE = 1024 * 8;

    /**
     * The XML.
     */
    private final Path file;

    /**
     * Message digest.
     */
    private final Digest dgst;

    /**
     * Ctor.
     * @param path The path
     * @param dgst The hashing algorithm for checksum computation
     */
    public FileChecksum(final Path path, final Digest dgst) {
        this.file = path;
        this.dgst = dgst;
    }

    @Override
    public Digest digest() {
        return this.dgst;
    }

    @Override
    public String hex() throws IOException {
        final MessageDigest digest = this.dgst.messageDigest();
        try (FileChannel chan = FileChannel.open(this.file, StandardOpenOption.READ)) {
            final ByteBuffer buf = ByteBuffer.allocateDirect(FileChecksum.BUF_SIZE);
            while (chan.read(buf) > 0) {
                buf.flip();
                digest.update(buf);
                buf.clear();
            }
        }
        return DatatypeConverter.printHexBinary(digest.digest())
            .toLowerCase(Locale.US);
    }
}
