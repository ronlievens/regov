package com.github.ronlievens.regov.task.rewrite.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ronlievens.regov.exceptions.ExitException;
import com.github.ronlievens.regov.shell.AzureRestShell;
import com.github.ronlievens.regov.shell.AzureShell;
import com.github.ronlievens.regov.shell.Shell;
import com.github.ronlievens.regov.task.config.Settings;
import com.github.ronlievens.regov.task.rewrite.RewriteContext;
import com.github.ronlievens.regov.task.rewrite.RewriteRunnableTask;
import com.github.ronlievens.regov.util.LogbackUtils;
import com.github.ronlievens.regov.util.MapperUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.ronlievens.regov.task.config.Settings.properties;
import static com.github.ronlievens.regov.task.rewrite.RewriteContext.SOURCE_COMMIT_PREFIX;
import static com.github.ronlievens.regov.task.rewrite.report.ReportRepositoryModel.map;
import static com.github.ronlievens.regov.task.rewrite.utils.AzureSearchUtils.getCommitsFromPullRequestList;
import static com.github.ronlievens.regov.task.rewrite.utils.AzureSearchUtils.searchYamlUrl;
import static com.github.ronlievens.regov.task.rewrite.utils.CsvUtils.loadSearchResultFromCsv;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@RequiredArgsConstructor
public class RewriteReportTask implements RewriteRunnableTask {

    private final static ObjectMapper MAPPER = MapperUtils.createJsonMapper(true, true);
    private final static String BUILD_PIPELINE_URL = "https://dev.azure.com/%s/%s/_build/results?buildId=%s";
    private final static String STATUS = "all";

    private final static String PULLREQUEST_ARGO_ACCEPTANCE_POSTFIX = "%s_%s_acceptance";
    private final static String PULLREQUEST_ARGO_PRODUCTION_POSTFIX = "%s_%s_production";

    private final AzureShell azureShell;
    private final AzureRestShell azureRestShell;

    public RewriteReportTask() {
        val shell = new Shell();
        azureShell = new AzureShell(shell);
        azureRestShell = new AzureRestShell(shell);
    }

    @Override
    public void run(@NonNull final RewriteContext rewriteContext) throws ExitException {
        try {
            if (rewriteContext.isTrace()) {
                LogbackUtils.attachTraceLogging("report", rewriteContext.getResultFile().getParent());
            }

            try {
                Files.createDirectories(rewriteContext.getDestination());
            } catch (IOException ioe) {
                log.error("Unable to create folder '{}'", rewriteContext.getDestination().toAbsolutePath(), ioe);
                throw new ExitException();
            }

            try {
                loadSearchResultFromCsv(rewriteContext);
            } catch (IOException e) {
                log.error("IOException: {}", e.getMessage());
                throw new ExitException();
            }

            val result = readPreviousResult(rewriteContext);
            val pullRequestArgoList = new HashMap<String, String>();
            val pullRequestArgoCommitsList = new HashMap<String, Map<String, String>>();
            for (val organizationEntrySet : Settings.properties().getAzure().getOrganizations().entrySet()) {
                for (val project : organizationEntrySet.getValue().getProjects()) {
                    val pullRequestArgoAcceptance = azureShell.listPullRequest(organizationEntrySet.getKey(),
                        project,
                        organizationEntrySet.getValue().getArgocd().getAcceptance().getRepository(),
                        organizationEntrySet.getValue().getArgocd().getAcceptance().getSourceBranch().formatted(result.getTicket()),
                        organizationEntrySet.getValue().getArgocd().getAcceptance().getTargetBranch(),
                        STATUS,
                        rewriteContext.isTrace());

                    val pullRequestArgoProduction = azureShell.listPullRequest(organizationEntrySet.getKey(),
                        project,
                        organizationEntrySet.getValue().getArgocd().getProduction().getRepository(),
                        organizationEntrySet.getValue().getArgocd().getProduction().getSourceBranch().formatted(result.getTicket()),
                        organizationEntrySet.getValue().getArgocd().getProduction().getTargetBranch(),
                        STATUS,
                        rewriteContext.isTrace());

                    val acceptanceKey = PULLREQUEST_ARGO_ACCEPTANCE_POSTFIX.formatted(organizationEntrySet.getKey(), project);
                    pullRequestArgoList.put(acceptanceKey, pullRequestArgoAcceptance);
                    pullRequestArgoCommitsList.put(acceptanceKey, getCommitsFromPullRequestList(azureRestShell, pullRequestArgoAcceptance, rewriteContext.isTrace()));

                    val productionKey = PULLREQUEST_ARGO_PRODUCTION_POSTFIX.formatted(organizationEntrySet.getKey(), project);
                    pullRequestArgoList.put(productionKey, pullRequestArgoAcceptance);
                    pullRequestArgoCommitsList.put(acceptanceKey, getCommitsFromPullRequestList(azureRestShell, pullRequestArgoProduction, rewriteContext.isTrace()));
                }
            }

            if (rewriteContext.getRepositories() != null) {
                var count = 1;
                for (val repository : result.getRepositories()) {
                    log.info("Reporting on repository {} [{}/{}] ", repository.getName(), count, rewriteContext.getRepositories().size());

                    if (repository.getChange().getBuildStatus() == null || (repository.getChange().isCommit() && !repository.getChange().isMerged())) {
                        val pullRequest = azureShell.listPullRequest(repository.getOrganizationName(),
                            repository.getProjectName(),
                            repository.getName(),
                            rewriteContext.getBranchName(),
                            properties().getGit().getGitBranchMain(),
                            STATUS,
                            rewriteContext.isTrace());
                        if (pullRequest != null) {
                            repository.setChange(map(pullRequest));
                        }
                    }

                    lookupRun(repository.getOrganizationName(), repository.getProjectId(), repository.getName(), result.getTicket(), rewriteContext.isTrace(), repository.getChange());

                    val acceptanceKey = PULLREQUEST_ARGO_ACCEPTANCE_POSTFIX.formatted(repository.getOrganizationName(), repository.getProjectName());
                    var containerNewVersionUrl = searchYamlUrl(pullRequestArgoCommitsList.get(acceptanceKey), repository.getName());
                    if (isNotBlank(containerNewVersionUrl)) {
                        var containerOriginalVersionUrl = containerNewVersionUrl.substring(0, containerNewVersionUrl.lastIndexOf("?")) + "?versionOptions=firstParent";
                        repository.setAcceptance(map(pullRequestArgoList.get(acceptanceKey), searchYamlContainerVersion(containerOriginalVersionUrl, rewriteContext.isTrace()), searchYamlContainerVersion(containerNewVersionUrl, rewriteContext.isTrace())));
                    }

                    val productionKey = PULLREQUEST_ARGO_PRODUCTION_POSTFIX.formatted(repository.getOrganizationName(), repository.getProjectName());
                    containerNewVersionUrl = searchYamlUrl(pullRequestArgoCommitsList.get(productionKey), repository.getName());
                    if (isNotBlank(containerNewVersionUrl)) {
                        var containerOriginalVersionUrl = containerNewVersionUrl.substring(0, containerNewVersionUrl.lastIndexOf("?")) + "?versionOptions=firstParent";
                        repository.setProduction(map(pullRequestArgoList.get(productionKey), searchYamlContainerVersion(containerOriginalVersionUrl, rewriteContext.isTrace()), searchYamlContainerVersion(containerNewVersionUrl, rewriteContext.isTrace())));
                    }

                    count++;
                }
            }

            var filename = FilenameUtils.removeExtension(rewriteContext.getResultFile().toString());
            ExcelReportUtil.createExcelReport(Settings.properties().getTicketUrl(), Path.of(filename + ".xlsx"), result);

            try {
                Files.writeString(rewriteContext.getResultFile(), MAPPER.writeValueAsString(result));
            } catch (IOException e) {
                log.error("IOException: {}", e.getMessage());
                throw new ExitException();
            }
        } catch (IOException e) {
            log.error("{}", e.getMessage());
            throw new ExitException();
        }
    }

