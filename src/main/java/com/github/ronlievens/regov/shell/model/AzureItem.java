package com.github.ronlievens.regov.shell.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.URL;

@ToString
@Getter
@Setter
public class AzureItem {
    private String objectId;
    private String gitObjectType;
    private String commitId;
    private String path;
    private Boolean isFolder;
    private URL url;
}
