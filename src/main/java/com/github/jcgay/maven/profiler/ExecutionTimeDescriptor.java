package com.github.jcgay.maven.profiler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ExecutionTimeDescriptor {

    @VisibleForTesting int maxKeyLength;
    private Table<MavenProject, MojoExecution, Stopwatch> timers;

    private ExecutionTimeDescriptor(Table<MavenProject, MojoExecution, Stopwatch> timers) {
        this.timers = timers;
        if (!timers.isEmpty()) {
            this.maxKeyLength = maxToStringLength(timers.columnKeySet());
        }
    }

    public String getFormattedLine(Map.Entry<MojoExecution, Stopwatch> entry) {
        return String.format(String.format("%-" + maxKeyLength + "s %s", entry.getKey(), entry.getValue()));
    }

    public static ExecutionTimeDescriptor instance(Table<MavenProject, MojoExecution, Stopwatch> timers) {
        return new ExecutionTimeDescriptor(timers);
    }

    public List<Map.Entry<MojoExecution, Stopwatch>> getSortedMojosByTime(MavenProject project) {
        return Ordering.natural()
                .onResultOf(StopWatchFunction.toElapsedTime())
                .reverse()
                .sortedCopy(timers.row(project).entrySet());
    }

    private int maxToStringLength(Collection<MojoExecution> mojos) {
        return MojoFunction.toLength().apply(
                Ordering.natural()
                        .onResultOf(MojoFunction.toLength())
                        .max(mojos)
        );
    }

    private static class MojoFunction implements Function<MojoExecution, Integer> {
        @Override
        public Integer apply(MojoExecution input) {
            return input.toString().length();
        }

        public static MojoFunction toLength() {
            return new MojoFunction();
        }
    }

    private static class StopWatchFunction implements Function<Map.Entry<MojoExecution, Stopwatch>, Long> {
        @Override
        public Long apply(Map.Entry<MojoExecution, Stopwatch> input) {
            return input.getValue().elapsedMillis();
        }

        public static StopWatchFunction toElapsedTime() {
            return new StopWatchFunction();
        }
    }
}
