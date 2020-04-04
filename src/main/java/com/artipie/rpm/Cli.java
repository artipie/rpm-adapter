package com.artipie.rpm;

import org.apache.commons.cli.*;

public class Cli {
    private static final Option namingOption = Option.builder("np")
            .longOpt( "naming-policy" )
            .desc("(optional, default simple) configures NamingPolicy for Rpm")
            .hasArg()
            .build();
    private static final Option digestOption = Option.builder("d")
            .longOpt( "digest" )
            .desc("(optional, default sha256) configures Digest instance for Rpm")
            .hasArg()
            .build();

    public static void main(String[] args) {
        try {
            CommandLine cmd = parseCommand(args);
            /*
            * @todo  #68:30min start execution of the command
            *  you can use cmd.getArgs() to get args as array of strings
            *  you would expect something like ["update", "./package.rpm"]
            *  use this link for documentation
            *  https://commons.apache.org/proper/commons-cli/usage.html
            * */
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parse the args and get the command.
     *
     * @param args array of string of arguments
     * @return CommandLine object of the command
     * @throws ParseException if parsing failed
     */
    static private CommandLine parseCommand(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption(namingOption);
        options.addOption(digestOption);
        CommandLineParser parser = new DefaultParser();
        CommandLine c = parser.parse(options, args);
        if(c.getArgs().length!=2) throw new ParseException("Wrong arguments count, something is missing");
        return c;
    }
}
