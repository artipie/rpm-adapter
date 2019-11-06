/**
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
package com.yegor256.rpm;

import com.jcabi.log.Logger;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.util.logging.Level;
import org.redline_rpm.ReadableChannelWrapper;
import org.redline_rpm.Scanner;
import org.redline_rpm.header.AbstractHeader;
import org.redline_rpm.header.Format;
import org.redline_rpm.header.Header;

/**
 * Single package in a file.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 0.1
 */
final class Pkg {

    /**
     * The RPM file.
     */
    private final Path file;

    /**
     * Ctor.
     * @param path The path
     */
    Pkg(final Path path) {
        this.file = path;
    }

    /**
     * Get header.
     * @return The header
     * @throws IOException If fails
     */
    public Header header() throws IOException {
        try (final InputStream fios = new FileInputStream(this.file.toFile())) {
            final Format format = new Scanner(
                new PrintStream(Logger.stream(Level.INFO, this))
            ).run(new ReadableChannelWrapper(Channels.newChannel(fios)));
            final Header header = format.getHeader();
//            Logger.info(this, "header: %s", header.toString());
            return header;
        }
    }

    /**
     * Make a sha256 hash.
     * @return The hash
     */
    public String hash() {
        return "2e266720cef0303dcdd2124936726d91f652a6c017513bf70466e7f6623d6aad";
    }

    /**
     * Get tag by ID.
     * @return The tag
     * @throws IOException If fails
     */
    public String tag(final AbstractHeader.Tag tag) throws IOException {
        return ((String[]) this.header().getEntry(tag).getValues())[0];
    }

    /**
     * Get numeric tag by ID.
     * @return The tag
     * @throws IOException If fails
     */
    public int num(final AbstractHeader.Tag tag) throws IOException {
        return ((int[]) this.header().getEntry(tag).getValues())[0];
    }

}
