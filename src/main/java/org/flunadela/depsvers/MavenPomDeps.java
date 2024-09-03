package org.flunadela.depsvers;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MavenPomDeps {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenPomDeps.class);

    public static final String UNDEFINED = "UNDEFINED";

    public List<DependencyEntry> parsePom(String fileFullPath) throws XmlPullParserException, IOException {
        LOGGER.info("Parsing POM file: {}", fileFullPath);

        FileInputStream fileInStream = new FileInputStream(fileFullPath);

        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model pomModel = reader.read(fileInStream);

        Properties pomProps = pomModel.getProperties();
        DependencyManagement pomDepMgmt = pomModel.getDependencyManagement();
        List<Dependency> pomDepsInMgmt = null;

        if (pomDepMgmt != null && pomDepMgmt.getDependencies() != null && !pomDepMgmt.getDependencies().isEmpty()) {
            pomDepsInMgmt = pomDepMgmt.getDependencies();
        }

        return getDeps(pomDepsInMgmt, pomModel.getDependencies(), pomProps);
    }

    private List<DependencyEntry> getDeps(List<Dependency> pomDepsInMgmt, List<Dependency> dependencies, Properties pomProps) {
        List<DependencyEntry> allDeps = new ArrayList<>();
        if (pomDepsInMgmt != null) {
            pomDepsInMgmt.forEach(dep -> allDeps.add(new DependencyEntry(dep.getGroupId(), dep.getArtifactId(), resolveVersion(pomProps, dep.getVersion()))));
        }

        if (dependencies != null) {
            dependencies.forEach(dep -> {
                String version = resolveVersion(pomProps, dep.getVersion());
                DependencyEntry de = new DependencyEntry(dep.getGroupId(), dep.getArtifactId(), version);

                if (!allDeps.contains(de)) {
                    allDeps.add(de);
                }
            });
        }

        return allDeps;
    }

    private String resolveVersion(Properties pomProps, String version) {
        if (version != null && version.startsWith("${") && version.endsWith("}")) {
            String propName = version.substring(2, version.length() - 1);
            return pomProps.getProperty(propName);
        }
        return UNDEFINED;
    }
}
