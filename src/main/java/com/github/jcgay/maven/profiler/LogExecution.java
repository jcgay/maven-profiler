package com.github.jcgay.maven.profiler;

import com.google.common.base.Stopwatch;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.logging.Logger;

import java.util.Map;

public class LogExecution implements ExecutionRendering {

    private Logger logger;

    public LogExecution(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void title() {
        logger.info("EXECUTION TIME");
    }

    @Override
    public void separator() {
        logger.info("------------------------------------------------------------------------");
    }

    @Override
    public void projectSummary(String name, Stopwatch totalTime) {
        logger.info(name + ": " + totalTime);
    }

    @Override
    public void mojoExecution(Map.Entry<MojoExecution, Stopwatch> mojo, ExecutionTimeDescriptor descriptor) {
        logger.info(descriptor.getFormattedLine(mojo));
    }
}
