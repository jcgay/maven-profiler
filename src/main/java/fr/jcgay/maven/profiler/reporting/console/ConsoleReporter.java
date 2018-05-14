package fr.jcgay.maven.profiler.reporting.console;

import fr.jcgay.maven.profiler.reporting.ReportDirectory;
import fr.jcgay.maven.profiler.reporting.Reporter;
import fr.jcgay.maven.profiler.reporting.template.Data;

public class ConsoleReporter implements Reporter {

   @Override
   public void write(Data data, ReportDirectory directory) {
      System.out.println(ToHumanReadable.INSTANCE.apply(data));
   }
}
