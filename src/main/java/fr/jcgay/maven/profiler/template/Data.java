package fr.jcgay.maven.profiler.template;

import com.google.common.base.Stopwatch;
import org.eclipse.aether.artifact.Artifact;

import java.util.List;
import java.util.Properties;

public class Data {

    private List<Project> projects;
    private List<EntryAndTime<Artifact>> downloads;
    private Stopwatch totalDownloadTime;
    private Stopwatch buildTime;
    private String date;
    private String name;
    private String goals;
    private Properties parameters;

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

    public String getGoals() {
        return goals;
    }

    public Properties getParameters() {
        return parameters;
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

    public Data setGoals(String goals) {
        this.goals = goals;
        return this;
    }

    public Data setParameters(Properties parameters) {
        this.parameters = parameters;
        return this;
    }
}
