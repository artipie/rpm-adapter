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

import com.artipie.rpm.TestRpm;
import com.artipie.rpm.hm.IsXmlEqual;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link XmlMetaJoin}.
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class XmlMetaJoinTest {

    @Test
    void joinsTwoMetaXmlFiles(@TempDir final Path temp) throws IOException {
        final Path file = Files.copy(
            new TestRpm.TestResource("repodata/primary.xml.example.first").file(),
            temp.resolve("target.xml")
        );
        new XmlMetaJoin("metadata").merge(
            file, new TestRpm.TestResource("repodata/primary.xml.example.second").file()
        );
        MatcherAssert.assertThat(
            file,
            new IsXmlEqual(new TestRpm.TestResource("repodata/primary.xml.example").file())
        );
    }

    @Test
    void joinsWhenXmlIsNotProperlySeparatedWithLineBreaks(@TempDir final Path temp)
        throws IOException {
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
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <parent>",
                "<c>2</c>",
                "<d>3</d></parent>"
            ).getBytes()
        );
        new XmlMetaJoin("parent").merge(target, part);
        MatcherAssert.assertThat(
            target,
            new IsXmlEqual(
                String.join(
                    "\n",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                    "<parent>",
                    "<a>1</a><b>2</b><c>2</c><d>3</d>",
                    "</parent>"
                )
            )
        );
    }

    @Test
    void joinsOneLineXmls(@TempDir final Path temp) throws IOException {
        final Path target = temp.resolve("target.xml");
        final Path part = temp.resolve("part.xml");
        Files.write(
            target,
            String.join(
                "",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<parent>",
                "<a>A</a>",
                "<b>B</b>",
                "</parent>"
            ).getBytes()
        );
        Files.write(
            part,
            String.join(
                "",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<parent>",
                "<c>C</c>",
                "<d>D</d>",
                "</parent>"
            ).getBytes()
        );
        new XmlMetaJoin("parent").merge(target, part);
        MatcherAssert.assertThat(
            target,
            new IsXmlEqual(
                String.join(
                    "\n",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                    "<parent>",
                    "<a>A</a><b>B</b><c>C</c><d>D</d>",
                    "</parent>"
                )
            )
        );
    }

}
