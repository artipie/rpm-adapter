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
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.util.logging.Level;
import org.cactoos.Scalar;
import org.cactoos.scalar.Sticky;
import org.cactoos.scalar.Unchecked;
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
     * The header.
     */
    private final Unchecked<Header> hdr;

    /**
     * Ctor.
     * @param path The path
     */
    Pkg(final Path path) {
        this.file = path;
        this.hdr = new Unchecked<>(
            new Sticky<>(
                new Scalar<Header>() {
                    @Override
                    public Header value() throws Exception {
                        try (final InputStream fios =
                            new FileInputStream(Pkg.this.file.toFile())) {
                            final Format format = new Scanner(
                                new PrintStream(Logger.stream(Level.INFO, this))
                            // @checkstyle LineLength (1 line)
                            ).run(new ReadableChannelWrapper(Channels.newChannel(fios)));
                            final Header header = format.getHeader();
                            Logger.debug(this, "header: %s", header.toString());
                            return header;
                        }
                    }
                }
            )
        );
    }

    /**
     * Get path.
     * @return Path
     */
    public Path path() {
        return this.file;
    }

    /**
     * Get header.
     * @return The header
     */
    public Header header() {
        return this.hdr.value();
    }

    /**
     * Get tag by ID.
     * @param tag The tag
     * @return The tag
     */
    public String tag(final AbstractHeader.Tag tag) {
        final AbstractHeader.Entry<?> entry = this.header().getEntry(tag);
        final String val;
        if (entry == null) {
            val = "";
        } else {
            val = ((String[]) entry.getValues())[0];
        }
        return val;
    }

    /**
     * Get numeric tag by ID.
     * @param tag The tag
     * @return The tag
     */
    public int num(final AbstractHeader.Tag tag) {
        final AbstractHeader.Entry<?> entry = this.header().getEntry(tag);
        final int val;
        if (entry == null) {
            val = 0;
        } else {
            val = ((int[]) entry.getValues())[0];
        }
        return val;
    }

}
