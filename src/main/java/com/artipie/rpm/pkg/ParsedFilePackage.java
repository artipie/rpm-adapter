/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.rpm.Digest;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.file.Path;
import org.redline_rpm.header.Header;

/**
 * Package with parsed RPM headers.
 * @since 0.8
 */
final class ParsedFilePackage implements Package {

    /**
     * Package metadata.
     */
    private final Header header;

    /**
     * Package file.
     */
    private final Path file;

    /**
     * The RPM file location relatively to the updated repository.
     */
    private final String location;

    /**
     * Ctor.
     * @param meta Package metadata
     * @param file File path
     * @param location File relative location
     */
    ParsedFilePackage(final Header meta, final Path file, final String location) {
        this.header = meta;
        this.file = file;
        this.location = location;
    }

    @Override
    public void save(final PackageOutput out, final Digest digest) throws IOException {
        Logger.debug(this, "accepting %s", this.file.getFileName());
        out.accept(new FilePackage.Headers(this.header, this.file, digest, this.location));
    }
}
