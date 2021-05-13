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
package com.artipie.rpm.pkg;

import com.jcabi.log.Logger;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import org.redline_rpm.ReadableChannelWrapper;
import org.redline_rpm.Scanner;
import org.redline_rpm.header.Format;
import org.redline_rpm.header.Header;

/**
 * Header of RPM package file.
 *
 * @since 0.10
 */
public final class FilePackageHeader {

    /**
     * The RPM file.
     */
    private final Path file;

    /**
     * Ctor.
     *
     * @param file The RPM file.
     */
    public FilePackageHeader(final Path file) {
        this.file = file;
    }

    /**
     * Get header.
     *
     * @return The header.
     * @throws InvalidPackageException In case package is invalid.
     * @throws IOException In case of I/O error.
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Header header() throws InvalidPackageException, IOException {
        try (FileChannel chan = FileChannel.open(this.file, StandardOpenOption.READ)) {
            final Format format;
            try {
                format = new Scanner(
                    new PrintStream(Logger.stream(Level.FINE, this))
                ).run(new ReadableChannelWrapper(chan));
                // @checkstyle IllegalCatchCheck (1 line)
            } catch (final RuntimeException ex) {
                throw new InvalidPackageException(ex);
            }
            final Header header = format.getHeader();
            Logger.debug(this, "header: %s", header.toString());
            return header;
        }
    }
}
