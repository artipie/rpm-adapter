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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hamcrest.core.IsEqual;
import org.llorllale.cactoos.matchers.MatcherEnvelope;

/**
 * Metadata has given amount of packages.
 * @since 0.10
 * @todo #307:30min Use this matcher in `RpmTest` to validate that generated metadata files have
 *  correct packages amount.
 */
public final class NodeHasPkgCount extends MatcherEnvelope<XMLDocument> {

    /**
     * RegEx pattern for packages attribute.
     */
    private static final Pattern ATTR = Pattern.compile("packages=\"(\\d+)\"");

    /**
     * Ctor.
     * @param count Expected packages count
     * @param tag Xml tag
     */
    public NodeHasPkgCount(final int count, final String tag) {
        super(
            xml -> new IsEqual<>(count).matches(countPackages(tag, xml))
                && new IsEqual<>(count).matches(packagesAttributeValue(xml)),
            desc -> desc.appendText(String.format("%d packages expected", count)),
            (xml, desc) -> desc.appendText(
                String.format(
                    "%d packages found, `packages` attribute value is %d",
                    countPackages(tag, xml), packagesAttributeValue(xml)
                )
            )
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

    /**
     * Returns `packages` attribute value.
     * @param xml Xml document
     * @return Value of the attribute
     */
    private static int packagesAttributeValue(final XMLDocument xml) {
        final Matcher matcher = ATTR.matcher(xml.toString());
        int res = Integer.MIN_VALUE;
        if (matcher.find()) {
            res = Integer.parseInt(matcher.group(1));
        }
        return res;
    }

}
