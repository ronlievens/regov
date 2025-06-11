package com.github.ronlievens.regov.command;

import com.github.ronlievens.regov.command.rewrite.RewriteCommand;
import com.github.ronlievens.regov.exceptions.ExitException;
import com.github.ronlievens.regov.shell.AzureShell;
import com.github.ronlievens.regov.shell.Shell;
import com.github.ronlievens.regov.task.config.Settings;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import static com.github.ronlievens.regov.shell.Shell.isCommandAvailable;
import static com.github.ronlievens.regov.util.LogbackUtils.adjustLogLevel;
import static com.github.ronlievens.regov.util.LogbackUtils.attachLogFile;

@Slf4j
@Command(
    name = "regov",
    description = "(\"Rewrite\" + \"Governance\") – Emphasizes code rewriting, governance, and structured tracking.",
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider.class,
    subcommands = {
        GenerateCompletion.class,
        RewriteCommand.class
    }
)
public class RootCommand {

    private static final String[] NEEDED_COMMANDS = {"curl", "jq", "git", "az"};

    @Option(names = {"-P", "--profile"}, description = "Set profile", defaultValue = "default")
    public void setProfile(final String profile) throws ExitException {
        log.info("Enable profile: {}", profile);
        Settings.getInstance(profile);
    }

    @Option(names = "--log-level", description = "Set log level (default log level is INFO)")
    public void setLogLevel(String logLevel) {
        log.info("Enable log level: {}", logLevel);
        adjustLogLevel(logLevel);
    }

    @Option(names = "--log-file", description = "Enable log to a file (log to stdout will not change)")
    public void setLogFile(String logFile) {
        log.info("Enable log file: {}", logFile);
        attachLogFile(logFile);
    }

    public static void main(final String... args) {
        for (val cmd : NEEDED_COMMANDS) {
            if (!isCommandAvailable(cmd)) {
                log.error("{} must be available on the system to use in the shell. Please install {} and try again.", cmd, cmd);
                System.exit(404);
            }
        }
        val azureShell = new AzureShell(new Shell());
        if (!azureShell.checkLoggedIn(false)) {
            log.info("Please log into azure before running regov! (az login) or (az login --use-device-code)");
            return;
        }

        val rootCommand = new RootCommand();
        val exitCode = new CommandLine(rootCommand)
            .setExecutionStrategy(rootCommand::executionStrategy)
            .execute(args);
        System.exit(exitCode);
    }

    private int executionStrategy(CommandLine.ParseResult parseResult) {
        return new CommandLine.RunLast().execute(parseResult);
    }
}
