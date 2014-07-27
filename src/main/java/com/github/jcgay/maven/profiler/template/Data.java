package com.github.jcgay.maven.profiler.template;

import com.google.common.base.Stopwatch;
import org.eclipse.aether.artifact.Artifact;

import java.util.List;

public class Data {

    private List<Project> projects;
    private List<EntryAndTime<Artifact>> downloads;
    private Stopwatch totalDownloadTime;
    private Stopwatch buildTime;
    private String date;
    private String name;

    public List<Project> getProjects() {
        return projects;
    }

    public List<EntryAndTime<Artifact>> getDownloads() {
        return downloads;
    }

    public Stopwatch getTotalDownloadTime() {
        return totalDownloadTime;
    }

    public boolean isDownloadSectionDisplayed() {
        return downloads != null && !downloads.isEmpty();
    }

    public Stopwatch getBuildTime() {
        return buildTime;
    }

    public String getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public Data setProjects(List<Project> projects) {
        this.projects = projects;
        return this;
    }

    public Data setDownloads(List<EntryAndTime<Artifact>> downloads) {
        this.downloads = downloads;
        return this;
    }

    public Data setTotalDownloadTime(Stopwatch time) {
        this.totalDownloadTime = time;
        return this;
    }

    public Data setBuildTime(Stopwatch time) {
        this.buildTime = time;
        return this;
    }

    public Data setDate(String date) {
        this.date = date;
        return this;
    }

    public Data setName(String name) {
        this.name = name;
        return this;
    }
}
