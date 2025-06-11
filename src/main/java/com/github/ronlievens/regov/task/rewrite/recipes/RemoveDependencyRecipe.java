package com.github.ronlievens.regov.task.rewrite.recipes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openrewrite.*;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RemoveDependencyRecipe extends Recipe {

    @Option(displayName = "GroupId", description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`.", example = "com.google.guava")
    private final String groupId;

    @Option(displayName = "ArtifactId", description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`.", example = "guava")
    private final String artifactId;

    @Option(displayName = "Scope", description = "Only remove dependencies if they are in this scope. If 'runtime', this will" + "also remove dependencies in the 'compile' scope because 'compile' dependencies are part of the runtime dependency set", valid = {"compile", "test", "runtime", "provided"}, example = "compile", required = false)
    private final String scope;

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
            private final XPathMatcher xPathMatcher = new XPathMatcher("/project/dependencies/dependency");

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (xPathMatcher.matches(getCursor())) {
                    assert tag.getContent() != null;
                    Map<String, String> dependency = tag.getContent().stream().filter(content -> content instanceof Xml.Tag).filter(content -> ((Xml.Tag) content).getValue().isPresent()).map(content -> (Xml.Tag) content).collect(Collectors.toMap(Xml.Tag::getName, content -> content.getValue().get()));
                    if (dependency.get("groupId").equals(groupId) && dependency.get("artifactId").equals(artifactId)) {
                        doAfterVisit(new RemoveContentVisitor<>(tag, true, true));
                    }
                }
                return super.visitTag(tag, ctx);
            }
        });
    }
}
