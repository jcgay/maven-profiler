package fr.jcgay.maven.profiler.sorting.time;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;

import java.util.List;
import java.util.Map;

class ProjectsSorter {

    private ProjectsSorter() {
        // Prevents instantiation.
    }

    public static <T> List<T> byExecutionTime(Map<T, Stopwatch> projects) {
        Ordering<Map.Entry<T, Stopwatch>> descending = new Ordering<Map.Entry<T, Stopwatch>>() {
            @Override
            public int compare(Map.Entry<T, Stopwatch> left, Map.Entry<T, Stopwatch> right) {
                return Longs.compare(right.getValue().elapsedMillis(), left.getValue().elapsedMillis());
            }
        };

        return Lists.transform(descending.sortedCopy(projects.entrySet()), Map.Entry::getKey);
    }
}
