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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Cli tool argument parsing.
 *
 * Arguments values must be passed immediately after argument declaration (e.g
 * -dsha256) or after ´=´ signal (e.g -d=sha256).
 *
 * @since 0.9
 */
public final class CliArguments {

    /**
     * Digest option.
     */
    private static final Option DIGEST = Option.builder("d")
        .argName("dgst")
        .longOpt("digest")
        .desc("(optional, default sha256) configures Digest instance for Rpm: sha256 or sha1")
        .hasArg()
        .build();

    /**
     * Naming policy option.
     */
    private static final Option NAMING_POLICY = Option.builder("n")
        .argName("np")
        .longOpt("naming-policy")
        .desc("(optional, default plain) configures NamingPolicy for Rpm: plain, sha256 or sha1")
        .hasArg()
        .build();

    /**
     * FileLists option.
     */
    private static final Option FILE_LISTS = Option.builder("f")
        .argName("fl")
        .longOpt("filelists")
        .desc("(optional, default true) includes File Lists for Rpm: true or false")
        .hasArg()
        .build();

    /**
     * Cli options.
     */
    private final Options options;

    /**
     * Ctor.
     */
    public CliArguments() {
        this (
            new Options()
                .addOption(CliArguments.DIGEST)
                .addOption(CliArguments.NAMING_POLICY)
                .addOption(CliArguments.FILE_LISTS)
        );
    }

    /**
     * Ctor.
     *
     * @param options Cli options.
     */
    private CliArguments(final Options options) {
        this.options = options;
    }

    /**
     * Parsed cli arguments.
     *
     * @param args Command line arguments
     * @return Parsed arguments
     * @throws IllegalArgumentException If there is an error during arg parsing
     */
    public CliParsedArguments parsed(final String... args) {
        try {
            return new CliParsedArguments(
                new DefaultParser().parse(this.options, args)
            );
        } catch (final ParseException ex) {
            throw new IllegalArgumentException(
                String.format("Can't parse arguments '%s'", Arrays.asList(args)),
                ex
            );
        }
    }

    /**
     * Cli tool parsed argument.
     *
     * @since 0.9
     */
    public final class CliParsedArguments {

        /**
         * Cli.
         */
        private final CommandLine cli;

        /**
         * Ctor.
         * @param cli Cli.
         */
        private CliParsedArguments(final CommandLine cli) {
            this.cli = cli;
        }

        /**
         * Digest.
         *
         * @return Digest.
         * @throws IllegalArgumentException If the arg value is incorrect
         */
        public Digest digest() {
            return Digest.valueOf(
                this.cli.getOptionValue(
                    CliArguments.DIGEST.getOpt(), "sha256"
                ).toUpperCase(Locale.US)
            );
        }

        /**
         * Naming.
         *
         * @return Naming.
         * @throws IllegalArgumentException If the arg value is incorrect
         */
        public NamingPolicy naming() {
            return StandardNamingPolicy.valueOf(
                this.cli.getOptionValue(
                    CliArguments.NAMING_POLICY.getOpt(), "plain"
                ).toUpperCase(Locale.US)
            );
        }

        /**
         * Repository.
         *
         * @return Repository.
         * @throws IllegalArgumentException If the arg value is incorrect
         */
        public Path repository() {
            final List<String> args = this.cli.getArgList();
            if (args.size() != 1) {
                throw new IllegalArgumentException(
                    String.format(
                        "Expected repository path but got: %s",
                        args
                    )
                );
            }
            return Paths.get(args.get(0));
        }

        /**
         * Include File Lists.
         *
         * @return Boolean.
         * @throws IllegalArgumentException If the arg value is incorrect
         */
        public boolean fileLists() {
            return Boolean.parseBoolean(
                this.cli.getOptionValue(CliArguments.FILE_LISTS.getOpt(), "true")
            );
        }
    }
}
