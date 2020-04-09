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

import com.artipie.asto.Key;
import com.artipie.asto.fs.FileStorage;
import io.vertx.reactivex.core.Vertx;
import java.nio.file.Paths;
import java.util.Locale;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Cli tool main class.
 *
 * @since 0.6
 * @todo #89:30min Cli options are not parsed correctly. getOptionValue method always return
 *  default value for both options, so it's not possible to override them. But the tool is working
 *  correctly. To build it use `cli` maven profile, see the README.
 */
public final class Cli {

    /**
     * Rpm tool.
     */
    private final Rpm rpm;

    /**
     * Ctor.
     * @param rpm Rpm instance
     */
    private Cli(final Rpm rpm) {
        this.rpm = rpm;
    }

    /**
     * Main method of Cli tool.
     *
     * @param args Arguments of command line
     * @throws ParseException if parsing failed
     * @checkstyle IllegalCatchCheck (70 lines)
     * @checkstyle LineLengthCheck (50 lines)
     */
    @SuppressWarnings(
        {
            "PMD.SystemPrintln", "PMD.AvoidCatchingGenericException",
            "PMD.DoNotCallSystemExit", "PMD.AvoidDuplicateLiterals"
        }
    )
    public static void main(final String... args) throws ParseException {
        final CommandLine cli = new DefaultParser().parse(
            new Options()
                .addOption(
                    Option.builder("n")
                        .argName("np")
                        .longOpt("naming-policy")
                        .desc("(optional, default plain) configures NamingPolicy for Rpm: plain, sha256 or sha1")
                        .hasArg()
                        .build()
                )
                .addOption(
                    Option.builder("d")
                        .argName("dgst")
                        .longOpt("digest")
                        .desc("(optional, default sha256) configures Digest instance for Rpm: sha256 or sha1")
                        .hasArg()
                        .build()
                ),
            args
        );
        if (cli.getArgs().length != 1) {
            System.err.println("expected repository path");
            System.exit(1);
        }
        final StandardNamingPolicy naming = StandardNamingPolicy.valueOf(
            cli.getOptionValue("np", "plain").toUpperCase(Locale.US)
        );
        System.out.printf("RPM naming-policy=%s\n", naming);
        final Digest digest = Digest.valueOf(
            cli.getOptionValue("dgst", "sha256").toUpperCase(Locale.US)
        );
        System.out.printf("RPM digest=%s\n", digest);
        final Vertx vertx = Vertx.vertx();
        try {
            new Cli(
                new Rpm(
                    new FileStorage(
                        Paths.get(cli.getArgList().get(0)),
                        vertx.fileSystem()
                    ),
                    naming,
                    digest
                )
            ).run();
        } catch (final Exception err) {
            System.err.printf("RPM failed: %s\n", err.getLocalizedMessage());
            err.printStackTrace(System.err);
        } finally {
            vertx.close();
        }
    }

    /**
     * Run CLI tool.
     */
    private void run() {
        this.rpm.batchUpdate(Key.ROOT).blockingAwait();
    }
}
