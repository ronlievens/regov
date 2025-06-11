package com.github.ronlievens.regov.task.config.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.TreeMap;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Getter
@Setter
@JsonPropertyOrder(alphabetic = true)
public class AzureSettingsModel {

    private static final String DEVOPS_SCOPE = "https://app.vssps.visualstudio.com";
    private static final String URL_SERVER = "https://dev.azure.com";
    private static final String API_PARAMETER = "api-version=7.1";

    private String devopsScope;
    private String urlServer;
    private String apiParameter;
    private Map<String, AzureOrganizationSettingsModel> organizations;

    public AzureSettingsModel() {
        organizations = new TreeMap<>();
    }

    public AzureOrganizationSettingsModel getOrganization(final String organization) {
        if (isNotBlank(organization)) {
            return organizations.get(organization);
        }
        return null;
    }

    public String getDevopsScope() {
        if (isBlank(devopsScope)) {
            return DEVOPS_SCOPE;
        }
        return devopsScope;
    }

    public String getUrlServer() {
        if (isBlank(urlServer)) {
            return URL_SERVER;
        }
        return urlServer;
    }

    public String getApiParameter() {
        if (isBlank(apiParameter)) {
            return API_PARAMETER;
        }
        return apiParameter;
    }

    @JsonIgnore
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasOrganizationProject() {
        if (organizations != null && !organizations.isEmpty()) {
            for (var organization : organizations.values()) {
                if (organization.getProjects() != null && !organization.getProjects().isEmpty()) {
                    return true;
                }
            }
        }
        log.warn("Please add organization projects to the configuration");
        return false;
    }
}