    private ReportModel readPreviousResult(final RewriteContext rewriteContext) throws IOException {
        if (rewriteContext.getResultFile() != null && Files.exists(rewriteContext.getResultFile())) {
            return MAPPER.readValue(Files.readString(rewriteContext.getResultFile()), ReportModel.class);
        }
        val report = new ReportModel();
        report.setTicket(rewriteContext.getTicket());
        for (val repository : rewriteContext.getRepositories()) {
            report.getRepositories().add(ReportRepositoryModel.create(repository));
        }
        return report;
    }

    private void lookupRun(@NonNull final String organization, @NonNull final UUID project, @NonNull final String name, @NonNull final String ticket, final boolean trace, final ReportRepositoryChangeModel change) {
        val buildDefinitions = azureShell.listBuildDefinition(organization, project, name, trace);
        if (isNotBlank(buildDefinitions)) {
            try {
                val definitions = MAPPER.readValue(buildDefinitions, new TypeReference<List<Map<String, Object>>>() {
                });

                val propetries = Settings.properties().getAzure().getOrganization(organization);
                for (val definition : definitions) {
                    if (definition.containsKey("path") && propetries.getPipeline().getPath().equals(definition.get("path"))) {
                        val runList = azureShell.listPipelinesRuns(organization, project, (Integer) definition.get("id"), propetries.getPipeline().getBranch(), trace);
                        if (isNotBlank(runList)) {
                            val runs = MAPPER.readValue(runList, new TypeReference<List<Map<String, Object>>>() {
                            });
                            for (val run : runs) {
                                if (run.containsKey("triggerInfo")) {
                                    val triggerInfo = (Map<String, String>) run.get("triggerInfo");
                                    if (StringUtils.equalsIgnoreCase(SOURCE_COMMIT_PREFIX.formatted(ticket), triggerInfo.get("ci.message"))) {
                                        val buildId = (Integer) run.get("id");
                                        change.setBuildId(buildId);
                                        change.setBuildUrl(BUILD_PIPELINE_URL.formatted(organization, project, buildId));
                                        val buildRuns = azureShell.pipelinesRuns(organization, project, buildId, trace);
                                        if (isNotBlank(buildRuns)) {
                                            val buildRun = MAPPER.readValue(buildRuns, new TypeReference<Map<String, Object>>() {
                                            });
                                            change.setBuildStatus((String) buildRun.get("result"));
                                        }
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (JsonProcessingException e) {
                log.warn("JsonProcessingException: {}", e.getMessage());
            }
        }
    }

    private String searchYamlContainerVersion(final String url, final boolean trace) {
        try {
            val yaml = azureRestShell.call(url, trace);

            val yamlMapper = new Yaml();
            Map<String, Object> container = yamlMapper.load(yaml);

            if (container != null && !container.isEmpty() && container.containsKey("image") && container.get("image") instanceof Map) {
                val containerImage = (Map<String, String>) container.get("image");
                return containerImage.get("repository") + ": " + containerImage.get("tag");
            }

        } catch (Exception e) {
            log.warn("Unable to retrieve container version for: {}", url);
            log.trace("Unable to retrieve container version for: {}", url, e);
        }

        return null;
    }
}
