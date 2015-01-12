package com.github.jcgay.maven.profiler
import com.google.common.base.Stopwatch
import com.google.common.collect.HashBasedTable
import org.apache.maven.model.Model
import org.apache.maven.model.Plugin
import org.apache.maven.project.MavenProject
import org.assertj.core.api.Condition
import org.codehaus.plexus.util.StringUtils
import org.testng.annotations.Test

import static com.github.jcgay.maven.profiler.KnownElapsedTimeTicker.aStopWatchWithElapsedTime
import static java.util.concurrent.TimeUnit.*
import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.atIndex

class ExecutionTimeDescriptorTest {

    @Test
    void 'should get mojos execution time ordered by spent time'() throws Exception {
        MavenProject project = aMavenProject('project')
        HashBasedTable<MavenProject, org.apache.maven.plugin.MojoExecution, Stopwatch> timers = HashBasedTable.create()
        timers.put(project, aMojoExecution('1'), aStopWatchWithElapsedTime(MILLISECONDS.toNanos(20)))
        timers.put(project, aMojoExecution('2'), aStopWatchWithElapsedTime(SECONDS.toNanos(1)))
        timers.put(project, aMojoExecution('3'), aStopWatchWithElapsedTime(SECONDS.toNanos(2)))
        timers.put(project, aMojoExecution('4'), aStopWatchWithElapsedTime(SECONDS.toNanos(3)))
        timers.put(project, aMojoExecution('5'), aStopWatchWithElapsedTime(MINUTES.toNanos(1)))

        ExecutionTimeDescriptor result = ExecutionTimeDescriptor.instance(timers)

        assertThat(result.getSortedMojosByTime(project))
                .has(MojoExecution.id('5'), atIndex(0))
                .has(MojoExecution.id('4'), atIndex(1))
                .has(MojoExecution.id('3'), atIndex(2))
                .has(MojoExecution.id('2'), atIndex(3))
                .has(MojoExecution.id('1'), atIndex(4))
    }

    @Test
    void 'should be a running stopwatch'() throws Exception {
        assertThat(new Stopwatch({ MINUTES.toNanos(1) }).start().isRunning()).isTrue()
    }

    @Test
    void 'should be a stopwatch with elapsed time'() throws Exception {
        assertThat(new Stopwatch(new KnownElapsedTimeTicker(MINUTES.toNanos(1))).start().stop().elapsedTime(MINUTES)).isEqualTo(1)
    }

    @Test
    void 'should get formatted line to print'() throws Exception {
        MavenProject project = aMavenProject('project')
        MavenProject project_two = aMavenProject('project-2')
        HashBasedTable<MavenProject, org.apache.maven.plugin.MojoExecution, Stopwatch> timers = HashBasedTable.create()
        timers.put(project, aMojoExecutionWithPrintSize(7), aStopWatchWithElapsedTime(MILLISECONDS.toNanos(20)))
        timers.put(project_two, aMojoExecutionWithPrintSize(10), aStopWatchWithElapsedTime(SECONDS.toNanos(1)))
        timers.put(project, aMojoExecutionWithPrintSize(3), aStopWatchWithElapsedTime(SECONDS.toNanos(2)))

        ExecutionTimeDescriptor result = ExecutionTimeDescriptor.instance(timers)

        assertThat(result.maxKeyLength).isEqualTo(10)
    }

    private static org.apache.maven.plugin.MojoExecution aMojoExecution(String id) {
        new org.apache.maven.plugin.MojoExecution(new Plugin(), 'goal', id)
    }

    private static org.apache.maven.plugin.MojoExecution aMojoExecutionWithPrintSize(int size) {
        new MojoExecutionWithPrintSize(size)
    }

    private static MavenProject aMavenProject(String name) {
        Model model = new Model()
        model.name = name
        model.groupId = "groupId-$name"
        model.artifactId = 'artifactId'
        model.version = 'version'
        new MavenProject(model)
    }

    private static class MojoExecution extends Condition<Map.Entry<org.apache.maven.plugin.MojoExecution, Stopwatch>> {

        private final String id

        private MojoExecution(String id) {
            super("a MojoExecution with ID: $id")
            this.id = id
        }

        static MojoExecution id(String id) {
            new MojoExecution(id)
        }

        @Override
        boolean matches(Map.Entry<org.apache.maven.plugin.MojoExecution, Stopwatch> entry) {
            entry.getKey().getExecutionId().equals(id)
        }
    }

    private static class MojoExecutionWithPrintSize extends org.apache.maven.plugin.MojoExecution {

        private final int printSize

        MojoExecutionWithPrintSize(int printSize) {
            super(new Plugin(), 'a-goal', 'an-execution-id')
            this.printSize = printSize
        }

        @Override
        String toString() {
            StringUtils.repeat("*", printSize)
        }
    }
}
