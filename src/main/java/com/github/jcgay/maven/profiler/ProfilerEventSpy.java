package com.github.jcgay.maven.profiler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
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
    private ConcurrentHashMap<MavenProject, ConcurrentHashMap<MojoExecution, Stopwatch>> result =
            new ConcurrentHashMap<MavenProject, ConcurrentHashMap<MojoExecution, Stopwatch>>();

    public ProfilerEventSpy() {
        // Do nothing.
    }

    @VisibleForTesting
    ProfilerEventSpy(Logger logger,
                     Stopwatch projectStopWatch,
                     ConcurrentHashMap<MavenProject, ConcurrentHashMap<MojoExecution, Stopwatch>> mojosStopWatchesByProject) {
        this.logger = logger;
        this.projectStopWatch = projectStopWatch;
        this.result = mojosStopWatchesByProject;
    }

    @Override
    public void onEvent(Object event) throws Exception {
        if (event instanceof ExecutionEvent) {
            ExecutionEvent currentEvent = (ExecutionEvent) event;
            ExecutionEvent.Type type = currentEvent.getType();
            if (type == ExecutionEvent.Type.ProjectStarted) {
                projectStopWatch.start();
            }
            if (type == ExecutionEvent.Type.ProjectSucceeded || type == ExecutionEvent.Type.ProjectFailed) {
                projectStopWatch.stop();
            }
            MavenProject currentProject = currentEvent.getSession().getCurrentProject();
            if (type == ExecutionEvent.Type.MojoStarted) {
                Map<MojoExecution, Stopwatch> mojos = result.get(currentProject);
                if (mojos == null) {
                    result.putIfAbsent(currentProject, new ConcurrentHashMap<MojoExecution, Stopwatch>());
                }
                result.get(currentProject).putIfAbsent(currentEvent.getMojoExecution(), new Stopwatch().start());
            }
            if (type == ExecutionEvent.Type.MojoSucceeded || type == ExecutionEvent.Type.MojoFailed) {
                ConcurrentHashMap<MojoExecution, Stopwatch> project = result.get(currentProject);
                if (project != null) {
                    Stopwatch stopwatch = project.get(currentEvent.getMojoExecution());
                    if (stopwatch != null) {
                        stopwatch.stop();
                    }
                }
            }
        }
        super.onEvent(event);
    }

    @Override
    public void close() throws Exception {

        logger.info("EXECUTION TIME");
        logger.info("------------------------------------------------------------------------");
        for (Map.Entry<MavenProject, ConcurrentHashMap<MojoExecution, Stopwatch>> project : result.entrySet()) {
            logger.info(project.getKey().getName() + ": ");
            PrintDescriptor descriptor = PrintDescriptor.instance(project.getValue());
            for (Map.Entry<MojoExecution, Stopwatch> mojo : descriptor.getSortedMojosExecutionTime()) {
                logger.info(descriptor.getFormattedLine(mojo));
            }
        }
        logger.info("\nProject time: " + projectStopWatch);
        super.close();
    }
}
