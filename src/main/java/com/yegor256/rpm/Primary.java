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
package com.yegor256.rpm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.redline_rpm.header.Header;
import org.xembly.Directives;

/**
 * The primary XML file.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class Primary {

    /**
     * The XML.
     */
    private final Path xml;

    /**
     * Ctor.
     * @param path The path
     */
    Primary(final Path path) {
        this.xml = path;
    }

    /**
     * Update.
     * @param key The key
     * @param pkg The package
     * @throws IOException If fails
     */
    public void update(final String key, final Pkg pkg) throws IOException {
        new Update(this.xml).apply(
            new Directives()
                .xpath("/")
                .addIf("metadata")
                .xpath(
                    String.format(
                        "/metadata/package[name='%s']",
                        pkg.tag(Header.HeaderTag.NAME)
                    )
                )
                .remove()
                .xpath("/metadata")
                .attr("xmlns", "http://linux.duke.edu/metadata/common")
                .attr("xmlns:rpm", "http://linux.duke.edu/metadata/rpm")
                .attr("packages", 1)
                .add("package")
                .attr("type", "rpm")
                .add("checksum")
                .attr("type", "sha256")
                .attr("pkgid", "YES")
                .set(new Checksum(pkg.path()).sha())
                .up()
                .add("name")
                .set(pkg.tag(Header.HeaderTag.NAME))
                .up()
                .add("arch")
                .set(pkg.tag(Header.HeaderTag.ARCH))
                .up()
                .add("version")
                .attr("epoch", pkg.num(Header.HeaderTag.EPOCH))
                .attr("ver", pkg.tag(Header.HeaderTag.VERSION))
                .attr("rel", pkg.tag(Header.HeaderTag.RELEASE))
                .up()
                .add("summary")
                .set(pkg.tag(Header.HeaderTag.SUMMARY))
                .up()
                .add("description")
                .set(pkg.tag(Header.HeaderTag.DESCRIPTION))
                .up()
                .add("packager")
                .set(pkg.tag(Header.HeaderTag.PACKAGER))
                .up()
                .add("url")
                .set(pkg.tag(Header.HeaderTag.URL))
                .up()
                .add("location")
                .attr("href", key)
                .up()
                .add("size")
                .attr("package", Files.size(pkg.path()))
                .attr("installed", pkg.num(Header.HeaderTag.FILESIZES))
                .attr("archive", pkg.num(Header.HeaderTag.ARCHIVESIZE))
                .up()
                .add("time")
                .attr("file", pkg.num(Header.HeaderTag.FILEMTIMES))
                .attr("build", pkg.num(Header.HeaderTag.BUILDTIME))
                .up()
                .add("format")
                .add("rpm:vendor")
                .set(pkg.tag(Header.HeaderTag.VENDOR))
                .up()
                .add("rpm:license")
                .set(pkg.tag(Header.HeaderTag.LICENSE))
                .up()
                .up()
        );
    }

}
