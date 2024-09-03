package org.flunadela.depsvers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.TreeMap;

public class RunMe {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunMe.class);

    public static void main(String[] args) {

        String pomFileToParse = args[0];
        MavenPomDeps mavenPomDeps = new MavenPomDeps();

        try {
            List<DependencyEntry> deps = mavenPomDeps.parsePom(pomFileToParse);

            MavenMetadata mavenMetadata = new MavenMetadata("https://repo.maven.apache.org/maven2");

            TreeMap<String, List<String>> versionsTree = new TreeMap<>();

            deps.forEach(dep -> {
                if (dep.version() == null || dep.version().equals(MavenPomDeps.UNDEFINED)) {
                    LOGGER.info("Skipping {} as version is UNDEFINED", dep.artifactId());
                    return;
                }
                MavenMetadataVersioning meta = mavenMetadata.getArtifactMetadata(
                        dep.groupId(),
                        dep.artifactId(),
                        dep.version());

                if (meta != null) {
                    versionsTree.put(dep.artifactId(), meta.versioning.versions);
                }
            });

            versionsTree.forEach((k, v) -> LOGGER.info("Latest versions for {}: {}", k, v));

        } catch (Exception e) {
            LOGGER.error("Exception produced", e);
        }
    }
}
