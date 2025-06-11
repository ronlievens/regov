package com.github.ronlievens.regov.task.rewrite.search;

import com.github.ronlievens.regov.exceptions.ExitException;
import com.github.ronlievens.regov.shell.AzureSearchShell;
import com.github.ronlievens.regov.task.rewrite.RewriteContext;
import com.github.ronlievens.regov.task.rewrite.RewriteRunnableTask;
import com.github.ronlievens.regov.util.LogbackUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;

import static com.github.ronlievens.regov.task.rewrite.utils.CsvUtils.writeSearchResultToCsv;
import static com.github.ronlievens.regov.util.PathUtils.deleteFolderWhenItExists;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@RequiredArgsConstructor
public class RewriteSearchTask implements RewriteRunnableTask {

    private final AzureSearchShell azureSearchShell;

    public RewriteSearchTask() {
        azureSearchShell = new AzureSearchShell();
    }

    @Override
    public void run(@NonNull final RewriteContext rewriteContext) throws ExitException {

        try {
            if (!deleteFolderWhenItExists(rewriteContext.getDestination(), rewriteContext.isForce())) {
                throw new ExitException();
            }
        } catch (IOException ioe) {
            log.error("Unable to delete folder '{}'", rewriteContext.getDestination().toAbsolutePath(), ioe);
            throw new ExitException();
        }

        if (rewriteContext.isTrace()) {
            LogbackUtils.attachTraceLogging("search", rewriteContext.getDestination());
        }

        try {
            Files.createDirectories(rewriteContext.getDestination());
        } catch (IOException ioe) {
            log.error("Unable to create folder '{}'", rewriteContext.getDestination().toAbsolutePath(), ioe);
            throw new ExitException();
        }

        if (isNotBlank(rewriteContext.getQuery())) {
            log.info("Start search for: {}", rewriteContext.getQuery());
            rewriteContext.setRepositories(azureSearchShell.search(rewriteContext.getAzureOrganizationProjects(), rewriteContext.getQuery(), rewriteContext.isTrace()));

            try {
                writeSearchResultToCsv(rewriteContext);
            } catch (IOException ioe) {
                log.error("IOException: {}", ioe.getMessage(), ioe);
                throw new ExitException();
            }
        }
    }
}
