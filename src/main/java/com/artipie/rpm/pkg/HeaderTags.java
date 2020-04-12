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
package com.artipie.rpm.pkg;

import java.util.List;
import org.redline_rpm.header.Header;

/**
 * Helper object to read metadata header tags from RPM package.
 *
 * @since 0.6
 * @todo #69:30min Write javadocs for all these method. When done, remove
 *  JavadocMethodCheck suppression annotation for PMD checker.
 * @checkstyle JavadocMethodCheck (500 lines)
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

    public String name() {
        return this.meta.header(Header.HeaderTag.NAME).asString("");
    }

    public String arch() {
        return this.meta.header(Header.HeaderTag.ARCH).asString("");
    }

    public int epoch() {
        return this.meta.header(Header.HeaderTag.EPOCH).asInt(0);
    }

    public String version() {
        return this.meta.header(Header.HeaderTag.VERSION).asString("");
    }

    public String release() {
        return this.meta.header(Header.HeaderTag.RELEASE).asString("");
    }

    public String summary() {
        return this.meta.header(Header.HeaderTag.SUMMARY).asString("");
    }

    public String description() {
        return this.meta.header(Header.HeaderTag.DESCRIPTION).asString("");
    }

    public String packager() {
        return this.meta.header(Header.HeaderTag.PACKAGER).asString("");
    }

    public String url() {
        return this.meta.header(Header.HeaderTag.URL).asString("");
    }

    public int fileTimes() {
        return this.meta.header(Header.HeaderTag.FILEMTIMES).asInt(0);
    }

    public int buildTime() {
        return this.meta.header(Header.HeaderTag.BUILDTIME).asInt(0);
    }

    public int installedSize() {
        return this.meta.header(Header.HeaderTag.SIZE).asInt(0);
    }

    public int archiveSize() {
        return this.meta.header(Header.HeaderTag.ARCHIVESIZE).asInt(0);
    }

    public String license() {
        return this.meta.header(Header.HeaderTag.LICENSE).asString("");
    }

    public String vendor() {
        return this.meta.header(Header.HeaderTag.VENDOR).asString("");
    }

    public String group() {
        return this.meta.header(Header.HeaderTag.GROUP).asString("");
    }

    public String buildHost() {
        return this.meta.header(Header.HeaderTag.BUILDHOST).asString("");
    }

    public String sourceRmp() {
        return this.meta.header(Header.HeaderTag.SOURCERPM).asString("");
    }

    public List<String> providers() {
        return this.meta.header(Header.HeaderTag.PROVIDENAME).asStrings();
    }

    public List<String> requires() {
        return this.meta.header(Header.HeaderTag.REQUIRENAME).asStrings();
    }

    public List<String> baseNames() {
        return this.meta.header(Header.HeaderTag.BASENAMES).asStrings();
    }

    public List<String> dirNames() {
        return this.meta.header(Header.HeaderTag.DIRNAMES).asStrings();
    }

    public int[] dirIndexes() {
        return this.meta.header(Header.HeaderTag.DIRINDEXES).asInts();
    }
}
