/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.asto.test.TestResource;
import com.artipie.rpm.hm.IsXmlEqual;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link XmlStreamJoin}.
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class XmlStreamJoinTest {

    @Test
    void joinsTwoMetaXmlFiles(@TempDir final Path temp) throws IOException {
        final Path file = Files.copy(
            new TestResource("repodata/primary.xml.example.first").asPath(),
            temp.resolve("target.xml")
        );
        new XmlStreamJoin("metadata").merge(
            file, new TestResource("repodata/primary.xml.example.second").asPath()
        );
        MatcherAssert.assertThat(
            file,
            new IsXmlEqual(new TestResource("repodata/primary.xml.example").asPath())
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
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<parent><c>2</c>",
                "<d>3</d></parent>"
            ).getBytes()
        );
        new XmlStreamJoin("parent").merge(target, part);
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

}
