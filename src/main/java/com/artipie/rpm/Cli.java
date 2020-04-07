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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Cli tool main class.
 *
 * @since 0.1
 */
public final class Cli {
    /**
     * Naming policy option object.
     */
    private static final Option NAMING_OPTION = Option.builder("np")
        .longOpt("naming-policy")
        .desc("(optional, default simple) configures NamingPolicy for Rpm")
        .hasArg()
        .build();

    /**
     * Digest option object.
     */
    private static final Option DIGEST_OPTION = Option.builder("d")
        .longOpt("digest")
        .desc("(optional, default sha256) configures Digest instance for Rpm")
        .hasArg()
        .build();

    /**
     * Options object.
     */
    private static final Options OPTIONS = new Options();

    static {
        Cli.OPTIONS.addOption(Cli.NAMING_OPTION);
        Cli.OPTIONS.addOption(Cli.DIGEST_OPTION);
    }

    /**
     * Command line parser object.
     */
    private static final CommandLineParser PARSER = new DefaultParser();

    /**
     * Ctor.
     */
    private Cli() {
    }

    /**
     * Main method of Cli tool.
     *
     * @param args Arguments of command line
     * @throws ParseException if parsing failed
     * @todo  #68:30min start execution of the command
     *  you can use cmd.getArgs() to get args as array of strings
     *  you would expect something like ["update", "./package.rpm"]
     *  use this link for documentation
     *  https://commons.apache.org/proper/commons-cli/usage.html
     */
    @SuppressWarnings("ProhibitPublicStaticMethods")
    public static void main(final String[] args) throws ParseException {
        @SuppressWarnings("PMD.UnusedLocalVariable")
        final CommandLine cmd = parseCommand(args);
    }

    /**
     * Parse the args and get the command.
     *
     * @param args Array of string of arguments
     * @return CommandLine Object of the command
     * @throws ParseException if parsing failed
     */
    private static CommandLine parseCommand(final String... args) throws ParseException {
        final CommandLine cmd = Cli.PARSER.parse(Cli.OPTIONS, args);
        if (cmd.getArgs().length != 2) {
            throw new Cli.WrongArgumentException();
        }
        return cmd;
    }

    /**
     * Exception for passing wrong number of arguments.
     *
     * @since 0.1
     */
    public static final class WrongArgumentException extends ParseException {
        /**
         * This exception {@code serialVersionUID}.
         */
        private static final long serialVersionUID = 9112808380089253196L;

        /**
         * Construct a new <code>ParseException</code>
         * with the specified detail message.
         */
        public WrongArgumentException() {
            super("Wrong arguments count, something is missing");
        }
    }
}
