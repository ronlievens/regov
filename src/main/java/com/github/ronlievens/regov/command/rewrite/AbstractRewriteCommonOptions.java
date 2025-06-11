package com.github.ronlievens.regov.command.rewrite;

import com.github.ronlievens.regov.exceptions.ExitException;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
public abstract class AbstractRewriteCommonOptions {

    @Option(names = {"-p", "--path"}, description = "Optional destination path to write output to. It defaults to the 'current' directory.")
    private Path destination;

    @Option(names = {"--force"}, description = "Force delete local project if exist.")
    protected boolean force;

    @Option(names = {"-s", "--skip-remote"}, description = "Do not push or create project to remote azure devops (no build pipeline and no git repository)")
    protected boolean skipRemote;

    @Option(names = "--trace", description = "Create trace file with all azure communication")
    protected boolean trace;

    @Option(names = {"-t", "--ticket"}, description = "The ID of the related ticket.", required = true)
    private String ticket;

    protected String getTicket() throws ExitException {
        return ticket.trim().toUpperCase();
    }

    protected Path getDestination() throws IOException {
        if (destination == null) {
            destination = Files.createTempDirectory("regov-");
            destination.toFile().deleteOnExit();
            log.info("No path set fallback on current path with project name: {}", destination.toAbsolutePath());
        }
        return destination;
    }
}
