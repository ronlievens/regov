package com.github.ronlievens.regov.task.config.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Setter;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Setter
@JsonPropertyOrder(alphabetic = true)
public class GitSettingsModel {

    private static final String BRANCH_MAIN = "main";

    private String mainBranch;

    public String getGitBranchMain() {
        if (isBlank(mainBranch)) {
            return BRANCH_MAIN;
        }
        return mainBranch;
    }
}
