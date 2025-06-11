package com.github.ronlievens.regov.task.config.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonPropertyOrder(alphabetic = true)
public class AzureOrganizationArgocdSettingsModel {

    private AzureOrganizationArgocdSettingModel acceptance;
    private AzureOrganizationArgocdSettingModel production;

    public AzureOrganizationArgocdSettingsModel() {
        acceptance = new AzureOrganizationArgocdSettingModel();
        production = new AzureOrganizationArgocdSettingModel();
    }
}
