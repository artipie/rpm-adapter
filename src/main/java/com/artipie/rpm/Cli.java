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
package com.artipie.rpm;

import com.artipie.asto.Key;
import com.artipie.asto.fs.FileStorage;
import com.artipie.rpm.CliArguments.CliParsedArguments;
import io.vertx.reactivex.core.Vertx;
import java.nio.file.Path;

/**
 * Cli tool main class.
 *
 * @since 0.6
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
     * @checkstyle IllegalCatchCheck (70 lines)
     * @checkstyle LineLengthCheck (50 lines)
     */
    @SuppressWarnings(
        {
            "PMD.SystemPrintln",
            "PMD.AvoidCatchingGenericException",
            "PMD.AvoidDuplicateLiterals"
        }
    )
    public static void main(final String... args) {
        final CliParsedArguments cliargs = new CliArguments().parsed(args);
        final NamingPolicy naming = cliargs.naming();
        System.out.printf("RPM naming-policy=%s\n", naming);
        final Digest digest = cliargs.digest();
        System.out.printf("RPM digest=%s\n", digest);
        final boolean filelists = cliargs.fileLists();
        System.out.printf("RPM file-lists=%s\n", filelists);
        final Path repository = cliargs.repository();
        System.out.printf("RPM repository=%s\n", repository);
        final Vertx vertx = Vertx.vertx();
        try {
            new Cli(
                new Rpm(
                    new FileStorage(
                        repository,
                        vertx.fileSystem()
                    ),
                    naming,
                    digest,
                    filelists
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
