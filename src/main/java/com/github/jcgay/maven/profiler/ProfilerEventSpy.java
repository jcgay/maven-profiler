package com.github.jcgay.maven.profiler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
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

    private Map<MavenProject, Stopwatch> projects = new ConcurrentHashMap<MavenProject, Stopwatch>();
    private Table<MavenProject, MojoExecution, Stopwatch> timers = HashBasedTable.create();

    public ProfilerEventSpy() {
        // Do nothing.
    }

    @VisibleForTesting
    ProfilerEventSpy(Logger logger,
                     ConcurrentHashMap<MavenProject, Stopwatch> projects,
                     Table<MavenProject, MojoExecution, Stopwatch> timers) {
        this.logger = logger;
        this.projects = projects;
        this.timers = timers;
    }

    @Override
    public void onEvent(Object event) throws Exception {
        logger.debug("Received event: " + event);
        if (event instanceof ExecutionEvent) {
            ExecutionEvent currentEvent = (ExecutionEvent) event;
            ExecutionEvent.Type type = currentEvent.getType();
            MavenProject currentProject = currentEvent.getSession().getCurrentProject();
            if (type == ExecutionEvent.Type.ProjectStarted) {
                startProject(currentProject);
            }
            if (type == ExecutionEvent.Type.ProjectSucceeded || type == ExecutionEvent.Type.ProjectFailed) {
                stopProject(currentProject);
            }
            if (type == ExecutionEvent.Type.MojoStarted) {
                startMojo(currentEvent, currentProject);
            }
            if (type == ExecutionEvent.Type.MojoSucceeded || type == ExecutionEvent.Type.MojoFailed) {
                stopMojo(currentEvent, currentProject);
            }
        }
        super.onEvent(event);
    }

    private void stopMojo(ExecutionEvent currentEvent, MavenProject currentProject) {
        logger.debug(String.format("Stopping timer for mojo [%s] in project [%s].", currentEvent.getMojoExecution(), currentProject));
        timers.get(currentProject, currentEvent.getMojoExecution()).stop();
    }

    private void startMojo(ExecutionEvent currentEvent, MavenProject currentProject) {
        logger.debug(String.format("Starting timer for mojo [%s] in project [%s].", currentEvent.getMojoExecution(), currentProject));
        synchronized (currentEvent) {
            timers.put(currentProject, currentEvent.getMojoExecution(), new Stopwatch().start());
        }
    }

    private void stopProject(MavenProject currentProject) {
        logger.debug("Stopping timer for project: " + currentProject);
        projects.get(currentProject).stop();
    }

    private void startProject(MavenProject currentProject) {
        logger.debug("Starting timer for project: " + currentProject);
        projects.put(currentProject, new Stopwatch().start());
    }

    @Override
    public void close() throws Exception {

        logger.info("EXECUTION TIME");
        logger.info("------------------------------------------------------------------------");
        for (MavenProject project : ProjectsSorter.byExecutionTime(projects)) {
            logger.info(project.getName() + ": " + projects.get(project));
            PrintDescriptor descriptor = PrintDescriptor.instance(timers.row(project));
            for (Map.Entry<MojoExecution, Stopwatch> mojo : descriptor.getSortedMojosExecutionTime()) {
                logger.info(descriptor.getFormattedLine(mojo));
            }
        }
        super.close();
    }
}
