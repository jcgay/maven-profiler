package com.github.jcgay.maven.profiler;

import com.google.common.base.Throwables;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.eclipse.aether.DefaultRepositorySystemSession;

class TestExecutionEvent implements ExecutionEvent {

    private MavenSession session;
    private Type type;
    private MojoExecution mojoExecution;

    public TestExecutionEvent(Type type, MojoExecution mojoExecution, MavenProject project) {
        this.type = type;
        this.mojoExecution = mojoExecution;
        try {
            this.session = buildDefaultSession();
            this.session.setCurrentProject(project);
        } catch (PlexusContainerException e) {
            Throwables.propagate(e);
        }
    }

    private MavenSession buildDefaultSession() throws PlexusContainerException {
        return new MavenSession(new DefaultPlexusContainer(), new DefaultRepositorySystemSession(), new DefaultMavenExecutionRequest(), new DefaultMavenExecutionResult());
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public MavenSession getSession() {
        return session;
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
