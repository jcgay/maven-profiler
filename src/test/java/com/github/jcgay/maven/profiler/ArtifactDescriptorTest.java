package com.github.jcgay.maven.profiler;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.jcgay.maven.profiler.KnownElapsedTimeTicker.aStopWatchWithElapsedTime;
import static org.assertj.core.api.Assertions.assertThat;

public class ArtifactDescriptorTest {

    @Test
    public void should_get_max_length() throws Exception {

        HashMap<Artifact, Stopwatch> timers = new HashMap<Artifact, Stopwatch>();
        timers.put(artifactWithSize(2), null);
        timers.put(artifactWithSize(5), null);
        timers.put(artifactWithSize(10), null);

        ArtifactDescriptor result = ArtifactDescriptor.instance(timers);

        assertThat(result.maxLength).isEqualTo(10);
    }

    @Test
    public void should_not_fail_when_input_map_is_null_or_empty() throws Exception {

        assertThat(ArtifactDescriptor.instance(Collections.<Artifact, Stopwatch>emptyMap()).maxLength).isZero();
        assertThat(ArtifactDescriptor.instance(null).maxLength).isZero();
    }

    @Test
    public void should_get_total_time_spent_downloading_thing() throws Exception {

        HashMap<Artifact, Stopwatch> timers = new HashMap<Artifact, Stopwatch>();
        timers.put(anArtifact("1"), aStopWatchWithElapsedTime(TimeUnit.SECONDS.toNanos(1L)));
        timers.put(anArtifact("2"), aStopWatchWithElapsedTime(TimeUnit.SECONDS.toNanos(2L)));
        timers.put(anArtifact("3"), aStopWatchWithElapsedTime(TimeUnit.SECONDS.toNanos(3L)));

        ArtifactDescriptor result = ArtifactDescriptor.instance(timers);

        assertThat(result.getTotalTimeSpentDownloadingArtifacts().elapsedTime(TimeUnit.SECONDS)).isEqualTo(6L);
    }

    private static Artifact anArtifact(String artifactId) {
        return new DefaultArtifact("groupId", artifactId, "extension", "version");
    }

    private static Artifact artifactWithSize(final int size) {
        return new Artifact() {
            @Override
            public String getGroupId() {
                return null;
            }

            @Override
            public String getArtifactId() {
                return null;
            }

            @Override
            public String getVersion() {
                return null;
            }

            @Override
            public Artifact setVersion(String version) {
                return null;
            }

            @Override
            public String getBaseVersion() {
                return null;
            }

            @Override
            public boolean isSnapshot() {
                return false;
            }

            @Override
            public String getClassifier() {
                return null;
            }

            @Override
            public String getExtension() {
                return null;
            }

            @Override
            public File getFile() {
                return null;
            }

            @Override
            public Artifact setFile(File file) {
                return null;
            }

            @Override
            public String getProperty(String key, String defaultValue) {
                return null;
            }

            @Override
            public Map<String, String> getProperties() {
                return null;
            }

            @Override
            public Artifact setProperties(Map<String, String> properties) {
                return null;
            }

            @Override
            public String toString() {
                return Strings.repeat("a", size);
            }
        };
    }
}
