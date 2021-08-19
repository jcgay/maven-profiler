package fr.jcgay.maven.profiler.reporting

import fr.jcgay.maven.profiler.MavenStubs
import org.apache.maven.project.MavenProject
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import java.text.SimpleDateFormat

import static fr.jcgay.maven.profiler.reporting.ReportFormat.HTML
import static org.assertj.core.api.Assertions.assertThat

class ReportDirectoryTest {

    Date now = new Date()

    @BeforeMethod
    void 'clean property'() {
        System.clearProperty('maven-profiler-report-directory')
    }

    @Test
    void 'write reports in hidden profiler folder'() {
        MavenProject project = MavenStubs.aMavenProject('default-directory')

        def result = new ReportDirectory(project).fileName(now, HTML)

        assertThat(result.path).isEqualTo(".profiler/profiler-report-${new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(now)}.html" as String)
    }

    @Test
    void 'write reports in folder specified in the project properties'() {
        MavenProject project = MavenStubs.aMavenProject('default-directory')
        project.properties['maven-profiler-report-directory'] = 'target/site/'

        def result = new ReportDirectory(project).fileName(now, HTML)

        assertThat(result.path).isEqualTo("target/site/profiler-report-${new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(now)}.html" as String)
    }

    @Test
    void 'write reports in folder specified as system property'() {
        System.setProperty('maven-profiler-report-directory', '/tmp/custom-directory')
        MavenProject project = MavenStubs.aMavenProject('default-directory')

        def result = new ReportDirectory(project).fileName(now, HTML)

        assertThat(result.path).isEqualTo("/tmp/custom-directory/profiler-report-${new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(now)}.html" as String)
    }
}
