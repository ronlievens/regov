package com.github.ronlievens.regov.shell;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ronlievens.regov.shell.model.*;
import com.github.ronlievens.regov.util.MapperUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Map;
import java.util.UUID;

import static com.github.ronlievens.regov.task.config.Settings.properties;


@Slf4j
@RequiredArgsConstructor
public class AzureRestShell {

    private static final ObjectMapper mapper = MapperUtils.createJsonMapper();

    private final Shell shell;

    public String call(@NonNull final String url, final boolean trace) {
        val encodedUrl = url.replaceAll(" ", "%20");
        log.trace("Calling: {}", encodedUrl);
        val result = shell.execute("az rest --method get --uri \"%s\" --resource \"%s\"".formatted(encodedUrl, properties().getAzure().getDevopsScope()), trace);
        if (result.exitCode() != 0) {
            throw new RuntimeException("Error calling rest %s".formatted(encodedUrl));
        }
        return result.value();
    }

    public AzureListWrapper<AzureProject> listProjects(@NonNull final String organization, final boolean trace) {
        try {
            return mapper.readValue(call("%s/%s/_apis/projects?%s".formatted(properties().getAzure().getUrlServer(), organization, properties().getAzure().getApiParameter()), trace), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public AzureListWrapper<AzureRepository> listRepositories(@NonNull final String organization, @NonNull final String project, final boolean trace) {
        try {
            return mapper.readValue(call("%s/%s/%s/_apis/git/repositories?%s".formatted(properties().getAzure().getUrlServer(), organization, project, properties().getAzure().getApiParameter()), trace), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public AzureListWrapper<AzureItem> listItems(@NonNull final String organization, @NonNull final String project, @NonNull final UUID repository, final boolean trace) {
        try {
            return mapper.readValue(call("%s/%s/%s/_apis/git/repositories/%s/items?%s&recursionLevel=Full".formatted(properties().getAzure().getUrlServer(), organization, project, repository, properties().getAzure().getApiParameter()), trace), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getRepositoryFile(@NonNull final String organization, @NonNull final String project, @NonNull final UUID repository, @NonNull final String file, final boolean trace) {
        return call("%s/%s/%s/_apis/git/repositories/%s/items?scopePath=%s&download=true&%s".formatted(properties().getAzure().getUrlServer(), organization, project, repository, file, properties().getAzure().getApiParameter()), trace);
    }

    public Map getRepository(@NonNull final String organization, @NonNull final UUID project, @NonNull final UUID repository, final boolean trace) {
        try {
            return mapper.readValue(call("%s/%s/%s/_apis/git/repositories/%s".formatted(properties().getAzure().getUrlServer(), organization, project, repository), trace), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public AzureListWrapper<AzureRefs> getRepositoryRefs(@NonNull final String organization, @NonNull final String project, @NonNull final UUID repository, final boolean trace) {
        try {
            return mapper.readValue(call("%s/%s/%s/_apis/git/repositories/%s/refs?%s".formatted(properties().getAzure().getUrlServer(), organization, project, repository, properties().getAzure().getApiParameter()), trace), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public AzureListWrapper<AzureRefs> getRepositoryTags(@NonNull final String organization, @NonNull final String project, @NonNull final UUID repository, final boolean trace) {
        try {
            return mapper.readValue(call("%s/%s/%s/_apis/git/repositories/%s/refs?filterContains=tags&%s".formatted(properties().getAzure().getUrlServer(), organization, project, repository, properties().getAzure().getApiParameter()), trace), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
