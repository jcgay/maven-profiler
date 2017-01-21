package fr.jcgay.maven.profiler.sorting.time;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Map;

class ExecutionTimeSorter {

    private final Table<MavenProject, MojoExecution, Stopwatch> timers;

    private ExecutionTimeSorter(Table<MavenProject, MojoExecution, Stopwatch> timers) {
        this.timers = timers;
    }

    public static ExecutionTimeSorter instance(Table<MavenProject, MojoExecution, Stopwatch> timers) {
        return new ExecutionTimeSorter(timers);
    }

    public List<Map.Entry<MojoExecution, Stopwatch>> getSortedMojosByTime(MavenProject project) {
        return Ordering.natural()
            .onResultOf(StopWatchFunction.toElapsedTime())
            .reverse()
            .sortedCopy(timers.row(project).entrySet());
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
