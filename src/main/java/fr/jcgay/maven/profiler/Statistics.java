package fr.jcgay.maven.profiler;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static org.slf4j.LoggerFactory.getLogger;

public class Statistics {

    private static final Logger LOGGER = getLogger(Statistics.class);

    private final Map<MavenProject, Stopwatch> projects = new LinkedHashMap<MavenProject, Stopwatch>();
    private final Map<MavenProject, Map<MojoExecution, Stopwatch>> executions = new LinkedHashMap<MavenProject, Map<MojoExecution, Stopwatch>>();
    private final Map<Artifact, Stopwatch> downloadTimers = new LinkedHashMap<Artifact, Stopwatch>();

    private MavenProject topProject;
    private Set<String> goals = emptySet();
    private Properties properties = new Properties();
    private Date startTime;

    public Statistics setTopProject(MavenProject topProject) {
        this.topProject = topProject;
        return this;
    }

    public MavenProject topProject() {
        return topProject;
    }

    public Statistics setGoals(Set<String> goals) {
        this.goals = goals;
        return this;
    }

    public Iterable<String> goals() {
        return goals;
    }

    public Statistics setProperties(Properties properties) {
        this.properties = properties;
        return this;
    }

    public Properties properties() {
        return properties;
    }

    public Map<MavenProject, Stopwatch> projects() {
        return unmodifiableMap(projects);
    }

    public Table<MavenProject, MojoExecution, Stopwatch> executions() {
        ImmutableTable.Builder<MavenProject, MojoExecution, Stopwatch> builder = ImmutableTable.builder();
        for (Map.Entry<MavenProject, Map<MojoExecution, Stopwatch>> byProject : executions.entrySet()) {
            for (Map.Entry<MojoExecution, Stopwatch> executionTimer : byProject.getValue().entrySet()) {
                builder.put(byProject.getKey(), executionTimer.getKey(), executionTimer.getValue());
            }
        }
        return builder.build();
    }

    public Map<Artifact, Stopwatch> downloads() {
        return unmodifiableMap(downloadTimers);
    }

    public synchronized Statistics startProject(MavenProject project) {
        LOGGER.debug("Starting timer for project: " + project);
        projects.put(project, new Stopwatch().start());
        return this;
    }

    public synchronized Statistics startDownload(Artifact artifact) {
        LOGGER.debug("Starting timer for artifact [{}]", artifact);
        downloadTimers.put(ArtifactProfiled.of(artifact), new Stopwatch().start());
        return this;
    }

    public synchronized Statistics startExecution(MavenProject project, MojoExecution execution) {
        LOGGER.debug("Starting timer for mojo [{}] in project [{}].", execution, project);
        Map<MojoExecution, Stopwatch> projectExecutions = executions.get(project);
        if (projectExecutions == null) {
            projectExecutions = new LinkedHashMap<MojoExecution, Stopwatch>();
            executions.put(project, projectExecutions);
        }
        projectExecutions.put(execution, new Stopwatch().start());
        return this;
    }

    public Statistics stopDownload(Artifact artifact) {
        LOGGER.debug("Stopping timer for artifact [{}]", artifact);
        downloads().get(ArtifactProfiled.of(artifact)).stop();
        return this;
    }

    public Statistics stopExecution(MavenProject project, MojoExecution execution) {
        LOGGER.debug("Stopping timer for mojo [{}] in project [{}].", execution, project);
        Map<MojoExecution, Stopwatch> projectExecutions = executions.get(project);
        if (projectExecutions == null) {
            throw new IllegalStateException("Cannot stop a timer execution because project has not been registered");
        }
        Stopwatch stopwatch = projectExecutions.get(execution);
        if (stopwatch == null) {
            throw new IllegalStateException("Cannot stop a timer execution because execution has not been registered");
        }
        stopwatch.stop();
        return this;
    }

    public Statistics stopProject(MavenProject project) {
        LOGGER.debug("Stopping timer for project: " + project);
        projects.get(project).stop();
        return this;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getStartTime() {
        return startTime;
    }
}
