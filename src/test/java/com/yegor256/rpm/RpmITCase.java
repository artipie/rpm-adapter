/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Yegor Bugayenko
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
package com.yegor256.rpm;

import com.jcabi.log.Logger;
import com.yegor256.asto.Storage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Integration case for {@link Rpm}.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class RpmITCase {

    /**
     * Temp folder for all tests.
     */
    @Rule
    @SuppressWarnings("PMD.BeanMembersShouldSerialize")
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Make sure Docker is here.
     * @throws Exception If fails
     */
    @Before
    public void dockerExists() throws Exception {
        Assume.assumeThat(
            "Docker is NOT present at the build machine",
            new ProcessBuilder()
                .command("which", "docker")
                .start()
                .waitFor(),
            Matchers.equalTo(0)
        );
    }

    /**
     * RPM works.
     * @throws Exception If some problem inside
     */
    @Test
    public void savesAndLoads() throws Exception {
        final Path repo = this.folder.newFolder("repo").toPath();
        final Storage storage = new Storage.Simple(repo);
        final Rpm rpm = new Rpm(storage);
        final Path bin = this.folder.newFile("x.rpm").toPath();
        final String key = "nginx-1.16.1-1.el8.ngx.x86_64.rpm";
        Files.copy(
            RpmITCase.class.getResourceAsStream(
                String.format("/%s", key)
            ),
            bin,
            StandardCopyOption.REPLACE_EXISTING
        );
        storage.save(key, bin);
        rpm.update(key);
        final Path stdout = this.folder.newFile("stdout.txt").toPath();
        new ProcessBuilder()
            .command(
                "docker",
                "run",
                "--rm",
                "--volume",
                String.format("%s:/repo", repo),
                "yegor256/yum-utils",
                "/bin/bash",
                "-c",
                String.join(
                    "\n",
                    "set -e",
                    "set -x",
                    "cat /repo/repodata/primary.xml",
                    "cat /repo/repodata/filelists.xml",
                    "cat /repo/repodata/other.xml",
                    "dnf config-manager --add-repo=file:///repo",
                    "echo 'gpgcheck=0' >> /etc/yum.repos.d/repo.repo",
                    // @checkstyle LineLength (2 lines)
                    "dnf repolist --verbose --disablerepo='*' --enablerepo='repo'",
                    "ls -la /etc/yum.repos.d/",
                    "cat /etc/yum.repos.d/repo.repo",
                    "dnf install -y --verbose --disablerepo='*' --enablerepo='repo' nginx"
                )
            )
            .redirectOutput(stdout.toFile())
            .redirectErrorStream(true)
            .start()
            .waitFor();
        final String log = new String(Files.readAllBytes(stdout));
        Logger.debug(this, "Full stdout/stderr:\n%s", log);
        MatcherAssert.assertThat(
            log,
            Matchers.allOf(
                Matchers.containsString("nginx-1:1.16.1-1.el8.ngx.x86_64"),
                Matchers.containsString("Installed"),
                Matchers.containsString("Complete!")
            )
        );
    }

}
