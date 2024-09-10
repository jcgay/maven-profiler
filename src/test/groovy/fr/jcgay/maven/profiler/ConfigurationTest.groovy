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

    @DataProvider
    Object[][] 'active profile properties'() {
        [[""], ["true"], ["Profile Name"]]
    }

    @Test(dataProvider = 'active profile properties')
    void 'indicate that profiling is active'(String profileValue) {
        System.setProperty('profile', profileValue)

        def result = Configuration.read()

        assertThat(result.isProfiling()).isTrue()
    }

    @DataProvider
    Object[][] 'profile names'() {
        [["", ""], ["true", ""], ["Profile Name", "Profile Name"]]
    }

    @Test(dataProvider = 'profile names')
    void 'keeps profile name'(String profileValue, String expectedName) {
        System.setProperty('profile', profileValue)

        def result = Configuration.read()

        assertThat(result.profileName()).isEqualTo(expectedName)
    }

    @DataProvider
    Object[][] 'html format'() {
        [['html'], ['HTML'], ['HtmL']]
    }

    @Test(dataProvider = 'html format')
    void 'report format is html'(String format) {
        System.setProperty('profileFormat', format)

        def result = Configuration.read()

        assertThat(result.reporter().delegates).extracting("class").containsExactly(HtmlReporter)
    }

    @DataProvider
    Object[][] 'json format'() {
        [['json'], ['JSON'], ['jSoN']]
    }

    @Test(dataProvider = 'json format')
    void 'report format is json'(String format) {
        System.setProperty('profileFormat', format)

        def result = Configuration.read()

        assertThat(result.reporter().delegates).extracting("class").containsExactly(JsonReporter)
    }

    @DataProvider
    Object[][] 'two formats'() {
        [['json,html'], ['JSON,HTML'], ['jSoN,HtMl']]
    }

    @Test(dataProvider = 'two formats')
    void 'two report formats'(String format) {
        System.setProperty('profileFormat', format)

        def result = Configuration.read()

        assertThat(result.reporter().delegates).extracting("class").containsExactly(JsonReporter, HtmlReporter)
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
        assertThat(result.profileName()).isEmpty()
        assertThat(result.reporter().delegates).extracting("class").containsExactly(HtmlReporter)
        assertThat(result.sorter()).isExactlyInstanceOf(ByExecutionTime)
    }

    @Test
    void 'disable parameter report when hideParameters is unset'() {
        System.clearProperty(Configuration.DISABLE_PARAMETERS_REPORT)

        def result = Configuration.read()

        assertThat(result.shouldPrintParameters()).isFalse()
    }

    @Test
    void 'disable parameter report when hideParameters is true'() {
        System.setProperty(Configuration.DISABLE_PARAMETERS_REPORT, Boolean.TRUE.toString())

        def result = Configuration.read()

        assertThat(result.shouldPrintParameters()).isFalse()
    }

    @Test
    void 'enable parameter report when hideParameters is false'() {
        System.setProperty(Configuration.DISABLE_PARAMETERS_REPORT, Boolean.FALSE.toString())

        def result = Configuration.read()

        assertThat(result.shouldPrintParameters()).isTrue()
    }
}
