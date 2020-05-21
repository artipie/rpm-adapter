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

import com.artipie.rpm.files.Gzip;
import com.artipie.rpm.files.TestBundle;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.matchers.CompareMatcher;

/**
 * Test for {@link XmlMetaJoin}.
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledIfSystemProperty(named = "it.longtests.enabled", matches = "true")
class XmlMetaJoinITCase {
    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    static Path tmp;

    /**
     * Gzipped bundle of RPMs.
     */
    private static Path bundle;

    /**
     * Gzipped bundle of RPMs.
     */
    private static Path repo;

    /**
     * Resources dir.
     */
    private static final String RESOURCES = "src/test/resources-binary/repodata";

    @BeforeAll
    static void setUpClass() throws Exception {
        XmlMetaJoinITCase.bundle = new TestBundle(
            TestBundle.Size.valueOf(
                System.getProperty("it.longtests.size", "hundred")
                    .toUpperCase(Locale.US)
            )
        ).unpack(XmlMetaJoinITCase.tmp);
    }

    @BeforeEach
    void setUp() throws Exception {
        XmlMetaJoinITCase.repo = Files.createDirectory(XmlMetaJoinITCase.tmp.resolve("repo"));
        new Gzip(XmlMetaJoinITCase.bundle).unpackTar(XmlMetaJoinITCase.repo);
    }

    @AfterEach
    void tearDown() throws Exception {
        FileUtils.deleteDirectory(XmlMetaJoinITCase.repo.toFile());
    }

    @Test
    void joinsBigMetadataWithSmall() throws IOException {
        final Path fast = XmlMetaJoinITCase.repo.resolve("big.primary.xml");
        new Gzip(meta(XmlMetaJoinITCase.repo, "primary")).unpack(fast);
        new XmlMetaJoin("metadata")
            .fastMerge(fast, Paths.get(XmlMetaJoinITCase.RESOURCES, "primary.xml.example"));
        final Path stream = XmlMetaJoinITCase.repo.resolve("big.primary.xml");
        new Gzip(meta(XmlMetaJoinITCase.repo, "primary")).unpack(stream);
        new XmlMetaJoin("metadata")
            .merge(stream, Paths.get(XmlMetaJoinITCase.RESOURCES, "primary.xml.example"));
        MatcherAssert.assertThat(
            fast,
            CompareMatcher.isIdenticalTo(stream)
        );
    }

    @Test
    void joinsSmallMetadataWithBig() throws IOException {
        final Path big = XmlMetaJoinITCase.repo.resolve("big.filelists.xml");
        final Path resfast = XmlMetaJoinITCase.repo.resolve("fast.filelists.xml");
        Files.copy(Paths.get(XmlMetaJoinITCase.RESOURCES, "filelists.xml.example"), resfast);
        new Gzip(meta(XmlMetaJoinITCase.repo, "filelists")).unpack(big);
        new XmlMetaJoin("filelists").fastMerge(resfast, big);
        final Path resstream = XmlMetaJoinITCase.repo.resolve("stream.filelists.xml");
        Files.copy(Paths.get(XmlMetaJoinITCase.RESOURCES, "filelists.xml.example"), resstream);
        new XmlMetaJoin("filelists").merge(resstream, big);
        MatcherAssert.assertThat(
            resfast,
            CompareMatcher.isIdenticalTo(resstream)
        );
    }

    /**
     * Searches for the meta file by substring in folder.
     * @param dir Where to look for the file
     * @param substr What to find
     * @return Path to find
     * @throws IOException On error
     */
    private static Path meta(final Path dir, final String substr) throws IOException {
        final Optional<Path> res = Files.walk(dir)
            .filter(
                path -> path.getFileName().toString().endsWith(String.format("%s.xml.gz", substr))
            ).findFirst();
        if (res.isPresent()) {
            return res.get();
        } else {
            throw new IllegalStateException(
                String.format("Metafile %s does not exists in %s", substr, dir.toString())
            );
        }
    }

}
