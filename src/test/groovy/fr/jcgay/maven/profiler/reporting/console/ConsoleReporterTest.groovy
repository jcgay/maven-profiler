package fr.jcgay.maven.profiler.reporting.console


import fr.jcgay.maven.profiler.reporting.template.Data
import fr.jcgay.maven.profiler.reporting.template.EntryAndTime
import fr.jcgay.maven.profiler.reporting.template.Project
import org.apache.maven.model.Plugin
import org.apache.maven.plugin.MojoExecution
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import java.text.SimpleDateFormat

import static fr.jcgay.maven.profiler.KnownElapsedTimeTicker.aStopWatchWithElapsedTime
import static java.util.Collections.singletonList
import static org.assertj.core.api.Assertions.assertThat

class ConsoleReporterTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @BeforeMethod
    void setUp() {
        System.setOut(new PrintStream(outContent))
    }

    @AfterMethod
    void tearDown() {
        System.setOut(System.out)
    }

    @Test
    void 'write report to console'() {
        def date = new Date()
        def project = new Project("project-1", aStopWatchWithElapsedTime(100L))
        project.addMojoTime(new EntryAndTime<MojoExecution>(new MojoExecution(new Plugin(), 'goal', 'execution.id'), aStopWatchWithElapsedTime(50)))
        def data = new Data()
            .setProjects(singletonList(project))
            .setName("test-project")
            .setBuildTime(aStopWatchWithElapsedTime(834L))
            .setGoals("clean verify")
            .setDate(date)

        new ConsoleReporter().write(data, null)

        assertThat(printable(outContent.toString())).isEqualTo(printable(String.format(
            "┌──────────────────────────────────────────────────────────────────────────────┐\n" +
                "│Project: test-project built in 834,0 ns                                       │\n" +
                "│Run clean verify on %s with parameters: {}                   │\n" +
                "└──────────────────────────────────────────────────────────────────────────────┘\n",
            new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(date))))
    }

    static String printable(String string) {
        string.replaceAll(" ", "·").replaceAll( "[\n\r]+", "¬\n" )
    }
}
