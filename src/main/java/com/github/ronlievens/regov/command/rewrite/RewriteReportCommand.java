package com.github.ronlievens.regov.command.rewrite;

import com.github.ronlievens.regov.task.rewrite.RewriteContext;
import com.github.ronlievens.regov.task.rewrite.RewriteRunnableTask;
import com.github.ronlievens.regov.task.rewrite.report.RewriteReportTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import static com.github.ronlievens.regov.task.config.Settings.properties;

@Slf4j
@Command(name = "report", description = "Report on rewrite results")
@RequiredArgsConstructor
public class RewriteReportCommand extends AbstractRewriteCommonOptions implements Callable<Integer> {

    private static final String DEFAULT_RESULT_FILE = "rewrite-result-list.json";

    private final RewriteRunnableTask task;

    @Option(names = {"-b", "--batch-file"}, description = "Batch file of repositories recipe executed on", required = true)
    private Path batchFile;

    @Option(names = {"--result-file"}, description = "Re-used result file of the previous report (" + DEFAULT_RESULT_FILE + ")")
    private Path resultFile;

    public RewriteReportCommand() {
        task = new RewriteReportTask();
    }

    @Override
    public Integer call() throws Exception {

        if (batchFile == null && resultFile == null) {
            log.error("Either batch-file or result-file must be specified");
            return 1;
        }

        if (!properties().getAzure().hasOrganizationProject()) {
            return 1;
        }

        if (resultFile == null || !Files.exists(resultFile)) {
            val destination = getDestination();
            resultFile = destination.resolve(DEFAULT_RESULT_FILE);
        }

        task.run(RewriteContext.builder()
            .trace(trace)
            .ticket(getTicket())
            .destination(getDestination())
            .force(force)
            .azureOrganizationProjects(properties().getAzure().getOrganizations())
            .batchFile(batchFile)
            .resultFile(resultFile)
            .build());
        return 0;
    }
}
