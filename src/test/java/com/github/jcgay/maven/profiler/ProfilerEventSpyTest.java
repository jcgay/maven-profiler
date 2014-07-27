package com.github.jcgay.maven.profiler;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Condition;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.github.jcgay.maven.profiler.ProfilerEventSpy.PROFILE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

public class ProfilerEventSpyTest {

    private ProfilerEventSpy profiler;
    private Table<MavenProject, MojoExecution, Stopwatch> timers;
    private ConcurrentMap<Artifact, Stopwatch> downloadTimers;
    private ConcurrentHashMap<MavenProject, Stopwatch> projects;
    private MavenProject topProject;

    @BeforeMethod
    public void setUp() throws Exception {
        timers = HashBasedTable.create();
        downloadTimers = new ConcurrentHashMap<Artifact, Stopwatch>();

        projects = new ConcurrentHashMap<MavenProject, Stopwatch>();

        topProject = aMavenProject("top-project");
        topProject.setFile(File.createTempFile("pom", ".xml"));

        System.setProperty("profile", "true");
        profiler = new ProfilerEventSpy(
                projects,
                timers,
                downloadTimers,
                topProject
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
        RepositoryEvent startDownloadEvent = aRepositoryEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADING, anArtifact()).build();
        RepositoryEvent endDownloadEvent = aRepositoryEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADED, anArtifact()).build();
        ProfilerEventSpy spy = new ProfilerEventSpy(projects, timers, downloadTimers, topProject);

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
        RepositoryEvent event = aRepositoryEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADING, artifact).build();

        profiler.onEvent(event);

        assertThat(downloadTimers).containsKey(artifact);
        assertThat(downloadTimers.get(event.getArtifact()).isRunning()).isTrue();
    }

    @Test
    public void should_stop_timer_when_artifact_downloading_finish() throws Exception {

        Artifact artifact = anArtifact();
        given_artifact_is_being_downloaded(artifact);
        RepositoryEvent event = aRepositoryEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADED, artifact).build();

        profiler.onEvent(event);

        assertThat(downloadTimers.get(event.getArtifact()).isRunning()).isFalse();
        assertThat(downloadTimers.get(event.getArtifact()).elapsedMillis()).isPositive();
    }

    @Test
    public void should_not_stop_timer_if_artifact_has_not_been_downloaded() throws Exception {

        Artifact artifact = anArtifact();
        given_artifact_is_being_downloaded(artifact);
        RepositoryEvent event = artifact_downloaded_but_not_found(artifact).build();

        profiler.onEvent(event);

        assertThat(downloadTimers.get(event.getArtifact()).isRunning()).isTrue();
    }

    @Test
    public void should_write_html_report() throws Exception {

        MavenProject project = aMavenProject("a-project");
        given_project_has_start();
        given_event_has_start(aMojoEvent(ExecutionEvent.Type.MojoStarted, project));
        profiler.onEvent(aMojoEvent(ExecutionEvent.Type.MojoStarted, project));
        profiler.onEvent(aRepositoryEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADING, anArtifact()));

        profiler.close();

        File destination = new File(topProject.getFile().getParentFile(), ".profiler");
        assertThat(destination).exists().isDirectory();
        assertThat(destination.list()).haveAtLeast(1, new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.startsWith("profiler-report-") && value.endsWith(".html");
            }
        });

    }

    private static RepositoryEvent.Builder artifact_downloaded_but_not_found(Artifact artifact) {
        return aRepositoryEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADED, artifact)
                .setException(new ArtifactNotFoundException(artifact, new RemoteRepository.Builder("", "", "").build()));
    }

    private static Artifact anArtifact() {
        return ArtifactProfiled.of(new DefaultArtifact("groupId", "artifactId", "jar", "1.0"));
    }

    private void given_artifact_is_being_downloaded(Artifact artifact) throws Exception {
        profiler.onEvent(aRepositoryEvent(RepositoryEvent.EventType.ARTIFACT_DOWNLOADING, artifact).build());
        MILLISECONDS.sleep(1);
    }

    private static RepositoryEvent.Builder aRepositoryEvent(RepositoryEvent.EventType type, Artifact artifact) {
        return new RepositoryEvent.Builder(new DefaultRepositorySystemSession(), type)
                .setArtifact(artifact);
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
        MILLISECONDS.sleep(1);
    }

    private void given_event_has_start(ExecutionEvent event) throws Exception {
        profiler.onEvent(aMojoEvent(ExecutionEvent.Type.MojoStarted, event.getMojoExecution(), event.getSession().getCurrentProject()));
        MILLISECONDS.sleep(1);
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

