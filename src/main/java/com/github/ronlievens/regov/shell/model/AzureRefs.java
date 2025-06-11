package com.github.ronlievens.regov.shell.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.net.URL;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@ToString
@Getter
@Setter
public class AzureRefs {
    private String name;
    private String objectId;
    private AzureCreator creator;
    private URL url;

    public SemanticVersion getVersion() {
        if (isNotBlank(name) && name.contains("-")) {
            val version = name.substring(name.lastIndexOf("-") + 1);
            if (isNotBlank(version)) {
                return new SemanticVersion(version.trim());
            }
        }
        return null;
    }
}
