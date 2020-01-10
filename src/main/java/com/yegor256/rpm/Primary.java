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
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import org.redline_rpm.header.Header;
import org.xembly.Directives;

/**
 * The primary XML file.
 *
 * Non suppoted elements:
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
     *
     * @param key The key
     * @param pkg The package
     * @return Completion or error signal.
     */
    public Completable update(final String key, final Pkg pkg) {
        return Single.fromCallable(() -> Files.size(pkg.path()))
            .flatMapCompletable(
                size ->
                    new Checksum(pkg.path()).sha().flatMapCompletable(checksum ->
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
                                .add("checksum")
                                .attr("type", "sha256")
                                .attr("pkgid", "YES")
                                .set(checksum)
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
                                .add("time")
                                .attr("file", pkg.num(Header.HeaderTag.FILEMTIMES))
                                .attr("build", pkg.num(Header.HeaderTag.BUILDTIME))
                                .up()
                                .add("size")
                                .attr("package", size)
                                .attr("installed", pkg.num(Header.HeaderTag.SIZE))
                                .attr("archive", pkg.num(Header.HeaderTag.ARCHIVESIZE))
                                .up()
                                .add("location")
                                .attr("href", key)
                                .up()
                                .add("format")
                                .add("rpm:license")
                                .set(pkg.tag(Header.HeaderTag.LICENSE))
                                .up()
                                .add("rpm:vendor")
                                .set(pkg.tag(Header.HeaderTag.VENDOR))
                                .up()
                                .add("rpm:group")
                                .set(pkg.tag(Header.HeaderTag.GROUP))
                                .up()
                                .add("rpm:buildhost")
                                .set(pkg.tag(Header.HeaderTag.BUILDHOST))
                                .up()
                                .add("rpm:sourcerpm")
                                .set(pkg.tag(Header.HeaderTag.SOURCERPM))
                                .up()
                                .add("rpm:header-range")
                                .attr("start", pkg.header().getStartPos())
                                .attr("end", pkg.header().getEndPos())
                                .up()
                                .append(rpmProvides(pkg))
                                .append(rpmRequires(pkg))
                                .append(files(pkg))
                                .up()
                        )
                    )
            );
    }

    /**
     * Create directives which add {@code '<file>'} tags.
     * @param pkg The package to generate directives for
     * @return A set of generated directives.
     */
    private static Directives files(final Pkg pkg) {
        final Directives provides = new Directives();
        final String[] files = (String[]) pkg.header()
            .getEntry(Header.HeaderTag.BASENAMES).getValues();
        final String[] dirs = (String[]) pkg.header()
            .getEntry(Header.HeaderTag.DIRNAMES).getValues();
        final int[] dirsidx = (int[]) pkg.header()
            .getEntry(Header.HeaderTag.DIRINDEXES).getValues();
        final Set<String> dirset =
            Arrays.stream(dirs).collect(Collectors.toSet());
        for (int idx = 0; idx < files.length; idx += 1) {
            if (files[idx].charAt(0) == '.') {
                continue;
            }
            final String text = String.format(
                "%s%s",
                dirs[dirsidx[idx]],
                files[idx]
            );
            provides.add("file")
                .set(text);
            if (dirset.contains(String.format("%s/", text))) {
                provides.attr("type", "dir");
            }
            provides.up();
        }
        return provides;
    }

    /**
     * Create directive which add rpm:provides xml tag.
     * @param pkg The package to generate directives for
     * @return A set of generated directives.
     */
    private static Directives rpmProvides(final Pkg pkg) {
        final Directives provides = new Directives()
            .add("rpm:provides");
        final String[] values = (String[])
            pkg.header().getEntry(Header.HeaderTag.PROVIDENAME).getValues();
        for (final String value : values) {
            provides.add("rpm:entry")
                .attr("name", value)
                .up();
        }
        return provides.up();
    }

    /**
     * Create directive which add rpm:requires xml tag.
     * @param pkg The package to generate directives for
     * @return A set of generated directives.
     */
    private static Directives rpmRequires(final Pkg pkg) {
        final Directives provides = new Directives()
            .add("rpm:requires");
        final Set<String> values = Arrays.stream(
            (String[]) pkg.header()
                .getEntry(Header.HeaderTag.REQUIRENAME).getValues())
            .filter(name -> !name.startsWith("rpmlib("))
            .collect(Collectors.toSet());
        for (final String value : values) {
            provides.add("rpm:entry")
                .attr("name", value)
                .up();
        }
        return provides.up();
    }

}
