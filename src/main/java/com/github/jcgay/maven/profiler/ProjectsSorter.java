package com.github.jcgay.maven.profiler;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Map;

public class ProjectsSorter {

    private ProjectsSorter() {
        // Prevents instantiation.
    }

    public static List<MavenProject> byExecutionTime(Map<MavenProject, Stopwatch> projects) {
        Ordering<Map.Entry<MavenProject, Stopwatch>> descending = new Ordering<Map.Entry<MavenProject, Stopwatch>>() {
            @Override
            public int compare(Map.Entry<MavenProject, Stopwatch> left, Map.Entry<MavenProject, Stopwatch> right) {
                return Longs.compare(right.getValue().elapsedMillis(), left.getValue().elapsedMillis());
            }
        };

        return Lists.transform(descending.sortedCopy(projects.entrySet()), new Function<Map.Entry<MavenProject, Stopwatch>, MavenProject>() {
            @Override
            public MavenProject apply(Map.Entry<MavenProject, Stopwatch> input) {
                return input.getKey();
            }
        });
    }
}
