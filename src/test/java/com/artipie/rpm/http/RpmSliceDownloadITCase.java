/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.http;

import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permissions;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.TestRpm;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * Integration test for {@link RpmSlice}.
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
final class RpmSliceDownloadITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Port.
     */
    private int port;

    @Test
    void installs() throws Exception {
        final TestRpm rpm = new TestRpm.Time();
        this.start(rpm, Permissions.FREE, Authentication.ANONYMOUS);
        MatcherAssert.assertThat(
            this.yumInstall(
                String.format(
                    "http://host.testcontainers.internal:%d/%s.rpm",
                    this.port, rpm.name()
                )
            ),
            new StringContainsInOrder(new ListOf<>(rpm.name(), "Complete!"))
        );
    }

    @Test
    void installsWithAuth() throws Exception {
        final String john = "john";
        final String pswd = "123";
        final TestRpm rpm = new TestRpm.Time();
        this.start(
            rpm,
            new Permissions.Single(john, "download"),
            new Authentication.Single(john, pswd)
        );
        MatcherAssert.assertThat(
            this.yumInstall(
                String.format(
                    "http://%s:%s@host.testcontainers.internal:%d/%s.rpm", john,
                    pswd, this.port, rpm.name()
                )
            ),
            new StringContainsInOrder(new ListOf<>(rpm.name(), "Complete!"))
        );
    }

    @AfterEach
    void stopContainer() {
        this.server.close();
        this.cntn.stop();
    }

    @AfterAll
    static void close() {
        RpmSliceDownloadITCase.VERTX.close();
    }

    private String yumInstall(final String url) throws IOException, InterruptedException {
        return this.cntn.execInContainer(
            "yum", "-y", "install", url
        ).getStdout();
    }

    private void start(
        final TestRpm rpm, final Permissions perms, final Authentication auth
    ) throws Exception {
        final Storage storage = new InMemoryStorage();
        rpm.put(storage);
        this.server = new VertxSliceServer(
            RpmSliceDownloadITCase.VERTX,
            new LoggingSlice(new RpmSlice(storage, perms, auth, new RepoConfig.Simple()))
        );
        this.port = this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>("centos:centos8")
            .withCommand("tail", "-f", "/dev/null");
        this.cntn.start();
    }

}
