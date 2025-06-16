package org.flunadela.depsvers;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

@Getter
public class MavenMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenMetadata.class);

    private static final String METADATA_FILE_NAME = "maven-metadata.xml";

    private final String repoBaseUrl;
    private String pomMinVersion;

    public MavenMetadata(String repoBaseUrl) {
        this.repoBaseUrl = StringUtils.removeEnd(repoBaseUrl, "/");
    }

    public MavenMetadataVersioning getArtifactMetadata(String groupId,
                                                       String artifactId,
                                                       String pomMinVersion) {
        this.pomMinVersion = StringUtils.trimToNull(pomMinVersion);
        InputStream xmlInput = null;

        try {
            String artifactUrl = getRepoBaseUrl() + "/" + StringUtils.replace(groupId, ".", "/") + "/" + artifactId + "/" + METADATA_FILE_NAME;
            URL url = URI.create(artifactUrl).toURL();
            URLConnection conn = url.openConnection();
            xmlInput = conn.getInputStream();

            Thread currentThread = Thread.currentThread();
            ClassLoader originalContext = currentThread.getContextClassLoader();

            try {
                currentThread.setContextClassLoader(MavenMetadata.class.getClassLoader());
                JAXBContext context = JAXBContext.newInstance(MavenMetadataVersioning.class);
                Unmarshaller unmarshaller = context.createUnmarshaller();

                // Disable XXE by configuring the underlying XMLInputFactory
                XMLInputFactory xif = XMLInputFactory.newFactory();
                xif.setProperty(XMLInputFactory.SUPPORT_DTD, false); // Disallow DTDs
                xif.setProperty("javax.xml.stream.isSupportingExternalEntities", false); // Disallow external entities

                XMLStreamReader xsr = xif.createXMLStreamReader(xmlInput);
                MavenMetadataVersioning metadata = (MavenMetadataVersioning) unmarshaller.unmarshal(xsr);
                removeVersionsLowerThan(metadata, pomMinVersion);

                return metadata;
            } finally {
                currentThread.setContextClassLoader(originalContext);
            }
        } catch (FileNotFoundException fnfe) {
            LOGGER.warn("Could not find maven-metadata.xml for {} : {}", groupId, artifactId);
            return null;
        } catch (Exception e) {
            LOGGER.warn("Could not parse maven-metadata.xml", e);
            return null;
        } finally {
            IOUtils.closeQuietly(xmlInput);
        }
    }

    private void removeVersionsLowerThan(MavenMetadataVersioning metadata, String minVersion) {
        if (minVersion == null) {
            return;
        }

        metadata.versioning.versions.removeIf(this::isVersionLowerThan);
    }

    private boolean isVersionLowerThan(String version) {
        if (StringUtils.isBlank(pomMinVersion)) {
            return false;
        }

        String[] minVers = StringUtils.split(pomMinVersion, ".");
        String[] vers = StringUtils.split(version, ".");

        int parts = Math.max(minVers.length, vers.length);

        for (int i = 0; i < parts; i++) {
            if (i >= minVers.length) { // case when version has more parts than minVersion
                return false;
            }

            String v;
            if (i > vers.length - 1) { // case when minVersion has more parts than version
                v = "0";
            } else {
                v = vers[i];
            }

            String minV = minVers[i];

            if (StringUtils.isNumeric(v) && (!StringUtils.isAlphaSpace(v) || !StringUtils.contains(v, "-")) && StringUtils.isNumeric(minV)) {
                int compRes = Integer.parseInt(v) - Integer.parseInt(minV);
                if (compRes < 0) {
                    return true;
                }
            } else {
                int compRes = v.compareTo(minV);
                if (compRes < 0) {
                    return true;
                }
            }
        }
        return false;
    }
}