package com.github.jcgay.maven.profiler;

import com.google.common.base.Stopwatch;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ProfilerEventSpyTest {

    private ProfilerEventSpy profiler;
    private Map<MojoExecution, Stopwatch> mojos;
    private Stopwatch project;

    @BeforeMethod
    public void setUp() throws Exception {
        mojos = new HashMap<MojoExecution, Stopwatch>();
        project = new Stopwatch();

        profiler = new ProfilerEventSpy(
                null,
                project,
                mojos
        );
    }

    @Test
    public void should_start_a_timer_when_a_mojo_start() throws Exception {

        ExecutionEvent event = aMojoEvent(ExecutionEvent.Type.MojoStarted);

        profiler.onEvent(event);

        assertThat(mojos.get(event.getMojoExecution()).isRunning()).isTrue();
    }

    @Test(dataProvider = "mojo_succeed_and_fail")
    public void should_stop_the_timer_when_a_mojo_succeed_or_fail(ExecutionEvent.Type type) throws Exception {

        ExecutionEvent event = aMojoEvent(type);
        given_event_has_start(event);

        profiler.onEvent(event);

        assertThat(mojos.get(event.getMojoExecution()).isRunning()).isFalse();
        assertThat(mojos.get(event.getMojoExecution()).elapsedMillis()).isPositive();
    }

    @Test(dataProvider = "mojo_succeed_and_fail")
    public void should_not_fail_if_a_mojo_is_stopped_before_it_has_been_started(ExecutionEvent.Type type) throws Exception {

        ExecutionEvent event = aMojoEvent(type);

        profiler.onEvent(event);

        assertThat(mojos.get(event.getMojoExecution())).isNull();
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

    private void given_project_has_start() throws Exception {
        profiler.onEvent(aProjectEvent(ExecutionEvent.Type.ProjectStarted));
        TimeUnit.MILLISECONDS.sleep(1);
    }

    private void given_event_has_start(ExecutionEvent event) throws Exception {
        profiler.onEvent(aMojoEvent(ExecutionEvent.Type.MojoStarted, event.getMojoExecution()));
        TimeUnit.MILLISECONDS.sleep(1);
    }

    private static ExecutionEvent aMojoEvent(ExecutionEvent.Type type) {
        return new TestExecutionEvent(type, new MojoExecution(new Plugin(), "goal", "execution.id"));
    }

    private static ExecutionEvent aMojoEvent(ExecutionEvent.Type type, MojoExecution mojoExecution) {
        return new TestExecutionEvent(type, mojoExecution);
    }

    private static ExecutionEvent aProjectEvent(ExecutionEvent.Type type) {
        return new TestExecutionEvent(type);
    }
}

