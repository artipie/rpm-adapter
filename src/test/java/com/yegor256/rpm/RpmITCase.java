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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

/**
 * Integration case for {@link Rpm}.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
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
     *
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
     *
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
        storage.save(key, bin).blockingAwait();
        rpm.update(key).blockingAwait();
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
                    "cat /repo/repodata/repomd.xml",
                    "dnf config-manager --add-repo=file:///repo",
                    "echo 'gpgcheck=0' >> /etc/yum.repos.d/repo.repo",
                    // @checkstyle LineLength (2 lines)
                    "dnf repolist --verbose --disablerepo='*' --enablerepo='repo'",
                    "ls -la /etc/yum.repos.d/",
                    "ls -lhS /repo/repodata",
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

    /**
     * RPM meta info generated in the same way as native createrepo do it.
     *
     * @throws Exception If some problem inside
     */
    @Test
    public void primaryFileIsTheSameAsCreateRepoGenerates() throws Exception {
        final Path repo = this.folder.newFolder("rpm-files-repo").toPath();
        final Storage storage = new Storage.Simple(repo);
        final String key = "nginx-1.16.1-1.el8.ngx.x86_64.rpm";
        final Path bin = this.folder.newFile(key).toPath();
        Files.copy(
            RpmITCase.class.getResourceAsStream(String.format("/%s", key)),
            bin,
            StandardCopyOption.REPLACE_EXISTING
        );
        storage.save(key, bin).blockingAwait();
        final Rpm rpm = new Rpm(storage);
        rpm.update(key).blockingAwait();
        final String expected = this.etalon(bin);
        final String actual = primary(repo);
        comparePrimaryFiles(expected, actual);
    }

    private static void comparePrimaryFiles(
        final String expected,
        final String actual
    ) {
        final Diff diff = DiffBuilder.compare(Input.fromString(expected))
            .withTest(Input.fromString(actual))
            .ignoreWhitespace()
            .ignoreComments()
            .checkForSimilar()
            .normalizeWhitespace()
            .ignoreElementContentWhitespace()
            .withAttributeFilter(attr -> !attr.getName().equals("archive"))
            .withNodeFilter(
                node ->
                    !new ListOf<>(
                        "time",
                        "rpm:requires",
                        "rpm:provides",
                        "file"
                    ).contains(node.getNodeName())
            )
            .build();
        Logger.info(
            RpmITCase.class,
            "Comapring file:\n%s\nwith:\n%s\n",
            expected,
            actual
        );
        final ArrayList<Difference> diffarr = new ArrayList<>(0);
        diff.getDifferences().forEach(diffarr::add);
        final String diffstr = diffarr.stream()
            .map(Difference::toString)
            .collect(Collectors.joining("\n"));
        MatcherAssert.assertThat(
            // @checkstyle LineLength (1 lines)
            String.format("There are %d differences between rpm-files and createrepo primary.xml files:\n\n%s\n", diffarr.size(), diffstr),
            diff.hasDifferences(), Matchers.is(false)
        );
    }

    private String etalon(
        final Path rpm) throws IOException, InterruptedException {
        final Path repo =
            this.folder.newFolder("createrepo-repo").toPath();
        final Path stdout = this.folder.newFile("stdout.txt").toPath();
        final File originalprkg = new File(
            String.format(
                "%s/%s",
                repo.toAbsolutePath().toString(),
                rpm.getFileName()
            )
        );
        Files.copy(rpm, originalprkg.toPath());
        new ProcessBuilder()
            .command(
                "docker",
                "run",
                "--rm",
                "--volume",
                String.format("%s:/createrepo-repo", repo),
                "yegor256/yum-utils",
                "/bin/bash",
                "-c",
                String.join(
                    "\n",
                    "set -e",
                    "set -x",
                    "createrepo /createrepo-repo",
                    "createrepo --update /createrepo-repo"
                )
            )
            .redirectOutput(stdout.toFile())
            .redirectErrorStream(true)
            .start()
            .waitFor();
        final String log = new String(Files.readAllBytes(stdout));
        Logger.debug(this, "Full stdout/stderr:\n%s", log);
        return primary(repo);
    }

    private static String primary(final Path repo) throws IOException {
        final File folder =
            new File(String.format("%s/repodata", repo.toString()));
        final File[] primarygzip = Objects.requireNonNull(
            folder.listFiles((dir, name) -> name.endsWith("primary.xml.gz"))
        );
        if (primarygzip.length == 1) {
            return readGzFile(primarygzip[0]);
        }
        throw new IllegalStateException(".*primary.xml.gz file not found");
    }

    private static String readGzFile(final File file) throws IOException {
        try (
            BufferedReader stream = new BufferedReader(
                new InputStreamReader(
                    new GZIPInputStream(
                        Files.newInputStream(file.toPath())
                    )
                )
            )
        ) {
            final StringBuilder res = new StringBuilder();
            String line = stream.readLine();
            while (line != null) {
                res.append(line).append('\n');
                line = stream.readLine();
            }
            return res.toString();
        }
    }
}
