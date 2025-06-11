package com.github.ronlievens.regov.shell.search;

import jakarta.el.ELProcessor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;

import java.lang.reflect.Method;

import static com.github.ronlievens.regov.shell.search.AzurePomSearchUtils.*;


@Slf4j
public class AzureELProcessor {

    private static Model pom;
    private final ELProcessor elProcessor;

    public AzureELProcessor(@NonNull final Model pom) throws NoSuchMethodException {
        AzureELProcessor.pom = pom;
        elProcessor = new ELProcessor();
        elProcessor.defineFunction("cli", "parent", resolveFunction("parent"));
        elProcessor.defineFunction("cli", "artifact", resolveFunction("artifact"));
        elProcessor.defineFunction("cli", "dependency", resolveFunction("dependency"));
        elProcessor.defineFunction("cli", "property", resolveFunction("property"));
    }

    public boolean evaluate(@NonNull final String expression) {
        try {
            return elProcessor.eval(expression);
        } catch (Exception e) {
            log.debug("{}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static Method resolveFunction(@NonNull final String localName) throws NoSuchMethodException {
        return AzureELProcessor.class.getMethod(localName, String.class, String.class);
    }

    public static boolean parent(final String comparator, @NonNull final String value) {
        return hasParent(pom, value, comparator);
    }

    public static boolean artifact(final String comparator, @NonNull final String value) {
        return hasArtifact(pom, value, comparator);
    }

    public static boolean dependency(final String comparator, @NonNull final String value) {
        return hasDependency(pom, value, comparator);
    }

    public static boolean property(final String comparator, @NonNull final String value) {
        return hasProperty(pom, value, comparator);
    }
}
