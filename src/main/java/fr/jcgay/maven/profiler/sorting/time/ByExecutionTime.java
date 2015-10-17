package fr.jcgay.maven.profiler.sorting.time;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Table;
import fr.jcgay.maven.profiler.sorting.Sorter;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;

import java.util.List;
import java.util.Map;

public class ByExecutionTime implements Sorter {

    @Override
    public List<MavenProject> projects(Map<MavenProject, Stopwatch> projects) {
        return ProjectsSorter.byExecutionTime(projects);
    }

    @Override
    public List<Artifact> downloads(Map<Artifact, Stopwatch> projects) {
        return ProjectsSorter.byExecutionTime(projects);
    }

    @Override
    public List<Map.Entry<MojoExecution, Stopwatch>> mojoExecutionsOf(MavenProject project, Table<MavenProject, MojoExecution, Stopwatch> executions) {
        return ExecutionTimeSorter
            .instance(executions)
            .getSortedMojosByTime(project);
    }
}
