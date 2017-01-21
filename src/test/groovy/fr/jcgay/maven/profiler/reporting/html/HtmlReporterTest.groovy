package fr.jcgay.maven.profiler.reporting.html

import fr.jcgay.maven.profiler.reporting.ReportDirectory
import fr.jcgay.maven.profiler.reporting.template.Data
import org.testng.annotations.Test

import static fr.jcgay.maven.profiler.MavenStubs.aMavenTopProject
import static org.assertj.core.api.Assertions.assertThat

class HtmlReporterTest {

    @Test
    void 'write html report'() {
        def project = aMavenTopProject('test-project')

        new HtmlReporter()
            .write(new Data().setDate(new Date()), new ReportDirectory(project))

        File destination = new File(project.file.parentFile, '.profiler')
        assertThat(destination)
            .exists()
            .isDirectory()
        assertThat(destination.list())
            .haveAtLeast(1, { it.startsWith('profiler-report-') && it.endsWith('.html') })
    }
}
