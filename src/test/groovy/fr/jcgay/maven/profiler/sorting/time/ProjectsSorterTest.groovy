package fr.jcgay.maven.profiler.sorting.time

import com.google.common.base.Stopwatch
import org.apache.maven.model.Model
import org.apache.maven.project.MavenProject
import org.testng.annotations.Test

import java.util.function.Function

import static fr.jcgay.maven.profiler.KnownElapsedTimeTicker.aStopWatchWithElapsedTime
import static java.util.concurrent.TimeUnit.HOURS
import static java.util.concurrent.TimeUnit.MINUTES
import static java.util.concurrent.TimeUnit.SECONDS
import static org.assertj.core.api.Assertions.assertThat

class ProjectsSorterTest {

    @Test
    void 'should order projects by their execution time'() throws Exception {

        Map<MavenProject, Stopwatch> projects = [:]
        projects[aMavenProject('1')] = aStopWatchWithElapsedTime(SECONDS.toNanos(1))
        projects[aMavenProject('2')] = aStopWatchWithElapsedTime(MINUTES.toNanos(3))
        projects[aMavenProject('3')] = aStopWatchWithElapsedTime(HOURS.toNanos(1))

        List<MavenProject> result = ProjectsSorter.byExecutionTime(projects)

        assertThat(result).extracting({ it.name } as Function<MavenProject, String>).containsExactly('3', '2', '1')
    }

    private static MavenProject aMavenProject(String name) {
        Model model = new Model()
        model.name = name
        model.groupId = "groupId-$name"
        model.artifactId = 'artifactId'
        model.version = 'version'
        new MavenProject(model)
    }
}
