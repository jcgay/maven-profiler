package com.github.jcgay.maven.profiler
import com.google.common.base.Stopwatch
import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import org.apache.maven.execution.DefaultMavenExecutionRequest
import org.apache.maven.execution.ExecutionEvent
import org.apache.maven.model.Model
import org.apache.maven.model.Plugin
import org.apache.maven.plugin.MojoExecution
import org.apache.maven.project.MavenProject
import org.assertj.guava.api.Assertions
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositoryEvent
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.transfer.ArtifactNotFoundException
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

import static com.github.jcgay.maven.profiler.ProfilerEventSpy.PROFILE
import static java.util.Arrays.asList
import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.apache.maven.execution.ExecutionEvent.Type.*
import static org.assertj.core.api.Assertions.assertThat
import static org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_DOWNLOADED
import static org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_DOWNLOADING

class ProfilerEventSpyTest {

    private ProfilerEventSpy profiler
    private Table<MavenProject, MojoExecution, Stopwatch> timers
    private ConcurrentMap<Artifact, Stopwatch> downloadTimers
    private ConcurrentHashMap<MavenProject, Stopwatch> projects
    private MavenProject topProject

    @BeforeMethod
    void setUp() throws Exception {
        timers = HashBasedTable.create()
        downloadTimers = new ConcurrentHashMap<Artifact, Stopwatch>()

        projects = new ConcurrentHashMap<MavenProject, Stopwatch>()

        topProject = aMavenProject('top-project')
        topProject.file = File.createTempFile('pom', '.xml')

        System.setProperty('profile', 'true')
        System.clearProperty('profileFormat')

        profiler = new ProfilerEventSpy(
                projects,
                timers,
                downloadTimers,
                topProject
        )
    }

    @Test
    void 'should start a timer when a mojo start'() throws Exception {
        ExecutionEvent event = aMojoEvent(MojoStarted, aMavenProject('a-project'))

        profiler.onEvent(event)

        Assertions.assertThat(timers)
                .containsRows(event.getSession().getCurrentProject())
                .containsColumns(event.getMojoExecution())
        assertThat(timers.row(event.getSession().getCurrentProject()).get(event.getMojoExecution()).isRunning())
                .isTrue()
    }

    @Test(dataProvider = 'mojo succeed and fail')
    void 'should stop the timer when a mojo succeed or fail'(ExecutionEvent.Type type) throws Exception {
        ExecutionEvent event = aMojoEvent(type, aMavenProject('a-project'))
        given_event_has_start(event)

        profiler.onEvent(event)

        Assertions.assertThat(timers)
                .containsRows(event.getSession().getCurrentProject())
                .containsColumns(event.getMojoExecution())

        assertThat(timers.row(event.getSession().getCurrentProject()).get(event.getMojoExecution()).isRunning())
                .isFalse()
        assertThat(timers.row(event.getSession().getCurrentProject()).get(event.getMojoExecution()).elapsedMillis())
                .isPositive()
    }

    @DataProvider
    private Object[][] 'mojo succeed and fail'() {
        [[MojoSucceeded], [MojoFailed]]
    }

    @Test
    void 'should start timer when project start'() throws Exception {
        ExecutionEvent event = aProjectEvent(ProjectStarted)

        profiler.onEvent(event)

        assertThat(projects.get(event.getSession().getCurrentProject()).isRunning()).isTrue()
    }

    @Test(dataProvider = 'project succeed and fail')
    void 'should stop timer when project fail or succeed'(ExecutionEvent.Type type) throws Exception {
        given_project_has_start()
        ExecutionEvent event = aProjectEvent(type)

        profiler.onEvent(event)

        def project = projects.get(event.getSession().getCurrentProject())
        assertThat(project.isRunning()).isFalse()
        assertThat(project.elapsedMillis()).isPositive()
    }

    @DataProvider
    private Object[][] 'project succeed and fail'() {
        [[ProjectSucceeded], [ProjectFailed]]
    }

    @Test
    void 'should not log time when property profile is not set'() throws Exception {
        // Given
        System.setProperty(PROFILE, "false")

        ExecutionEvent startEvent = aMojoEvent(MojoSucceeded, aMavenProject('a-project'))
        ExecutionEvent endEvent = aMojoEvent(MojoSucceeded, aMavenProject('a-project'))
        RepositoryEvent startDownloadEvent = aRepositoryEvent(ARTIFACT_DOWNLOADING, anArtifact()).build()
        RepositoryEvent endDownloadEvent = aRepositoryEvent(ARTIFACT_DOWNLOADED, anArtifact()).build()
        ProfilerEventSpy spy = new ProfilerEventSpy(projects, timers, downloadTimers, topProject)

        // When
        spy.onEvent(startEvent)
        spy.onEvent(endEvent)
        spy.onEvent(startDownloadEvent)
        spy.onEvent(endDownloadEvent)

        // Then
        assertThat(projects).isEmpty()
        Assertions.assertThat(timers).isEmpty()
        assertThat(downloadTimers).isEmpty()
    }

    @Test
    void 'should start timer when artifact downloading start'() throws Exception {
        Artifact artifact = anArtifact()
        RepositoryEvent event = aRepositoryEvent(ARTIFACT_DOWNLOADING, artifact).build()

        profiler.onEvent(event)

        assertThat(downloadTimers).containsKey(artifact)
        assertThat(downloadTimers.get(event.getArtifact()).isRunning()).isTrue()
    }

