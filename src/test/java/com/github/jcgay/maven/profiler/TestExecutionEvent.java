package com.github.jcgay.maven.profiler;

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

public class TestExecutionEvent implements ExecutionEvent {

    private Type type;
    private MojoExecution mojoExecution;

    public TestExecutionEvent(Type type, MojoExecution mojoExecution) {
        this.type = type;
        this.mojoExecution = mojoExecution;
    }

    public TestExecutionEvent(Type type) {
        this.type = type;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public MavenSession getSession() {
        return null;
    }

    @Override
    public MavenProject getProject() {
        return null;
    }

    @Override
    public MojoExecution getMojoExecution() {
        return this.mojoExecution;
    }

    @Override
    public Exception getException() {
        return null;
    }
}
