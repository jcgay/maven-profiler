package fr.jcgay.maven.profiler

import com.google.common.base.Stopwatch
import fr.jcgay.maven.profiler.reporting.ReportDirectory
import fr.jcgay.maven.profiler.reporting.Reporter
import fr.jcgay.maven.profiler.reporting.template.Data
import fr.jcgay.maven.profiler.sorting.Sorter
import org.apache.maven.execution.DefaultMavenExecutionRequest
import org.apache.maven.execution.ExecutionEvent
import org.assertj.guava.api.Assertions
import org.eclipse.aether.RepositoryEvent
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.transfer.ArtifactNotFoundException
import org.mockito.ArgumentCaptor
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import static fr.jcgay.maven.profiler.MavenStubs.*
import static java.util.Calendar.JANUARY
import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.apache.maven.execution.ExecutionEvent.Type.*
import static org.assertj.core.api.Assertions.assertThat
import static org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_DOWNLOADED
import static org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_DOWNLOADING
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify

class ProfilerEventSpyTest {

    private ProfilerEventSpy profiler
    private Statistics statistics
    private Reporter reporter
    private Sorter sorter
    private Date finishTime = date(2017, JANUARY, 22)

    @BeforeMethod
    void setUp() throws Exception {
        reporter = mock(Reporter)
        sorter = mock(Sorter)

        def topProject = aMavenTopProject('top-project')

        statistics = new Statistics()
            .setTopProject(topProject)

        def profileName = "P1"
        profiler = new ProfilerEventSpy(() -> statistics, () -> new Configuration(true, profileName, reporter, sorter, true), { finishTime })
        profiler.init(null)
    }

    @Test
    void 'should start a timer when a mojo start'() throws Exception {
        ExecutionEvent event = aMojoEvent(MojoStarted, aMavenProject('a-project'))

        profiler.onEvent(event)

        def result = statistics.executions()
        Assertions.assertThat(result)
            .containsRows(event.getSession().getCurrentProject())
            .containsColumns(event.getMojoExecution())
        assertThat(result.row(event.getSession().getCurrentProject()).get(event.getMojoExecution()).isRunning())
            .isTrue()
    }

    @Test(dataProvider = 'mojo succeed and fail')
    void 'should stop the timer when a mojo succeed or fail'(ExecutionEvent.Type type) throws Exception {
        ExecutionEvent event = aMojoEvent(type, aMavenProject('a-project'))
        given_started_event(event)

        profiler.onEvent(event)

        def result = statistics.executions()
        Assertions.assertThat(result)
            .containsRows(event.getSession().getCurrentProject())
            .containsColumns(event.getMojoExecution())

        Stopwatch timer = result.row(event.getSession().getCurrentProject()).get(event.getMojoExecution())
        assertThat(timer.isRunning()).isFalse()
        assertThat(timer.elapsedMillis()).isPositive()
    }

    @DataProvider
    private Object[][] 'mojo succeed and fail'() {
        [[MojoSucceeded], [MojoFailed]]
    }

    @Test
    void 'should start timer when project start'() throws Exception {
        ExecutionEvent event = aProjectEvent(ProjectStarted)

        profiler.onEvent(event)

        assertThat(statistics.projects()[event.session.currentProject].isRunning()).isTrue()
    }

    @Test(dataProvider = 'project succeed and fail')
    void 'should stop timer when project fail or succeed'(ExecutionEvent.Type type) throws Exception {
        given_started_project()
        ExecutionEvent event = aProjectEvent(type)

        profiler.onEvent(event)

        def project = statistics.projects()[event.session.currentProject]
        assertThat(project.isRunning()).isFalse()
        assertThat(project.elapsedMillis()).isPositive()
    }

    @DataProvider
    private Object[][] 'project succeed and fail'() {
        [[ProjectSucceeded], [ProjectFailed]]
    }

    @Test
    void 'should start timer when artifact downloading start'() throws Exception {
        Artifact artifact = anArtifact()
        RepositoryEvent event = aRepositoryEvent(ARTIFACT_DOWNLOADING, artifact).build()

        profiler.onEvent(event)

        def result = statistics.downloads()
        assertThat(result).containsKey(artifact)
        assertThat(result[event.artifact].isRunning()).isTrue()
    }

    @Test
    void 'should stop timer when artifact downloading finish'() throws Exception {
        Artifact artifact = anArtifact()
        given_artifact_is_downloading(artifact)
        RepositoryEvent event = aRepositoryEvent(ARTIFACT_DOWNLOADED, artifact).build()

        profiler.onEvent(event)

        def result = statistics.downloads()
        assertThat(result[event.artifact].isRunning()).isFalse()
        assertThat(result[event.artifact].elapsedMillis()).isPositive()
    }

    @Test
    void 'should not stop timer if artifact has not been downloaded'() throws Exception {
        Artifact artifact = anArtifact()
        given_artifact_is_downloading(artifact)
        RepositoryEvent event = artifact_downloaded_but_not_found(artifact).build()

        profiler.onEvent(event)

        assertThat(statistics.downloads()[event.artifact].isRunning()).isTrue()
    }

    @Test
    void 'should write report'() throws Exception {
        profiler.close()

        verify(reporter).write(any(Data), any(ReportDirectory))
    }

    @Test
    void 'should get goals'() throws Exception {
        DefaultMavenExecutionRequest event = new DefaultMavenExecutionRequest()
        event.goals = ['clean', 'install']
        event.userProperties = ['profile': true] as Properties

        profiler.onEvent(event)
        profiler.close()

        def data = ArgumentCaptor.forClass(Data)
        verify(reporter).write(data.capture(), any(ReportDirectory))

        def result = data.value
        assertThat(result.goals).isEqualTo('clean install')
        assertThat(result.parameters).isEqualTo(event.getUserProperties())
    }

    @Test
    void 'should report total execution time'() throws Exception {
        ExecutionEvent event = new TestExecutionEvent(ProjectDiscoveryStarted, null, null)
        event.session.request.startTime = date(2017, JANUARY, 21)

        profiler.onEvent(event)
        profiler.close()

        def data = ArgumentCaptor.forClass(Data)
        verify(reporter).write(data.capture(), any(ReportDirectory))

        def result = data.value
        assertThat(result.buildTime.elapsedMillis()).isEqualTo(finishTime.time - date(2017, JANUARY, 21).time)
    }

    private static Date date(int year, int month, int day) {
        def calendar = Calendar.getInstance()
        calendar.set(year, month, day, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.time
    }

    private static RepositoryEvent.Builder artifact_downloaded_but_not_found(Artifact artifact) {
        aRepositoryEvent(ARTIFACT_DOWNLOADED, artifact)
            .setException(new ArtifactNotFoundException(artifact, new RemoteRepository.Builder('', '', '').build()))
    }

    private void given_artifact_is_downloading(Artifact artifact) throws Exception {
        profiler.onEvent(aRepositoryEvent(ARTIFACT_DOWNLOADING, artifact).build())
        MILLISECONDS.sleep(1)
    }

    private void given_started_project() throws Exception {
        profiler.onEvent(aProjectEvent(ProjectStarted))
        MILLISECONDS.sleep(1)
    }

    private void given_started_event(ExecutionEvent event) throws Exception {
        profiler.onEvent(aMojoEvent(MojoStarted, event.getMojoExecution(), event.getSession().getCurrentProject()))
        MILLISECONDS.sleep(1)
    }
}

