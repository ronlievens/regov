package com.github.ronlievens.regov.shell.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@AllArgsConstructor
@Getter
@Setter
public class SemanticVersion implements Comparable<SemanticVersion> {

    private Integer major;
    private Integer minor;
    private Integer patch;

    public SemanticVersion(final String semanticVersion) {
        if (isNotBlank(semanticVersion)) {
            var subs = semanticVersion.split("\\.");
            if (subs.length == 3) {
                major = Integer.parseInt(subs[0]);
                minor = Integer.parseInt(subs[1]);
                patch = Integer.parseInt(subs[2]);
            } else {
                throw new IllegalArgumentException("Semantic version is invalid");
            }
        } else {
            throw new IllegalArgumentException("Semantic version is invalid");
        }
    }

    @Override
    public int compareTo(@NonNull final SemanticVersion semanticVersion) {
        var compare = major.compareTo(semanticVersion.getMajor());
        if (compare != 0) return compare;
        compare = minor.compareTo(semanticVersion.getMinor());
        if (compare != 0) return compare;
        return patch.compareTo(semanticVersion.getPatch());
    }

    public boolean isEqual(final SemanticVersion semanticVersion) {
        if (semanticVersion == null) return false;
        return this.compareTo(semanticVersion) == 0;
    }


    public boolean isHigher(final SemanticVersion semanticVersion) {
        if (semanticVersion == null) return false;
        return this.compareTo(semanticVersion) > 0;
    }

    public boolean isLower(final SemanticVersion semanticVersion) {
        if (semanticVersion == null) return false;
        return this.compareTo(semanticVersion) < 0;
    }

    public String toString() {
        return "%s.%s.%s".formatted(major, minor, patch);
    }
}
