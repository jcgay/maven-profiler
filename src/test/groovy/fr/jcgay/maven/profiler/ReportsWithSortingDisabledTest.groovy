package fr.jcgay.maven.profiler
import com.google.common.base.Stopwatch
import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import org.apache.maven.execution.DefaultMavenExecutionRequest
import org.apache.maven.execution.ExecutionEvent
import org.apache.maven.model.Model
import org.apache.maven.model.Plugin
import org.apache.maven.plugin.MojoExecution
import org.apache.maven.project.MavenProject
import org.assertj.guava.api.Assertions
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositoryEvent
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.transfer.ArtifactNotFoundException
import org.testng.Reporter
import org.testng.ReporterConfig
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

import static fr.jcgay.maven.profiler.ProfilerEventSpy.PROFILE
import static java.util.Arrays.asList
import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.apache.maven.execution.ExecutionEvent.Type.*
import static org.assertj.core.api.Assertions.assertThat
import static org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_DOWNLOADED
import static org.eclipse.aether.RepositoryEvent.EventType.ARTIFACT_DOWNLOADING

class ReportsWithSortingDisabledTest {

    private static Random random

    private ProfilerEventSpy profiler
    private Table<MavenProject, MojoExecution, Stopwatch> timers
    private ConcurrentMap<Artifact, Stopwatch> downloadTimers
    private ConcurrentHashMap<MavenProject, Stopwatch> projects
    private MavenProject topProject
    private List<ProfilerEventSpy.SequenceEvent> sequenceEvents
    private List<Artifact> sequenceDownloads

    static {
        random = new Random()
    }

    @BeforeMethod
    void setUp() throws Exception {
        timers = HashBasedTable.create()
        downloadTimers = new ConcurrentHashMap<Artifact, Stopwatch>()

        projects = new ConcurrentHashMap<MavenProject, Stopwatch>()

        topProject = aMavenProject('top-project')
        topProject.file = File.createTempFile('pom', '.xml')

        System.setProperty('profile', 'true')
        System.clearProperty('profileFormat')
        System.setProperty('disableTimeSorting', 'true')

        sequenceEvents = new LinkedList<>()
        sequenceDownloads = new LinkedList<>()

        profiler = new ProfilerEventSpy(
                projects,
                timers,
                downloadTimers,
                topProject,
                sequenceEvents,
                sequenceDownloads
        )
    }

    @Test
    void 'should report artifact downloads in the order in which they were downloaded'() throws Exception {
        def NUM_ARTIFACTS = 10
        List<Artifact> artifacts = new ArrayList<>();
        0.upto(NUM_ARTIFACTS, {
            def fakeArtifact = anArtifact("test-artifact-${it}")
            artifacts.add(fakeArtifact)
            simulateArtifactDownloaded(profiler, fakeArtifact)
        })

        assertThat(sequenceDownloads.size()).isEqualTo(artifacts.size())

        sequenceDownloads.eachWithIndex{ artifact, artifactIndex ->
            assertThat(artifact).isEqualTo(artifacts.get(artifactIndex))
        }
    }

    @Test
    void 'should report mojos in order of execution'() throws Exception {
        MavenProject project = aMavenProject('test-project')

        def mojos = [new Tuple('compile', 'maven-compile'),
                     new Tuple('test', 'maven-test'),
                     new Tuple('package', 'maven-package'),
                     new Tuple('install', 'maven-install')] as Tuple[]

        simulateProjectStartedExecution(profiler, project)

        mojos.each {
            simulateMojoExecutedSuccessfully(profiler, project, it.get(0), it.get(1))
        }

        simulateProjectEndedExecution(profiler, project)
        profiler.close()

        sequenceEvents.eachWithIndex {event, index ->
            assertThat(event.mojo.goal).isEqualTo(mojos[index].get(0))
            assertThat(event.mojo.executionId).isEqualTo(mojos[index].get(1))
        }
    }

    @Test
    void 'should report projects in order of execution'() throws Exception {
        List<MavenProject> projects = new LinkedList<>();
        def NUM_PROJECTS = 20 + (random.nextInt() % 10)
        0.upto(NUM_PROJECTS, { projects.add(aMavenProject("test-project-${it}")) })

        projects.each {
            simulateProjectStartedExecution(profiler, it)
            simulateMojoExecutedSuccessfully(profiler, it, 'test', it.name + '-testing-mojo')
            simulateProjectEndedExecution(profiler, it)
        }

        profiler.close()

        sequenceEvents.eachWithIndex {event, index ->
            assertThat(event.project.model.name).isEqualTo(projects[index].model.name)
        }
    }

    @Test
    void 'should ignore the duplicated project instance and report all mojo\'s executions under the same project'()
        throws Exception {
        List<MavenProject> projects = new LinkedList<>();

        MavenProject originalMavenProject = aMavenProject("test-project")
        MavenProject duplicatedMavenProject = aMavenProject("test-project")

        simulateProjectStartedExecution(profiler, originalMavenProject)

        def NUM_MOJO_EVENTS_USING_ORIGINAL_PROJECT = 5
        0.upto(NUM_MOJO_EVENTS_USING_ORIGINAL_PROJECT, {
            simulateMojoExecutedSuccessfully(profiler, originalMavenProject, "test-${it}", "testing-mojo-${it}")
        })

        def NUM_MOJO_EVENTS_USING_DUPLICATED_PROJECT = NUM_MOJO_EVENTS_USING_ORIGINAL_PROJECT + 5
        (NUM_MOJO_EVENTS_USING_ORIGINAL_PROJECT + 1).upto(NUM_MOJO_EVENTS_USING_DUPLICATED_PROJECT, {
            simulateMojoExecutedSuccessfully(profiler, duplicatedMavenProject, "test-${it}", "testing-mojo-${it}")
        })

        simulateProjectEndedExecution(profiler, originalMavenProject);

        profiler.close()

        assertThat(sequenceEvents.size()).isEqualTo(11)

        sequenceEvents.eachWithIndex {event, index ->
            assertThat(event.project).isEqualTo(originalMavenProject)
        }
    }

