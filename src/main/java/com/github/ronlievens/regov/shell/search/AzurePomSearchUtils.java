package com.github.ronlievens.regov.shell.search;

import com.github.ronlievens.regov.shell.model.SemanticVersion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AzurePomSearchUtils {

    public static final String COMPARATOR_NOT = "!=";
    public static final String COMPARATOR_LESS_EQUAL = "<=";
    public static final String COMPARATOR_GREATER_EQUAL = ">=";
    public static final String COMPARATOR_IS = "=";
    public static final String COMPARATOR_LESS = "<";
    public static final String COMPARATOR_GREATER = ">";

    private static final String MAVEN_DELIMITER = ":";

    public static String[] splitCoordinates(@NonNull final String coordinates) {
        val result = new String[3];
        val parts = coordinates.split(MAVEN_DELIMITER);
        for (int i = 0; i < parts.length && i < result.length; i++) {
            result[i] = parts[i];
        }
        return result;
    }

    public static boolean hasProperty(@NonNull final Model pom, @NonNull final String value, final String comparator) {
        if (isBlank(comparator) || !COMPARATOR_NOT.equals(comparator) && !COMPARATOR_IS.equals(comparator)) {
            throw new IllegalArgumentException("Comparator can only be '%s' or '%s'".formatted(COMPARATOR_IS, COMPARATOR_NOT));
        }

        if (isNotBlank(comparator) && COMPARATOR_NOT.equals(comparator)) {
            return pom.getProperties().isEmpty() || !(pom.getProperties().containsKey(value) && StringUtils.compareIgnoreCase(value, (String) pom.getProperties().get(value)) == 0);
        }

        return !pom.getProperties().isEmpty() && pom.getProperties().containsKey(value) && StringUtils.compareIgnoreCase(value, (String) pom.getProperties().get(value)) == 0;
    }

    public static boolean hasDependency(@NonNull final Model pom, @NonNull final String coordinates, final String comparator) {
        val mavenCoordinates = splitCoordinates(coordinates);
        return hasDependency(pom, mavenCoordinates[0], mavenCoordinates[1], mavenCoordinates[2], comparator);
    }

    public static boolean hasDependency(@NonNull final Model pom, @NonNull final String groupId, final String artifactId, final String version, final String comparator) {
        for (val dependency : pom.getDependencies()) {
            var result = false;
            val compared = compareArtifact(groupId, artifactId, version, dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), comparator);
            if (!COMPARATOR_NOT.equals(comparator)) {
                result = compared;
            } else {
                result = !compared;
            }
            if (result) {
                return !COMPARATOR_NOT.equals(comparator);
            }
        }
        return COMPARATOR_NOT.equals(comparator);
    }

    public static boolean hasParent(@NonNull final Model pom, @NonNull final String coordinates, final String comparator) {
        val mavenCoordinates = splitCoordinates(coordinates);
        return hasParent(pom, mavenCoordinates[0], mavenCoordinates[1], mavenCoordinates[2], comparator);
    }

    public static boolean hasParent(@NonNull final Model pom, @NonNull final String groupId, final String artifactId, final String version, final String comparator) {
        val parent = pom.getParent();
        if (parent != null) {
            return compareArtifact(groupId, artifactId, version, parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), comparator);
        }
        return false;
    }

    public static boolean hasArtifact(@NonNull final Model pom, @NonNull final String coordinates, final String comparator) {
        val mavenCoordinates = splitCoordinates(coordinates);
        return hasArtifact(pom, mavenCoordinates[0], mavenCoordinates[1], mavenCoordinates[2], comparator);
    }

    public static boolean hasArtifact(@NonNull final Model pom, @NonNull final String groupId, final String artifactId, final String version, final String comparator) {
        var projectGroupId = pom.getGroupId();
        if (StringUtils.isBlank(projectGroupId)) {
            projectGroupId = pom.getParent().getGroupId();
        }
        log.debug("using groupId {}", projectGroupId);
        return compareArtifact(groupId, artifactId, version, projectGroupId, pom.getArtifactId(), pom.getVersion(), comparator);
    }

    public static boolean compareArtifact(final String expectedGroupId, final String expectedArtifactId, final String expectedVersion, final String actualGroupId, final String actualArtifactId, final String actualVersion, final String comparator) {
        if (isBlank(expectedGroupId) && isBlank(expectedArtifactId) && isBlank(expectedVersion)) {
            throw new IllegalArgumentException("All expected artifact variables are null");
        }

        if (isBlank(actualGroupId) && isBlank(actualArtifactId) && isBlank(actualVersion)) {
            throw new IllegalArgumentException("All expected actual variables are null");
        }

        if (isBlank(expectedGroupId) || compare(actualGroupId, expectedGroupId, COMPARATOR_IS)) {
            if (isBlank(expectedArtifactId) || compare(actualArtifactId, expectedArtifactId, COMPARATOR_IS)) {
                if (isBlank(actualVersion) || isBlank(expectedVersion)) {
                    return !COMPARATOR_NOT.equals(comparator);
                }
                return compareVersion(actualVersion, expectedVersion, comparator);
            }
        }

        return COMPARATOR_NOT.equals(comparator);
    }

    public static boolean compare(@NonNull final String actual, @NonNull final String expected) {
        return compare(actual, expected, COMPARATOR_IS);
    }

    public static boolean compare(@NonNull final String actual, @NonNull String expected, @NonNull final String comparator) {
        expected = expected.replaceAll("\\*", "[a-zA-Z_0-9.]*");
        if (isNotBlank(comparator) && COMPARATOR_NOT.equals(comparator)) {
            return !actual.matches(expected);
        }
        return actual.matches(expected);
    }


    public static boolean compareVersion(@NonNull final String actual, @NonNull final String expected, @NonNull final String comparator) {
        val semanticVersionActual = new SemanticVersion(actual);
        val semanticVersionExpected = new SemanticVersion(expected);

        if (isBlank(comparator) || COMPARATOR_IS.equals(comparator)) {
            return semanticVersionActual.isEqual(semanticVersionExpected);
        }

        if (COMPARATOR_NOT.equals(comparator)) {
            return !semanticVersionActual.isEqual(semanticVersionExpected);
        }

        if (COMPARATOR_LESS.equals(comparator)) {
            return semanticVersionActual.isLower(semanticVersionExpected);
        }

        if (COMPARATOR_GREATER.equals(comparator)) {
            return semanticVersionActual.isHigher(semanticVersionExpected);
        }

        if (COMPARATOR_LESS_EQUAL.equals(comparator)) {
            return semanticVersionActual.isLower(semanticVersionExpected) || semanticVersionActual.isEqual(semanticVersionExpected);
        }

        if (COMPARATOR_GREATER_EQUAL.equals(comparator)) {
            return semanticVersionActual.isHigher(semanticVersionExpected) || semanticVersionActual.isEqual(semanticVersionExpected);
        }

        throw new IllegalArgumentException("Unsupported comparator: " + comparator);
    }
}
