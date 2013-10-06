package com.github.jcgay.maven.profiler;

import com.google.common.base.Objects;
import org.sonatype.aether.artifact.Artifact;

import java.io.File;
import java.util.Map;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;

public class ArtifactProfiled implements Artifact {

    private final Artifact artifact;

    private ArtifactProfiled(Artifact artifact) {
        this.artifact = artifact;
    }

    public static ArtifactProfiled of(Artifact artifact) {
        return new ArtifactProfiled(checkNotNull(artifact));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(
                artifact.getArtifactId(),
                artifact.getGroupId(),
                artifact.getVersion(),
                artifact.getExtension(),
                artifact.getClassifier(),
                artifact.getBaseVersion()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ArtifactProfiled other = (ArtifactProfiled) obj;
        return equal(this.artifact.getArtifactId(), other.artifact.getArtifactId())
                && equal(this.artifact.getGroupId(), other.artifact.getGroupId())
                && equal(this.artifact.getVersion(), other.artifact.getVersion())
                && equal(this.artifact.getExtension(), other.artifact.getExtension())
                && equal(this.artifact.getClassifier(), other.artifact.getClassifier())
                && equal(this.artifact.getBaseVersion(), other.artifact.getBaseVersion());
    }

    @Override
    public String toString() {
        return artifact.toString();
    }

    @Override
    public String getGroupId() {
        return artifact.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return artifact.getArtifactId();
    }

    @Override
    public String getVersion() {
        return artifact.getVersion();
    }

    @Override
    public Artifact setVersion(String version) {
        return artifact.setVersion(version);
    }

    @Override
    public String getBaseVersion() {
        return artifact.getBaseVersion();
    }

    @Override
    public boolean isSnapshot() {
        return artifact.isSnapshot();
    }

    @Override
    public String getClassifier() {
        return artifact.getClassifier();
    }

    @Override
    public String getExtension() {
        return artifact.getExtension();
    }

    @Override
    public File getFile() {
        return artifact.getFile();
    }

    @Override
    public Artifact setFile(File file) {
        return artifact.setFile(file);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return artifact.getProperty(key, defaultValue);
    }

    @Override
    public Map<String, String> getProperties() {
        return artifact.getProperties();
    }

    @Override
    public Artifact setProperties(Map<String, String> properties) {
        return artifact.setProperties(properties);
    }
}
