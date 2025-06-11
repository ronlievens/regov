package com.github.ronlievens.regov.task.rewrite.recipes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openrewrite.*;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AddDependencyRecipe extends Recipe {

    @Option(displayName = "New groupId", description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`.", example = "com.google.guava", required = true)
    private final String groupId;

    @Option(displayName = "New artifactId", description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`.", example = "guava", required = true)
    private final String artifactId;

    @Option(displayName = "New version", description = "New version of the dependency", example = "1.0.0", required = false)
    private final String version;

    @Option(displayName = "New scope", description = "New Scope of the dependency", valid = {"compile", "test", "runtime", "provided"}, example = "compile", required = false)
    private final String scope;

    @Override
    public String getDisplayName() {
        return "Add Maven dependency";
    }

    @Override
    public String getDescription() {
        return "Add a single dependency in the <dependencies> section of the pom.xml.";
    }


    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles("pom.xml"), new XmlIsoVisitor<ExecutionContext>() {
            private final XPathMatcher dependencyXpath = new XPathMatcher("/project/dependencies");

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (dependencyXpath.matches(getCursor()) && !dependencyAlreadyPresent(tag)) {
                    Xml.Tag dependencyTag = Xml.Tag.build("\n<dependency>\n" + "<groupId>" + groupId + "</groupId>\n" + "<artifactId>" + artifactId + "</artifactId>\n" + (version == null ? "" : "<version>" + version + "</version>\n") + (scope == null || "compile".equals(scope) ? "" : "<scope>" + scope + "</scope>\n") + "</dependency>");
                    List<Content> newDependencyList = new ArrayList<>();
                    newDependencyList.add(dependencyTag);

                    List<Content> list = Stream.concat(newDependencyList.stream(), tag.getContent().stream()).toList();
                    tag = autoFormat(tag.withContent(list), ctx);
                    super.visitTag(tag, ctx);
                }


                return super.visitTag(tag, ctx);
            }

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext executionContext) {
                return super.visitDocument(document, executionContext);
            }
        });
    }

    public boolean dependencyAlreadyPresent(Xml.Tag tag) {
        return getChild(tag, "artifactId", artifactId) && getChild(tag, "groupId", groupId);
    }

    private boolean getChild(Xml.Tag tag, String childName, String expectedValue) {
        return tag.getChildren().stream().map(dependency -> dependency.getChild(childName)).flatMap(Optional::stream).map(Xml.Tag::getValue).flatMap(Optional::stream).anyMatch(artifact -> artifact.equals(expectedValue));
    }
}
