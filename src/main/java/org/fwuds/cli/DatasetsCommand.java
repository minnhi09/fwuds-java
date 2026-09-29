package org.fwuds.cli;

import picocli.CommandLine.Command;

@Command(name = "datasets", mixinStandardHelpOptions = true,
        description = "Dataset utilities.", subcommands = DatasetVerifyCommand.class)
final class DatasetsCommand implements Runnable {
    @Override public void run() { picocli.CommandLine.usage(this, System.out); }
}
