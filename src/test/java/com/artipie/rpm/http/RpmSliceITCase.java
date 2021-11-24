/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permissions;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.rpm.Digest;
import com.artipie.rpm.NamingPolicy;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.Rpm;
import com.artipie.rpm.TestRpm;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.nio.file.Files;
import java.nio.file.Path;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * Test for {@link RpmSlice}.
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
public final class RpmSliceITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Installed packages verifier.
     */
    private static final ListOf<String> INSTALLED = new ListOf<>(
        "Installed", "aspell-12:0.60.6.1-9.el7.x86_64",
        "time-1.7-45.el7.x86_64", "Complete!"
    );

    /**
     * Packaged list verifier.
     */
    private static final ListOf<String> AVAILABLE = new ListOf<>(
        "Available Packages", "aspell.x86_64", "12:0.60.6.1-9.el7", "time.x86_64", "1.7-45.el7"
    );

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    @ParameterizedTest
    @CsvSource({
        "centos:centos8,yum,repo-pkgs",
        "fedora:32,dnf,repository-packages"
    })
    void canListAndInstallFromArtipieRepo(final String linux,
        final String mngr, final String rey) throws Exception {
        this.start(Permissions.FREE, Authentication.ANONYMOUS, "", linux);
        MatcherAssert.assertThat(
            "Lists 'time' and 'aspell' packages",
            this.exec(mngr, rey, "list"),
            new StringContainsInOrder(RpmSliceITCase.AVAILABLE)
        );
        MatcherAssert.assertThat(
            "Installs 'time' and 'aspell' package",
            this.exec(mngr, rey, "install"),
            new StringContainsInOrder(RpmSliceITCase.INSTALLED)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "centos:centos8,yum,repo-pkgs",
        "fedora:32,dnf,repository-packages"
    })
    void canListAndInstallFromArtipieRepoWithAuth(final String linux,
        final String mngr, final String key) throws Exception {
        final String mark = "mark";
        final String pswd = "abc";
        this.start(
            new Permissions.Single(mark, "download"),
            new Authentication.Single(mark, pswd),
            String.format("%s:%s@", mark, pswd),
            linux
        );
        MatcherAssert.assertThat(
            "Lists 'time' package",
            this.exec(mngr, key, "list"),
            new StringContainsInOrder(RpmSliceITCase.AVAILABLE)
        );
        MatcherAssert.assertThat(
            "Installs 'time' package",
            this.exec(mngr, key, "install"),
            new StringContainsInOrder(RpmSliceITCase.INSTALLED)
        );
    }

    @AfterEach
    void stopContainer() {
        this.server.close();
        this.cntn.stop();
    }

    @AfterAll
    static void close() {
        RpmSliceITCase.VERTX.close();
    }

    /**
     * Executes yum command in container.
     * @param mngr Rpm manager
     * @param key Key to specify repo
     * @param action What to do
     * @return String stdout
     * @throws Exception On error
     */
    private String exec(final String mngr, final String key, final String action) throws Exception {
        return this.cntn.execInContainer(
            mngr, "-y", key, "example", action
        ).getStdout();
    }

    /**
     * Starts VertxSliceServer and docker container.
     * @param perms Permissions
     * @param auth Authentication
     * @param cred String with user name and password to add in url, uname:pswd@
     * @param linux Linux distribution name and version
     * @throws Exception On error
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    private void start(final Permissions perms, final Authentication auth, final String cred,
        final String linux) throws Exception {
        final Storage storage = new InMemoryStorage();
        new TestRpm.Time().put(storage);
        new TestRpm.Aspell().put(new SubStorage(new Key.From("spelling"), storage));
        final RepoConfig config = new RepoConfig.Simple(
            Digest.SHA256, new NamingPolicy.HashPrefixed(Digest.SHA1), true
        );
        new Rpm(storage, config).batchUpdate(Key.ROOT).blockingAwait();
        this.server = new VertxSliceServer(
            RpmSliceITCase.VERTX,
            new LoggingSlice(new RpmSlice(storage, perms, auth, config))
        );
        final int port = this.server.start();
        Testcontainers.exposeHostPorts(port);
        final Path setting = this.tmp.resolve("example.repo");
        this.tmp.resolve("example.repo").toFile().createNewFile();
        Files.write(
            setting,
            new ListOf<>(
                "[example]",
                "name=Example Repository",
                String.format("baseurl=http://%shost.testcontainers.internal:%d/", cred, port),
                "enabled=1",
                "gpgcheck=0"
            )
        );
        this.cntn = new GenericContainer<>(linux)
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
        this.cntn.execInContainer("mv", "/home/example.repo", "/etc/yum.repos.d/");
    }

}
