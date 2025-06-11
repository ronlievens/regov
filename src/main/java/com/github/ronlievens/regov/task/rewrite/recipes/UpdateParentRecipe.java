package com.github.ronlievens.regov.task.rewrite.recipes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.openrewrite.*;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.openrewrite.xml.AddOrUpdateChild.addOrUpdateChild;

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UpdateParentRecipe extends Recipe {

    @Option(displayName = "New groupId", description = "The first part of a parent coordinate `com.google.guava:guava:VERSION`.", example = "com.google.guava", required = false)
    private final String newGroupId;

    @Option(displayName = "New artifactId", description = "The second part of a parent coordinate `com.google.guava:guava:VERSION`.", example = "guava", required = false)
    private final String newArtifactId;

    @Option(displayName = "New version", description = "New version of the parent", example = "1.0.0", required = false)
    private final String newVersion;

    @Override
    public String getDisplayName() {
        return "Update Maven parent";
    }

    @Override
    public String getDescription() {
        return "Update the parent from the <parent> section of the pom.xml.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles("pom.xml"), new XmlIsoVisitor<ExecutionContext>() {
            private final XPathMatcher parentXpath = new XPathMatcher("/project/parent");

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (parentXpath.matches(getCursor())) {
                    assert tag.getContent() != null;

                    tag.getContent().stream().filter(content -> content instanceof Xml.Tag).filter(content -> ((Xml.Tag) content).getValue().isPresent()).map(content -> (Xml.Tag) content).collect(Collectors.toMap(Xml.Tag::getName, content -> content.getValue().get()));

                    if (StringUtils.isNotBlank(newGroupId)) {
                        tag = addOrUpdateChild(tag, Xml.Tag.build("<groupId>%s</groupId>".formatted(newGroupId)), getCursor().getParentOrThrow());
                    }

                    if (StringUtils.isNotBlank(newArtifactId)) {
                        tag = addOrUpdateChild(tag, Xml.Tag.build("<artifactId>%s</artifactId>".formatted(newArtifactId)), getCursor().getParentOrThrow());
                    }

                    if (newVersion != null) {
                        if (StringUtils.isNotBlank(newVersion)) {
                            List<Content> content = new ArrayList<>(tag.getContent().stream().filter(c -> !((Xml.Tag) c).getName().equals("version")).toList());
                            AtomicInteger counter = new AtomicInteger();
                            content.stream().peek(c -> counter.incrementAndGet()).filter(c -> ((Xml.Tag) c).getName().equals("artifactId")).findFirst();
                            content.add(counter.get(), Xml.Tag.build("<version>%s</version>".formatted(newVersion)));
                            tag = autoFormat(tag.withContent(content), ctx);
                        } else if (tag.getChild("version").isPresent()) {
                            doAfterVisit(new RemoveContentVisitor<>(tag.getChild("version").get(), true, true));
                        }
                    }
                }

                return super.visitTag(tag, ctx);
            }
        });
    }
}
