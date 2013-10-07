package com.github.jcgay.maven.profiler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.RepositoryEvent;
import org.sonatype.aether.artifact.Artifact;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component(role = EventSpy.class, hint = "profiler", description = "Measure times taken by Maven.")
public class ProfilerEventSpy extends AbstractEventSpy {

    static final String PROFILE = "profile";

    @Requirement
    private Logger logger;

    private Map<MavenProject, Stopwatch> projects = new ConcurrentHashMap<MavenProject, Stopwatch>();
    private Table<MavenProject, MojoExecution, Stopwatch> timers = HashBasedTable.create();
    private ConcurrentMap<Artifact, Stopwatch> downloadTimers = new ConcurrentHashMap<Artifact, Stopwatch>();
    private boolean isActive;

    public ProfilerEventSpy() {
        String parameter = System.getProperty(PROFILE);
        isActive = parameter != null && !"false".equalsIgnoreCase(parameter);
    }

    @VisibleForTesting
    ProfilerEventSpy(Logger logger,
                     ConcurrentHashMap<MavenProject, Stopwatch> projects,
                     Table<MavenProject, MojoExecution, Stopwatch> timers,
                     ConcurrentMap<Artifact, Stopwatch> downloadTimers) {
        this();
        this.logger = logger;
        this.projects = projects;
        this.timers = timers;
        this.downloadTimers = downloadTimers;
    }

    @Override
    public void onEvent(Object event) throws Exception {
        if (isActive) {
            if (event instanceof ExecutionEvent) {
                saveTime((ExecutionEvent) event);
            } else if (event instanceof RepositoryEvent) {
                logDownloadingTime((RepositoryEvent) event);
            }

        }
        super.onEvent(event);
    }

    @Override
    public void close() throws Exception {
        if (isActive) {
            printTime();
            printDownloadTime();
        }
        super.close();
    }

    private void logDownloadingTime(RepositoryEvent event) {
        logger.debug(String.format("Received event (%s): %s", event.getClass(), event));
        if (event.getType() == RepositoryEvent.EventType.ARTIFACT_DOWNLOADING) {
            startDownload(event);
        }
        if (event.getType() == RepositoryEvent.EventType.ARTIFACT_DOWNLOADED) {
            stopDownload(event);
        }
    }

    private void stopDownload(RepositoryEvent event) {
        logger.debug(String.format("Stopping timer for artifact [%s]", event.getArtifact()));
        downloadTimers.get(ArtifactProfiled.of(event.getArtifact())).stop();
    }

    private void startDownload(RepositoryEvent event) {
        logger.debug(String.format("Starting timer for artifact [%s]", event.getArtifact()));
        downloadTimers.putIfAbsent(ArtifactProfiled.of(event.getArtifact()), new Stopwatch().start());
    }

    private void saveTime(ExecutionEvent event) {
        logger.debug(String.format("Received event (%s): %s", event.getClass(), event));
        ExecutionEvent.Type type = event.getType();
        MavenProject currentProject = event.getSession().getCurrentProject();
        if (type == ExecutionEvent.Type.ProjectStarted) {
            startProject(currentProject);
        }
        if (type == ExecutionEvent.Type.ProjectSucceeded || type == ExecutionEvent.Type.ProjectFailed) {
            stopProject(currentProject);
        }
        if (type == ExecutionEvent.Type.MojoStarted) {
            startMojo(event, currentProject);
        }
        if (type == ExecutionEvent.Type.MojoSucceeded || type == ExecutionEvent.Type.MojoFailed) {
            stopMojo(event, currentProject);
        }
    }

    private void stopMojo(ExecutionEvent currentEvent, MavenProject currentProject) {
        logger.debug(String.format("Stopping timer for mojo [%s] in project [%s].", currentEvent.getMojoExecution(), currentProject));
        timers.get(currentProject, currentEvent.getMojoExecution()).stop();
    }

    private void startMojo(ExecutionEvent currentEvent, MavenProject currentProject) {
        logger.debug(String.format("Starting timer for mojo [%s] in project [%s].", currentEvent.getMojoExecution(), currentProject));
        synchronized (currentEvent) {
            timers.put(currentProject, currentEvent.getMojoExecution(), new Stopwatch().start());
        }
    }

    private void stopProject(MavenProject currentProject) {
        logger.debug("Stopping timer for project: " + currentProject);
        projects.get(currentProject).stop();
    }

    private void startProject(MavenProject currentProject) {
        logger.debug("Starting timer for project: " + currentProject);
        projects.put(currentProject, new Stopwatch().start());
    }

    private void printTime() {
        logger.info("EXECUTION TIME");
        separator();
        ExecutionTimeDescriptor descriptor = ExecutionTimeDescriptor.instance(timers);
        for (MavenProject project : ProjectsSorter.byExecutionTime(projects)) {
            logger.info(project.getName() + ": " + projects.get(project));
            for (Map.Entry<MojoExecution, Stopwatch> mojo : descriptor.getSortedMojosByTime(project)) {
                logger.info(descriptor.getFormattedLine(mojo));
            }
        }
    }

    private void separator() {
        logger.info("------------------------------------------------------------------------");
    }

    private void printDownloadTime() {
        if (downloadTimers.isEmpty()) {
            logger.info("No new artifact downloaded...");
            return;
        }
        separator();
        logger.info("DOWNLOADING TIME");
        separator();
        ArtifactDescriptor descriptor = ArtifactDescriptor.instance(downloadTimers);
        for (Artifact artifact : ProjectsSorter.byExecutionTime(downloadTimers)) {
            logger.info(descriptor.getFormattedLine(artifact) + downloadTimers.get(artifact));
        }
    }
}
