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

package com.artipie.rpm;

import java.util.Iterator;
import org.xembly.Directive;
import org.xembly.Directives;

/**
 * RepoXml.
 *
 * @since 0.3
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class RepoXml implements Iterable<Directive> {

    /**
     * Directives in xml.
     */
    private final Directives directives;

    /**
     * Ctor.
     */
    RepoXml() {
        this(new Directives());
    }

    /**
     * Ctor.
     *
     * @param directives Directives in xml
     */
    RepoXml(final Directives directives) {
        this.directives = directives;
    }

    /**
     * Creates base of xml.
     *
     * @param type Type of file
     * @param location Location reference for file
     * @return Base of xml
     */
    public RepoXml base(final String type, final String location) {
        return new RepoXml(
            this.directives.xpath("/repomd")
            .addIf("revision").set("1")
            .xpath(String.format("/repomd/data[type='%s']", type))
            .remove()
            .xpath("/repomd")
            .add("data")
            .attr("type", type)
            .add("location")
            .attr("href", location)
            .up()
        );
    }

    /**
     * Adds open size.
     *
     * @param size Size of file
     * @return RepoXml with open size
     */
    public RepoXml openSize(final long size) {
        return new RepoXml(this.directives.add("open-size").set(size).up());
    }

    /**
     * Adds size.
     *
     * @param size Size of file
     * @return RepoXml with size
     */
    public RepoXml size(final long size) {
        return new RepoXml(this.directives.add("size").set(size).up());
    }

    /**
     * Checksum.
     *
     * @param checksum Chucksum of file
     * @return RepoXml with checksum
     */
    public RepoXml checksum(final String checksum) {
        return new RepoXml(this.directives.add("checksum")
            .attr("type", "sha256")
            .set(checksum)
            .up()
        );
    }

    /**
     * Open checksum.
     *
     * @param open Open checksum
     * @return RepoXml with open checksum
     */
    public RepoXml openChecksum(final String open) {
        return new RepoXml(this.directives.add("open-checksum")
            .attr("type", "sha256")
            .set(open)
            .up()
        );
    }

    /**
     * Timestamp.
     *
     * @return RepoXml with timestamp
     */
    public RepoXml timestamp() {
        return new RepoXml(
            this.directives.add("timestamp")
            // @checkstyle MagicNumberCheck (1 line)
            .set(System.currentTimeMillis() / 1000L)
            .up()
        );
    }

    @Override
    public Iterator<Directive> iterator() {
        return this.directives.iterator();
    }

    @Override
    public String toString() {
        return this.directives.toString();
    }
}
