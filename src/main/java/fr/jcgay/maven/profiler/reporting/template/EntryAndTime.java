package fr.jcgay.maven.profiler.reporting.template;

import com.google.common.base.Stopwatch;

public class EntryAndTime<T> {

    private final T entry;
    private final Stopwatch time;

    public EntryAndTime(T entry, Stopwatch time) {
        this.entry = entry;
        this.time = time;
    }

    public T getEntry() {
        return entry;
    }

    public Stopwatch getTime() {
        return time;
    }

    public String getMillisTimeStamp() {
        return String.valueOf(time.elapsedMillis()) + " ms";
    }
}
