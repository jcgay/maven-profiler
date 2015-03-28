package fr.jcgay.maven.profiler

import com.google.common.base.Throwables
import groovy.transform.PackageScope
import org.apache.maven.execution.DefaultMavenExecutionRequest
import org.apache.maven.execution.DefaultMavenExecutionResult
import org.apache.maven.execution.ExecutionEvent
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.MojoExecution
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.DefaultPlexusContainer
import org.codehaus.plexus.PlexusContainerException
import org.eclipse.aether.DefaultRepositorySystemSession

@PackageScope
class TestExecutionEvent implements ExecutionEvent {

    private MavenSession session
    private ExecutionEvent.Type type
    private MojoExecution mojoExecution

    TestExecutionEvent(ExecutionEvent.Type type, MojoExecution mojoExecution, MavenProject project) {
        this.type = type
        this.mojoExecution = mojoExecution
        try {
            this.session = buildDefaultSession()
            this.session.currentProject = project
        } catch (PlexusContainerException e) {
            Throwables.propagate(e)
        }
    }

    private static MavenSession buildDefaultSession() throws PlexusContainerException {
        new MavenSession(new DefaultPlexusContainer(), new DefaultRepositorySystemSession(), new DefaultMavenExecutionRequest(), new DefaultMavenExecutionResult())
    }

    @Override
    ExecutionEvent.Type getType() {
        this.type
    }

    @Override
    MavenSession getSession() {
        session
    }

    @Override
    MavenProject getProject() {
        null
    }

    @Override
    MojoExecution getMojoExecution() {
        this.mojoExecution
    }

    @Override
    Exception getException() {
        null
    }
}
