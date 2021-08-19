package fr.jcgay.maven.profiler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import fr.jcgay.maven.profiler.reporting.ReportDirectory;
import fr.jcgay.maven.profiler.reporting.template.Data;
import fr.jcgay.maven.profiler.reporting.template.EntryAndTime;
import fr.jcgay.maven.profiler.reporting.template.Project;
import fr.jcgay.maven.profiler.sorting.Sorter;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import static fr.jcgay.maven.profiler.KnownElapsedTimeTicker.aStopWatchWithElapsedTime;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.maven.execution.ExecutionEvent.Type.ProjectDiscoveryStarted;
import static org.apache.maven.execution.ExecutionEvent.Type.SessionStarted;
import static org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_DOWNLOADED;
import static org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_DOWNLOADING;

@Component(role = EventSpy.class, hint = "profiler", description = "Measure times taken by Maven.")
public class ProfilerEventSpy extends AbstractEventSpy {

    private final Supplier<Statistics> statisticsSupplier;
    private final Supplier<Configuration> configurationSupplier;
    private final Supplier<Date> now;

    private Configuration configuration;
    private Statistics statistics;

    @Requirement
    private Logger logger;

    public ProfilerEventSpy() {
        this.statisticsSupplier = Statistics::new;
        this.configurationSupplier = Configuration::read;
        this.now = Date::new;
    }

    @VisibleForTesting
    ProfilerEventSpy(Supplier<Statistics> statisticsSupplier, Supplier<Configuration> configurationSupplier, Supplier<Date> now) {
        this.statisticsSupplier = statisticsSupplier;
        this.configurationSupplier = configurationSupplier;
        this.logger = new ConsoleLogger();
        this.now = now;
    }

    @Override
    public void init(Context context) throws Exception {
        super.init(context);

        this.configuration = configurationSupplier.get();
        this.statistics = statisticsSupplier.get();

        if (configuration.isProfiling()) {
            logger.info("Profiling mvn execution...");
        }
    }

    @Override
    public void onEvent(Object event) throws Exception {
        super.onEvent(event);
        if (configuration.isProfiling()) {
            if (event instanceof DefaultMavenExecutionRequest) {
                DefaultMavenExecutionRequest mavenEvent = (DefaultMavenExecutionRequest) event;
                statistics.setGoals(new LinkedHashSet<>(mavenEvent.getGoals()));
                statistics.setProperties(mavenEvent.getUserProperties());
            } else if (event instanceof ExecutionEvent) {
                storeExecutionEvent((ExecutionEvent) event);
                trySaveTopProject((ExecutionEvent) event);
                storeStartTime((ExecutionEvent) event);
            } else if (event instanceof RepositoryEvent) {
                storeDownloadingArtifacts((RepositoryEvent) event);
            }
        }
    }

    private void storeStartTime(ExecutionEvent event) {
        if (event.getType() == ProjectDiscoveryStarted) {
            statistics.setStartTime(event.getSession().getStartTime());
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (configuration.isProfiling()) {
            Date finishTime = now.get();
            Data context = new Data()
                .setProjects(sortedProjects())
                .setDate(finishTime)
                .setTopProjectName(statistics.topProject().getName())
                .setProfileName(configuration.profileName())
                .setGoals(Joiner.on(' ').join(statistics.goals()))
                .setParameters(statistics.properties());
            setDownloads(context);

            if (statistics.getStartTime() != null) {
                context.setBuildTime(aStopWatchWithElapsedTime(MILLISECONDS.toNanos(finishTime.getTime() - statistics.getStartTime().getTime())));
            }

            configuration.reporter().write(context, new ReportDirectory(statistics.topProject()));
        }
    }

    private void trySaveTopProject(ExecutionEvent event) {
        if (event.getType() == SessionStarted) {
            statistics.setTopProject(event.getSession().getTopLevelProject());
        }
    }

    private List<Project> sortedProjects() {
        Sorter sorter = configuration.sorter();

        List<Project> result = new ArrayList<>();
        Map<MavenProject, Stopwatch> allProjectsWithTimer = statistics.projects();
        for (MavenProject project : sorter.projects(allProjectsWithTimer)) {
            Project currentProject = new Project(project.getName(), allProjectsWithTimer.get(project));
            for (Entry<MojoExecution, Stopwatch> mojo : sorter.mojoExecutionsOf(project, statistics.executions())) {
                currentProject.addMojoTime(new EntryAndTime<>(mojo.getKey(), mojo.getValue()));
            }
            result.add(currentProject);
        }
        return result;
    }

    private void setDownloads(Data data) {
        Map<Artifact, Stopwatch> downloadedArtifacts = statistics.downloads();

        List<EntryAndTime<Artifact>> result = new ArrayList<>();
        for (Artifact artifact : configuration.sorter().downloads(downloadedArtifacts)) {
            result.add(new EntryAndTime<>(artifact, downloadedArtifacts.get(artifact)));
        }

        data.setDownloads(result)
            .setTotalDownloadTime(ArtifactDescriptor.instance(downloadedArtifacts).getTotalTimeSpentDownloadingArtifacts());
    }

    private void storeDownloadingArtifacts(RepositoryEvent event) {
        logger.debug(String.format("Received event (%s): %s", event.getClass(), event));
        if (event.getType() == ARTIFACT_DOWNLOADING) {
            statistics.startDownload(event.getArtifact());

        } else if (event.getType() == ARTIFACT_DOWNLOADED) {
            if (hasNoException(event)) {
                statistics.stopDownload(event.getArtifact());
            }
        }
    }

    private static boolean hasNoException(RepositoryEvent event) {
        List<Exception> exceptions = event.getExceptions();
        return exceptions == null || exceptions.isEmpty();
    }

    private void storeExecutionEvent(ExecutionEvent event) {
        logger.debug(String.format("Received event (%s): %s", event.getClass(), event));

        MavenProject currentProject = event.getSession().getCurrentProject();
        switch (event.getType()) {
            case ProjectStarted:
                statistics.startProject(currentProject);
                break;
            case ProjectSucceeded:
            case ProjectFailed:
                statistics.stopProject(currentProject);
                break;
            case MojoStarted:
                statistics.startExecution(currentProject, event.getMojoExecution());
                break;
            case MojoSucceeded:
            case MojoFailed:
                statistics.stopExecution(currentProject, event.getMojoExecution());
                break;
        }
    }
}
