package fr.jcgay.maven.profiler.reporting.html

import fr.jcgay.maven.profiler.reporting.ReportDirectory
import fr.jcgay.maven.profiler.reporting.template.Data
import org.assertj.core.api.Condition
import org.testng.annotations.Test

import java.util.function.Predicate

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
            .haveAtLeast(1, aProfilerReportHtmlFile())
    }

    private static Condition<String> aProfilerReportHtmlFile() {
        return new Condition<String>(
            { it.startsWith('profiler-report-') && it.endsWith('.html')} as Predicate<String>,
            "a HTML file with name starting with 'profiler-report'")
    }
}
