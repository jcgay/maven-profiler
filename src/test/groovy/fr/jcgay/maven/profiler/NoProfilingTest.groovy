package fr.jcgay.maven.profiler

import fr.jcgay.maven.profiler.reporting.Reporter
import fr.jcgay.maven.profiler.sorting.Sorter
import groovy.transform.CompileStatic
import org.apache.maven.execution.DefaultMavenExecutionRequest
import org.apache.maven.execution.ExecutionEvent
import org.assertj.guava.api.Assertions
import org.eclipse.aether.RepositoryEvent
import org.testng.annotations.Test

import static fr.jcgay.maven.profiler.MavenStubs.*
import static org.apache.maven.execution.ExecutionEvent.Type.MojoSucceeded
import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.entry
import static org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_DOWNLOADED
import static org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_DOWNLOADING
import static org.mockito.Mockito.mock

@CompileStatic
class NoProfilingTest {

    @Test
    void 'should not log time when property profile is not set'() {
        // Given
        ExecutionEvent startEvent = aMojoEvent(MojoSucceeded, aMavenProject('a-project'))
        ExecutionEvent endEvent = aMojoEvent(MojoSucceeded, aMavenProject('a-project'))
        RepositoryEvent startDownloadEvent = aRepositoryEvent(ARTIFACT_DOWNLOADING, anArtifact()).build()
        RepositoryEvent endDownloadEvent = aRepositoryEvent(ARTIFACT_DOWNLOADED, anArtifact()).build()

        def statistics = new Statistics()
        ProfilerEventSpy profiler = new ProfilerEventSpy(() -> statistics, () -> new Configuration(false, "", mock(Reporter), mock(Sorter), true), { new Date() })
        profiler.init(null)

        // When
        profiler.onEvent(startEvent)
        profiler.onEvent(endEvent)
        profiler.onEvent(startDownloadEvent)
        profiler.onEvent(endDownloadEvent)

        // Then
        assertThat(statistics.projects()).isEmpty()
        assertThat(statistics.downloads()).isEmpty()
        Assertions.assertThat(statistics.executions()).isEmpty()
    }

    @Test
    void 'should not report parameters when configuration is disable'() {
        def statistics = new Statistics()
        statistics.setTopProject(aMavenTopProject("project-hiding-properties"))
        ProfilerEventSpy profiler = new ProfilerEventSpy(() -> statistics, () -> new Configuration(true, "", mock(Reporter), mock(Sorter), false), { new Date() })
        def event = new DefaultMavenExecutionRequest()
        def properties = new Properties()
        properties.put("should-not-be-reported", "Hide me please!")
        event.setUserProperties(properties)

        profiler.init(null)
        profiler.onEvent(event)
        profiler.close()

        assertThat(statistics.properties()).isEmpty()
    }

    @Test
    void 'should report parameters when configuration is enable'() {
        def statistics = new Statistics()
        statistics.setTopProject(aMavenTopProject("project-reporting-properties"))
        ProfilerEventSpy profiler = new ProfilerEventSpy(() -> statistics, () -> new Configuration(true, "", mock(Reporter), mock(Sorter), true), { new Date() })
        def event = new DefaultMavenExecutionRequest()
        def properties = new Properties()
        properties.put("should-be-reported", "Print me please!")
        event.setUserProperties(properties)

        profiler.init(null)
        profiler.onEvent(event)
        profiler.close()

        assertThat(statistics.properties())
            .containsExactly(entry("should-be-reported", "Print me please!"))
    }
}
