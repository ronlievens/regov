package com.github.ronlievens.regov.shell;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.ronlievens.regov.util.MapperUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.HashMap;
import java.util.Map;

import static com.github.ronlievens.regov.task.config.Settings.properties;
import static com.github.ronlievens.regov.util.LogbackUtils.isTrace;

@Slf4j
@RequiredArgsConstructor
public class AzureMavenVersionShell {

    private static final String URL_FORMAT = "%s/%s/%s/maven-metadata.xml";
    private static final XmlMapper MAPPER = MapperUtils.createXmlMapper();

    private final AzureRestShell driver;

    public AzureMavenVersionShell() {
        this.driver = new AzureRestShell(new Shell());
    }

    public String lookupLastVersion(@NonNull final String groupId, @NonNull final String artifactId) {
        for (val azureOrganization : properties().getAzure().getOrganizations().keySet()) {
            for (val url : properties().getAzure().getOrganizations().get(azureOrganization).getMavenRepositories()) {
                try {
                    val mavenXml = MAPPER.readValue(driver.call(URL_FORMAT.formatted(url, groupId.replaceAll("\\.", "/"), artifactId), isTrace()), HashMap.class);
                    val versioning = (Map<String, String>) mavenXml.get("versioning");
                    return versioning.get("latest");
                } catch (JsonProcessingException e) {
                    log.warn(e.getMessage(), e);
                }
            }
        }
        return null;
    }
}
