package com.github.jcgay.maven.profiler;

import com.google.common.base.Stopwatch;
import org.apache.maven.plugin.MojoExecution;

import java.util.Map;

public interface ExecutionRendering {

    void title();

    void separator();

    void projectSummary(String name, Stopwatch totalTime);

    void mojoExecution(Map.Entry<MojoExecution, Stopwatch> mojo, ExecutionTimeDescriptor descriptor);
}
