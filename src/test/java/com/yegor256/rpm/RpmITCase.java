/**
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Integration case for {@link Rpm}.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 0.1
 */
public final class RpmITCase {

    /**
     * Temp folder for all tests.
     */
    @Rule
    @SuppressWarnings("PMD.BeanMembersShouldSerialize")
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * RPM works.
     * @throws Exception If some problem inside
     */
    @Test
    public void savesAndLoads() throws Exception {
        final Path repo = this.folder.newFolder("repo").toPath();
        final Rpm rpm = new Rpm(new Storage.Simple(repo));
        final Path bin = this.folder.newFile("x.rpm").toPath();
        Files.copy(
            RpmITCase.class.getResourceAsStream(
                "/nginx-module-xslt-1.16.1-1.el7.ngx.x86_64.rpm"
            ),
            bin,
            StandardCopyOption.REPLACE_EXISTING
        );
        rpm.publish(bin);
        final Path stdout = this.folder.newFile("stdout.txt").toPath();
        new ProcessBuilder()
            .command(
                "docker",
                "run",
                "--rm",
                "--volume",
                String.format("%s:/repo", repo),
                "af27e27cbf9d",
                "/bin/bash",
                "-c",
                String.join(
                    ";",
                    "createrepo /repo",
                    "yum-config-manager --add-repo file:///repo",
                    "yum --disablerepo='*' --enablerepo='repo' list available"
                )
            )
            .redirectOutput(stdout.toFile())
            .redirectError(stdout.toFile())
            .start()
            .waitFor();
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(stdout)),
            Matchers.allOf(
                Matchers.containsString("nginx-module-xslt.x86_64"),
                Matchers.containsString("1:1.16.1-1.el7.ngx")
            )
        );
    }

}
