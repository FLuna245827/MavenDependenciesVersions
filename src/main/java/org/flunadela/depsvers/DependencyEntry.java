package org.flunadela.depsvers;

public record DependencyEntry (String groupId, String artifactId, String version) {
    public DependencyEntry {
        if (groupId == null || artifactId == null) {
            throw new IllegalArgumentException("groupId and artifactId must not be null");
        }
    }
}
