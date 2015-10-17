package fr.jcgay.maven.profiler.sorting;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Table;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;

import java.util.List;
import java.util.Map;

public interface Sorter {

    List<MavenProject> projects(Map<MavenProject, Stopwatch> projects);

    List<Artifact> downloads(Map<Artifact, Stopwatch> projects);

    List<Map.Entry<MojoExecution, Stopwatch>> mojoExecutionsOf(MavenProject project, Table<MavenProject, MojoExecution, Stopwatch> executions);
}
