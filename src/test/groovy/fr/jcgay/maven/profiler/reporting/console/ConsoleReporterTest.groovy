package fr.jcgay.maven.profiler.reporting.console


import fr.jcgay.maven.profiler.KnownElapsedTimeTicker
import fr.jcgay.maven.profiler.reporting.template.Data
import fr.jcgay.maven.profiler.reporting.template.EntryAndTime
import fr.jcgay.maven.profiler.reporting.template.Project
import org.apache.maven.plugin.MojoExecution
import org.assertj.core.api.Assertions
import org.eclipse.aether.artifact.Artifact
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test


class ConsoleReporterTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @BeforeMethod
    void setUp() {
        Locale.setDefault(new Locale("en", "EN"));
        System.setOut(new PrintStream(outContent))
    }

    @AfterMethod
    void tearDown() {
        System.setOut(System.out)
    }

    @Test
    void 'write report to console'() {
       def data = new Data()
       data.setTopProjectName("maven-profiler")
       data.setBuildTime(KnownElapsedTimeTicker.aStopWatchWithElapsedTime(5700))
       data.setGoals("install")
       data.setDate(new Date(2021, 12, 1, 7, 52, 0))
       def module1 = new Project("module1", KnownElapsedTimeTicker.aStopWatchWithElapsedTime(2000))
       module1.addMojoTime(new EntryAndTime<MojoExecution>("Mojo1.1", KnownElapsedTimeTicker.aStopWatchWithElapsedTime(1000)))
       module1.addMojoTime(new EntryAndTime<MojoExecution>("Mojo1.2", KnownElapsedTimeTicker.aStopWatchWithElapsedTime(1000)))
       def module2 = new Project("module2", KnownElapsedTimeTicker.aStopWatchWithElapsedTime(3000))
       module2.addMojoTime(new EntryAndTime<MojoExecution>("Mojo2.1", KnownElapsedTimeTicker.aStopWatchWithElapsedTime(1000)))
       module2.addMojoTime(new EntryAndTime<MojoExecution>("Mojo2.2", KnownElapsedTimeTicker.aStopWatchWithElapsedTime(1000)))
       module2.addMojoTime(new EntryAndTime<MojoExecution>("Mojo2.3", KnownElapsedTimeTicker.aStopWatchWithElapsedTime(1000)))
       data.setProjects(Arrays.asList(module1, module2))

       data.setTotalDownloadTime(KnownElapsedTimeTicker.aStopWatchWithElapsedTime(700))
       data.setDownloads(Arrays.asList(
           new EntryAndTime<Artifact>("artifact1", KnownElapsedTimeTicker.aStopWatchWithElapsedTime(500)),
           new EntryAndTime<Artifact>("artifact2", KnownElapsedTimeTicker.aStopWatchWithElapsedTime(200))))

       new ConsoleReporter()
           .write(data, null)

       Assertions.assertThat(outContent.toString()).contains("╒══════════════════════════════════════════════════════════════════════════════════════════════════════════╕\n" +
           "│ maven-profiler (5.700 μs)                                                                                │\n" +
           "├──────────────────────────────────────────────────────────────────────────────────────────────────────────┤\n" +
           "│ Run install on 3922/01/01 07:52:00 without parameters                                                    │\n" +
           "├──────────────────────────────────────────────────────────────────────────────────────────────────────────┤\n" +
           "│ module1 (2.000 μs)                                                                                       │\n" +
           "├──────────────────────────────────────────────────┬───────────────────────────────────────────────────────┤\n" +
           "│ Plugin execution                                 │ Duration                                              │\n" +
           "├──────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤\n" +
           "│ Mojo1.1                                          │ 1.000 μs                                              │\n" +
           "│ Mojo1.2                                          │ 1.000 μs                                              │\n" +
           "├──────────────────────────────────────────────────┴───────────────────────────────────────────────────────┤\n" +
           "│ module2 (3.000 μs)                                                                                       │\n" +
           "├──────────────────────────────────────────────────┬───────────────────────────────────────────────────────┤\n" +
           "│ Plugin execution                                 │ Duration                                              │\n" +
           "├──────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤\n" +
           "│ Mojo2.1                                          │ 1.000 μs                                              │\n" +
           "│ Mojo2.2                                          │ 1.000 μs                                              │\n" +
           "│ Mojo2.3                                          │ 1.000 μs                                              │\n" +
           "╞══════════════════════════════════════════════════╧═══════════════════════════════════════════════════════╡\n" +
           "│ Artifact Downloading 700.0 ns                                                                            │\n" +
           "├──────────────────────────────────────────────────┬───────────────────────────────────────────────────────┤\n" +
           "│ Artifact                                         │ Duration                                              │\n" +
           "├──────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤\n" +
           "│ artifact1                                        │ 500.0 ns                                              │\n" +
           "├──────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤\n" +
           "│ artifact2                                        │ 200.0 ns                                              │\n" +
           "╘══════════════════════════════════════════════════╧═══════════════════════════════════════════════════════╛")
    }
}
