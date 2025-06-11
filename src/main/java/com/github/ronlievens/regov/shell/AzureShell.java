package com.github.ronlievens.regov.shell;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@RequiredArgsConstructor
@Slf4j
public class AzureShell {

    private final Shell shell;

    public boolean checkLoggedIn(final boolean trace) {
        return shell.execute("az account show", trace).exitCode() == 0;
    }

    public String listPullRequest(@NonNull final String organization,
                                  @NonNull final String project,
                                  @NonNull final String repository,
                                  @NonNull final String sourceBranch,
                                  @NonNull final String targetBranch,
                                  @NonNull final String status,
                                  final boolean trace) {
        val result = shell.execute(("az repos pr list --organization \"%s\" --project \"%s\" --repository \"%s\" --source-branch \"%s\" --target-branch \"%s\" --status \"%s\"")
            .formatted(organization, project, repository, sourceBranch, targetBranch, status), trace);

        if (result.exitCode() == 0) {
            return result.value();
        }
        return null;
    }

    public String createPullRequest(@NonNull final String organization,
                                    @NonNull final String project,
                                    @NonNull final String repository,
                                    @NonNull final String sourceBranch,
                                    @NonNull final String title,
                                    @NonNull final String description,
                                    @NonNull final String targetBranch,
                                    @NonNull final String mergeCommitMessage,
                                    final boolean trace) {
        val prID = shell.execute(("""
             az repos pr create \
              --auto-complete\
              --organization "%s"\
              --project "%s"\
              --repository "%s"\
              --source-branch %s\
              --target-branch %s\
              --delete-source-branch true \
              --title "%s"\
              --description "%s"\
              --query "pullRequestId"\
              --merge-commit-message "%s"
            """).formatted(organization, project, repository, sourceBranch, targetBranch, title, description, mergeCommitMessage), trace);
        if (prID.exitCode() == 0) {
            return prID.value().trim().replace(System.lineSeparator(), "");
        }
        return null;
    }

    public String createPullRequestAndApprove(@NonNull final String organization,
                                              @NonNull final String project,
                                              @NonNull final String repository,
                                              @NonNull final String sourceBranch,
                                              @NonNull final String title,
                                              @NonNull final String description,
                                              @NonNull final String targetBranch,
                                              @NonNull final String mergeCommitMessage,
                                              final boolean trace) {

        val prID = createPullRequest(organization,
            project,
            repository,
            sourceBranch,
            title,
            description,
            targetBranch,
            mergeCommitMessage,
            trace);

        if (isNotBlank(prID)) {
            val result = shell.execute(("""
                 az repos pr set-vote \
                  --id "%s"\
                  --vote approve\
                  --organization "%s"\
                """).formatted(prID, organization), trace);
            if (result.exitCode() != 0) {
                log.warn("Unable to auto approve pull request {}", prID);
            }
            return prID;
        }
        log.warn("Unable to create approve pull request for {} in {}", repository, project);
        return null;
    }

    public String listBuildDefinition(@NonNull final String organization,
                                      @NonNull final UUID project,
                                      @NonNull final String repository,
                                      final boolean trace) {
        val result = shell.execute(("az pipelines build definition list --organization \"%s\"  --project \"%s\" --name \"%s\"").formatted(organization, project, repository), trace);
        if (result.exitCode() == 0) {
            return result.value();
        }
        return null;
    }

    public String listPipelinesRuns(@NonNull final String organization,
                                    @NonNull final UUID project,
                                    @NonNull final Integer pipelineId,
                                    @NonNull final String branch,
                                    final boolean trace) {
        val result = shell.execute(("az pipelines runs list --organization \"%s\" --project \"%s\" --pipeline-ids \"%s\" --branch \"%s\"").formatted(organization, project, pipelineId, branch), trace);
        if (result.exitCode() == 0) {
            return result.value();
        }
        return null;
    }

    public String pipelinesRuns(@NonNull final String organization,
                                @NonNull final UUID project,
                                @NonNull final Integer id,
                                final boolean trace) {
        val result = shell.execute(("az pipelines runs show --organization \"%s\" --project \"%s\" --id \"%s\"").formatted(organization, project, id), trace);
        if (result.exitCode() == 0) {
            return result.value();
        }
        return null;
    }
}
