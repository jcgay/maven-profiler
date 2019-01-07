package fr.jcgay.maven.profiler.reporting.json

import fr.jcgay.maven.profiler.reporting.ReportDirectory
import fr.jcgay.maven.profiler.reporting.template.Data
import org.assertj.core.api.Condition
import org.testng.annotations.Test

import java.util.function.Predicate

import static fr.jcgay.maven.profiler.MavenStubs.aMavenTopProject
import static org.assertj.core.api.Assertions.assertThat

class JsonReporterTest {

    @Test
    void 'write json report'() {
        def project = aMavenTopProject('test-project')

        new JsonReporter()
            .write(new Data().setDate(new Date()), new ReportDirectory(project))

        File destination = new File(project.file.parentFile, '.profiler')
        assertThat(destination)
            .exists()
            .isDirectory()
        assertThat(destination.list())
            .haveAtLeast(1, aProfilerReportJsonFile())
    }

    private static Condition<String> aProfilerReportJsonFile() {
        return new Condition<String>(
            { it.startsWith('profiler-report-') && it.endsWith('.json') } as Predicate<String>,
            "a JSON file with name starting with 'profiler-report'")
    }

}
