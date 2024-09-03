package org.flunadela.depsvers;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import lombok.ToString;

@ToString
@XmlRootElement(name = "metadata")
public class MavenMetadataVersioning {

    @XmlElement
    Versioning versioning = new Versioning();

    @ToString
    public static class Versioning {
        @XmlElement
        String       release;

        @XmlElement
        String       latest;

        @XmlElement
        String       lastUpdated;

        @XmlElementWrapper(name = "versions")
        @XmlElement(name = "version")
        List<String> versions = new ArrayList<>();
    }
}