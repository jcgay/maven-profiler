package com.github.jcgay.maven.profiler;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.mockito.Mockito;
import org.sonatype.aether.RepositoryEvent;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.DefaultRequestTrace;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.listener.DefaultRepositoryEvent;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static com.github.jcgay.maven.profiler.ProfilerEventSpy.PROFILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ProfilerEventSpyTest {

    private ProfilerEventSpy profiler;
    private Table<MavenProject, MojoExecution, Stopwatch> timers;
    private ConcurrentMap<Artifact, Stopwatch> downloadTimers;
    private ConcurrentHashMap<MavenProject, Stopwatch> projects;
    private Logger logger;

    @BeforeMethod
    public void setUp() throws Exception {
        timers = HashBasedTable.create();
        downloadTimers = new ConcurrentHashMap<Artifact, Stopwatch>();

        projects = new ConcurrentHashMap<MavenProject, Stopwatch>();
        logger = new ConsoleLogger();

        System.setProperty("profile", "true");
        profiler = new ProfilerEventSpy(
                logger,
                projects,
                timers,
                downloadTimers
        );
    }

    @Test
    public void should_start_a_timer_when_a_mojo_start() throws Exception {

        ExecutionEvent event = aMojoEvent(ExecutionEvent.Type.MojoStarted, aMavenProject("a-project"));

        profiler.onEvent(event);

        assertThat(timers)
                .containsRows(event.getSession().getCurrentProject())
                .containsColumns(event.getMojoExecution());
        assertThat(timers.row(event.getSession().getCurrentProject()).get(event.getMojoExecution()).isRunning())
                .isTrue();
    }

    @Test(dataProvider = "mojo_succeed_and_fail")
    public void should_stop_the_timer_when_a_mojo_succeed_or_fail(ExecutionEvent.Type type) throws Exception {

        ExecutionEvent event = aMojoEvent(type, aMavenProject("a-project"));
        given_event_has_start(event);

        profiler.onEvent(event);

        assertThat(timers)
                .containsRows(event.getSession().getCurrentProject())
                .containsColumns(event.getMojoExecution());

        assertThat(timers.row(event.getSession().getCurrentProject()).get(event.getMojoExecution()).isRunning())
                .isFalse();
        assertThat(timers.row(event.getSession().getCurrentProject()).get(event.getMojoExecution()).elapsedMillis())
                .isPositive();
    }

    @DataProvider
    private Object[][] mojo_succeed_and_fail() {
        return new Object[][] {
                {ExecutionEvent.Type.MojoSucceeded},
                {ExecutionEvent.Type.MojoFailed}
        };
    }

    @Test
    public void should_start_timer_when_project_start() throws Exception {

        ExecutionEvent event = aProjectEvent(ExecutionEvent.Type.ProjectStarted);

        profiler.onEvent(event);

        assertThat(projects.get(event.getSession().getCurrentProject()).isRunning()).isTrue();
    }

    @Test(dataProvider = "project_succeed_and_fail")
    public void should_stop_timer_when_project_fail_or_succeed(ExecutionEvent.Type type) throws Exception {

        given_project_has_start();
        ExecutionEvent event = aProjectEvent(type);

        profiler.onEvent(event);

        assertThat(projects.get(event.getSession().getCurrentProject()).isRunning()).isFalse();
        assertThat(projects.get(event.getSession().getCurrentProject()).elapsedMillis()).isPositive();
    }

    @DataProvider
    private Object[][] project_succeed_and_fail() {
        return new Object[][] {
                {ExecutionEvent.Type.ProjectSucceeded},
                {ExecutionEvent.Type.ProjectFailed}
        };
    }

    @Test
    public void should_not_log_time_when_property_profile_is_not_set() throws Exception {

        // Given
        System.setProperty(PROFILE, "false");

        ExecutionEvent startEvent = aMojoEvent(ExecutionEvent.Type.MojoSucceeded, aMavenProject("a-project"));
        ExecutionEvent endEvent = aMojoEvent(ExecutionEvent.Type.MojoSucceeded, aMavenProject("a-project"));
        DefaultRepositoryEvent startDownloadEvent = aRepositoryEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADING, anArtifact());
        DefaultRepositoryEvent endDownloadEvent = aRepositoryEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADED, anArtifact());
        ProfilerEventSpy spy = new ProfilerEventSpy(logger, projects, timers, downloadTimers);

        // When
        spy.onEvent(startEvent);
        spy.onEvent(endEvent);
        spy.onEvent(startDownloadEvent);
        spy.onEvent(endDownloadEvent);

        // Then
        assertThat(projects).isEmpty();
        assertThat(timers).isEmpty();
        assertThat(downloadTimers).isEmpty();
    }

    @Test
    public void should_start_timer_when_artifact_downloading_start() throws Exception {

        Artifact artifact = anArtifact();
        DefaultRepositoryEvent event = aRepositoryEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADING, artifact);

        profiler.onEvent(event);

        assertThat(downloadTimers).containsKey(artifact);
        assertThat(downloadTimers.get(event.getArtifact()).isRunning()).isTrue();
    }

    @Test
    public void should_stop_timer_when_artifact_downloading_finish() throws Exception {

        Artifact artifact = anArtifact();
        given_artifact_is_being_downloaded(artifact);
        DefaultRepositoryEvent event = aRepositoryEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADED, artifact);

        profiler.onEvent(event);

        assertThat(downloadTimers.get(event.getArtifact()).isRunning()).isFalse();
        assertThat(downloadTimers.get(event.getArtifact()).elapsedMillis()).isPositive();
    }

    @Test
    public void should_not_log_download_time_if_nothing_has_been_downloaded() throws Exception {

        // Given
        System.setProperty(PROFILE, "true");
        Logger mockLogger = mock(Logger.class);

        ProfilerEventSpy spy = new ProfilerEventSpy(mockLogger, projects, timers, downloadTimers);

        // When
        spy.close();

        // Then
        verify(mockLogger).info("No new artifact downloaded...");
    }

    private static Artifact anArtifact() {
        return ArtifactProfiled.of(new DefaultArtifact("groupId", "artifactId", "jar", "1.0"));
    }

    private void given_artifact_is_being_downloaded(Artifact artifact) throws Exception {
        profiler.onEvent(aRepositoryEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADING, artifact));
        TimeUnit.MILLISECONDS.sleep(1);
    }

    private static DefaultRepositoryEvent aRepositoryEvent(RepositoryEvent.EventType type, Artifact artifact) {
        DefaultRepositoryEvent event = new DefaultRepositoryEvent(type, new DefaultRepositorySystemSession(), new DefaultRequestTrace(null));
        event.setArtifact(artifact);
        return event;
    }

    private static MavenProject aMavenProject(String name) {
        Model model = new Model();
        model.setName(name);
        MavenProject project = new MavenProject(model);
        project.setGroupId("groupId");
        project.setArtifactId("artifactId");
        project.setVersion("1.0");
        return project;
    }

    private void given_project_has_start() throws Exception {
        profiler.onEvent(aProjectEvent(ExecutionEvent.Type.ProjectStarted));
        TimeUnit.MILLISECONDS.sleep(1);
    }

    private void given_event_has_start(ExecutionEvent event) throws Exception {
        profiler.onEvent(aMojoEvent(ExecutionEvent.Type.MojoStarted, event.getMojoExecution(), event.getSession().getCurrentProject()));
        TimeUnit.MILLISECONDS.sleep(1);
    }

    private static ExecutionEvent aMojoEvent(ExecutionEvent.Type type, MavenProject mavenProject) {
        return aMojoEvent(type, new MojoExecution(new Plugin(), "goal", "execution.id"), mavenProject);
    }

    private static ExecutionEvent aMojoEvent(ExecutionEvent.Type type, MojoExecution mojoExecution, MavenProject mavenProject) {
        return new TestExecutionEvent(type, mojoExecution, mavenProject);
    }

    private static ExecutionEvent aProjectEvent(ExecutionEvent.Type type) {
        return aMojoEvent(type, new MojoExecution(new Plugin(), "goal", "execution.id"), aMavenProject("project"));
    }
}

