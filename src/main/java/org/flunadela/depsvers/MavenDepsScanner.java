package org.flunadela.depsvers;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenDepsScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenDepsScanner.class);

    public void scan(String pomFileToParse, final boolean showOnlyLatestArg) {
        MavenPomDeps mavenPomDeps = new MavenPomDeps();

        try {
            File pomFile = new File(pomFileToParse);

            if (pomFile.toPath().normalize().getNameCount() < 1 || !pomFile.isAbsolute()) {
                LOGGER.error("The specified POM file path is not valid: {}", pomFileToParse);
                return;
            }

            if (!pomFile.exists()) {
                LOGGER.error("The specified POM file does not exist: {}", pomFileToParse);
                return;
            }

            displayResults(mavenPomDeps.getVersionsTree(pomFileToParse), showOnlyLatestArg);

        } catch (Exception e) {
            LOGGER.error("Exception produced", e);
        }
    }

    private void displayResults(Map<String, List<String>> versionsTree, final boolean showOnlyLatestArg) {
        versionsTree.forEach((artifact, upVersionslist) -> {
            if (!CollectionUtils.isEmpty(upVersionslist)) {
                String actualVersion = upVersionslist.getFirst();
                upVersionslist.removeFirst();

                if (!CollectionUtils.isEmpty(upVersionslist)) { // if there are upgradable versions
                    if (showOnlyLatestArg || upVersionslist.size() == 1) {
                        LOGGER.info("{} : {} -> {}", artifact, actualVersion, upVersionslist.getLast());
                    } else {
                        LOGGER.info("ALL UP VERSIONS {} : {} -> {}", artifact, actualVersion, upVersionslist);
                    }
                }
            }
        });
    }
}
