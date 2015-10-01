package fr.jcgay.maven.profiler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.artifact.Artifact;

import fr.jcgay.maven.profiler.template.Data;
import fr.jcgay.maven.profiler.template.EntryAndTime;
import fr.jcgay.maven.profiler.template.Project;

@Component(role = EventSpy.class, hint = "profiler", description = "Measure times taken by Maven.")
public class ProfilerEventSpy extends AbstractEventSpy {

    static final String PROFILE = "profile";
    static final String PROFILE_FORMAT = "profileFormat";
    static final String DISABLE_TIME_SORTING = "disableTimeSorting";

    @Requirement
    private Logger logger;

    static class SequenceEvent {
        private MavenProject project;
        private MojoExecution mojo;

        SequenceEvent(MavenProject project, MojoExecution mojo) {
            this.project = project;
            this.mojo = mojo;
        }

        public MojoExecution getMojo() {
            return mojo;
        }

        public void setMojo(MojoExecution mojo) {
            this.mojo = mojo;
        }

        public MavenProject getProject() {
            return project;
        }

        public void setProject(MavenProject project) {
            this.project = project;
        }
    }

    private List<SequenceEvent> sequenceEvents;
    private List<Artifact> sequenceDownloads;

    private final Map<MavenProject, Stopwatch> projects;
    private final Table<MavenProject, MojoExecution, Stopwatch> timers;
    private final ConcurrentMap<Artifact, Stopwatch> downloadTimers;
    private final boolean isActive;
    private final boolean isSortingActive;
    private MavenProject topProject;
    private List<String> goals = new ArrayList<String>();
    private Properties properties = new Properties();
    private enum ReportFormat { HTML, JSON };

    public ProfilerEventSpy() {
        this(
                new ConcurrentHashMap<MavenProject, Stopwatch>(),
                HashBasedTable.<MavenProject, MojoExecution, Stopwatch>create(),
                new ConcurrentHashMap<Artifact, Stopwatch>()
        );
    }

    private ProfilerEventSpy(Map<MavenProject, Stopwatch> projects,
                             Table<MavenProject, MojoExecution, Stopwatch> timers,
                             ConcurrentMap<Artifact, Stopwatch> downloadTimers) {
        this.projects = projects;
        this.timers = timers;
        this.downloadTimers = downloadTimers;
        this.isSortingActive = isSortingActive();
        this.isActive = isActive();

        if (!isSortingActive) {
            sequenceEvents = Collections.synchronizedList(new LinkedList<SequenceEvent>());
            sequenceDownloads = Collections.synchronizedList(new LinkedList<Artifact>());
        }
    }

    @VisibleForTesting
    ProfilerEventSpy(ConcurrentHashMap<MavenProject, Stopwatch> projects,
                     Table<MavenProject, MojoExecution, Stopwatch> timers,
                     ConcurrentMap<Artifact, Stopwatch> downloadTimers,
                     MavenProject topProject,
                     List<SequenceEvent> sequenceEvents,
                     List<Artifact> sequenceDownloads) {
        this(projects, timers, downloadTimers);
        this.topProject = topProject;
        this.logger = new ConsoleLogger();
        this.sequenceEvents = sequenceEvents;
        this.sequenceDownloads = sequenceDownloads;
    }

    @VisibleForTesting
    ProfilerEventSpy(ConcurrentHashMap<MavenProject, Stopwatch> projects,
                     Table<MavenProject, MojoExecution, Stopwatch> timers,
                     ConcurrentMap<Artifact, Stopwatch> downloadTimers,
                     MavenProject topProject) {
        this(projects, timers, downloadTimers);
        this.topProject = topProject;
        this.logger = new ConsoleLogger();
    }

    private boolean isSortingActive() {
        String parameter = System.getProperty(DISABLE_TIME_SORTING);
        return parameter == null || "false".equalsIgnoreCase(parameter);
    }

    private boolean isActive() {
        String parameter = System.getProperty(PROFILE);
        return parameter != null && !"false".equalsIgnoreCase(parameter);
    }

    public List<SequenceEvent> getSequenceEvents() {
        return sequenceEvents;
    }

    public List<Artifact> getSequenceDownloads() {
        return sequenceDownloads;
    }

