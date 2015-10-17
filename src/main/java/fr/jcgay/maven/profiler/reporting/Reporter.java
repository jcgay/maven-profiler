package fr.jcgay.maven.profiler.reporting;

import fr.jcgay.maven.profiler.reporting.template.Data;

public interface Reporter {

    void write(Data data, ReportDirectory directory);
}
