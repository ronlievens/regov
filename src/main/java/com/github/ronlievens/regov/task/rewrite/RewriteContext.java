package com.github.ronlievens.regov.task.rewrite;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.ronlievens.regov.shell.model.AzureRepository;
import com.github.ronlievens.regov.task.config.model.AzureOrganizationSettingsModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Builder
@AllArgsConstructor
@Data
@ToString
@JsonPropertyOrder(alphabetic = true)
public class RewriteContext {

    public final static String SOURCE_COMMIT_PREFIX = "autorewrite: %s";
    private final static String SOURCE_FEATURE_BRANCH = "feature/autorewrite_%s";
    private static final String RECIPE_LOCATION_PREFIX = "com.github.ronlievens.regov.task.rewrite.recipes.";

    private final Path destination;
    private final boolean force;
    private final boolean skipRemote;
    private final String ticket;
    private final boolean trace;

    // search
    private final Map<String, AzureOrganizationSettingsModel> azureOrganizationProjects;
    private final String query;
    private final Integer numberRows;

    // rewrite
    private final Path batchFile;
    private final String recipe;
    private final String recipeLocation;

    // Report
    private final Path resultFile;

    // work data
    private TreeSet<AzureRepository> repositories;

    @JsonIgnore
    public String getRecipeName() {
        if (isNotBlank(recipeLocation)) {
            return recipeLocation.replace(RECIPE_LOCATION_PREFIX, "").trim();
        }
        return recipe.replace(RECIPE_LOCATION_PREFIX, "").trim();
    }

    @JsonIgnore
    public String getBranchName() {
        return SOURCE_FEATURE_BRANCH.formatted(ticket);
    }
}
