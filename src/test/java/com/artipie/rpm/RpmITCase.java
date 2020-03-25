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
package com.artipie.rpm;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.fs.RxFile;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
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
import org.cactoos.text.Joined;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
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
    private static Path folder;

    /**
     * Yum repository dir. Used to verify yum is able to install stored package.
     */
    private static Path repodir;

    /**
     * Createrepo repository dir. Used to compare adapter generated metadata
     * with createrepo tool generated metadata
     */
    private static Path createrepodir;

    /**
     * Docker container to execute createrepo command.
     */
    private static YumUtilsContainer yumutils;

    /**
     * RPM works.
     *
     * @throws Exception If some problem inside
     */
    @Test
    public void savesAndLoads() throws Exception {
        final Vertx vertx = Vertx.vertx();
        final Storage storage = new FileStorage(RpmITCase.repodir, vertx.fileSystem());
        final Rpm rpm = new Rpm(storage, vertx);
        final Path bin = RpmITCase.folder.resolve("x.rpm");
        final String key = "nginx-1.16.1-1.el8.ngx.x86_64.rpm";
        Files.copy(
            RpmITCase.class.getResourceAsStream(
                String.format("/%s", key)
            ),
            bin,
            StandardCopyOption.REPLACE_EXISTING
        );
        storage.save(
            new Key.From(key),
            new Content.From(
                new RxFile(bin, vertx.fileSystem()).flow()
            )
        ).get();
        rpm.batchUpdate(Key.ROOT).blockingAwait();
        final Container.ExecResult result = RpmITCase.yumutils.execInContainer(
            "/bin/bash",
            "-c",
            String.join(
                "\n",
                "set -e",
                "set -x",
                "dnf config-manager --add-repo=file:///repo",
                "echo 'gpgcheck=0' >> /etc/yum.repos.d/repo.repo",
                "dnf repolist --verbose --disablerepo='*' --enablerepo='repo'",
                "dnf install -y --verbose --disablerepo='*' --enablerepo='repo' nginx"
            )
        );
        final String log = new Joined("\n", result.getStdout(), result.getStderr()).asString();
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
        final Vertx vertx = Vertx.vertx();
        final Path repo = RpmITCase.folder.resolve("rpm-files-repo");
        final Storage storage = new FileStorage(repo, vertx.fileSystem());
        final String key = "nginx-1.16.1-1.el8.ngx.x86_64.rpm";
        final Path bin = RpmITCase.folder.resolve(key);
        Files.copy(
            RpmITCase.class.getResourceAsStream(String.format("/%s", key)),
            bin,
            StandardCopyOption.REPLACE_EXISTING
        );
        storage.save(
            new Key.From(key),
            new Content.From(
                new RxFile(bin, vertx.fileSystem()).flow()
            )
        ).get();
        final Rpm rpm = new Rpm(storage, vertx);
        rpm.update(new Key.From(key)).blockingAwait();
        final String expected = this.etalon(bin);
        final String actual = primary(repo);
        comparePrimaryFiles(expected, actual);
        RpmITCase.yumutils.execInContainer(
            "rm", "-fr", "/createrepo-repo/repodata"
        );
    }

    /**
     * Prepare temporary folders for repositories and start docker container with yum utils.
     * @param tempdir Temporary folder fot tests
     * @throws Exception If fails
     */
    @BeforeAll
    static void startContainer(@TempDir final Path tempdir) throws Exception {
        RpmITCase.folder = tempdir;
        RpmITCase.repodir = Files.createDirectory(RpmITCase.folder.resolve("repo"));
        RpmITCase.createrepodir = Files.createDirectory(
            RpmITCase.folder.resolve("createrepo-repo")
        );
        RpmITCase.yumutils = new YumUtilsContainer()
            .withFileSystemBind(RpmITCase.repodir.toString(), "/repo")
            .withFileSystemBind(RpmITCase.createrepodir.toString(), "/createrepo-repo")
            .withCommand("tail", "-f", "/dev/null");
        RpmITCase.yumutils.start();
    }

    /**
     * Stop docker container with yum utils.
     */
    @AfterAll
    static void stopContainer() throws Exception {
        RpmITCase.yumutils.stop();
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
        final File originalprkg = new File(
            String.format(
                "%s/%s",
                RpmITCase.createrepodir.toAbsolutePath().toString(),
                rpm.getFileName()
            )
        );
        Files.copy(rpm, originalprkg.toPath());
        final Container.ExecResult result = RpmITCase.yumutils.execInContainer(
            "/bin/bash",
            "-c",
            String.join(
                "\n",
                "set -e",
                "set -x",
                "createrepo /createrepo-repo",
                "createrepo --update /createrepo-repo"
            )
        );
        final String log = new Joined("\n", result.getStdout(), result.getStderr()).asString();
        Logger.debug(this, "Full stdout/stderr:\n%s", log);
        return primary(RpmITCase.createrepodir);
    }

    private static String primary(final Path repo) throws IOException {
        final File repodata =
            new File(String.format("%s/repodata", repo.toString()));
        final File[] primarygzip = Objects.requireNonNull(
            repodata.listFiles((dir, name) -> name.endsWith("primary.xml.gz"))
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

    /**
     * Inner subclass to instantiate yum-utils container.
     * @since 0.3.3
     */
    private static class YumUtilsContainer extends GenericContainer<YumUtilsContainer> {
        YumUtilsContainer() {
            super("yegor256/yum-utils");
        }
    }
}
