package fr.jcgay.maven.profiler
import com.google.common.base.Stopwatch
import com.google.common.base.Strings
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.testng.annotations.Test

import static fr.jcgay.maven.profiler.KnownElapsedTimeTicker.aStopWatchWithElapsedTime
import static java.util.concurrent.TimeUnit.SECONDS
import static org.assertj.core.api.Assertions.assertThat

class ArtifactDescriptorTest {

    @Test
    void 'should get max length'() throws Exception {
        Map<Artifact, Stopwatch> timers = [:]
        timers[artifactWithSize(2)] = null
        timers[artifactWithSize(5)] = null
        timers[artifactWithSize(10)] = null

        ArtifactDescriptor result = ArtifactDescriptor.instance(timers)

        assertThat(result.maxLength).isEqualTo(10)
    }

    @Test
    void 'should not fail when input map is null or empty'() throws Exception {
        assertThat(ArtifactDescriptor.instance([:]).maxLength).isZero()
        assertThat(ArtifactDescriptor.instance(null).maxLength).isZero()
    }

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

    private static Artifact artifactWithSize(final int size) {
        new Artifact() {
            @Override
            String getGroupId() {
                null
            }

            @Override
            String getArtifactId() {
                null
            }

            @Override
            String getVersion() {
                null
            }

            @Override
            Artifact setVersion(String version) {
                null
            }

            @Override
            String getBaseVersion() {
                null
            }

            @Override
            boolean isSnapshot() {
                false
            }

            @Override
            String getClassifier() {
                null
            }

            @Override
            String getExtension() {
                null
            }

            @Override
            File getFile() {
                null
            }

            @Override
            Artifact setFile(File file) {
                null
            }

            @Override
            String getProperty(String key, String defaultValue) {
                null
            }

            @Override
            Map<String, String> getProperties() {
                null
            }

            @Override
            Artifact setProperties(Map<String, String> properties) {
                null
            }

            @Override
            String toString() {
                Strings.repeat("a", size)
            }
        }
    }
}
