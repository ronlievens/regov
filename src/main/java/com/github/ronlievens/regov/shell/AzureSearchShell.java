package com.github.ronlievens.regov.shell;

import com.github.ronlievens.regov.shell.model.AzureRepository;
import com.github.ronlievens.regov.shell.search.AzureELProcessor;
import com.github.ronlievens.regov.shell.search.AzureELQuery;
import com.github.ronlievens.regov.task.config.model.AzureOrganizationSettingsModel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.maven.model.Model;

import java.util.Map;
import java.util.TreeSet;

import static com.github.ronlievens.regov.util.MavenUtils.parsePom;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@RequiredArgsConstructor
public class AzureSearchShell {

    private static final String LOCATION_POM = "/pom.xml";

    private final AzureRestShell driver;

    public AzureSearchShell() {
        this.driver = new AzureRestShell(new Shell());
    }

    public TreeSet<AzureRepository> search(@NonNull final Map<String, AzureOrganizationSettingsModel> azureOrganizationProjects, @NonNull final String searchQuery, final boolean trace) {
        val result = new TreeSet<AzureRepository>();
        if (isNotBlank(searchQuery)) {
            for (val azureOrganization : azureOrganizationProjects.keySet()) {
                for (val azureProject : azureOrganizationProjects.get(azureOrganization).getProjects()) {
                    try {
                        log.info("Start search in azure organisation '{}' project '{}'.", azureOrganization, azureProject);
                        val repositoryList = driver.listRepositories(azureOrganization, azureProject, trace);
                        for (val repository : repositoryList.getValue()) {
                            if (!repository.getIsDisabled() && !repository.getIsInMaintenance()) {
                                try {
                                    val search = new AzureELQuery(searchQuery);
                                    if (isNeededPom(parsePom(driver.getRepositoryFile(azureOrganization, azureProject, repository.getId(), LOCATION_POM, trace)), search)) {
                                        result.add(repository);
                                        repository.getProject().setOrganizationName(azureOrganization);
                                    }
                                } catch (Exception e) {
                                    if (trace) {
                                        log.warn("Unable to run search in repository {} with query {}", repository.getName(), searchQuery);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Invalid azure project: {}", azureProject, e);
                    }
                }
            }
        }
        return result;
    }

    private boolean isNeededPom(@NonNull final Model pom, @NonNull final AzureELQuery elSearch) {
        if (!elSearch.containsSearch()) {
            return false;
        }

        try {
            val azureELProcessor = new AzureELProcessor(pom);
            return azureELProcessor.evaluate(elSearch.getElQuery());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
