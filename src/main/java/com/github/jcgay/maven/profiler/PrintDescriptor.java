package com.github.jcgay.maven.profiler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.apache.maven.plugin.MojoExecution;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.sort;

public class PrintDescriptor {

    @VisibleForTesting int maxKeyLength;
    private List<Map.Entry<MojoExecution, Stopwatch>> mojosStopWatch;

    public static PrintDescriptor instance(Map<MojoExecution, Stopwatch> mojosStopWatch) {
        List<Map.Entry<MojoExecution, Stopwatch>> result = newArrayList(mojosStopWatch.entrySet());
        sort(result, new Comparator<Map.Entry<MojoExecution, Stopwatch>>() {
            @Override
            public int compare(Map.Entry<MojoExecution, Stopwatch> o1, Map.Entry<MojoExecution, Stopwatch> o2) {
                return Longs.compare(o2.getValue().elapsedMillis(), o1.getValue().elapsedMillis());
            }
        });
        return new PrintDescriptor(result);
    }

    private PrintDescriptor(List<Map.Entry<MojoExecution,Stopwatch>> mojosStopWatch) {
        this.mojosStopWatch = mojosStopWatch;
        this.maxKeyLength = findMaxKeyLength();
    }

    private int findMaxKeyLength() {
        Ordering<Map.Entry<MojoExecution, Stopwatch>> byKeyLength = new Ordering<Map.Entry<MojoExecution, Stopwatch>>() {
            @Override
            public int compare(Map.Entry<MojoExecution, Stopwatch> left, Map.Entry<MojoExecution, Stopwatch> right) {
                return Ints.compare(left.getKey().toString().length(), right.getKey().toString().length());
            }
        };
        return byKeyLength.max(mojosStopWatch).getKey().toString().length();
    }

    public List<Map.Entry<MojoExecution, Stopwatch>> getSortedMojosExecutionTime() {
        return mojosStopWatch;
    }

    public String getFormattedLine(Map.Entry<MojoExecution, Stopwatch> entry) {
        return String.format(String.format("%-" + maxKeyLength + "s %s", entry.getKey(), entry.getValue()));
    }
}
