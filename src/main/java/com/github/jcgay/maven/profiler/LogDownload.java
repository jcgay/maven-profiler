package com.github.jcgay.maven.profiler;

import com.google.common.base.Stopwatch;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.artifact.Artifact;

import java.util.Map;

public class LogDownload implements DownloadRendering {

    private final Logger logger;
    private final Map<Artifact, Stopwatch> downloadTimers;

    public LogDownload(Logger logger, Map<Artifact, Stopwatch> downloadTimers) {
        this.logger = logger;
        this.downloadTimers = downloadTimers;
    }

    @Override
    public void title() {
        if (downloadTimers.isEmpty()) {
            logger.info("No new artifact downloaded...");
        } else {
            logger.info("DOWNLOADING TIME");
        }
    }

    @Override
    public void separator() {
        logger.info("------------------------------------------------------------------------");
    }

    @Override
    public void artifactTime(Artifact artifact, Stopwatch time, ArtifactDescriptor descriptor) {
        logger.info(descriptor.getFormattedLine(artifact) + time);
    }

    @Override
    public void totalTime(Stopwatch time) {
        if (!downloadTimers.isEmpty()) {
            logger.info("Total Time: " + time);
        }
    }
}
