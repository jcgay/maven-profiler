package fr.jcgay.maven.profiler.reporting.console


import fr.jcgay.maven.profiler.reporting.template.Data
import fr.jcgay.maven.profiler.reporting.template.EntryAndTime
import fr.jcgay.maven.profiler.reporting.template.Project
import groovy.transform.CompileStatic
import org.apache.maven.plugin.MojoExecution
import org.eclipse.aether.artifact.Artifact
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static fr.jcgay.maven.profiler.KnownElapsedTimeTicker.aStopWatchWithElapsedTime
import static fr.jcgay.maven.profiler.MavenStubs.aMojoExecution
import static fr.jcgay.maven.profiler.MavenStubs.anArtifact
import static org.assertj.core.api.Assertions.assertThat

@CompileStatic
class ConsoleReporterTest {

    private ByteArrayOutputStream outContent

    private Locale defaultLocale
    private PrintStream defaultOut

    @BeforeMethod
    void setUp() {
        outContent = new ByteArrayOutputStream()

        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)

        defaultOut = System.out
        System.setOut(new PrintStream(outContent))
    }

    @AfterMethod
    void tearDown() {
        Locale.setDefault(defaultLocale)
        System.setOut(defaultOut)
    }

    @Test
    void 'write report to console'() {
        def data = new Data()
        data.setTopProjectName("maven-profiler")
        data.setBuildTime(aStopWatchWithElapsedTime(5700))
        data.setGoals("install")
        data.setDate(new Date(2021, 12, 1, 7, 52, 0))
        def module1 = new Project("module1", aStopWatchWithElapsedTime(2000))
        module1.addMojoTime(new EntryAndTime<MojoExecution>(aMojoExecution("Mojo1.1"), aStopWatchWithElapsedTime(1000)))
        module1.addMojoTime(new EntryAndTime<MojoExecution>(aMojoExecution("Mojo1.2"), aStopWatchWithElapsedTime(1000)))
        def module2 = new Project("module2", aStopWatchWithElapsedTime(3000))
        module2.addMojoTime(new EntryAndTime<MojoExecution>(aMojoExecution("Mojo2.1"), aStopWatchWithElapsedTime(1000)))
        module2.addMojoTime(new EntryAndTime<MojoExecution>(aMojoExecution("Mojo2.2"), aStopWatchWithElapsedTime(1000)))
        module2.addMojoTime(new EntryAndTime<MojoExecution>(aMojoExecution("Mojo2.3"), aStopWatchWithElapsedTime(1000)))
        data.setProjects(Arrays.asList(module1, module2))

        data.setTotalDownloadTime(aStopWatchWithElapsedTime(700))
        data.setDownloads(Arrays.asList(
            new EntryAndTime<Artifact>(anArtifact("artifact1"), aStopWatchWithElapsedTime(500)),
            new EntryAndTime<Artifact>(anArtifact("artifact2"), aStopWatchWithElapsedTime(200))))

        new ConsoleReporter()
            .write(data, null)

        assertThat(outContent.toString()).contains("╒══════════════════════════════════════════════════════════════════════════════════════════════════════════╕\n" +
            "│ maven-profiler (5.700 μs)                                                                                │\n" +
            "├──────────────────────────────────────────────────────────────────────────────────────────────────────────┤\n" +
            "│ Run install on 3922/01/01 07:52:00 without parameters                                                    │\n" +
            "├──────────────────────────────────────────────────────────────────────────────────────────────────────────┤\n" +
            "│ module1 (2.000 μs)                                                                                       │\n" +
            "├──────────────────────────────────────────────────┬───────────────────────────────────────────────────────┤\n" +
            "│ Plugin execution                                 │ Duration                                              │\n" +
            "├──────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤\n" +
            "│ {execution: Mojo1.1}                             │ 1.000 μs                                              │\n" +
            "│ {execution: Mojo1.2}                             │ 1.000 μs                                              │\n" +
            "├──────────────────────────────────────────────────┴───────────────────────────────────────────────────────┤\n" +
            "│ module2 (3.000 μs)                                                                                       │\n" +
            "├──────────────────────────────────────────────────┬───────────────────────────────────────────────────────┤\n" +
            "│ Plugin execution                                 │ Duration                                              │\n" +
            "├──────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤\n" +
            "│ {execution: Mojo2.1}                             │ 1.000 μs                                              │\n" +
            "│ {execution: Mojo2.2}                             │ 1.000 μs                                              │\n" +
            "│ {execution: Mojo2.3}                             │ 1.000 μs                                              │\n" +
            "╞══════════════════════════════════════════════════╧═══════════════════════════════════════════════════════╡\n" +
            "│ Artifact Downloading 700.0 ns                                                                            │\n" +
            "├──────────────────────────────────────────────────┬───────────────────────────────────────────────────────┤\n" +
            "│ Artifact                                         │ Duration                                              │\n" +
            "├──────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤\n" +
            "│ groupId:artifact1:jar:1.0                        │ 500.0 ns                                              │\n" +
            "├──────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤\n" +
            "│ groupId:artifact2:jar:1.0                        │ 200.0 ns                                              │\n" +
            "╘══════════════════════════════════════════════════╧═══════════════════════════════════════════════════════╛")
    }
}
