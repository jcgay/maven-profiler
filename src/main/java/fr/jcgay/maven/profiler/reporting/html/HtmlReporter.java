package fr.jcgay.maven.profiler.reporting.html;

import com.github.jknack.handlebars.Handlebars;
import fr.jcgay.maven.profiler.reporting.Files;
import fr.jcgay.maven.profiler.reporting.ReportDirectory;
import fr.jcgay.maven.profiler.reporting.Reporter;
import fr.jcgay.maven.profiler.reporting.template.Data;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

import static fr.jcgay.maven.profiler.reporting.ReportFormat.HTML;
import static org.slf4j.LoggerFactory.getLogger;

public class HtmlReporter implements Reporter {

    private static final Logger LOGGER = getLogger(HtmlReporter.class);

    @Override
    public void write(Data data, ReportDirectory directory) {
        String report;

        try {
            report = new Handlebars()
                .compile("report-template")
                .apply(data);
        } catch (IOException e) {
            LOGGER.error("Cannot render profiler report.", e);
            return;
        }

        File file = directory.fileName(data.getDate(), HTML);
        Files.write(file, report);
        LOGGER.info("HTML profiling report has been saved in: {}", file);
    }
}
