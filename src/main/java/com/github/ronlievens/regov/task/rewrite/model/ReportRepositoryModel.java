package com.github.ronlievens.regov.task.rewrite.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ronlievens.regov.util.MapperUtils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReportRepositoryModel implements Comparable<ReportRepositoryModel> {

    private static final ObjectMapper MAPPER = MapperUtils.createJsonMapper(true, true);

    private UUID id;
    private String name;
    private URL url;
    private UUID projectId;
    private String projectName;

    private ReportRepositoryChangeModel change;
    private ReportRepositoryChangeModel acceptance;
    private ReportRepositoryChangeModel production;

    @Override
    public int compareTo(@NotNull ReportRepositoryModel o) {
        return StringUtils.compareIgnoreCase(name, o.name);
    }

    public boolean isDone() {
        return change.isBuildSuccess() && isNotBlank(acceptance.getContainerNewVersion()) && isNotBlank(production.getContainerNewVersion());
    }

    public static ReportRepositoryChangeModel map(@NonNull final String pullRequestJson) {
        return map(pullRequestJson, null, null);
    }

    public static ReportRepositoryChangeModel map(@NonNull final String pullRequestJson, final String containerOriginalVersion, final String containerNewVersion) {
        val result = new ReportRepositoryChangeModel();
        if (isNotBlank(pullRequestJson)) {
            try {
                val pullRequestList = MAPPER.readValue(pullRequestJson, new TypeReference<List<Map<String, Object>>>() {
                });

                if (!pullRequestList.isEmpty()) {
                    for (val pullRequest : pullRequestList) {
                        if (pullRequest.get("pullRequestId") != null && pullRequest.get("pullRequestId") instanceof Integer) {
                            result.setPullRequestId((Integer) pullRequest.get("pullRequestId"));
                            result.setPullRequestUrl((String) pullRequest.get("url"));
                            result.setPullRequestStatus((String) pullRequest.get("status"));

                            if (isNotBlank(containerOriginalVersion)) {
                                result.setContainerOriginalVersion(containerOriginalVersion);
                            }
                            if (isNotBlank(containerNewVersion)) {
                                result.setContainerNewVersion(containerNewVersion);
                            }
                            break;
                        }
                    }
                }
            } catch (JsonProcessingException e) {
                log.info(pullRequestJson);
                log.error("{}", e.getMessage());
            }
        }
        return result;
    }
}

