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
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;

/**
 * Metadata has given amount of packages.
 * @since 0.10
 * @todo #241:30min This matcher should also verify that `packages` attribute of metadata files
 *  has correct value of packages (the same as packages count). Add this check and do not forget
 *  about tests. After that use this matcher in `RpmTest`, `RpmITCase` and possibly in
 *  ModifiableMetadataTest and MetadataFileTest.
 */
public final class MdHasPkgAmount extends TypeSafeMatcher<Path> {

    /**
     * Count matcher.
     */
    private final Matcher<Integer> matcher;

    /**
     * Xml tag.
     */
    private final String tag;

    /**
     * Ctor.
     * @param matcher Expected packages count
     * @param tag Xml tag
     */
    public MdHasPkgAmount(final Matcher<Integer> matcher, final String tag) {
        this.matcher = matcher;
        this.tag = tag;
    }

    /**
     * Ctor.
     * @param count Expected packages count
     * @param tag Xml tag
     */
    public MdHasPkgAmount(final int count, final String tag) {
        this(new IsEqual<>(count), tag);
    }

    @Override
    public boolean matchesSafely(final Path item)  {
        try {
            return this.matcher.matches(
                Integer.parseInt(
                    new XMLDocument(item).xpath(
                        String.format(
                            "count(/*[local-name()='%s']/*[local-name()='package'])", this.tag
                        )
                    ).get(0)
                )
            );
        } catch (final FileNotFoundException err) {
            throw new UncheckedIOException(err);
        }
    }

    @Override
    public void describeTo(final Description description) {
        description.appendDescriptionOf(this.matcher);
    }
}
