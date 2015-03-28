package fr.jcgay.maven.profiler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Ordering;
import org.eclipse.aether.artifact.Artifact;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static fr.jcgay.maven.profiler.KnownElapsedTimeTicker.aStopWatchWithElapsedTime;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.filter;

public class ArtifactDescriptor {

    @VisibleForTesting final int maxLength;
    private final Stopwatch totalStopwatch;

    private ArtifactDescriptor(int maxLength, long totalTime) {
        this.maxLength = maxLength;
        this.totalStopwatch = aStopWatchWithElapsedTime(totalTime);
    }

    public static ArtifactDescriptor instance(Map<Artifact, Stopwatch> times) {
        if (times == null || times.isEmpty()) {
            return new ArtifactDescriptor(0, 0);
        }

        return new ArtifactDescriptor(maxToStringSize(times), totalTime(times));
    }

    private static long totalTime(Map<Artifact, Stopwatch> times) {
        long totalTime = 0;
        for (Stopwatch stopwatch : filter(times.values(), notNull())) {
            totalTime += stopwatch.elapsedTime(TimeUnit.NANOSECONDS);
        }
        return totalTime;
    }

    private static Integer maxToStringSize(Map<Artifact, Stopwatch> times) {
        return ArtifactFunction.toLength().apply(
                Ordering.natural()
                        .onResultOf(ArtifactFunction.toLength())
                        .max(times.keySet()));
    }

    public Stopwatch getTotalTimeSpentDownloadingArtifacts() {
        return totalStopwatch;
    }

    private static class ArtifactFunction implements Function<Artifact, Integer> {

        @Override
        public Integer apply(Artifact input) {
            return input.toString().length();
        }

        public static ArtifactFunction toLength() {
            return new ArtifactFunction();
        }
    }
}
