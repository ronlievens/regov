package com.github.ronlievens.regov.command.rewrite;

import com.github.ronlievens.regov.exceptions.ExitException;
import com.github.ronlievens.regov.task.rewrite.RewriteContext;
import com.github.ronlievens.regov.task.rewrite.RewriteRunnableTask;
import com.github.ronlievens.regov.task.rewrite.search.RewriteSearchTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

import static com.github.ronlievens.regov.task.config.Settings.properties;

@Slf4j
@Command(name = "search", description = "Search azure devops for change")
@RequiredArgsConstructor
public class RewriteSearchCommand extends AbstractRewriteCommonOptions implements Callable<Integer> {

    private final RewriteRunnableTask task;

    public RewriteSearchCommand() {
        task = new RewriteSearchTask();
    }

    @Option(names = "--query", description = "Query to search azure repositories")
    private String query;

    @Option(names = "--number-rows", description = "Number of rows per result files")
    private Integer numberRows;

    @Override
    public Integer call() throws Exception {

        if (!properties().getAzure().hasOrganizationProject()) {
            return 1;
        }

        try {
            task.run(RewriteContext.builder()
                .trace(trace)
                .destination(getDestination())
                .force(force)
                .ticket(getTicket())
                .azureOrganizationProjects(properties().getAzure().getOrganizations())
                .query(query)
                .numberRows(numberRows)
                .build());
            return 0;
        } catch (ExitException aee) {
            log.error("Unable to finish report", aee);
            return 1;
        }
    }
}
