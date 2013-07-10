package com.github.jcgay.maven.profiler;

import com.google.common.base.Stopwatch;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ProfilerEventSpyTest {

    private ProfilerEventSpy profiler;
    private ConcurrentHashMap<MavenProject, ConcurrentHashMap<MojoExecution, Stopwatch>> result;
    private Stopwatch project;

    @BeforeMethod
    public void setUp() throws Exception {
        result = new ConcurrentHashMap<MavenProject, ConcurrentHashMap<MojoExecution, Stopwatch>>();
        project = new Stopwatch();

        profiler = new ProfilerEventSpy(
                null,
                project,
                result
        );
    }

    @Test
    public void should_start_a_timer_when_a_mojo_start() throws Exception {

        ExecutionEvent event = aMojoEvent(ExecutionEvent.Type.MojoStarted, aMavenProject("a-project"));

        profiler.onEvent(event);

        assertThat(result.get(event.getSession().getCurrentProject())
                         .get(event.getMojoExecution())
                         .isRunning())
                .isTrue();
    }

    @Test(dataProvider = "mojo_succeed_and_fail")
    public void should_stop_the_timer_when_a_mojo_succeed_or_fail(ExecutionEvent.Type type) throws Exception {

        ExecutionEvent event = aMojoEvent(type, aMavenProject("a-project"));
        given_event_has_start(event);

        profiler.onEvent(event);

        assertThat(result.get(event.getSession().getCurrentProject())
                         .get(event.getMojoExecution())
                         .isRunning())
                .isFalse();
        assertThat(result.get(event.getSession().getCurrentProject())
                         .get(event.getMojoExecution())
                         .elapsedMillis())
                .isPositive();
    }

    @DataProvider
    private Object[][] mojo_succeed_and_fail() {
        return new Object[][] {
                {ExecutionEvent.Type.MojoSucceeded},
                {ExecutionEvent.Type.MojoFailed}
        };
    }

    @Test
    public void should_start_timer_when_project_start() throws Exception {

        profiler.onEvent(aProjectEvent(ExecutionEvent.Type.ProjectStarted));

        assertThat(project.isRunning()).isTrue();
    }

    @Test(dataProvider = "project_succeed_and_fail")
    public void should_stop_timer_when_project_fail_or_succeed(ExecutionEvent.Type type) throws Exception {

        given_project_has_start();

        profiler.onEvent(aProjectEvent(type));

        assertThat(project.isRunning()).isFalse();
        assertThat(project.elapsedMillis()).isPositive();
    }

    @DataProvider
    private Object[][] project_succeed_and_fail() {
        return new Object[][] {
                {ExecutionEvent.Type.ProjectSucceeded},
                {ExecutionEvent.Type.ProjectFailed}
        };
    }

    private static MavenProject aMavenProject(String name) {
        Model model = new Model();
        model.setName(name);
        MavenProject project = new MavenProject(model);
        project.setGroupId("groupId");
        project.setArtifactId("artifactId");
        project.setVersion("1.0");
        return project;
    }

    private void given_project_has_start() throws Exception {
        profiler.onEvent(aProjectEvent(ExecutionEvent.Type.ProjectStarted));
        TimeUnit.MILLISECONDS.sleep(1);
    }

    private void given_event_has_start(ExecutionEvent event) throws Exception {
        profiler.onEvent(aMojoEvent(ExecutionEvent.Type.MojoStarted, event.getMojoExecution(), event.getSession().getCurrentProject()));
        TimeUnit.MILLISECONDS.sleep(1);
    }

    private static ExecutionEvent aMojoEvent(ExecutionEvent.Type type, MavenProject mavenProject) {
        return aMojoEvent(type, new MojoExecution(new Plugin(), "goal", "execution.id"), mavenProject);
    }

    private static ExecutionEvent aMojoEvent(ExecutionEvent.Type type, MojoExecution mojoExecution, MavenProject mavenProject) {
        return new TestExecutionEvent(type, mojoExecution, mavenProject);
    }

    private static ExecutionEvent aProjectEvent(ExecutionEvent.Type type) {
        return aMojoEvent(type, new MojoExecution(new Plugin(), "goal", "execution.id"), aMavenProject("project"));
    }
}