    @Test
    void 'should report both projects and mojos in order of execution'() throws Exception {
        List<MavenProject> projects = new LinkedList<>();

        def NUM_PROJECTS = 5
        0.upto(NUM_PROJECTS, { projects.add(aMavenProject("test-project-${it}")) })

        def NUM_MOJOS = 5
        projects.each { project ->
            simulateProjectStartedExecution(profiler, project)

            0.upto(NUM_MOJOS, {
                simulateMojoExecutedSuccessfully(profiler, project, "test-${it}", "testing-mojo-${it}")
            })

            simulateProjectEndedExecution(profiler, project)
        }

        profiler.close()

        def eventIndex = 0
        for (def projectIndex = 0; projectIndex < NUM_PROJECTS; projectIndex++) {
            def event = sequenceEvents.get(eventIndex)
            eventIndex++
            assertThat(event.project.model.name).isEqualTo(projects.get(projectIndex).model.name)

            for (def mojoIndex = 0; mojoIndex < NUM_MOJOS; mojoIndex++) {
                assertThat(event.mojo.executionId).isEqualTo('testing-mojo-' + mojoIndex.toString())

                event = sequenceEvents.get(eventIndex)
                eventIndex++
            }
        }
    }

    private static Artifact anArtifact(String artifactId) {
        anArtifact('groupId', artifactId, 'jar', '1.0.0')
    }

    private static Artifact anArtifact(String groupId, String artifactId, String type, String version) {
        ArtifactProfiled.of(new DefaultArtifact(groupId, artifactId, type, version))
    }

    private static RepositoryEvent.Builder aRepositoryEvent(RepositoryEvent.EventType type, Artifact artifact) {
        new RepositoryEvent.Builder(new DefaultRepositorySystemSession(), type)
                .setArtifact(artifact)
    }

    private static MavenProject aMavenProject(String name) {
        Model model = new Model()
        model.name = name
        MavenProject project = new MavenProject(model)
        project.groupId = 'groupId'
        project.artifactId = 'artifactId'
        project.version = '1.0'
        project
    }

    private static void simulateArtifactDownloaded(ProfilerEventSpy profiler, Artifact artifact) {
        profiler.onEvent(aRepositoryEvent(ARTIFACT_DOWNLOADING, artifact).build())
        def MAX_RANDOM_DOWNLOAD_TIME = 10
        MILLISECONDS.sleep(random.nextInt() % MAX_RANDOM_DOWNLOAD_TIME)
        profiler.onEvent(aRepositoryEvent(ARTIFACT_DOWNLOADED, artifact).build())
    }

    private static void simulateProjectStartedExecution(ProfilerEventSpy profiler, MavenProject mavenProject) {
        String projectGoal = mavenProject.getDefaultGoal()
        String projectId = mavenProject.getId()
        ExecutionEvent projectStartEvent = aMojoEvent(ExecutionEvent.Type.ProjectStarted,
                                                      projectGoal, projectId,
                                                      mavenProject)
        profiler.onEvent(projectStartEvent)
    }

    private static void simulateProjectEndedExecution(ProfilerEventSpy profiler, MavenProject mavenProject) {
        String projectGoal = mavenProject.getDefaultGoal()
        String projectId = mavenProject.getId()
        ExecutionEvent projectStopEvent = aMojoEvent(ExecutionEvent.Type.ProjectSucceeded,
                                                     projectGoal, projectId,
                                                     mavenProject)
        profiler.onEvent(projectStopEvent)
    }

    private static void simulateMojoExecutedSuccessfully(ProfilerEventSpy profiler,
                                                         MavenProject mavenProject,
                                                         String goal, String id) {
        Plugin plugin = new Plugin()
        MojoExecution mojo = new MojoExecution(plugin, goal, id)

        ExecutionEvent startEvent = aMojoStartEvent(mojo, mavenProject)
        profiler.onEvent(startEvent)

        def MAX_RANDOM_EXECUTION_TIME = 10

        MILLISECONDS.sleep(random.nextInt() % MAX_RANDOM_EXECUTION_TIME)

        ExecutionEvent stopEvent = aMojoStopEvent(mojo, mavenProject)
        profiler.onEvent(stopEvent)
    }

    private static ExecutionEvent aMojoStartedEvent(ExecutionEvent.Type type, MavenProject mavenProject) {
        aMojoEvent(type, new MojoExecution(new Plugin(), 'goal', 'execution.id'), mavenProject)
    }

    private static ExecutionEvent aMojoStartEvent(MojoExecution mojoExecution, MavenProject mavenProject) {
        aMojoEvent(ExecutionEvent.Type.MojoStarted, mojoExecution, mavenProject)
    }

    private static ExecutionEvent aMojoStopEvent(MojoExecution mojoExecution, MavenProject mavenProject) {
        aMojoEvent(ExecutionEvent.Type.MojoSucceeded, mojoExecution, mavenProject)
    }

    private static ExecutionEvent aMojoEvent(ExecutionEvent.Type type,
                                             MojoExecution mojoExecution,
                                             MavenProject mavenProject) {
        new TestExecutionEvent(type, mojoExecution, mavenProject)
    }

    private static ExecutionEvent aMojoEvent(ExecutionEvent.Type type,
                                             String goal, String id,
                                             MavenProject mavenProject) {
        new TestExecutionEvent(type, new MojoExecution(new Plugin(), goal, id), mavenProject)
    }
}

