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

import com.artipie.rpm.TestResource;
import com.artipie.rpm.meta.XmlPackage;
import com.jcabi.xml.XMLDocument;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.llorllale.cactoos.matchers.Matches;
import org.llorllale.cactoos.matchers.Mismatches;

/**
 * Test for {@link NodeHasPkgCount}.
 * @since 0.10
 * @todo #307:30min Test methods for description verification fail on windows: figure out why,
 *  fix it and remove disable annotation.
 * @checkstyle MagicNumberCheck (500 lines)
 */
final class NodeHasPkgCountTest {

    /**
     * Wrong xml path.
     */
    private static final Path WRONG =
        new TestResource("repodata/wrong-package.xml.example").file();

    /**
     * Primary xml example path.
     */
    private static final Path PRIMARY =
        new TestResource("repodata/primary.xml.example").file();

    @Test
    void countsPackages() throws FileNotFoundException {
        MatcherAssert.assertThat(
            new NodeHasPkgCount(2, XmlPackage.OTHER.tag()),
            new Matches<>(
                new XMLDocument(
                    new TestResource("repodata/other.xml.example").file()
                )
            )
        );
    }

    @Test
    void doesNotMatchWhenPackagesAmountDiffers() throws FileNotFoundException {
        MatcherAssert.assertThat(
            new NodeHasPkgCount(10, XmlPackage.PRIMARY.tag()),
            new IsNot<>(
                new Matches<>(
                    new XMLDocument(NodeHasPkgCountTest.PRIMARY)
                )
            )
        );
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void describesCorrectlyWhenPackagesAmountDiffers() throws FileNotFoundException {
        MatcherAssert.assertThat(
            new NodeHasPkgCount(10, XmlPackage.PRIMARY.tag()),
            new Mismatches<>(
                new XMLDocument(NodeHasPkgCountTest.PRIMARY),
                "10 packages expected",
                "2 packages found, 'packages' attribute value is 2"
            )
        );
    }

    @Test
    void doesNotMatchWhenPackageAttributeDiffers() throws FileNotFoundException {
        MatcherAssert.assertThat(
            new NodeHasPkgCount(2, XmlPackage.OTHER.tag()),
            new IsNot<>(
                new Matches<>(
                    new XMLDocument(NodeHasPkgCountTest.WRONG)
                )
            )
        );
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void describesCorrectlyWhenPackageAttributeDiffers() throws FileNotFoundException {
        MatcherAssert.assertThat(
            new NodeHasPkgCount(2, XmlPackage.OTHER.tag()),
            new Mismatches<>(
                new XMLDocument(NodeHasPkgCountTest.WRONG),
                "2 packages expected",
                "2 packages found, 'packages' attribute value is 3"
            )
        );
    }
}
