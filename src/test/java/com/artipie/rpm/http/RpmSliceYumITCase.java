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
import com.artipie.rpm.RepoConfig;
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
 * @since 0.10
 * @todo #273:30min We need to test that dnf rpm client can interact with rpm repository
 *  created by rpm-adapter. Test can be the same as this one but use dnf instead of yum. Please
 *  introduce separate class for this test.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class RpmSliceYumITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Settings files.
     */
    private final Path setting = Paths.get("src/test/resources/example.repo");

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    @Test
    void canListAndInstallFromArtipieRepo() throws Exception {
        this.start(Permissions.FREE, Identities.ANONYMOUS, "");
        MatcherAssert.assertThat(
            "Lists 'time' package",
            this.exec("list"),
            new StringContainsInOrder(new ListOf<>("time.x86_64", "1.7-45.el7"))
        );
        MatcherAssert.assertThat(
            "Installs time package",
            this.exec("install"),
            new StringContainsInOrder(new ListOf<>("time-1.7-45.el7.x86_64", "Complete!"))
        );
    }

    @Test
    void canListAndInstallFromArtipieRepoWithAuth() throws Exception {
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
        MatcherAssert.assertThat(
            "Lists 'time' package",
            this.exec("list"),
            new StringContainsInOrder(new ListOf<>("time.x86_64", "1.7-45.el7"))
        );
        MatcherAssert.assertThat(
            "Installs time package",
            this.exec("install"),
            new StringContainsInOrder(new ListOf<>("time-1.7-45.el7.x86_64", "Complete!"))
        );
    }

    @AfterEach
    void stopContainer() throws IOException {
        this.server.close();
        this.cntn.stop();
        Files.deleteIfExists(this.setting);
    }

    @AfterAll
    static void close() {
        RpmSliceYumITCase.VERTX.close();
    }

    /**
     * Executes command in container.
     * @param action What to do
     * @return String stdout
     * @throws Exception On error
     */
    private String exec(final String action) throws Exception {
        return this.cntn.execInContainer(
            "yum", "-y", "repo-pkgs", "example", action
        ).getStdout();
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
        final RepoConfig config = new RepoConfig.Simple(
            Digest.SHA256, new NamingPolicy.HashPrefixed(Digest.SHA1), true
        );
        new Rpm(storage, config).batchUpdate(Key.ROOT).blockingAwait();
        this.server = new VertxSliceServer(
            RpmSliceYumITCase.VERTX,
            new LoggingSlice(new RpmSlice(storage, perms, auth, config))
        );
        final int port = this.server.start();
        Testcontainers.exposeHostPorts(port);
        this.setting.toFile().createNewFile();
        Files.write(
            this.setting,
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
