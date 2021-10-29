/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.rpm.Digest;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.StandardNamingPolicy;
import com.artipie.rpm.http.RpmRemove;
import com.artipie.rpm.meta.XmlPackage;
import com.jcabi.matchers.XhtmlMatchers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AstoRepoRemove}.
 * @since 1.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AstoRepoRemoveTest {

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Test config.
     */
    private RepoConfig conf;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.conf = new RepoConfig.Simple(Digest.SHA256, StandardNamingPolicy.SHA256, false);
    }

    @Test
    void createsEmptyRepomdIfStorageIsEmpty() {
        new AstoRepoRemove(this.storage, this.conf).perform().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Storage should have 1 item",
            this.storage.list(Key.ROOT).join(),
            Matchers.iterableWithSize(1)
        );
        MatcherAssert.assertThat(
            "Repomd xml should be created",
            new String(
                new BlockingStorage(this.storage).value(new Key.From("metadata", "repomd.xml")),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='repomd']/*[local-name()='revision']"
            )
        );
    }

    @Test
    void removesPackagesFromRepository() throws IOException {
        new TestResource("abc-1.01-26.git20200127.fc32.ppc64le.rpm").saveTo(this.storage);
        new TestResource("libdeflt1_0-2020.03.27-25.1.armv7hl.rpm").saveTo(this.storage);
        this.storage.save(
            new Key.From(RpmRemove.TO_RM, "libdeflt1_0-2020.03.27-25.1.armv7hl.rpm"), Content.EMPTY
        ).join();
        new TestResource("AstoRepoRemoveTest/other.xml.gz")
            .saveTo(this.storage, new Key.From("metadata", "other.xml.gz"));
        new TestResource("AstoRepoRemoveTest/primary.xml.gz")
            .saveTo(this.storage, new Key.From("metadata", "primary.xml.gz"));
        new TestResource("AstoRepoRemoveTest/repomd.xml")
            .saveTo(this.storage, new Key.From("metadata", "repomd.xml"));
        new AstoRepoRemove(this.storage, this.conf).perform().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Package libdeflt should not exist",
            this.storage.exists(new Key.From("libdeflt1_0-2020.03.27-25.1.armv7hl.rpm")).join(),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Temp dir with packages to remove should not exist",
            this.storage.exists(RpmRemove.TO_RM).join(),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "There should be 4 items in storage",
            this.storage.list(Key.ROOT).join(),
            Matchers.iterableWithSize(4)
        );
        MatcherAssert.assertThat(
            "Primary xml should have `abc` record",
            new String(
                new MetadataBytes(this.storage, XmlPackage.PRIMARY).value(),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='metadata' and @packages='1']",
                //@checkstyle LineLengthCheck (1 line)
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='abc']"
            )
        );
        MatcherAssert.assertThat(
            "Other xml should have `abc` record",
            new String(
                new MetadataBytes(this.storage, XmlPackage.OTHER).value(),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='otherdata' and @packages='1']",
                "/*[local-name()='otherdata']/*[local-name()='package' and @name='abc']"
            )
        );
        MatcherAssert.assertThat(
            "Repomd xml should be created",
            new String(
                new BlockingStorage(this.storage).value(new Key.From("metadata", "repomd.xml")),
                StandardCharsets.UTF_8
            ),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='repomd']/*[local-name()='revision']",
                "/*[local-name()='repomd']/*[local-name()='data' and @type='primary']",
                "/*[local-name()='repomd']/*[local-name()='data' and @type='other']"
            )
        );
    }
}
