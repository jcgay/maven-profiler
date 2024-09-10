package fr.jcgay.maven.profiler;

import com.google.common.annotations.VisibleForTesting;
import fr.jcgay.maven.profiler.reporting.CompositeReporter;
import fr.jcgay.maven.profiler.reporting.Reporter;
import fr.jcgay.maven.profiler.reporting.console.ConsoleReporter;
import fr.jcgay.maven.profiler.reporting.html.HtmlReporter;
import fr.jcgay.maven.profiler.reporting.json.JsonReporter;
import fr.jcgay.maven.profiler.sorting.Sorter;
import fr.jcgay.maven.profiler.sorting.execution.ByExecutionOrder;
import fr.jcgay.maven.profiler.sorting.time.ByExecutionTime;

import static com.google.common.base.Functions.compose;
import static com.google.common.base.Functions.forMap;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;
import static java.util.Arrays.asList;

import java.util.List;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

public class Configuration {

    private static final String PROFILE = "profile";
    private static final String PROFILE_FORMAT = "profileFormat";
    private static final String DISABLE_TIME_SORTING = "disableTimeSorting";
    @VisibleForTesting
    static final String DISABLE_PARAMETERS_REPORT = "hideParameters";

    private static final Function<String,Reporter> reporters =  compose(forMap(ImmutableMap.<String,Reporter>builder()
    		.put("html", new HtmlReporter())
    		.put("json", new JsonReporter())
    		.put("console", new ConsoleReporter())
    		.build()), String::toLowerCase);

    private final boolean isProfiling;
    private final String profileName;
    private final Reporter reporter;
    private final Sorter sorter;
    private final boolean shouldPrintParameters;

    public Configuration(boolean isProfiling, String profileName, Reporter reporter, Sorter sorter, boolean shouldPrintParameters) {
        this.isProfiling = isProfiling;
        this.profileName = checkNotNull(profileName);
        this.reporter = reporter;
        this.sorter = sorter;
        this.shouldPrintParameters = shouldPrintParameters;
    }

    public static Configuration read() {
        return new Configuration(isActive(), getProfileName(), chooseReporter(), chooseSorter(), hasParametersReportEnabled());
    }

    public boolean isProfiling() {
        return isProfiling;
    }

    /** Returns the profile name. Never {@code null}, may be empty if not provided. */
    public String profileName() {
        return profileName;
    }

    public Reporter reporter() {
        return reporter;
    }

    public Sorter sorter() {
        return sorter;
    }

    public boolean shouldPrintParameters() {
        return shouldPrintParameters;
    }

    private static boolean hasParametersReportEnabled() {
        // Inversion of logic: System property is called "hideParameters", but method returns the inverted result
        // If the parameter is true or not set, the report is not printed (see issue 224)
        return !Boolean.parseBoolean(System.getProperty(DISABLE_PARAMETERS_REPORT, "true"));
    }

    private static Sorter chooseSorter() {
        if (isSortingActive()) {
            return new ByExecutionTime();
        }
        return new ByExecutionOrder();
    }

    private static Reporter chooseReporter() {
        List<String> formats = asList(System.getProperty(PROFILE_FORMAT, "html").split(","));
        return new CompositeReporter(transform(formats, reporters));
    }

    private static boolean isSortingActive() {
        String parameter = System.getProperty(DISABLE_TIME_SORTING);
        return parameter == null || "false".equalsIgnoreCase(parameter);
    }

    private static String getProfileName() {
        String profile = System.getProperty(PROFILE, "");
        if (profile.equals("true")) {
            // Use empty name if the property is specified as `-Dprofile`
            return "";
        }
        return profile;
    }

    private static boolean isActive() {
        String parameter = System.getProperty(PROFILE);
        return parameter != null && !"false".equalsIgnoreCase(parameter);
    }
}
