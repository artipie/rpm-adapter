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

import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.auth.BasicIdentities;
import com.artipie.http.auth.Identities;
import com.artipie.http.auth.Permissions;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.TestRpm;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
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
 * Integration test for {@link RpmSlice}.
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class RpmSliceITCase {

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
        this.start(Permissions.FREE, Identities.ANONYMOUS);
        MatcherAssert.assertThat(
            this.executeCommand(
                String.format(
                    "http://host.testcontainers.internal:%d/%s",
                    this.port, "time-1.7-45.el7.x86_64.rpm"
                )
            ),
            new StringContainsInOrder(new ListOf<>("time-1.7-45.el7.x86_64", "Complete!"))
        );
    }

    @Test
    void installsWithAuth() throws Exception {
        final String john = "john";
        final String pswd = "123";
        this.start(
            (name, perm) -> john.equals(name) && "download".equals(perm),
            new BasicIdentities(
                (name, pass) -> {
                    final Optional<String> res;
                    if (john.equals(name) && pswd.equals(pass)) {
                        res = Optional.of(name);
                    } else {
                        res = Optional.empty();
                    }
                    return res;
                }
            )
        );
        MatcherAssert.assertThat(
            this.executeCommand(
                String.format(
                    "http://%s:%s@host.testcontainers.internal:%d/%s", john,
                    pswd, this.port, "time-1.7-45.el7.x86_64.rpm"
                )
            ),
            new StringContainsInOrder(new ListOf<>("time-1.7-45.el7.x86_64", "Complete!"))
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

    private String executeCommand(final String url) throws IOException, InterruptedException {
        return this.cntn.execInContainer(
            "yum", "-y", "install", url
        ).getStdout();
    }

    private void start(final Permissions perms, final Identities auth) throws Exception {
        final Storage storage = new InMemoryStorage();
        new TestRpm.Time().put(storage);
        this.server = new VertxSliceServer(
            RpmSliceITCase.VERTX,
            new LoggingSlice(new RpmSlice(storage, perms, auth, new RepoConfig.Simple()))
        );
        this.port = this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>("centos:latest")
            .withCommand("tail", "-f", "/dev/null");
        this.cntn.start();
    }

}
