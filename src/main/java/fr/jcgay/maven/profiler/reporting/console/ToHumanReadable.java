package fr.jcgay.maven.profiler.reporting.console;

import com.google.common.base.Function;
import de.vandermeer.asciitable.AsciiTable;
import fr.jcgay.maven.profiler.reporting.template.Data;
import fr.jcgay.maven.profiler.reporting.template.Project;

public enum ToHumanReadable implements Function<Data, String> {
   INSTANCE;

   @Override
   public String apply(Data data) {
       StringBuilder result = new StringBuilder();
       result.append("Project: " + data.getName() + " built in " + data.getBuildTime());
       result.append(System.lineSeparator());
       result.append("Run " + data.getGoals() + " on " + data.getFormattedDate() + " with parameters: " + data.getParameters());
       result.append(System.lineSeparator());
       result.append("Projects");
       result.append(System.lineSeparator());

       AsciiTable at = new AsciiTable();
       at.addRule();
       for (Project project : data.getProjects()) {
           at.addRow(project.getName(), project.getTime());
           at.addRule();
       }
       return result + at.render();
   }

   private String template() {
      return "==================================================================================================\n" +
            "Project: <name> built in <buildTime>\n" +
            "Run <goals> on <formattedDate> with parameters: <parameters>\n" +
            "==================================================================================================\n" +
            "Projects: \n" +
            "--------------------------------------------------------------------------------------------------\n" +
            "<projects:{project|" +
            "<project.name>  <project.time>\n" +
            "<project.mojosWithTime: {mojo| Plugin execution: <mojo.entry> ran in => <mojo.time>\n}>" +
            "\n}>" +
            "==================================================================================================\n" +
            "Artifact Downloading <totalDownloadTime>\n" +
            "--------------------------------------------------------------------------------------------------\n" +
            "<downloads:{it| Artifact: <it.entry> downloaded in => <it.time>\n}>";
   }
}
