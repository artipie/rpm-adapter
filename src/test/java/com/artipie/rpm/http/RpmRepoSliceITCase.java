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
package com.artipie.rpm.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.auth.BasicIdentities;
import com.artipie.http.auth.Identities;
import com.artipie.http.auth.Permissions;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.rpm.Digest;
import com.artipie.rpm.NamingPolicy;
import com.artipie.rpm.Rpm;
import com.artipie.rpm.TestRpm;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * Test for {@link RpmSlice}.
 * @since 0.11
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class RpmRepoSliceITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Settings files.
     */
    private final Path stngs = Paths.get("src/test/resources/example.repo");

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    @Test
    void listAndInstall() throws Exception {
        this.start(Permissions.FREE, Identities.ANONYMOUS, "");
        this.asserts();
    }

    @Test
    void listAndInstallWithAuth() throws Exception {
        final String mark = "mark";
        final String pswd = "abc";
        this.start(
            (name, perm) -> mark.equals(name) && "download".equals(perm),
            new BasicIdentities(
                (name, pass) -> {
                    final Optional<String> res;
                    if (mark.equals(name) && pswd.equals(pass)) {
                        res = Optional.of(name);
                    } else {
                        res = Optional.empty();
                    }
                    return res;
                }
            ),
            String.format("%s:%s@", mark, pswd)
        );
        this.asserts();
    }

    @AfterEach
    void stopContainer() throws IOException {
        this.server.close();
        this.cntn.stop();
        Files.deleteIfExists(this.stngs);
    }

    @AfterAll
    static void close() {
        RpmRepoSliceITCase.VERTX.close();
    }

    /**
     * Assertions.
     * @throws Exception On error
     */
    private void asserts() throws Exception {
        MatcherAssert.assertThat(
            "Lists 'time' package",
            this.cntn.execInContainer(
                "yum", "repo-pkgs", "example", "list"
            ).getStdout(),
            new StringContainsInOrder(new ListOf<>("time.x86_64", "1.7-45.el7"))
        );
        MatcherAssert.assertThat(
            "Installs time package",
            this.cntn.execInContainer(
                "yum", "-y", "repo-pkgs", "example", "install"
            ).getStdout(),
            new StringContainsInOrder(new ListOf<>("time-1.7-45.el7.x86_64", "Complete!"))
        );
    }

    /**
     * Starts VertxSliceServer and docker container.
     * @param perms Permissions
     * @param auth Identities
     * @param cred String with user name and password to add in url, uname:pswd@
     * @throws Exception On error
     */
    private void start(final Permissions perms, final Identities auth, final String cred)
        throws Exception {
        final Storage storage = new InMemoryStorage();
        new TestRpm.Time().put(storage);
        new Rpm(storage, new NamingPolicy.HashPrefixed(Digest.SHA1), Digest.SHA256, true)
            .batchUpdate(Key.ROOT).blockingAwait();
        this.server = new VertxSliceServer(
            RpmRepoSliceITCase.VERTX,
            new LoggingSlice(new RpmSlice(storage, perms, auth))
        );
        final int port = this.server.start();
        Testcontainers.exposeHostPorts(port);
        this.stngs.toFile().createNewFile();
        Files.write(
            this.stngs,
            new ListOf<>(
                "[example]",
                "name=Example Repository",
                String.format("baseurl=http://%shost.testcontainers.internal:%d/", cred, port),
                "enabled=1",
                "gpgcheck=0"
            )
        );
        this.cntn = new GenericContainer<>("centos:latest")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind("./src/test/resources", "/home");
        this.cntn.start();
        this.cntn.execInContainer("mv", "/home/example.repo", "/etc/yum.repos.d/");
    }

}
