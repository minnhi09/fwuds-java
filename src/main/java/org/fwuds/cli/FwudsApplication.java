package org.fwuds.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "fwuds", mixinStandardHelpOptions = true, version = "fwuds-java 1.0.0-SNAPSHOT",
        description = "Mine frequent weighted utility patterns over sliding data streams.",
        subcommands = {MineCommand.class, BenchmarkCommand.class, DatasetsCommand.class})
public final class FwudsApplication implements Runnable {
    @Override public void run() { CommandLine.usage(this, System.out); }

    public static void main(String[] args) {
        int status = new CommandLine(new FwudsApplication()).execute(args);
        System.exit(status);
    }
}
