package fr.jcgay.maven.profiler;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import fr.jcgay.maven.profiler.template.Data;
import fr.jcgay.maven.profiler.template.EntryAndTime;
import fr.jcgay.maven.profiler.template.Project;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component(role = EventSpy.class, hint = "profiler", description = "Measure times taken by Maven.")
public class ProfilerEventSpy extends AbstractEventSpy {

    static final String PROFILE = "profile";
    static final String PROFILE_FORMAT = "profileFormat";

    @Requirement
    private Logger logger;

    private final Map<MavenProject, Stopwatch> projects;
    private final Table<MavenProject, MojoExecution, Stopwatch> timers;
    private final ConcurrentMap<Artifact, Stopwatch> downloadTimers;
    private final boolean isActive;
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
        this.isActive = isActive();
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

    private boolean isActive() {
        String parameter = System.getProperty(PROFILE);
        return parameter != null && !"false".equalsIgnoreCase(parameter);
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
            writer = new FileWriter(getOuputDestination(nowString, reportExtension));
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

    private File getOuputDestination(String now, String extension) {
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
