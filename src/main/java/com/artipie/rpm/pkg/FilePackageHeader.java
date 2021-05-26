/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
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
