package com.github.jcgay.maven.profiler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Ordering;
import org.sonatype.aether.artifact.Artifact;

import java.util.Map;

public class ArtifactDescriptor {

    @VisibleForTesting final int maxLength;

    private ArtifactDescriptor(int maxLength) {
        this.maxLength = maxLength;
    }

    public static ArtifactDescriptor instance(Map<Artifact, Stopwatch> times) {
        if (times == null || times.isEmpty()) {
            return new ArtifactDescriptor(0);
        }

        return new ArtifactDescriptor(
                ArtifactFunction.toLength().apply(
                        Ordering.natural()
                                .onResultOf(ArtifactFunction.toLength())
                                .max(times.keySet()))
        );
    }

    public String getFormattedLine(Artifact artifact) {
        return String.format("%-" + maxLength + "s ", artifact);
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
