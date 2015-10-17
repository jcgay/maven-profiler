package fr.jcgay.maven.profiler

import fr.jcgay.maven.profiler.reporting.html.HtmlReporter
import fr.jcgay.maven.profiler.reporting.json.JsonReporter
import fr.jcgay.maven.profiler.sorting.execution.ByExecutionOrder
import fr.jcgay.maven.profiler.sorting.time.ByExecutionTime
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import static org.assertj.core.api.Assertions.assertThat

class ConfigurationTest {

    @BeforeMethod
    void 'clean system properties'() {
        System.clearProperty('profile')
        System.clearProperty('profileFormat')
        System.clearProperty('disableTimeSorting')
    }

    @Test
    void 'indicate that profiling is active'() {
        System.setProperty('profile', 'true')

        def result = Configuration.read()

        assertThat(result.isProfiling()).isTrue()
    }

    @DataProvider
    Object[][] 'html format'() {
        [['html'], ['HTML'], ['HtmL']]
    }

    @Test(dataProvider = 'html format')
    void 'report format is html'(String format) {
        System.setProperty('profileFormat', format)

        def result = Configuration.read()

        assertThat(result.reporter()).isExactlyInstanceOf(HtmlReporter)
    }

    @DataProvider
    Object[][] 'json format'() {
        [['json'], ['JSON'], ['jSoN']]
    }

    @Test(dataProvider = 'json format')
    void 'report format is json'(String format) {
        System.setProperty('profileFormat', format)

        def result = Configuration.read()

        assertThat(result.reporter()).isExactlyInstanceOf(JsonReporter)
    }

    @Test
    void 'do not sort result, keep execution order'() {
        System.setProperty('disableTimeSorting', 'true')

        def result = Configuration.read()

        assertThat(result.sorter()).isExactlyInstanceOf(ByExecutionOrder)
    }

    @Test
    void 'default configuration when no property has been set'() {
        def result = Configuration.read()

        assertThat(result.isProfiling()).isFalse()
        assertThat(result.reporter()).isExactlyInstanceOf(HtmlReporter)
        assertThat(result.sorter()).isExactlyInstanceOf(ByExecutionTime)
    }
}