    @Test
    void 'should stop timer when artifact downloading finish'() throws Exception {
        Artifact artifact = anArtifact()
        given_artifact_is_being_downloaded(artifact)
        RepositoryEvent event = aRepositoryEvent(ARTIFACT_DOWNLOADED, artifact).build()

        profiler.onEvent(event)

        assertThat(downloadTimers.get(event.getArtifact()).isRunning()).isFalse()
        assertThat(downloadTimers.get(event.getArtifact()).elapsedMillis()).isPositive()
    }

    @Test
    void 'should not stop timer if artifact has not been downloaded'() throws Exception {
        Artifact artifact = anArtifact()
        given_artifact_is_being_downloaded(artifact)
        RepositoryEvent event = artifact_downloaded_but_not_found(artifact).build()

        profiler.onEvent(event)

        assertThat(downloadTimers.get(event.getArtifact()).isRunning()).isTrue()
    }

    @Test
    void 'should write html report'() throws Exception {
        MavenProject project = aMavenProject('a-project')
        given_project_has_start()
        given_event_has_start(aMojoEvent(MojoStarted, project))
        profiler.onEvent(aMojoEvent(MojoStarted, project))
        profiler.onEvent(aRepositoryEvent(ARTIFACT_DOWNLOADING, anArtifact()))

        profiler.close()

        File destination = new File(topProject.getFile().getParentFile(), '.profiler')
        assertThat(destination).exists().isDirectory()
        assertThat(destination.list()).haveAtLeast(1, { it.startsWith('profiler-report-') && it.endsWith('.html') })
    }

    @Test
    void 'should write json report'() throws Exception {
        System.setProperty('profileFormat', 'json')
        MavenProject project = aMavenProject('a-project')
        given_project_has_start()
        given_event_has_start(aMojoEvent(MojoStarted, project))
        profiler.onEvent(aMojoEvent(MojoStarted, project))
        profiler.onEvent(aRepositoryEvent(ARTIFACT_DOWNLOADING, anArtifact()))

        profiler.close()

        File destination = new File(topProject.getFile().getParentFile(), '.profiler')
        assertThat(destination).exists().isDirectory()
        assertThat(destination.list()).haveAtLeast(1, { it.startsWith('profiler-report-') && it.endsWith('.json') })
    }

    @Test
    void 'should get goals'() throws Exception {
        DefaultMavenExecutionRequest event = new DefaultMavenExecutionRequest()
        event.setGoals(asList('clean', 'install'))
        event.setUserProperties(new Properties())

        profiler.onEvent(event)

        assertThat(profiler.getGoals()).containsExactlyElementsOf(event.getGoals())
        assertThat(profiler.getProperties()).isEqualTo(event.getUserProperties())
    }

    private static RepositoryEvent.Builder artifact_downloaded_but_not_found(Artifact artifact) {
        aRepositoryEvent(ARTIFACT_DOWNLOADED, artifact)
                .setException(new ArtifactNotFoundException(artifact, new RemoteRepository.Builder('', '', '').build()))
    }

    private static Artifact anArtifact() {
        ArtifactProfiled.of(new DefaultArtifact('groupId', 'artifactId', 'jar', '1.0'))
    }

    private void given_artifact_is_being_downloaded(Artifact artifact) throws Exception {
        profiler.onEvent(aRepositoryEvent(ARTIFACT_DOWNLOADING, artifact).build())
        MILLISECONDS.sleep(1)
    }

    private static RepositoryEvent.Builder aRepositoryEvent(RepositoryEvent.EventType type, Artifact artifact) {
        new RepositoryEvent.Builder(new DefaultRepositorySystemSession(), type)
                .setArtifact(artifact)
    }

    private static MavenProject aMavenProject(String name) {
        Model model = new Model()
        model.name = name
        MavenProject project = new MavenProject(model)
        project.groupId = 'groupId'
        project.artifactId = 'artifactId'
        project.version = '1.0'
        project
    }

    private void given_project_has_start() throws Exception {
        profiler.onEvent(aProjectEvent(ProjectStarted))
        MILLISECONDS.sleep(1)
    }

    private void given_event_has_start(ExecutionEvent event) throws Exception {
        profiler.onEvent(aMojoEvent(MojoStarted, event.getMojoExecution(), event.getSession().getCurrentProject()))
        MILLISECONDS.sleep(1)
    }

    private static ExecutionEvent aMojoEvent(ExecutionEvent.Type type, MavenProject mavenProject) {
        aMojoEvent(type, new MojoExecution(new Plugin(), 'goal', 'execution.id'), mavenProject)
    }

    private static ExecutionEvent aMojoEvent(ExecutionEvent.Type type, MojoExecution mojoExecution, MavenProject mavenProject) {
        new TestExecutionEvent(type, mojoExecution, mavenProject)
    }

    private static ExecutionEvent aProjectEvent(ExecutionEvent.Type type) {
        aMojoEvent(type, new MojoExecution(new Plugin(), 'goal', 'execution.id'), aMavenProject('project'))
    }
}

