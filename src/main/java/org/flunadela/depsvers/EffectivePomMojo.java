package org.flunadela.depsvers;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.io.xpp3.MavenXpp3WriterEx;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.XmlWriterUtil;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import javax.xml.XMLConstants;


public class EffectivePomMojo {

    /**
     * The artifact for which to display the effective POM.
     * <br>
     * <b>Note</b>: Should respect the Maven format, i.e. <code>groupId:artifactId[:version]</code>. The
     * latest version of the artifact will be used when no version is specified.
     *
     * @since 3.0.0
     */
//    @Parameter(property = "artifact")
    private String artifact;

    /**
     * Output POM input location as comments.
     *
     * @since 3.2.0
     */
//    @Parameter(property = "verbose", defaultValue = "false")
    private boolean verbose = false;

    File output;
    MavenProject project;
    MavenSession session;
    RepositorySystem repositorySystem;
    ProjectBuilder projectBuilder;

    public void execute() throws Exception {
        if (artifact != null && !artifact.isEmpty()) {
            project = getMavenProject(artifact);
        }

        StringWriter w = new StringWriter();
        String encoding = output != null ? project.getModel().getModelEncoding() : System.getProperty("file.encoding");
        XMLWriter writer = new PrettyPrintXMLWriter(
                w, StringUtils.repeat(" ", XmlWriterUtil.DEFAULT_INDENTATION_SIZE), encoding, null);

        writeEffectivePom(project, writer);

        String effectivePom = prettyFormat(w.toString(), encoding, false);

        if (verbose) {
            // tweak location tracking comment, that are put on a separate line by pretty print
            effectivePom = effectivePom.replaceAll("(?m)>\\s+<!--}", ">  <!-- ");
        }

        if (output != null) {
            writeXmlFile(output, effectivePom);

        }
    }

    /**
     * Retrieves the Maven Project associated with the given artifact String, in the form of
     * <code>groupId:artifactId[:version]</code>. This resolves the POM artifact at those coordinates and then builds
     * the Maven project from it.
     *
     * @param artifactString Coordinates of the Maven project to get.
     * @return New Maven project.
     * @throws Exception If there was an error while getting the Maven project.
     */
    protected MavenProject getMavenProject(String artifactString) throws Exception {
        ProjectBuildingRequest pbr = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        pbr.setRemoteRepositories(project.getRemoteArtifactRepositories());
        pbr.setPluginArtifactRepositories(project.getPluginArtifactRepositories());
        pbr.setProject(null);
        pbr.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        pbr.setResolveDependencies(true);

        org.eclipse.aether.artifact.Artifact projectArtifact = resolveArtifact(getAetherArtifact(artifactString, "pom")).getArtifact();

        return projectBuilder.build(projectArtifact.getFile(), pbr).getProject();
    }

    protected org.eclipse.aether.resolution.ArtifactResult resolveArtifact(
            org.eclipse.aether.artifact.Artifact artifact) throws RepositoryException {
        RepositorySystemSession repositorySession = session.getRepositorySession();

        // use descriptor to respect relocation
        ArtifactDescriptorResult artifactDescriptor = repositorySystem.readArtifactDescriptor(
                repositorySession,
                new ArtifactDescriptorRequest(artifact, project.getRemoteProjectRepositories(), null));

        return repositorySystem.resolveArtifact(
                repositorySession,
                new ArtifactRequest(artifactDescriptor.getArtifact(), project.getRemoteProjectRepositories(), null));
    }

