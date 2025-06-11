package com.github.ronlievens.regov.shell.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.UUID;

@ToString
@Getter
@Setter
public class AzureProject {
    private UUID id;
    private String name;
    private String description;
    private URL url;
    private String state;
    private Long revision;
    private String visibility;
    private OffsetDateTime lastUpdateTime;
    private String organizationName;
}
