package org.flunadela.depsvers;

import java.io.FileNotFoundException;
import java.nio.file.FileSystems;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class MavenPomDeps {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenPomDeps.class);

    public static final String SKIP = "SKIP: ";
    public static final String RANGE_SKIP = SKIP + "RANGE SKIPPED";
    public static final String PROPERTY_UNDEFINED = SKIP + "PROPERTY UNDEFINED";
    public static final String UNDEFINED = SKIP + "UNDEFINED";
    public static final String UNRECOGNIZED = SKIP + "NOT RECOGNIZED";

    public static final String SYSTEM_FILE_SEPARATOR = FileSystems.getDefault().getSeparator();

    private final MavenMetadata mavenMetadata = new MavenMetadata("https://repo.maven.apache.org/maven2");

    public Map<String, List<String>> getVersionsTree(String pomFileToParse) throws XmlPullParserException, IOException {
        TreeMap<String, List<String>> versionsTree = new TreeMap<>();

        List<DependencyEntry> deps = parsePom(pomFileToParse);

        // collect the list of versions for each dependency
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

        return versionsTree;
    }

    public List<DependencyEntry> parsePom(String fileFullPath) throws XmlPullParserException, IOException {
        LOGGER.info("Parsing POM file: {}", fileFullPath);

        try (FileInputStream fileInStream = new FileInputStream(fileFullPath)) {
            Model pomModel = (new MavenXpp3Reader()).read(fileInStream);
            DependencyManagement pomDepMgmt = pomModel.getDependencyManagement();

            List<Dependency> pomDepsInMgmt = null;

            if (pomDepMgmt != null && pomDepMgmt.getDependencies() != null && !pomDepMgmt.getDependencies().isEmpty()) {
                pomDepsInMgmt = pomDepMgmt.getDependencies();
            }

            return getDeps(pomDepsInMgmt, pomModel.getDependencies(), getAllProperties(pomModel, fileFullPath));
        }
    }

    private Properties getAllProperties(Model pomModel, String childFullPath) {
        Properties allProps = new Properties();

        // collect properties from parent POMs if any
        Model somePom = pomModel;

        while (somePom.getParent() != null) {
            String parentRelativePath = somePom.getParent().getRelativePath();

            if (parentRelativePath.startsWith("..")) { // process only local parent POMs
                // reconstruct parent POM file path from the child POM file path
                String childDirPath = StringUtils.substringBeforeLast(childFullPath, SYSTEM_FILE_SEPARATOR);
                String parentFullPath = StringUtils.normalizeSpace(childDirPath + SYSTEM_FILE_SEPARATOR + parentRelativePath);

                try (FileInputStream fileInStream = new FileInputStream(parentFullPath)) {
                    somePom = (new MavenXpp3Reader()).read(fileInStream);

                    if (somePom.getProperties() != null) {
                        allProps.putAll(somePom.getProperties());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Couldn't process parent file", e);
                }
            } else if (parentRelativePath.isBlank()) {
                // TODO: implement processing of parent POM from local Maven repo(s) if needed
                break;
            } else {
                break;
            }
        }
        // add properties from the current POM
        if (pomModel.getProperties() != null) {
            allProps.putAll(pomModel.getProperties());
        }

        return allProps;
    }

    private List<DependencyEntry> getDeps(List<Dependency> pomDepsInMgmt, List<Dependency> dependencies, Properties pomProps) {
        List<DependencyEntry> allDeps = new ArrayList<>();

        if (pomDepsInMgmt != null) {
            pomDepsInMgmt.forEach(dep ->
                    allDeps.add(new DependencyEntry(
                            dep.getGroupId(),
                            dep.getArtifactId(),
                            resolveVersion(pomProps, dep.getVersion()))));
        }

        if (dependencies != null) {
            dependencies.forEach(dep -> {
                DependencyEntry de = new DependencyEntry(
                        dep.getGroupId(),
                        dep.getArtifactId(),
                        resolveVersion(pomProps, dep.getVersion()));

                if (!allDeps.contains(de)) {
                    allDeps.add(de);
                }
            });
        }

        return allDeps;
    }

    // TODO: enhance version resolution
    private String resolveVersion(Properties pomProps, String version) {
        if (version == null) {
            return UNDEFINED; // version may be defined in parent POM or some BOM import

        } else if (version.contains(",")) {
            return RANGE_SKIP;

        } else if (version.startsWith("${") && version.endsWith("}")) { // property reference
            String propName = version.substring(2, version.length() - 1);
            String propValue = pomProps.getProperty(propName);

            return Objects.requireNonNullElse(propValue, PROPERTY_UNDEFINED + ": " + version);

        } else if (StringUtils.isNumeric(version) || Strings.CS.contains(version, ".")) {
            return version;
        }

        return UNRECOGNIZED + ": " + version;
    }
}
