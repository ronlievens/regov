package com.github.ronlievens.regov.task.rewrite.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReportRepositoryChangeModel {

    private static final String VIEW_REMOVAL_GIT = "_apis/git/repositories";
    private static final String VIEW_ADD_GIT = "_git";
    private static final String VIEW_REMOVAL_PULLREQUEST = "pullRequests";
    private static final String VIEW_ADD_PULLREQUEST = "pullrequest";

    private Integer pullRequestId;
    private String pullRequestUrl;
    private String pullRequestStatus;

    private Integer buildId;
    private String buildUrl;
    private String buildStatus;

    @JsonIgnore
    private String containerOriginalVersion;
    @JsonIgnore
    private String containerNewVersion;

    @JsonIgnore
    public String getPullRequestViewUrl() {
        if (isNotBlank(pullRequestUrl)) {
            return pullRequestUrl.replace(VIEW_REMOVAL_GIT, VIEW_ADD_GIT).replace(VIEW_REMOVAL_PULLREQUEST, VIEW_ADD_PULLREQUEST);
        }
        return null;
    }

    @JsonIgnore
    public String getBuildViewUrl() {
        if (isNotBlank(buildUrl)) {
            return buildUrl.replace(VIEW_REMOVAL_GIT, VIEW_ADD_GIT).replace(VIEW_REMOVAL_PULLREQUEST, VIEW_ADD_PULLREQUEST);
        }
        return null;
    }

    @JsonIgnore
    public boolean isCommit() {
        return isNotBlank(pullRequestStatus);
    }

    @JsonIgnore
    public boolean isMerged() {
        return "completed".equals(pullRequestStatus);
    }

    @JsonIgnore
    public boolean hasBuild() {
        return isNotBlank(buildStatus);
    }

    @JsonIgnore
    public boolean isBuildSuccess() {
        return "completed".equals(buildStatus) || "succeeded".equals(buildStatus);
    }

    @JsonIgnore
    public boolean isBuildInProgress() {
        return "inProgress".equals(buildStatus);
    }

    @JsonIgnore
    public boolean isBuildFailed() {
        return "failed".equals(buildStatus);
    }
}
