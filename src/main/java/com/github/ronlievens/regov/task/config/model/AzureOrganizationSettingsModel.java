package com.github.ronlievens.regov.task.config.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.TreeSet;

@Setter
@Getter
@JsonPropertyOrder(alphabetic = true)
public class AzureOrganizationSettingsModel {

    private Set<String> projects;
    private Set<String> mavenRepositories;
    private AzureOrganizationArgocdSettingsModel argocd;
    private AzureOrganizationPipelineSettingsModel pipeline;

    public AzureOrganizationSettingsModel() {
        projects = new TreeSet<>();
        mavenRepositories = new TreeSet<>();
        argocd = new AzureOrganizationArgocdSettingsModel();
        pipeline = new AzureOrganizationPipelineSettingsModel();
    }
}