    @Override
    public void onEvent(Object event) throws Exception {
        super.onEvent(event);
        if (isActive) {
            if (event instanceof DefaultMavenExecutionRequest) {
                goals = ((DefaultMavenExecutionRequest) event).getGoals();
                properties = ((DefaultMavenExecutionRequest) event).getUserProperties();
            }
            else if (event instanceof ExecutionEvent) {
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
                .setName(topProject.getName())
                .setGoals(Joiner.on(' ').join(goals))
                .setParameters(properties);
        setDownloads(context);

        ReportFormat format = ReportFormat.HTML;
        String formatProperty = System.getProperty(PROFILE_FORMAT);
        if (formatProperty != null && "json".equalsIgnoreCase(formatProperty)) {
            format = ReportFormat.JSON;
        }

        String reportString = null;
        String reportExtension = null;

        switch (format) {
            case HTML:
                try {
                    Template template = new Handlebars().compile("report-template");
                    reportString = template.apply(context);
                    reportExtension = "html";
                } catch (IOException e) {
                    logger.error("Cannot render profiler report.", e);
                    return;
                }
                break;
            case JSON:
                reportString = getJSONRepresentation(context);
                reportExtension = "json";
                break;
        }

        FileWriter writer = null;
        try {
            String nowString = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(now);
            writer = new FileWriter(getOutputDestination(nowString, reportExtension));
            writer.write(reportString);
        } catch (IOException e) {
            logger.error("Cannot write profiler report.", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // Ignore errors
                }
            }
        }
    }

    private List<Project> allProjects() {
        if (isSortingActive) {
            return getProjectListSortedByTime();
        } else {
            return getProjectListSortedByExecution();
        }
    }

    private List<Project> getProjectListSortedByExecution() {
        List<Project> projects = new ArrayList<Project>();

        MavenProject currentMavenProject = null;
        Project currentProject = null;
        for (SequenceEvent sequenceEvent : sequenceEvents) {
            if (sequenceEvent.getProject() != currentMavenProject) {
                currentMavenProject = sequenceEvent.getProject();
                currentProject = new Project(currentMavenProject.getName(), this.projects.get(currentMavenProject));
                projects.add(currentProject);
            }
            Stopwatch stopwatch = timers.get(currentMavenProject, sequenceEvent.getMojo());
            currentProject.addMojoTime(new EntryAndTime<MojoExecution>(sequenceEvent.getMojo(), stopwatch));
        }
        return projects;
    }

    private List<Project> getProjectListSortedByTime() {
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

        java.util.Collection<Artifact> artifacts = null;
        if (isSortingActive) {
            artifacts = ProjectsSorter.byExecutionTime(downloadTimers);
        } else {
            artifacts = sequenceDownloads;
        }

        for (Artifact artifact : artifacts) {
            result.add(new EntryAndTime<Artifact>(artifact, downloadTimers.get(artifact)));
        }

        data.setDownloads(result).setTotalDownloadTime(descriptor.getTotalTimeSpentDownloadingArtifacts());
    }

    private File getOutputDestination(String now, String extension) {
        File directory = new File(topProject.getBasedir(), ".profiler");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new RuntimeException("Cannot create file to write profiler report: " + directory);
        }
        return new File(directory, "profiler-report-" + now + "." + extension);
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
            ArtifactProfiled artifactProfiled = ArtifactProfiled.of(event.getArtifact());
            downloadTimers.get(artifactProfiled).stop();

            if (!isSortingActive) {
                sequenceDownloads.add(artifactProfiled);
            }
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

        if (!isSortingActive) {
            SequenceEvent event = new SequenceEvent(currentProject, currentEvent.getMojoExecution());
            if (!sequenceEvents.isEmpty()) {
                SequenceEvent previousEvent = sequenceEvents.get(sequenceEvents.size() - 1);
                if (!previousEvent.equals(event)) {
                    sequenceEvents.add(event);
                }
            } else {
                sequenceEvents.add(event);
            }
        }
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

    List<String> getGoals() {
        return goals;
    }

    Properties getProperties() {
        return properties;
    }

    private String getJSONRepresentation(Data context) {
        JsonObject obj = new JsonObject();
        obj.add("name", context.getName());
        obj.add("goals", context.getGoals());
        obj.add("date", context.getDate());
        obj.add("parameters", context.getParameters().toString());

        JsonArray projectsArr = new JsonArray();
        for (Project project : context.getProjects()) {
            JsonObject projectObj = new JsonObject();
            projectObj.add("project", project.getName());
            projectObj.add("time", project.getMillisTimeStamp());
            JsonArray projectMojosArr = new JsonArray();
            for (EntryAndTime entry : project.getMojosWithTime()) {
                JsonObject projectMojoObj = new JsonObject();
                projectMojoObj.add("mojo", entry.getEntry().toString());
                projectMojoObj.add("time", entry.getMillisTimeStamp());
                projectMojosArr.add(projectMojoObj);
            }
            projectObj.add("mojos", projectMojosArr);
            projectsArr.add(projectObj);
        }
        obj.add("projects", projectsArr);

        if (context.isDownloadSectionDisplayed()) {
            JsonArray downloadsArr = new JsonArray();
            for (EntryAndTime download : context.getDownloads()) {
                JsonObject downloadObj = new JsonObject();
                downloadObj.add("download", download.getEntry().toString());
                downloadObj.add("time", download.getMillisTimeStamp());
                downloadsArr.add(downloadObj);
            }
            obj.add("downloads", downloadsArr);
        }

        return obj.toString();
    }
}
