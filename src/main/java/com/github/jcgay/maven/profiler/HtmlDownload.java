package com.github.jcgay.maven.profiler;

import com.github.jcgay.maven.profiler.template.EntryAndTime;
import com.google.common.base.Stopwatch;
import org.sonatype.aether.artifact.Artifact;

import java.util.ArrayList;
import java.util.List;

public class HtmlDownload implements DownloadRendering {

    private final List<EntryAndTime<Artifact>> downloads = new ArrayList<EntryAndTime<Artifact>>();
    private Stopwatch totalTime;

    @Override
    public void title() {
        // do nothing
    }

    @Override
    public void separator() {
        // do nothing
    }

    @Override
    public void artifactTime(Artifact artifact, Stopwatch time, ArtifactDescriptor descriptor) {
        downloads.add(new EntryAndTime<Artifact>(artifact, time));
    }

    @Override
    public void totalTime(Stopwatch time) {
        totalTime = time;
    }

    public List<EntryAndTime<Artifact>> getDownloads() {
        return downloads;
    }

    public Stopwatch getTotalTime() {
        return totalTime;
    }
}
