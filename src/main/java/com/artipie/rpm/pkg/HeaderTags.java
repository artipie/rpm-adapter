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

import java.util.List;
import org.redline_rpm.header.Header;

/**
 * Helper object to read metadata header tags from RPM package.
 *
 * @since 0.6
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class HeaderTags {

    /**
     * Metadata.
     */
    private final Package.Meta meta;

    /**
     * Ctor.
     * @param meta Metadata
     */
    public HeaderTags(final Package.Meta meta) {
        this.meta = meta;
    }

    /**
     * Get the name header.
     * @return Value of header tag NAME.
     */
    public String name() {
        return this.meta.header(Header.HeaderTag.NAME, "");
    }

    /**
     * Get the arch header.
     * @return Value of header tag ARCH.
     */
    public String arch() {
        return this.meta.header(Header.HeaderTag.ARCH, "");
    }

    /**
     * Get the epoch header.
     * @return Value of header tag EPOCH.
     */
    public int epoch() {
        return this.meta.header(Header.HeaderTag.EPOCH, 0);
    }

    /**
     * Get the version header.
     * @return Value of header tag VERSION.
     */
    public String version() {
        return this.meta.header(Header.HeaderTag.VERSION, "");
    }

    /**
     * Get the release header.
     * @return Value of header tag RELEASE.
     */
    public String release() {
        return this.meta.header(Header.HeaderTag.RELEASE, "");
    }

    /**
     * Get the summary header.
     * @return Value of header tag SUMMARY.
     */
    public String summary() {
        return this.meta.header(Header.HeaderTag.SUMMARY, "");
    }

    /**
     * Get the description header.
     * @return Value of header tag DESCRIPTION.
     */
    public String description() {
        return this.meta.header(Header.HeaderTag.DESCRIPTION, "");
    }

    /**
     * Get the package header.
     * @return Value of header tag PACKAGER.
     */
    public String packager() {
        return this.meta.header(Header.HeaderTag.PACKAGER, "");
    }

    /**
     * Get the url header.
     * @return Value of header tag URL.
     */
    public String url() {
        return this.meta.header(Header.HeaderTag.URL, "");
    }

    /**
     * Get the filemtimes header.
     * @return Value of header tag FILEMTIMES.
     */
    public int fileTimes() {
        return this.meta.header(Header.HeaderTag.FILEMTIMES, 0);
    }

    /**
     * Get the build time header.
     * @return Value of header tag BUILDTIME.
     */
    public int buildTime() {
        return this.meta.header(Header.HeaderTag.BUILDTIME, 0);
    }

    /**
     * Get the size header.
     * @return Value of header tag SIZE.
     */
    public int installedSize() {
        return this.meta.header(Header.HeaderTag.SIZE, 0);
    }

    /**
     * Get the archive size header.
     * @return Value of header tag ARCHIVESIZE.
     */
    public int archiveSize() {
        return this.meta.header(Header.HeaderTag.ARCHIVESIZE, 0);
    }

    /**
     * Get the license header.
     * @return Value of header tag LICENSE.
     */
    public String license() {
        return this.meta.header(Header.HeaderTag.LICENSE, "");
    }

    /**
     * Get the vendor header.
     * @return Value of header tag VENDOR.
     */
    public String vendor() {
        return this.meta.header(Header.HeaderTag.VENDOR, "");
    }

    /**
     * Get the group header.
     * @return Value of header tag GROUP.
     */
    public String group() {
        return this.meta.header(Header.HeaderTag.GROUP, "");
    }

    /**
     * Get the build host header.
     * @return Value of header tag BUILDHOST.
     */
    public String buildHost() {
        return this.meta.header(Header.HeaderTag.BUILDHOST, "");
    }

    /**
     * Get the source RPM header.
     * @return Value of header tag SOURCERPM.
     */
    public String sourceRmp() {
        return this.meta.header(Header.HeaderTag.SOURCERPM, "");
    }

    /**
     * Get the provide name header.
     * @return Value of header tag PROVIDENAME.
     */
    public List<String> providers() {
        return this.meta.headers(Header.HeaderTag.PROVIDENAME);
    }

    /**
     * Get the require name header.
     * @return Value of header tag REQUIRENAME.
     */
    public List<String> requires() {
        return this.meta.headers(Header.HeaderTag.REQUIRENAME);
    }

    /**
     * Get the base names header.
     * @return Value of header tag BASENAMES.
     */
    public List<String> baseNames() {
        return this.meta.headers(Header.HeaderTag.BASENAMES);
    }

    /**
     * Get the dir names header.
     * @return Value of header tag DIRNAMES.
     */
    public List<String> dirNames() {
        return this.meta.headers(Header.HeaderTag.DIRNAMES);
    }

    /**
     * Get the dir indexes header.
     * @return Value of header tag DIRINDEXES.
     */
    public int[] dirIndexes() {
        return this.meta.intHeaders(Header.HeaderTag.DIRINDEXES);
    }
}
