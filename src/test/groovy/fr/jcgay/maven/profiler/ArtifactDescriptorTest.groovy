package fr.jcgay.maven.profiler

import com.google.common.base.Stopwatch
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.testng.annotations.Test

import static fr.jcgay.maven.profiler.KnownElapsedTimeTicker.aStopWatchWithElapsedTime
import static java.util.concurrent.TimeUnit.SECONDS
import static org.assertj.core.api.Assertions.assertThat

class ArtifactDescriptorTest {

    @Test
    void 'should get total time spent downloading thing'() throws Exception {
        Map<Artifact, Stopwatch> timers = [:]
        timers[anArtifact("1")] = aStopWatchWithElapsedTime(SECONDS.toNanos(1L))
        timers[anArtifact("2")] = aStopWatchWithElapsedTime(SECONDS.toNanos(2L))
        timers[anArtifact("3")] = aStopWatchWithElapsedTime(SECONDS.toNanos(3L))

        ArtifactDescriptor result = ArtifactDescriptor.instance(timers)

        assertThat(result.getTotalTimeSpentDownloadingArtifacts().elapsedTime(SECONDS)).isEqualTo(6L)
    }

    private static Artifact anArtifact(String artifactId) {
        new DefaultArtifact("groupId", artifactId, "extension", "version")
    }

}
