package org.flunadela.depsvers;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;

@Getter
public class MavenMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenMetadata.class);

    private static final String METADATA_FILE_NAME = "maven-metadata.xml";

    private final String repoBaseUrl;
    private String minVersion;

    public MavenMetadata(String repoBaseUrl) {
        this.repoBaseUrl = StringUtils.removeEnd(repoBaseUrl, "/");
    }

    public MavenMetadataVersioning getArtifactMetadata(String groupId,
                                                       String artifactId,
                                                       String minVersion) {
        this.minVersion = StringUtils.trimToNull(minVersion);
        InputStream input = null;

        try {
            String artifactUrl = getRepoBaseUrl() + "/" + StringUtils.replace(groupId, ".", "/") + "/" + artifactId + "/" + METADATA_FILE_NAME;
            URL url = URI.create(artifactUrl).toURL();
            URLConnection conn = url.openConnection();
            input = conn.getInputStream();

            Thread currentThread = Thread.currentThread();
            ClassLoader originalContext = currentThread.getContextClassLoader();

            try {
                currentThread.setContextClassLoader(MavenMetadata.class.getClassLoader());
                JAXBContext context = JAXBContext.newInstance(MavenMetadataVersioning.class);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                MavenMetadataVersioning metadata = (MavenMetadataVersioning) unmarshaller.unmarshal(input);
                Collections.reverse(metadata.versioning.versions);
                removeVersionsLowerThan(metadata, minVersion);

                return metadata;
            } finally {
                currentThread.setContextClassLoader(originalContext);
            }
        } catch (FileNotFoundException fnfe) {
            LOGGER.warn("Could not find maven-metadata.xml for artifact: {} {}", groupId, artifactId);
            return null;
        } catch (Exception e) {
            LOGGER.warn("Could not parse maven-metadata.xml", e);
            return null;
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    private void removeVersionsLowerThan(MavenMetadataVersioning metadata, String minVersion) {
        if (minVersion == null) {
            return;
        }

        metadata.versioning.versions.removeIf(this::isVersionLowerThan);
    }

    private boolean isVersionLowerThan(String version) {
        if (StringUtils.isBlank(minVersion)) {
            return false;
        }

        String[] minVers = StringUtils.split(minVersion, ".");
        String[] vers = StringUtils.split(version, ".");

        for (int i = 0; i < vers.length; i++) {
            if (i >= minVers.length) {
                return false;
            }

            if (StringUtils.isNumeric(vers[i]) && (!StringUtils.isAlphaSpace(vers[i]) || !StringUtils.contains(vers[i], "-"))) {
                int compRes = Integer.parseInt(vers[i]) - Integer.parseInt(minVers[i]);
                if (compRes < 0) {
                    return true;
                }
            } else {
                int compRes = vers[i].compareTo(minVers[i]);
                if (compRes < 0) {
                    return true;
                }
            }
        }
        return false;
    }
}