package com.github.jcgay.maven.profiler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.sort;

public class PrintDescriptor {

    @VisibleForTesting int maxKeyLength;
    private Table<MavenProject, MojoExecution, Stopwatch> timers;

    private PrintDescriptor(Table<MavenProject, MojoExecution, Stopwatch> timers) {
        this.timers = timers;
        this.maxKeyLength = findMaxKeyLength(timers.columnKeySet());
    }

    private int findMaxKeyLength(Collection<MojoExecution> mojos) {
        Ordering<MojoExecution> byKeyLength = new Ordering<MojoExecution>() {
            @Override
            public int compare(MojoExecution left, MojoExecution right) {
                return Ints.compare(left.toString().length(), right.toString().length());
            }
        };
        return byKeyLength.max(mojos).toString().length();
    }

    public String getFormattedLine(Map.Entry<MojoExecution, Stopwatch> entry) {
        return String.format(String.format("%-" + maxKeyLength + "s %s", entry.getKey(), entry.getValue()));
    }

    public static PrintDescriptor instance(Table<MavenProject, MojoExecution, Stopwatch> timers) {
        return new PrintDescriptor(timers);
    }

    public List<Map.Entry<MojoExecution, Stopwatch>> getSortedMojosByTime(MavenProject project) {
        List<Map.Entry<MojoExecution, Stopwatch>> result = newArrayList(timers.row(project).entrySet());
        sort(result, new Comparator<Map.Entry<MojoExecution, Stopwatch>>() {
            @Override
            public int compare(Map.Entry<MojoExecution, Stopwatch> o1, Map.Entry<MojoExecution, Stopwatch> o2) {
                return Longs.compare(o2.getValue().elapsedMillis(), o1.getValue().elapsedMillis());
            }
        });
        return result;
    }
}
