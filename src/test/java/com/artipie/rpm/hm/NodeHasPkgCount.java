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
package com.artipie.rpm.hm;

import com.jcabi.xml.XMLDocument;
import org.hamcrest.Description;
import org.hamcrest.core.IsEqual;
import org.llorllale.cactoos.matchers.MatcherEnvelope;

/**
 * Metadata has given amount of packages.
 * @since 0.10
 * @todo #285:30min This matcher should also verify that `packages` attribute of metadata files
 *  has correct value of packages (the same as packages count). Add this check, correct mismatch
 *  description to say how many packages was found, what is `packages` attribute value and what is
 *  expected. The tests are already implemented, so enable them after implementing. After that
 *  use this matcher in `RpmTest`, `RpmITCase` and possibly in ModifiableMetadataTest and
 *  MetadataFileTest.
 */
public final class NodeHasPkgCount extends MatcherEnvelope<XMLDocument> {

    /**
     * Ctor.
     * @param count Expected packages count
     * @param tag Xml tag
     */
    public NodeHasPkgCount(final int count, final String tag) {
        super(
            xml -> new IsEqual<>(count).matches(countPackages(tag, xml)),
            desc -> desc.appendValue(count),
            (xml, desc) -> desc.appendValue(countPackages(tag, xml))
        );
    }

    /**
     * Constructor with {@link Description}.
     * @param count Expected packages count.
     * @param tag Xml tag.
     * @param description Hamcrest description for matcher.
     */
    public NodeHasPkgCount(final int count, final String tag, final Description description) {
        super(
            xml -> new IsEqual<>(count).matches(countPackages(tag, xml)),
            desc -> description.appendValue(count),
            (xml, desc) -> description.appendValue(countPackages(tag, xml))
        );
    }

    /**
     * Count packages in XMLDocument.
     * @param tag Tag
     * @param xml Xml document
     * @return Packages count
     */
    private static int countPackages(final String tag, final XMLDocument xml) {
        return Integer.parseInt(
            xml.xpath(
                String.format("count(/*[local-name()='%s']/*[local-name()='package'])", tag)
            ).get(0)
        );
    }

}
