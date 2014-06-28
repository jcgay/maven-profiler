package com.github.jcgay.maven.profiler;

import com.github.jcgay.maven.profiler.template.Data;
import com.github.jcgay.maven.profiler.template.EntryAndTime;
import com.github.jcgay.maven.profiler.template.Project;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.io.Closeables.closeQuietly;

@Component(role = EventSpy.class, hint = "profiler", description = "Measure times taken by Maven.")
public class ProfilerEventSpy extends AbstractEventSpy {

    static final String PROFILE = "profile";

    @Requirement
    private Logger logger;

    private Map<MavenProject, Stopwatch> projects = new ConcurrentHashMap<MavenProject, Stopwatch>();
    private Table<MavenProject, MojoExecution, Stopwatch> timers = HashBasedTable.create();
    private ConcurrentMap<Artifact, Stopwatch> downloadTimers = new ConcurrentHashMap<Artifact, Stopwatch>();
    private boolean isActive;
    private MavenProject topProject;

    public ProfilerEventSpy() {
        String parameter = System.getProperty(PROFILE);
        isActive = parameter != null && !"false".equalsIgnoreCase(parameter);
    }

    @VisibleForTesting
    ProfilerEventSpy(Logger logger,
                     ConcurrentHashMap<MavenProject, Stopwatch> projects,
                     Table<MavenProject, MojoExecution, Stopwatch> timers,
                     ConcurrentMap<Artifact, Stopwatch> downloadTimers,
                     MavenProject topProject) {
        this();
        this.logger = logger;
        this.projects = projects;
        this.timers = timers;
        this.downloadTimers = downloadTimers;
        this.topProject = topProject;
    }

    @Override
    public void onEvent(Object event) throws Exception {
        super.onEvent(event);
        if (isActive) {
            if (event instanceof ExecutionEvent) {
                saveExecutionTime((ExecutionEvent) event);
                saveProject((ExecutionEvent) event);
            } else if (event instanceof RepositoryEvent) {
                saveDownloadingTime((RepositoryEvent) event);
            }
        }
    }

    private void saveProject(ExecutionEvent event) {
        if (event.getType() == ExecutionEvent.Type.SessionStarted) {
            this.topProject = event.getProject();
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (isActive) {
            writeReport();
        }
    }

    private void writeReport() {

        Date now = new Date();

        Data context = new Data()
                .setProjects(allProjects())
                .setDate(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(now))
                .setName(topProject.getName());
        setDownloads(context);

        Handlebars handlebars = new Handlebars();
        FileWriter writer = null;
        try {
            Template template = handlebars.compile("report-template");
            writer = new FileWriter(getOuputDestination(new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(now)));
            writer.write(template.apply(context));
        } catch (IOException e) {
            logger.error("Cannot render profiler report.", e);
        } finally {
            closeQuietly(writer);
        }
    }

    private List<Project> allProjects() {
        List<Project> result = new ArrayList<Project>();
        ExecutionTimeDescriptor descriptor = ExecutionTimeDescriptor.instance(timers);
        for (MavenProject project : ProjectsSorter.byExecutionTime(projects)) {
            Project currentProject = new Project(project.getName(), projects.get(project));
            for (Map.Entry<MojoExecution, Stopwatch> mojo : descriptor.getSortedMojosByTime(project)) {
                currentProject.addMojoTime(new EntryAndTime<MojoExecution>(mojo.getKey(), mojo.getValue()));
            }
            result.add(currentProject);
        }
        return result;
    }

    private void setDownloads(Data data) {
        List<EntryAndTime<Artifact>> result = new ArrayList<EntryAndTime<Artifact>>();
        ArtifactDescriptor descriptor = ArtifactDescriptor.instance(downloadTimers);
        for (Artifact artifact : ProjectsSorter.byExecutionTime(downloadTimers)) {
            result.add(new EntryAndTime<Artifact>(artifact, downloadTimers.get(artifact)));
        }
        data.setDownloads(result)
            .setTotalDownloadTime(descriptor.getTotalTimeSpentDownloadingArtifacts());
    }

    private File getOuputDestination(String now) {
        File directory = new File(topProject.getBasedir(), ".profiler");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new RuntimeException("Cannot create file to write profiler report: " + directory);
        }
        return new File(directory, "profiler-report-" + now + ".html");
    }

    private void saveDownloadingTime(RepositoryEvent event) {
        logger.debug(String.format("Received event (%s): %s", event.getClass(), event));
        if (event.getType() == RepositoryEvent.EventType.ARTIFACT_DOWNLOADING) {
            startDownload(event);
        }
        if (event.getType() == RepositoryEvent.EventType.ARTIFACT_DOWNLOADED) {
            stopDownload(event);
        }
    }

    private void stopDownload(RepositoryEvent event) {
        if (hasNoException(event)) {
            logger.debug(String.format("Stopping timer for artifact [%s]", event.getArtifact()));
            downloadTimers.get(ArtifactProfiled.of(event.getArtifact())).stop();
        }
    }

    private static boolean hasNoException(RepositoryEvent event) {
        List<Exception> exceptions = event.getExceptions();
        return exceptions == null || exceptions.isEmpty();
    }

    private void startDownload(RepositoryEvent event) {
        logger.debug(String.format("Starting timer for artifact [%s]", event.getArtifact()));
        downloadTimers.putIfAbsent(ArtifactProfiled.of(event.getArtifact()), new Stopwatch().start());
    }

    private void saveExecutionTime(ExecutionEvent event) {
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
        synchronized (timers) {
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
}
