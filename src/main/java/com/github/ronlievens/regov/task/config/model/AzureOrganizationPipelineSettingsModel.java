package com.github.ronlievens.regov.task.config.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonPropertyOrder(alphabetic = true)
public class AzureOrganizationPipelineSettingsModel {

    private String path;
    private String branch;

}
