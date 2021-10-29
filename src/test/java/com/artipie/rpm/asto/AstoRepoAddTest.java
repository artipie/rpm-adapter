/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.rpm.Digest;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.StandardNamingPolicy;
import com.artipie.rpm.hm.IsXmlEqual;
import com.artipie.rpm.http.RpmUpload;
import com.artipie.rpm.meta.XmlPackage;
import com.jcabi.matchers.XhtmlMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Test for {@link AstoRepoAdd}.
 * @since 1.10
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AstoRepoAddTest {

    /**
     * Metadata key.
     */
    private static final Key MTD = new Key.From("metadata");

    /**
     * Test storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void createsEmptyMetadata() throws IOException {
        new AstoRepoAdd(
            this.storage,
            new RepoConfig.Simple(Digest.SHA256, StandardNamingPolicy.PLAIN, false)
        ).perform().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Failed to generate 3 items: primary, filelists and repomd",
            this.storage.list(Key.ROOT).join(),
            Matchers.iterableWithSize(3)
        );
        MatcherAssert.assertThat(
            "Failed to generate empty primary xml",
            new String(
                new MetadataBytes(this.storage, XmlPackage.PRIMARY).value(),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths("/*[local-name()='metadata' and @packages='0']")
        );
        MatcherAssert.assertThat(
            "Failed to generate empty other xml",
            new String(
                new MetadataBytes(this.storage, XmlPackage.OTHER).value(),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths("/*[local-name()='otherdata' and @packages='0']")
        );
        MatcherAssert.assertThat(
            new String(
                new BlockingStorage(this.storage)
                    .value(new Key.From(AstoRepoAddTest.MTD, "repomd.xml")),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths("/*[local-name()='repomd']/*[local-name()='revision']")
        );
    }

    @Test
    void addsPackagesToRepo() throws IOException {
        new TestResource("AstoRepoAddTest/filelists.xml.gz")
            .saveTo(this.storage, new Key.From(AstoRepoAddTest.MTD, "filelists.xml.gz"));
        new TestResource("AstoRepoAddTest/other.xml.gz")
            .saveTo(this.storage, new Key.From(AstoRepoAddTest.MTD, "other.xml.gz"));
        new TestResource("AstoRepoAddTest/primary.xml.gz")
            .saveTo(this.storage, new Key.From(AstoRepoAddTest.MTD, "primary.xml.gz"));
        final String time = "time-1.7-45.el7.x86_64.rpm";
        new TestResource(time).saveTo(this.storage, new Key.From(RpmUpload.TO_ADD, time));
        final String lib = "libnss-mymachines2-245-1.x86_64.rpm";
        new TestResource(lib).saveTo(this.storage, new Key.From(RpmUpload.TO_ADD, "lib", lib));
        new AstoRepoAdd(
            this.storage,
            new RepoConfig.Simple(Digest.SHA256, StandardNamingPolicy.PLAIN, true)
        ).perform().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Failed to have 6 items in storage: primary, other, filelists, repomd and 2 rpms",
            this.storage.list(Key.ROOT).join(),
            Matchers.iterableWithSize(6)
        );
        MatcherAssert.assertThat(
            "Failed to add `time` rpm to the correct location",
            this.storage.exists(new Key.From(time)).join(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Failed to add `lib` rpm to the correct location",
            this.storage.exists(new Key.From("lib", lib)).join(),
            new IsEqual<>(true)
        );
        this.checkMeta("primary-res.xml", XmlPackage.PRIMARY);
        this.checkMeta("other-res.xml", XmlPackage.OTHER);
        this.checkMeta("filelists-res.xml", XmlPackage.FILELISTS);
        MatcherAssert.assertThat(
            "Failed to generate repomd xml",
            new String(
                new BlockingStorage(this.storage)
                    .value(new Key.From(AstoRepoAddTest.MTD, "repomd.xml")),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='repomd']/*[local-name()='revision']",
                "/*[local-name()='repomd']/*[local-name()='data' and @type='primary']",
                "/*[local-name()='repomd']/*[local-name()='data' and @type='other']",
                "/*[local-name()='repomd']/*[local-name()='data' and @type='filelists']"
            )
        );
    }

    private void checkMeta(final String file, final XmlPackage primary) throws IOException {
        MatcherAssert.assertThat(
            String.format("Failed to generate %s xml", primary.lowercase()),
            new TestResource(String.format("AstoRepoAddTest/%s", file)).asPath(),
            new IsXmlEqual(
                new MetadataBytes(storage, primary).value()
            )
        );
    }
}
