package com.github.ronlievens.regov.command.rewrite;

import picocli.CommandLine.Command;

@Command(
    name = "rewrite",
    description = "Rewrites a given project using a predefined set of rules",
    subcommands = {
        RewriteReportCommand.class,
        RewriteSearchCommand.class,
        RewriteExecuteCommand.class
    })
public class RewriteCommand {
}
