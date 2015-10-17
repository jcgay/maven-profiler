package fr.jcgay.maven.profiler;

import fr.jcgay.maven.profiler.reporting.Reporter;
import fr.jcgay.maven.profiler.reporting.html.HtmlReporter;
import fr.jcgay.maven.profiler.reporting.json.JsonReporter;
import fr.jcgay.maven.profiler.sorting.Sorter;
import fr.jcgay.maven.profiler.sorting.execution.ByExecutionOrder;
import fr.jcgay.maven.profiler.sorting.time.ByExecutionTime;

public class Configuration {

    private static final String PROFILE = "profile";
    private static final String PROFILE_FORMAT = "profileFormat";
    private static final String DISABLE_TIME_SORTING = "disableTimeSorting";

    private final boolean isProfiling;
    private final Reporter reporter;
    private final Sorter sorter;

    public Configuration(boolean isProfiling, Reporter reporter, Sorter sorter) {
        this.isProfiling = isProfiling;
        this.reporter = reporter;
        this.sorter = sorter;
    }

    public static Configuration read() {
        return new Configuration(isActive(), chooseReporter(), chooseSorter());
    }

    public boolean isProfiling() {
        return isProfiling;
    }

    public Reporter reporter() {
        return reporter;
    }

    public Sorter sorter() {
        return sorter;
    }

    private static Sorter chooseSorter() {
        if (isSortingActive()) {
            return new ByExecutionTime();
        }
        return new ByExecutionOrder();
    }

    private static Reporter chooseReporter() {
        String formatProperty = System.getProperty(PROFILE_FORMAT);
        if (formatProperty != null && "json".equalsIgnoreCase(formatProperty)) {
            return new JsonReporter();
        }
        return new HtmlReporter();
    }

    private static boolean isSortingActive() {
        String parameter = System.getProperty(DISABLE_TIME_SORTING);
        return parameter == null || "false".equalsIgnoreCase(parameter);
    }

    private static boolean isActive() {
        String parameter = System.getProperty(PROFILE);
        return parameter != null && !"false".equalsIgnoreCase(parameter);
    }
}
