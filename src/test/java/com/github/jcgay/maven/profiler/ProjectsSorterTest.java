package com.github.jcgay.maven.profiler;

import com.google.common.base.Stopwatch;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.jcgay.maven.profiler.KnownElapsedTimeTicker.aStopWatchWithElapsedTime;
import static org.assertj.core.api.Assertions.assertThat;

public class ProjectsSorterTest {

    @Test
    public void should_order_projects_by_their_execution_time() throws Exception {

        Map<MavenProject, Stopwatch> projects = new HashMap<MavenProject, Stopwatch>();
        projects.put(aMavenProject("1"), aStopWatchWithElapsedTime(TimeUnit.SECONDS.toNanos(1)));
        projects.put(aMavenProject("2"), aStopWatchWithElapsedTime(TimeUnit.MINUTES.toNanos(3)));
        projects.put(aMavenProject("3"), aStopWatchWithElapsedTime(TimeUnit.HOURS.toNanos(1)));

        List<MavenProject> result = ProjectsSorter.byExecutionTime(projects);

        assertThat(result).extracting("name").containsExactly("3", "2", "1");
    }

    private static MavenProject aMavenProject(String name) {
        Model model = new Model();
        model.setName(name);
        model.setGroupId("groupId-" + name);
        model.setArtifactId("artifactId");
        model.setVersion("version");
        return new MavenProject(model);
    }
}
