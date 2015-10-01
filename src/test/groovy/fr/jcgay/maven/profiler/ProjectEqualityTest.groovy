package fr.jcgay.maven.profiler

import com.google.common.base.Stopwatch
import fr.jcgay.maven.profiler.template.EntryAndTime
import org.apache.maven.model.Plugin
import org.apache.maven.plugin.MojoExecution
import org.testng.annotations.Test

import fr.jcgay.maven.profiler.template.Project

import java.util.concurrent.TimeUnit

import static org.assertj.core.api.Assertions.assertThat

class ProjectEqualityTest {

    @Test
    void 'should compare two different instance of the same project'() throws Exception {
        Stopwatch stopwatch = new Stopwatch();

        Project project1 = new Project('test-project', stopwatch)
        Project project2 = new Project('test-project', stopwatch)

        assertThat(project1).is(project2)
    }

    @Test
    void 'should compare two different instance of the same project with same mojos'() throws Exception {
        Stopwatch stopwatch = aStopwatch();

        Project project1 = new Project('test-project', stopwatch)
        Project project2 = new Project('test-project', stopwatch)

        MojoExecution testMojo = new MojoExecution(new Plugin(), 'test-goal', 'test-id')
        EntryAndTime<MojoExecution> entryAndTime = new EntryAndTime<>(testMojo, aStopwatch());

        project1.addMojoTime(entryAndTime);
        project2.addMojoTime(entryAndTime);

        assertThat(project1).is(project2)
    }

    @Test
    void 'should report that projects are different because they have different mojos'() throws Exception {
        Stopwatch stopwatch = aStopwatch();

        Project project1 = new Project('test-project', stopwatch)
        Project project2 = new Project('test-project', stopwatch)

        EntryAndTime<MojoExecution> entryAndTime = aEntryAndTime('test-id')
        project1.addMojoTime(entryAndTime);

        assertThat(project1).isNotSameAs(project2)
    }

    @Test
    void 'should report that project string is "{test-project, 0 ms, totalMojos = 3}"'() throws Exception {
        Stopwatch stopwatch = new Stopwatch()
        Project project = new Project('test-project', stopwatch)
        project.addMojoTime(aEntryAndTime('test-entry-1'))
        project.addMojoTime(aEntryAndTime('test-entry-2'))
        project.addMojoTime(aEntryAndTime('test-entry-3'))
        assertThat(project.toString()).isEqualTo("{test-project, 0 ms, totalMojos = 3}")
    }

    private static EntryAndTime<MojoExecution> aEntryAndTime(testId) {
        MojoExecution testMojo = new MojoExecution(new Plugin(), 'test-goal', testId)
        new EntryAndTime<MojoExecution>(testMojo, aStopwatch());
    }

    private static Stopwatch aStopwatch() {
        Stopwatch stopwatch = new Stopwatch()
        stopwatch.start()
        TimeUnit.MILLISECONDS.sleep(10)
        stopwatch.stop()
        stopwatch
    }
}
