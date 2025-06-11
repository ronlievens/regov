package com.github.ronlievens.regov.command.rewrite;

import com.github.ronlievens.regov.exceptions.ExitException;
import com.github.ronlievens.regov.task.rewrite.RewriteContext;
import com.github.ronlievens.regov.task.rewrite.RewriteRunnableTask;
import com.github.ronlievens.regov.task.rewrite.execute.RewriteExecuteTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import static com.github.ronlievens.regov.task.config.Settings.properties;


@Slf4j
@RequiredArgsConstructor
@Command(name = "execute", description = "Execute rewrite on one or more repositories")
class RewriteExecuteCommand extends AbstractRewriteCommonOptions implements Callable<Integer> {

    private final RewriteRunnableTask task;

    @Option(names = {"-r", "--recipe"}, description = "Which recipe to use, when left empty only the ")
    private String recipe;

    @Option(names = {"-rl", "--recipe-location"}, description = "Provide the location to a yaml file containing a recipe")
    private String recipeLocation;

    @Option(names = {"-b", "--batch-file"}, description = "Batch file of repositories to run the recipe on", required = true)
    private Path batchFile;

    public RewriteExecuteCommand() {
        task = new RewriteExecuteTask();
    }

    @Override
    public Integer call() throws Exception {

        if (!properties().getAzure().hasOrganizationProject()) {
            return 1;
        }

        try {
            task.run(RewriteContext.builder()
                .trace(trace)
                .ticket(getTicket())
                .destination(getDestination())
                .force(force)
                .skipRemote(skipRemote)
                .azureOrganizationProjects(properties().getAzure().getOrganizations())
                .recipe(recipe)
                .recipeLocation(recipeLocation)
                .batchFile(batchFile)
                .build());
            return 0;
        } catch (ExitException aee) {
            return 1;
        }
    }
}

