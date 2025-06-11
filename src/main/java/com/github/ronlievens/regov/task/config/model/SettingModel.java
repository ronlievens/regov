package com.github.ronlievens.regov.task.config.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonPropertyOrder(alphabetic = true)
public class SettingModel {

    private Boolean autoApprove;
    private String ticketUrl;
    private GitSettingsModel git;
    private AzureSettingsModel azure;

    public SettingModel() {
        git = new GitSettingsModel();
        azure = new AzureSettingsModel();
    }

    @JsonIgnore
    public boolean isAutoApproveEnabled() {
        return Boolean.TRUE.equals(autoApprove);
    }
}
