package fr.jcgay.maven.profiler.reporting;

import org.apache.maven.project.MavenProject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReportDirectory {

    private final File directory;

    public ReportDirectory(MavenProject project) {
        directory = new File(project.getBasedir(), ".profiler");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new RuntimeException("Cannot create file to write profiler report: " + directory);
        }
    }

    public File fileName(Date date, ReportFormat format) {
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(date);
        return new File(directory, "profiler-report-" + formattedDate + "." + format.extension());
    }
}
