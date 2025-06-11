package com.github.ronlievens.regov.task.rewrite.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ronlievens.regov.shell.AzureRestShell;
import com.github.ronlievens.regov.util.MapperUtils;
import lombok.NonNull;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class AzureSearchUtils {

    private final static String COMMIT_URL = "%s/commits/%s";
    private final static ObjectMapper MAPPER = MapperUtils.createJsonMapper(true, true);

    // note: looping is a performance hit, this can be faster
    public static String searchYamlUrl(final Map<String, String> list, final String keyword) {
        if (isNotBlank(keyword)) {
            for (val item : list.keySet()) {
                if (isNotBlank(item)) {
                    val searchItem = item.substring(item.lastIndexOf("/") + 1, item.lastIndexOf("."));
                    if (StringUtils.equalsIgnoreCase(keyword, searchItem)) {
                        return list.get(item);
                    }
                }
            }
        }
        return null;
    }

    public static Map<String, String> getCommitsFromPullRequestList(@NonNull final AzureRestShell azureRestShell,
                                                                    final String prList,
                                                                    final boolean trace) throws JsonProcessingException {
        val resultSet = new HashMap<String, String>();
        if (isNotBlank(prList)) {
            val azurePrList = MAPPER.readValue(prList, new TypeReference<List<Map<String, Object>>>() {
            });
            if (azurePrList != null && !azurePrList.isEmpty()) {
                for (val azurePrItem : azurePrList) {
                    resultSet.putAll(getChangesFromPullRequest(azureRestShell, azurePrItem, trace));
                }
            }
        }
        return resultSet;
    }

    public static Map<String, String> getChangesFromPullRequest(@NonNull final AzureRestShell azureRestShell,
                                                                final Map pr,
                                                                final boolean trace) throws JsonProcessingException {
        val resultSet = new HashMap<String, String>();
        if (pr != null && pr.containsKey("url") && pr.containsKey("repository")) {
            val repository = MAPPER.readValue(azureRestShell.call((String) pr.get("url"), trace), Map.class);
            if (repository != null && repository.containsKey("_links") && repository.get("_links") instanceof Map links) {
                if (links.containsKey("iterations") && links.get("iterations") instanceof Map iterations) {
                    if (iterations.containsKey("href") && iterations.get("href") instanceof String url) {
                        resultSet.putAll(getChangesFromIteration(
                            (String) ((Map) pr.get("repository")).get("url"),
                            azureRestShell,
                            MAPPER.readValue(azureRestShell.call(url, trace), Map.class), trace));
                    }
                }
            }
        }
        return resultSet;
    }

    public static Map<String, String> getChangesFromIteration(@NonNull final String repositoryUrl,
                                                              @NonNull final AzureRestShell azureRestShell,
                                                              final Map iterations,
                                                              final boolean trace) throws JsonProcessingException {
        val resultSet = new HashMap<String, String>();
        val iterationList = (List<Map>) iterations.get("value");
        for (val iteration : iterationList) {
            val url = COMMIT_URL.formatted(repositoryUrl, ((Map<String, String>) iteration.get("sourceRefCommit")).get("commitId"));
            resultSet.putAll(getChanges(azureRestShell, MAPPER.readValue(azureRestShell.call(url, trace), Map.class), trace));
        }
        return resultSet;
    }

    public static Map<String, String> getChanges(@NonNull final AzureRestShell azureRestShell,
                                                 final Map commit,
                                                 final boolean trace) throws
        JsonProcessingException {
        val resultSet = new HashMap<String, String>();
        if (commit != null && commit.containsKey("_links") && commit.get("_links") instanceof Map links) {
            if (links.containsKey("changes") && links.get("changes") instanceof Map changes) {
                if (changes.containsKey("href") && changes.get("href") instanceof String url) {
                    resultSet.putAll(getItems(MAPPER.readValue(azureRestShell.call(url, trace), Map.class)));
                }
            }
        }
        return resultSet;
    }

    public static Map<String, String> getItems(final Map commit) {
        val resultSet = new HashMap<String, String>();
        if (commit != null && commit.containsKey("changes") && commit.get("changes") instanceof List) {
            for (val change : (List<Map>) commit.get("changes")) {
                if (change.containsKey("item") && change.get("item") instanceof Map item) {
                    if (item.containsKey("gitObjectType") && "blob".equals(item.get("gitObjectType")) && item.containsKey("path") && item.get("path") instanceof String) {
                        resultSet.put((String) item.get("path"), (String) item.get("url"));
                    }
                }
            }
        }
        return resultSet;
    }
}
