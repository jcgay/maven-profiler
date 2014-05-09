package com.github.jcgay.maven.profiler;

import com.google.common.base.Stopwatch;
import org.sonatype.aether.artifact.Artifact;

public interface DownloadRendering {

    void title();

    void separator();

    void artifactTime(Artifact artifact, Stopwatch time, ArtifactDescriptor descriptor);

    void totalTime(Stopwatch time);
}
