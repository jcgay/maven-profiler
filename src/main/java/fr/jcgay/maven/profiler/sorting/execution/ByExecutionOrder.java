package fr.jcgay.maven.profiler.sorting.execution;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Table;
import fr.jcgay.maven.profiler.sorting.Sorter;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ByExecutionOrder implements Sorter {

    @Override
    public List<MavenProject> projects(Map<MavenProject, Stopwatch> projects) {
        return new ArrayList<>(projects.keySet());
    }

    @Override
    public List<Artifact> downloads(Map<Artifact, Stopwatch> artifacts) {
        return new ArrayList<>(artifacts.keySet());
    }

    @Override
    public List<Map.Entry<MojoExecution, Stopwatch>> mojoExecutionsOf(MavenProject project, Table<MavenProject, MojoExecution, Stopwatch> executions) {
        return new ArrayList<>(executions.row(project).entrySet());
    }
}
