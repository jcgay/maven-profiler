package fr.jcgay.maven.profiler.reporting.console

import fr.jcgay.maven.profiler.reporting.template.Data
import org.assertj.core.api.Assertions
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import java.text.SimpleDateFormat

import static fr.jcgay.maven.profiler.MavenStubs.aMavenTopProject

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
        def project = aMavenTopProject('test-project')

        def date = new Date()
        new ConsoleReporter()
            .write(new Data().setDate(date), null)

        Assertions.assertThat(outContent.toString()).contains("Project:  built in")
        Assertions.assertThat(outContent.toString()).contains("Run  on " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(date) + " with parameters:")
        Assertions.assertThat(outContent.toString()).contains("Projects: ")
        Assertions.assertThat(outContent.toString()).contains("Artifact Downloading")
    }
}
