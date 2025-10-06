package com.github.ronlievens.regov.task.rewrite.execute;

import com.github.ronlievens.regov.exceptions.ExitException;
import com.github.ronlievens.regov.shell.AzureShell;
import com.github.ronlievens.regov.shell.GitShell;
import com.github.ronlievens.regov.shell.Shell;
import com.github.ronlievens.regov.shell.model.AzureRepository;
import com.github.ronlievens.regov.task.config.Settings;
import com.github.ronlievens.regov.task.rewrite.RewriteContext;
import com.github.ronlievens.regov.task.rewrite.RewriteRunnableTask;
import com.github.ronlievens.regov.task.rewrite.utils.RewriteUtils;
import com.github.ronlievens.regov.util.LogbackUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.openrewrite.Recipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static com.github.ronlievens.regov.task.config.Settings.properties;
import static com.github.ronlievens.regov.task.rewrite.RewriteContext.SOURCE_COMMIT_PREFIX;
import static com.github.ronlievens.regov.task.rewrite.utils.CsvUtils.loadSearchResultFromCsv;
import static com.github.ronlievens.regov.task.rewrite.utils.RewriteUtils.loadRecipe;
import static com.github.ronlievens.regov.util.PathUtils.createDirectory;
import static com.github.ronlievens.regov.util.PathUtils.deleteFolderWhenItExists;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@RequiredArgsConstructor
public class RewriteExecuteTask implements RewriteRunnableTask {

    private final GitShell gitShell;
    private final AzureShell azureShell;

    public RewriteExecuteTask() {
        val shell = new Shell();
        gitShell = new GitShell(shell);
        azureShell = new AzureShell(shell);
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
            LogbackUtils.attachTraceLogging(rewriteContext.getDestination());
        }

        try {
            Files.createDirectories(rewriteContext.getDestination());
        } catch (IOException ioe) {
            log.error("Unable to create folder '{}'", rewriteContext.getDestination().toAbsolutePath(), ioe);
            throw new ExitException();
        }

        if (rewriteContext.getBatchFile() != null) {
            if (!rewriteContext.isForce()) {
                System.out.println("Do you want to rewrite the repositories:");
                for (val repository : rewriteContext.getRepositories()) {
                    System.out.printf("- %s [%s]%n", repository.getName(), repository.isEnabledForRewrite() ? "Yes" : "No");
                }

                val scanner = new Scanner(System.in);
                System.out.print("Are you sure? (Y|n)");
                val userInput = scanner.next().trim().toUpperCase();
                if (!"Y".equals(userInput)) {
                    log.info("Command cancelled.");
                    throw new ExitException();
                }
            }
        }

        try {
            loadSearchResultFromCsv(rewriteContext);
        } catch (IOException e) {
            log.error("IOException: {}", e.getMessage());
            throw new ExitException();
        }

        if (isNotBlank(rewriteContext.getRecipe()) || isNotBlank(rewriteContext.getRecipeLocation())) {
            val recipe = loadRecipe(rewriteContext.getRecipeLocation(), rewriteContext.getRecipe());
            if (recipe.getRecipeList().isEmpty()) {
                log.error("No recipes found!");
                throw new ExitException();
            }

            if (rewriteContext.getRepositories() != null) {
                var count = 1;
                for (val repository : rewriteContext.getRepositories()) {
                    log.info("Run recipe for repository {} [{}/{}] ", repository.getName(), count, rewriteContext.getRepositories().size());
                    executeRecipe(rewriteContext, repository, recipe);
                    count++;
                }
            }
        }
    }

    private void executeRecipe(@NonNull final RewriteContext rewriteContext, @NonNull final AzureRepository repository, @NonNull final Recipe recipe) throws ExitException {
        var destinationGit = rewriteContext.getDestination();
        if (repository.getSshUrl() != null) {
            destinationGit = rewriteContext.getDestination().resolve(repository.getId().toString());
            createDirectory(destinationGit);
            log.info("Cloning {} to:{}", repository.getSshUrl(), destinationGit);
            gitShell.cloneTo(repository.getSshUrl(), destinationGit, rewriteContext.isTrace());
            log.info("Create feature branch {}", rewriteContext.getBranchName());
            gitShell.checkoutFeatureBranchLocal(rewriteContext.getBranchName(), destinationGit, rewriteContext.isTrace());
            gitShell.pullRemoteBranch(rewriteContext.getBranchName(), destinationGit, rewriteContext.isTrace());
        } else {
            val currentBranch = gitShell.getCurrentBranchInLocalRepository(rewriteContext.getDestination(), rewriteContext.isTrace());
            if (!rewriteContext.getBranchName().equals(currentBranch)) {
                log.info("Create feature branch {}", rewriteContext.getBranchName());
                gitShell.checkoutFeatureBranchLocal(rewriteContext.getBranchName(), rewriteContext.getDestination(), rewriteContext.isTrace());
            }
        }

        if (runRecipe(recipe, destinationGit)) {
            processRemote(rewriteContext, repository, destinationGit);
            return;
        }

        log.info("No changes detected for {} with recipe {}", repository.getSshUrl(), rewriteContext.getRecipeName());
    }

    private static boolean runRecipe(final Recipe recipe, final Path projectFile) throws ExitException {
        try {
            log.info("Running recipe {} on path [{}]", recipe, projectFile);
            return RewriteUtils.rewrite(projectFile, recipe);
        } catch (IOException e) {
            log.error("IOException: {}", e.getMessage(), e);
            throw new ExitException();
        }
    }

    private void processRemote(final RewriteContext rewriteContext, final AzureRepository repository, final Path destinationGit) throws ExitException {
        val message = "Update project with rewrite recipe %s".formatted(rewriteContext.getRecipeName());
        val isCommitted = gitShell.commit(message, destinationGit, rewriteContext.isTrace());
        log.info("Commit '{}' is successful {}.", message, isCommitted);

        if (!rewriteContext.isSkipRemote() && isCommitted) {
            if (isBlank(repository.getSshUrl())) {
                log.error("Please provide a git url");
                throw new ExitException();
            }
            gitShell.push(destinationGit, rewriteContext.isTrace());
            log.info("Commit pushed to remote repository: {}", destinationGit);

            val mergeCommitMessage = SOURCE_COMMIT_PREFIX.formatted(rewriteContext.getTicket());
            if (Settings.properties().isAutoApproveEnabled()) {
                azureShell.createPullRequestAndApprove("%s/%s".formatted(properties().getAzure().getUrlServer(), repository.getProject().getOrganizationName()),
                    repository.getProject().getName(),
                    repository.getName(),
                    rewriteContext.getBranchName(),
                    "Auto rewrite %s".formatted(rewriteContext.getRecipeName()),
                    message,
                    properties().getGit().getGitBranchMain(),
                    mergeCommitMessage,
                    rewriteContext.isTrace());
                log.info("Pull request created and approved: {}", mergeCommitMessage);
            } else {
                azureShell.createPullRequest("%s/%s".formatted(properties().getAzure().getUrlServer(), repository.getProject().getOrganizationName()),
                    repository.getProject().getName(),
                    repository.getName(),
                    rewriteContext.getBranchName(),
                    "Auto rewrite %s".formatted(rewriteContext.getRecipeName()),
                    message,
                    properties().getGit().getGitBranchMain(),
                    mergeCommitMessage,
                    false,
                    rewriteContext.isTrace());
                log.info("Pull request created: {}", mergeCommitMessage);
            }
        } else {
            log.info("--skip-remote is enabled, so commit NOT pushed to remote repository");
        }
    }
}
