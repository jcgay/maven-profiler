package fr.jcgay.maven.profiler.reporting.console;

import fr.jcgay.maven.profiler.reporting.template.Data;

import javax.annotation.Nullable;
import org.stringtemplate.v4.ST;
import com.google.common.base.Function;

public enum ToHumanReadable implements Function<Data, String> {
   INSTANCE;

   @Override
   public String apply(@Nullable Data data) {
      ST st = new ST(template());
      st.add("name", data.getName());
      st.add("buildTime", data.getBuildTime());
      st.add("goals", data.getGoals());
      st.add("formattedDate", data.getFormattedDate());
      st.add("parameters", data.getParameters());
      st.add("projects", data.getProjects());
      st.add("downloads", data.getDownloads());
      st.add("totalDownloadTime", data.getTotalDownloadTime());
      return st.render();
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
