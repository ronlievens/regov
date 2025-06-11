package com.github.ronlievens.regov.shell.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.util.UUID;

@ToString
@Getter
@Setter
public class AzureRepository implements Comparable<AzureRepository> {

    private UUID id;
    private String name;
    private URL url;
    private AzureProject project;
    private String defaultBranch;
    private Long size;
    private URL remoteUrl;
    private String sshUrl;
    private URL webUrl;
    private Boolean isDisabled;
    private Boolean isInMaintenance;

    private boolean enabledForRewrite;

    @Override
    public int compareTo(@NonNull final AzureRepository azureRepository) {
        return StringUtils.compareIgnoreCase(name, azureRepository.name);
    }
}
