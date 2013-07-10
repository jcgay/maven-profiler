package com.github.jcgay.maven.profiler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(role = EventSpy.class, hint = "profiler", description = "Measure times taken by Maven.")
public class ProfilerEventSpy extends AbstractEventSpy {

    @Requirement
    private Logger logger;

    private Stopwatch projectStopWatch = new Stopwatch();
    private Map<MojoExecution, Stopwatch> mojosStopWatch = new ConcurrentHashMap<MojoExecution, Stopwatch>();

    public ProfilerEventSpy() {
        // Do nothing.
    }

    @VisibleForTesting
    ProfilerEventSpy(Logger logger, Stopwatch projectStopWatch, Map<MojoExecution, Stopwatch> mojosStopWatch) {
        this.logger = logger;
        this.projectStopWatch = projectStopWatch;
        this.mojosStopWatch = mojosStopWatch;
    }

    @Override
    public void onEvent(Object event) throws Exception {
        if (event instanceof ExecutionEvent) {
            ExecutionEvent.Type type = ((ExecutionEvent) event).getType();
            if (type == ExecutionEvent.Type.ProjectStarted) {
                projectStopWatch.start();
            }
            if (type == ExecutionEvent.Type.ProjectSucceeded || type == ExecutionEvent.Type.ProjectFailed) {
                projectStopWatch.stop();
            }
            if (type == ExecutionEvent.Type.MojoStarted) {
                mojosStopWatch.put(((ExecutionEvent) event).getMojoExecution(), new Stopwatch().start());
            }
            if (type == ExecutionEvent.Type.MojoSucceeded || type == ExecutionEvent.Type.MojoFailed) {
                Stopwatch stopwatch = mojosStopWatch.get(((ExecutionEvent) event).getMojoExecution());
                if (stopwatch != null) {
                    stopwatch.stop();
                }
            }
        }
        super.onEvent(event);
    }

    @Override
    public void close() throws Exception {

        PrintDescriptor descriptor = PrintDescriptor.instance(mojosStopWatch);

        logger.info("EXECUTION TIME");
        logger.info("------------------------------------------------------------------------");
        for (Map.Entry<MojoExecution, Stopwatch> entry : descriptor.getSortedMojosExecutionTime()) {
            logger.info(descriptor.getFormattedLine(entry));
        }
        logger.info("Project time: " + projectStopWatch);
        super.close();
    }
}
