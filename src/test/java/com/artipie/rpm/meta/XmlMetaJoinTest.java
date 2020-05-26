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
package com.artipie.rpm.meta;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.matchers.CompareMatcher;

/**
 * Test for {@link XmlMetaJoin}.
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class XmlMetaJoinTest {

    /**
     * Test repo path.
     */
    private static final String REPO = "src/test/resources-binary/repodata";

    @Test
    void joinsTwoMetaXmlFiles(@TempDir final Path temp) throws IOException {
        final Path file = Files.copy(
            Paths.get(XmlMetaJoinTest.REPO, "primary.xml.example.first"), temp.resolve("target.xml")
        );
        new XmlMetaJoin("metadata").streamMerge(
            file, Paths.get(XmlMetaJoinTest.REPO, "primary.xml.example.second")
        );
        MatcherAssert.assertThat(
            file,
            CompareMatcher.isIdenticalTo(
                Paths.get(XmlMetaJoinTest.REPO, "primary.xml.example")
            )
        );
    }

    @Test
    void joinsTwoMetaXmlFilesFast(@TempDir final Path temp) throws IOException {
        final Path file = Files.copy(
            Paths.get(XmlMetaJoinTest.REPO, "primary.xml.example.first"), temp.resolve("target.xml")
        );
        new XmlMetaJoin("metadata").merge(
            file, Paths.get(XmlMetaJoinTest.REPO, "primary.xml.example.second")
        );
        MatcherAssert.assertThat(
            file,
            CompareMatcher.isIdenticalTo(
                Paths.get(XmlMetaJoinTest.REPO, "primary.xml.example")
            )
        );
    }

    @Test
    void joinsWhenXmlIsNotProperlyFormatted(@TempDir final Path temp) throws IOException {
        final Path target = temp.resolve("res.xml");
        final Path part = temp.resolve("part.xml");
        Files.write(
            target,
            String.join(
                "\n",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<parent><a>1</a>",
                "<b>2</b></parent>"
            ).getBytes()
        );
        Files.write(
            part,
            String.join(
                "\n",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<parent><c>2</c>",
                "<d>3</d></parent>"
            ).getBytes()
        );
        new XmlMetaJoin("parent").streamMerge(target, part);
        final Path expected = temp.resolve("expected.xml");
        Files.write(
            expected,
            String.join(
                "\n",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<parent>",
                "<a>1</a><b>2</b><c>2</c><d>3</d>",
                "</parent>"
            ).getBytes()
        );
        MatcherAssert.assertThat(
            target,
            CompareMatcher.isIdenticalTo(expected)
        );
    }

    @Test
    void joinsFastWhenXmlIsNotProperlyFormatted(@TempDir final Path temp) throws IOException {
        final Path target = temp.resolve("res.xml");
        final Path part = temp.resolve("part.xml");
        Files.write(
            target,
            String.join(
                "\n",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<a>1</a>",
                "<b>2</b></parent>"
            ).getBytes()
        );
        Files.write(
            part,
            String.join(
                "\n",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <parent>",
                "<c>2</c>",
                "<d>3</d></parent>"
            ).getBytes()
        );
        new XmlMetaJoin("parent").merge(target, part);
        final Path expected = temp.resolve("expected.xml");
        Files.write(
            expected,
            String.join(
                "\n",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<parent>",
                "<a>1</a><b>2</b><c>2</c><d>3</d>",
                "</parent>"
            ).getBytes()
        );
        MatcherAssert.assertThat(
            target,
            CompareMatcher.isIdenticalTo(expected)
        );
    }

}
