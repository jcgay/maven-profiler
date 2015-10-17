package fr.jcgay.maven.profiler;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;

public class KnownElapsedTimeTicker extends Ticker {

    private final long expectedElapsedTime;
    private boolean firstRead;

    public KnownElapsedTimeTicker(long expectedElapsedTime) {
        this.expectedElapsedTime = expectedElapsedTime;
    }

    public static Stopwatch aStopWatchWithElapsedTime(long elapsedTime) {
        return new Stopwatch(new KnownElapsedTimeTicker(elapsedTime)).start().stop();
    }

    @Override
    public long read() {
        firstRead = !firstRead;
        return firstRead ? 0 : expectedElapsedTime;
    }
}
