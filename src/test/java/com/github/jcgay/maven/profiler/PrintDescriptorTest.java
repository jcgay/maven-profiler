package com.github.jcgay.maven.profiler;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import org.apache.maven.model.Plugin;
import org.assertj.core.api.Condition;
import org.codehaus.plexus.util.StringUtils;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;

public class PrintDescriptorTest {

    @Test
    public void should_get_mojos_execution_time_ordered_by_spent_time() throws Exception {

        HashMap<org.apache.maven.plugin.MojoExecution, Stopwatch> mojos = new HashMap<org.apache.maven.plugin.MojoExecution, Stopwatch>();
        mojos.put(aMojoExecution("1"), aStopWatchWithElapsedTime(TimeUnit.MILLISECONDS.toNanos(20)));
        mojos.put(aMojoExecution("2"), aStopWatchWithElapsedTime(TimeUnit.SECONDS.toNanos(1)));
        mojos.put(aMojoExecution("3"), aStopWatchWithElapsedTime(TimeUnit.SECONDS.toNanos(2)));
        mojos.put(aMojoExecution("4"), aStopWatchWithElapsedTime(TimeUnit.SECONDS.toNanos(3)));
        mojos.put(aMojoExecution("5"), aStopWatchWithElapsedTime(TimeUnit.MINUTES.toNanos(1)));

        PrintDescriptor result = PrintDescriptor.instance(mojos);

        assertThat(result.getSortedMojosExecutionTime())
                .has(MojoExecution.id("5"), atIndex(0))
                .has(MojoExecution.id("4"), atIndex(1))
                .has(MojoExecution.id("3"), atIndex(2))
                .has(MojoExecution.id("2"), atIndex(3))
                .has(MojoExecution.id("1"), atIndex(4));
    }

    @Test
    public void should_be_a_running_stopwatch() throws Exception {
        assertThat(new Stopwatch(new Ticker() {
            @Override
            public long read() {
                return TimeUnit.MINUTES.toNanos(1);
            }
        }).start().isRunning()).isTrue();
    }

    @Test
    public void should_be_a_stopwatch_with_elapsed_time() throws Exception {
        assertThat(new Stopwatch(new KnownElapsedTimeTicker(TimeUnit.MINUTES.toNanos(1))).start().stop().elapsedTime(TimeUnit.MINUTES)).isEqualTo(1);
    }

    @Test
    public void should_get_formatted_line_to_print() throws Exception {

        HashMap<org.apache.maven.plugin.MojoExecution, Stopwatch> mojos = new HashMap<org.apache.maven.plugin.MojoExecution, Stopwatch>();
        mojos.put(aMojoExecutionWithPrintSize(7), aStopWatchWithElapsedTime(TimeUnit.MILLISECONDS.toNanos(20)));
        mojos.put(aMojoExecutionWithPrintSize(10), aStopWatchWithElapsedTime(TimeUnit.SECONDS.toNanos(1)));
        mojos.put(aMojoExecutionWithPrintSize(3), aStopWatchWithElapsedTime(TimeUnit.SECONDS.toNanos(2)));

        PrintDescriptor result = PrintDescriptor.instance(mojos);

        assertThat(result.maxKeyLength).isEqualTo(10);
    }

    private static org.apache.maven.plugin.MojoExecution aMojoExecution(String id) {
        return new org.apache.maven.plugin.MojoExecution(new Plugin(), "goal", id);
    }

    private static org.apache.maven.plugin.MojoExecution aMojoExecutionWithPrintSize(int size) {
        return new MojoExecutionWithPrintSize(size);
    }

    private static Stopwatch aStopWatchWithElapsedTime(long elapsedTime) {
        return new Stopwatch(new KnownElapsedTimeTicker(elapsedTime)).start().stop();
    }

    private static class MojoExecution extends Condition<Map.Entry<org.apache.maven.plugin.MojoExecution, Stopwatch>> {

        private String id;

        private MojoExecution(String id) {
            super("a MojoExecution with ID: " + id);
            this.id = id;
        }

        public static MojoExecution id(String id) {
            return new MojoExecution(id);
        }

        @Override
        public boolean matches(Map.Entry<org.apache.maven.plugin.MojoExecution, Stopwatch> entry) {
            return entry.getKey().getExecutionId().equals(id);
        }
    }

    private static class KnownElapsedTimeTicker extends Ticker {

        private long expectedElapsedTime;
        private boolean firstRead;

        private KnownElapsedTimeTicker(long expectedElapsedTime) {
            this.expectedElapsedTime = expectedElapsedTime;
        }

        @Override
        public long read() {
            firstRead = !firstRead;
            return firstRead ? 0 : expectedElapsedTime;
        }
    }

    private static class MojoExecutionWithPrintSize extends org.apache.maven.plugin.MojoExecution {

        private int printSize;

        public MojoExecutionWithPrintSize(int printSize) {
            super(new Plugin(), "a-goal", "an-execution-id");
            this.printSize = printSize;
        }

        @Override
        public String toString() {
            return StringUtils.repeat("*", printSize);
        }
    }
}
