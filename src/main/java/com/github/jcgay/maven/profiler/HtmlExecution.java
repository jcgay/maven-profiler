package com.github.jcgay.maven.profiler;

import com.github.jcgay.maven.profiler.template.EntryAndTime;
import com.github.jcgay.maven.profiler.template.Project;
import com.google.common.base.Stopwatch;
import org.apache.maven.plugin.MojoExecution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HtmlExecution implements ExecutionRendering {

    private final List<Project> projects = new ArrayList<Project>();
    private Project currentProject;

    @Override
    public void title() {
        // do nothing
    }

    @Override
    public void separator() {
        // do nothing
    }

    @Override
    public void projectSummary(String name, Stopwatch totalTime) {
        currentProject = new Project(name, totalTime);
        projects.add(currentProject);
    }

    @Override
    public void mojoExecution(Map.Entry<MojoExecution, Stopwatch> mojo, ExecutionTimeDescriptor descriptor) {
        currentProject.addMojoTime(new EntryAndTime<MojoExecution>(mojo.getKey(), mojo.getValue()));
    }

    public List<Project> getProjects() {
        return projects;
    }
}
