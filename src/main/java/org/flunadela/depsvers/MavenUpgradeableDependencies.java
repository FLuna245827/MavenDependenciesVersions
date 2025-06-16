package org.flunadela.depsvers;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.TreeMap;

import static org.flunadela.depsvers.MavenPomDeps.SKIP;

public class MavenUpgradeableDependencies {
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenUpgradeableDependencies.class);

    public static void main(String[] args) {
        boolean showOnlyLatestArg = false;

        String pomFileToParse = args[0];

        if (args.length == 2) {
            String showOnlyLatest = args[1];
            showOnlyLatestArg = showOnlyLatest.equals("showOnlyLatestVersion");
        }

        MavenPomDeps mavenPomDeps = new MavenPomDeps();

        try {
            File pomFile = new File(pomFileToParse);

            if (pomFile.toPath().normalize().getNameCount() < 1 || !pomFile.isAbsolute()) {
                LOGGER.error("The specified POM file is not valid: {}", pomFileToParse);
                return;
            }


            if (!pomFile.exists()) {
                LOGGER.error("The specified POM file does not exist: {}", pomFileToParse);
                return;
            }

            List<DependencyEntry> deps = mavenPomDeps.parsePom(pomFileToParse);

            MavenMetadata mavenMetadata = new MavenMetadata("https://repo.maven.apache.org/maven2");

            TreeMap<String, List<String>> versionsTree = new TreeMap<>();

            deps.forEach(dep -> {
                if (dep.version().startsWith(SKIP)) {
                    LOGGER.info("Not treated {} {}", dep.artifactId(), dep.version());
                    return;
                }
                MavenMetadataVersioning meta = mavenMetadata.getArtifactMetadata(
                        dep.groupId(),
                        dep.artifactId(),
                        dep.version());

                if (meta != null) {
                    versionsTree.put(dep.groupId() + ":" + dep.artifactId(), meta.versioning.versions);
                }
            });

            final boolean showOnlyLatestFlag = showOnlyLatestArg;

            versionsTree.forEach((artifact, upVersionslist) -> {
                if (!CollectionUtils.isEmpty(upVersionslist)) {
                    String actualVersion = upVersionslist.getFirst();
                    upVersionslist.removeFirst();

                    if (!CollectionUtils.isEmpty(upVersionslist)) { // if there are upgradable versions
                        if (showOnlyLatestFlag || upVersionslist.size() == 1) {
                            LOGGER.info("{} : {} -> {}", artifact, actualVersion, upVersionslist.getLast());
                        } else {
                            LOGGER.info("ALL UP VERSIONS {} : {} -> {}", artifact, actualVersion, upVersionslist);
                        }
                    }
                }
            });

        } catch (Exception e) {
            LOGGER.error("Exception produced", e);
        }
    }
}
