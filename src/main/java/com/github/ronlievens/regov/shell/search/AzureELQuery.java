package com.github.ronlievens.regov.shell.search;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Getter
public class AzureELQuery {

    private static final Set<String> KEYWORDS = Set.of("artifact", "parent", "dependency", "type");
    private static final Set<String> COMPARATORS = Set.of("!=", "<=", ">=", "=", "<", ">");

    private final String normalizedQuery;
    private final String elQuery;

    public AzureELQuery(@NonNull final String query) {
        normalizedQuery = normalize(query);
        elQuery = parseEL(normalizedQuery);
        log.debug("Normalized query: {}", normalizedQuery);
        log.debug("EL query: {}", elQuery);
    }

    public boolean containsSearch() {
        return isNotBlank(elQuery);
    }

    private String parseEL(@NonNull final String query) {
        val elQueryBuilder = new StringBuilder();
        for (val word : query.split(" ")) {
            if (isNotBlank(word)) {
                var keywordFound = false;
                for (val keyword : KEYWORDS) {
                    if (word.startsWith(keyword)) {
                        keywordFound = true;
                        for (val comparator : COMPARATORS) {
                            if (word.startsWith(keyword + comparator)) {
                                val value = word.substring(keyword.length() + comparator.length());
                                elQueryBuilder.append("cli:");
                                elQueryBuilder.append(keyword);
                                elQueryBuilder.append("(");
                                elQueryBuilder.append("'");
                                elQueryBuilder.append(comparator);
                                elQueryBuilder.append("','");
                                elQueryBuilder.append(value);
                                elQueryBuilder.append("'");
                                elQueryBuilder.append(")");
                                break;
                            }
                        }
                    }
                }
                if (!keywordFound) {
                    elQueryBuilder.append(word);
                }
            }
        }
        return cleanLogicalOperators(removeProjectSubquery(
            elQueryBuilder.toString()
                .trim()
                .replaceAll("&&", " && ")
                .replaceAll("\\|\\|", " || ")
        ));
    }

    private static String normalize(final String query) {
        return query.toLowerCase()
            .replaceAll("\n", "")
            .replaceAll("\r", "")
            .replaceAll("\\s+", "")
            .replaceAll("\\(", " ( ")
            .replaceAll("\\)", " ) ")
            // Add spaces around "&&"
            .replaceAll("&&", " && ")
            // Add spaces around "||"
            .replaceAll("\\|\\|", " || ")
            // Add spaces around "AND" (case-insensitive)
            .replaceAll("(?i)\\bAND\\b", " && ")
            // Add spaces around "OR" (case-insensitive)
            .replaceAll("(?i)\\bOR\\b", " || ")
            // Normalize extra spaces
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Removes all occurrences of `project(...)` from the EL query.
     *
     * @param query The original EL query.
     * @return The updated query without empty parentheses.
     */
    private static String removeProjectSubquery(final String query) {
        return query
            // Replace all occurrences of `project(...)` with an empty string
            .replaceAll("\\|\\|\\s*\\)", ")")
            .replaceAll("\\(\\s*\\|\\|", "(")
            .replaceAll("&&\\s*\\)", ")")

            // Regex to match empty parentheses
            .replaceAll("\\(\\s*\\)", "");
    }

    /**
     * Cleans unneeded logical operators like `&&` or `||` from an EL query.
     * - Removes starting `&&` or `||`.
     * - Removes trailing `&&` or `||` before closing parentheses.
     * - Ensures no redundant operators.
     *
     * @param query The original EL query.
     * @return Cleaned EL query.
     */
    public static String cleanLogicalOperators(final String query) {
        return query
            // Step 1: Remove leading `&&` or `||`
            .replaceAll("^\\s*&&\\s*", "").replaceAll("^\\s*\\|\\|\\s*", "")
            // Step 2: Remove trailing `&&` or `||` before closing parentheses
            .replaceAll("\\s*&&\\s*\\)", ")").replaceAll("\\s*\\|\\|\\s*\\)", ")")
            // Step 3: Remove redundant operators between empty parentheses or at the boundary
            .replaceAll("\\(\\s*\\|\\|\\s*", "(").replaceAll("\\(\\s*&&\\s*", "(")
            // Step 4: Remove redundant operators
            .replaceAll("(&&\\s*)+$", "").replaceAll("(\\|\\|\\s*)+$", "")
            .trim();
    }
}
