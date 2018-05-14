package fr.jcgay.maven.profiler

import org.apache.maven.execution.ExecutionEvent
import org.apache.maven.model.Model
import org.apache.maven.model.Plugin
import org.apache.maven.plugin.MojoExecution
import org.apache.maven.project.MavenProject
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositoryEvent
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact


class MavenStubs {

    static MavenProject aMavenProject(String name) {
        Model model = new Model()
        model.name = name
        MavenProject project = new MavenProject(model)
        project.groupId = 'groupId'
        project.artifactId = 'artifactId'
        project.version = '1.0'
        project
    }

    static MavenProject aMavenTopProject(String name) {
        def topProject = aMavenProject(name)
        topProject.file = File.createTempFile('pom', '.xml')
        topProject
    }

    static ExecutionEvent aMojoEvent(ExecutionEvent.Type type, MavenProject mavenProject) {
        aMojoEvent(type, new MojoExecution(new Plugin(), 'goal', 'execution.id'), mavenProject)
    }

    static ExecutionEvent aMojoEvent(ExecutionEvent.Type type, MojoExecution mojoExecution, MavenProject mavenProject) {
        new TestExecutionEvent(type, mojoExecution, mavenProject)
    }

    static ExecutionEvent aProjectEvent(ExecutionEvent.Type type) {
        aMojoEvent(type, aMojoExecution('execution.id'), aMavenProject('project'))
    }

    static RepositoryEvent.Builder aRepositoryEvent(RepositoryEvent.EventType type, Artifact artifact) {
        new RepositoryEvent.Builder(new DefaultRepositorySystemSession(), type)
            .setArtifact(artifact)
    }

    static Artifact anArtifact() {
        anArtifact('artifactId')
    }

    static Artifact anArtifact(String artifactId) {
        ArtifactProfiled.of(new DefaultArtifact('groupId', artifactId, 'jar', '1.0'))
    }

    static MojoExecution aMojoExecution(String id) {
        new MojoExecution(new Plugin(), 'goal', id)
    }
}