    /**
     * Parses the given String into GAV artifact coordinate information, adding the given type.
     *
     * @param artifactString should respect the format <code>groupId:artifactId[:version]</code>
     * @param type           The extension for the artifact, must not be <code>null</code>.
     * @return the <code>Artifact</code> object for the <code>artifactString</code> parameter.
     * @throws Exception if the <code>artifactString</code> doesn't respect the format.
     */
    protected org.eclipse.aether.artifact.Artifact getAetherArtifact(String artifactString, String type) throws Exception {
        if (artifactString == null || artifactString.isEmpty()) {
            throw new IllegalArgumentException("artifact parameter could not be empty");
        }

        String groupId; // required
        String artifactId; // required
        String version; // optional

        String[] artifactParts = artifactString.split(":");
        switch (artifactParts.length) {
            case 2:
                groupId = artifactParts[0];
                artifactId = artifactParts[1];
                version = Artifact.LATEST_VERSION;
                break;
            case 3:
                groupId = artifactParts[0];
                artifactId = artifactParts[1];
                version = artifactParts[2];
                break;
            default:
                throw new Exception("The artifact parameter '" + artifactString
                        + "' should be conform to: " + "'groupId:artifactId[:version]'.");
        }

        return new DefaultArtifact(groupId, artifactId, type, version);
    }

    private void writeEffectivePom(MavenProject project, XMLWriter writer) throws IOException {
        Model pom = project.getModel();
        cleanModel(pom);

        StringWriter sWriter = new StringWriter();

        if (verbose) {
            MavenXpp3WriterEx mavenXpp3WriterEx = new MavenXpp3WriterEx();
            mavenXpp3WriterEx.setStringFormatter(new InputLocationStringFormatter());
            mavenXpp3WriterEx.write(sWriter, pom);
        } else {
            new MavenXpp3Writer().write(sWriter, pom);
        }

        // This removes the XML declaration written by MavenXpp3Writer
        String effectivePom = prettyFormat(sWriter.toString(), null, true);
        writer.writeMarkup(effectivePom);
    }

    private static void cleanModel(Model pom) {
        Properties properties = new SortedProperties();
        properties.putAll(pom.getProperties());
        pom.setProperties(properties);
    }

    private static class InputLocationStringFormatter extends InputLocation.StringFormatter {
        @Override
        public String toString(InputLocation location) {
            InputSource source = location.getSource();

            String s = source.getModelId(); // by default, display modelId

            if (StringUtils.isBlank(s) || s.contains("[unknown-version]")) {
                // unless it is blank or does not provide version information
                s = source.toString();
            }

            return '}' + s + ((location.getLineNumber() >= 0) ? ", line " + location.getLineNumber() : "") + ' ';
        }
    }

    protected static void writeXmlFile(File output, String content) throws IOException {
        if (output == null) {
            return;
        }

        output.getParentFile().mkdirs();

        try (Writer out = WriterFactory.newXmlWriter(output)) {
            out.write(content);
        }
    }

    /**
     * @param effectiveModel  not null
     * @param encoding        not null
     * @param omitDeclaration whether the XML declaration should be omitted from the effective pom
     * @return pretty format of the xml or the original {@code effectiveModel} if an error occurred.
     */
    protected static String prettyFormat(String effectiveModel, String encoding, boolean omitDeclaration) {
        SAXBuilder builder = new SAXBuilder();
        builder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        builder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        try {
            Document effectiveDocument = builder.build(new StringReader(effectiveModel));

            StringWriter w = new StringWriter();
            Format format = Format.getPrettyFormat();
            if (encoding != null) {
                // This is a design flaw in JDOM, no NPE on null arguments, but null is not prohibited
                format.setEncoding(encoding);
            }
            format.setLineSeparator(System.lineSeparator());
            format.setOmitDeclaration(omitDeclaration);
            XMLOutputter out = new XMLOutputter(format);
            out.output(effectiveDocument, w);

            return w.toString();
        } catch (JDOMException | IOException e) {
            return effectiveModel;
        }
    }

    /**
     * Properties which provides a sorted keySet().
     */
    protected static class SortedProperties extends Properties {
        /**
         * serialVersionUID
         */
        @Serial
        private static final long serialVersionUID = -8985316072702233744L;

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public Set<Object> keySet() {
            Set<Object> keyNames = super.keySet();
            List list = new ArrayList(keyNames);
            Collections.sort(list);

            return new LinkedHashSet<>(list);
        }
    }
}