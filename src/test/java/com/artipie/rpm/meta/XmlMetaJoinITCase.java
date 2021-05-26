/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.asto.test.TestResource;
import com.artipie.rpm.TimingExtension;
import com.artipie.rpm.files.Gzip;
import com.artipie.rpm.files.TestBundle;
import com.artipie.rpm.hm.IsXmlEqual;
import com.artipie.rpm.misc.FileInDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link XmlMetaJoin}.
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledIfSystemProperty(named = "it.longtests.enabled", matches = "true")
@ExtendWith(TimingExtension.class)
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
     * Test repo.
     */
    private static Path repo;

    /**
     * Resources dir.
     */
    private static final Path FILELISTS =
        new TestResource("repodata/filelists.xml.example").asPath();

    @BeforeAll
    static void setUpClass() throws Exception {
        XmlMetaJoinITCase.bundle = new TestBundle(
            TestBundle.Size.valueOf(
                System.getProperty("it.longtests.size", "hundred")
                    .toUpperCase(Locale.US)
            )
        ).load(XmlMetaJoinITCase.tmp);
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
        final Path fast = XmlMetaJoinITCase.repo.resolve("big.filelists.xml");
        new Gzip(new FileInDir(XmlMetaJoinITCase.repo).find("filelists.xml.gz")).unpack(fast);
        new XmlMetaJoin("filelists").merge(fast, XmlMetaJoinITCase.FILELISTS);
        final Path result = XmlMetaJoinITCase.repo.resolve("res.filelists.xml");
        new Gzip(
            new FileInDir(XmlMetaJoinITCase.repo).find("filelists.xml.gz")
        ).unpack(result);
        new XmlStreamJoin("filelists").merge(
            result,
            XmlMetaJoinITCase.FILELISTS
        );
        MatcherAssert.assertThat(
            fast,
            new IsXmlEqual(result)
        );
    }

    @Test
    void joinsSmallMetadataWithBig() throws IOException {
        final Path big = XmlMetaJoinITCase.repo.resolve("big.filelists.xml");
        final Path fast = XmlMetaJoinITCase.repo.resolve("fast.filelists.xml");
        Files.copy(XmlMetaJoinITCase.FILELISTS, fast);
        new Gzip(new FileInDir(XmlMetaJoinITCase.repo).find("filelists.xml.gz")).unpack(big);
        new XmlMetaJoin("filelists").merge(fast, big);
        final Path result = XmlMetaJoinITCase.repo.resolve("res.filelists.xml");
        Files.copy(XmlMetaJoinITCase.FILELISTS, result);
        new XmlStreamJoin("filelists").merge(result, big);
        MatcherAssert.assertThat(
            fast,
            new IsXmlEqual(result)
        );
    }
}
