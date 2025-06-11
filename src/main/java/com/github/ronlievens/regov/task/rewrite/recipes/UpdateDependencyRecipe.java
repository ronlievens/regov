package com.github.ronlievens.regov.task.rewrite.recipes;

import com.github.ronlievens.regov.shell.AzureMavenVersionShell;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openrewrite.*;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.openrewrite.xml.AddOrUpdateChild.addOrUpdateChild;

@Slf4j
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UpdateDependencyRecipe extends Recipe {

    @Option(displayName = "Group", description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`.", example = "com.google.guava")
    private final String groupId;

    @Option(displayName = "Artifact", description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`.", example = "guava")
    private final String artifactId;

    @Option(displayName = "New group", description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`.", example = "com.google.guava", required = false)
    private final String newGroupId;

    @Option(displayName = "New artifact", description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`.", example = "guava", required = false)
    private final String newArtifactId;

    @Option(displayName = "New version", description = "New version of the dependency", example = "1.0.0", required = false)
    private final String newVersion;

    @Option(displayName = "New scope", description = "New scope of the dependency", valid = {"compile", "test", "runtime", "provided"}, example = "compile", required = false)
    private final String newScope;

    @Option(displayName = "AutoUpdate version", description = "The recipe will try to automatically download the latest released version, if no version is found and a newVersion is specified this will be used.", valid = {"true", "false"}, required = false)
    private final Boolean autoUpdateVersion = false;

    @Option(displayName = "DependencyManagement", description = "Indicates if the update is also applicable to the dependencyManagement dependencies", valid = {"true", "false"}, required = false)
    private final Boolean enableDependencyManagement = false;

    @Override
    public String getDisplayName() {
        return "Remove Maven dependency";
    }

    @Override
    public String getDescription() {
        return "Removes a single dependency from the <dependencies> section of the pom.xml.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles("pom.xml"), new XmlIsoVisitor<ExecutionContext>() {
            private final XPathMatcher dependencyXpath = new XPathMatcher("/project/dependencies/dependency");
            private final XPathMatcher dependencyManagementXpath = new XPathMatcher("/project/dependencyManagement/dependencies/dependency");

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (dependencyXpath.matches(getCursor()) || (enableDependencyManagement && dependencyManagementXpath.matches(getCursor()))) {
                    assert tag.getContent() != null;
                    Map<String, String> dependency = tag.getContent().stream().filter(content -> content instanceof Xml.Tag).filter(content -> ((Xml.Tag) content).getValue().isPresent()).map(content -> (Xml.Tag) content).collect(Collectors.toMap(Xml.Tag::getName, content -> content.getValue().get()));

                    if (dependency.get("groupId").equals(groupId) && dependency.get("artifactId").equals(artifactId)) {
                        if (StringUtils.isNotBlank(newGroupId)) {
                            tag = addOrUpdateChild(tag, Xml.Tag.build("<groupId>%s</groupId>".formatted(newGroupId)), getCursor().getParentOrThrow());
                        }

                        if (StringUtils.isNotBlank(newArtifactId)) {
                            tag = addOrUpdateChild(tag, Xml.Tag.build("<artifactId>%s</artifactId>".formatted(newArtifactId)), getCursor().getParentOrThrow());
                        }

                        tag = updateVersion(tag, ctx, dependency);

                        if (newScope != null) {

                            if (StringUtils.isNotBlank(newScope)) {
                                tag = addOrUpdateChild(tag, Xml.Tag.build("<scope>%s</scope>".formatted(newScope)), getCursor().getParentOrThrow());
                            } else if (tag.getChild("scope").isPresent()) {
                                doAfterVisit(new RemoveContentVisitor<>(tag.getChild("scope").get(), true, true));
                            }
                        }
                    }
                }
                return super.visitTag(tag, ctx);
            }

            private Xml.Tag updateVersion(Xml.Tag tag, ExecutionContext ctx, Map<String, String> dependency) {
                if (autoUpdateVersion) {
                    Optional<String> latestVersion = findLatestVersion(dependency);
                    if (latestVersion.isPresent()) {
                        return updateVersionTag(tag, ctx, latestVersion.get());
                    }
                }
                if (newVersion != null) {
                    if (StringUtils.isNotBlank(newVersion)) {
                        tag = updateVersionTag(tag, ctx, newVersion);

                    } else if (tag.getChild("version").isPresent()) {
                        doAfterVisit(new RemoveContentVisitor<>(tag.getChild("version").get(), true, true));
                    }
                }
                return tag;
            }

            private Xml.Tag updateVersionTag(Xml.Tag tag, ExecutionContext ctx, String newVersion) {
                final List<Content> content = new ArrayList<>(tag.getContent().stream().filter(c -> c instanceof Xml.Tag).filter(c -> !((Xml.Tag) c).getName().equals("version")).toList());
                val counter = new AtomicInteger();
                //find the version tag
                content.stream().peek(c -> counter.incrementAndGet()).filter(c -> ((Xml.Tag) c).getName().equals("artifactId")).findFirst();

                content.add(counter.get(), Xml.Tag.build("<version>%s</version>".formatted(newVersion)));
                tag = autoFormat(tag.withContent(content), ctx);
                return tag;
            }

            private Optional<String> findLatestVersion(final Map<String, String> dependency) {
                AzureMavenVersionShell azureMavenVersionShell = null;
                try {
                    azureMavenVersionShell = RecipeDependencyService.getInstance().getDependency(AzureMavenVersionShell.class);
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                var groupId = dependency.get("groupId");
                if (StringUtils.isNotBlank(newGroupId)) {
                    groupId = newGroupId;
                }
                val artifactId = StringUtils.isNotBlank(newArtifactId) ? newArtifactId : dependency.get("artifactId");
                val latestVersion = azureMavenVersionShell.lookupLastVersion(groupId, artifactId);
                return Optional.ofNullable(latestVersion).stream().peek(version -> log.info("Found new version {} for {}", version, artifactId)).findAny();
            }
        });
    }
}
