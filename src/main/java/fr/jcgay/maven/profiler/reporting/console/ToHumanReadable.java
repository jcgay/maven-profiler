package fr.jcgay.maven.profiler.reporting.console;

import com.google.common.base.Function;
import de.vandermeer.asciitable.v2.RenderedTable;
import de.vandermeer.asciitable.v2.V2_AsciiTable;
import de.vandermeer.asciitable.v2.render.V2_AsciiTableRenderer;
import de.vandermeer.asciitable.v2.render.WidthLongestLine;
import de.vandermeer.asciitable.v2.themes.V2_E_TableThemes;
import fr.jcgay.maven.profiler.reporting.template.Data;

import javax.annotation.Nullable;

public enum ToHumanReadable implements Function<Data, String> {
    INSTANCE;

    @Override
    public String apply(@Nullable Data data) {
       final V2_AsciiTable at = new V2_AsciiTable();
       V2_AsciiTableRenderer renderer = new V2_AsciiTableRenderer();
       renderer.setTheme(V2_E_TableThemes.UTF_STRONG_DOUBLE.get());

       renderer.setWidth(new WidthLongestLine().add(50, 100));
       at.addStrongRule();
       at.addRow(null, String.format("%s (%s)", data.getTopProjectName(), data.getBuildTime()));
       at.addRule();
       String params = data.getParameters().isEmpty() ? "without parameters" : String.format("with parameters: %s", data.getParameters());
       at.addRow(null, String.format("Run %s on %s %s", data.getGoals(), data.getFormattedDate(), params));

       data.getProjects().forEach(project -> {
           at.addRule();
           at.addRow(null, String.format("%s (%s)", project.getName(), project.getTime()));
           at.addRule();
           if (project.getMojosWithTime() != null && !project.getMojosWithTime().isEmpty()) {
              at.addRow("Plugin execution", "Duration");
              at.addRule();
              project.getMojosWithTime().forEach(mojo -> {
                  at.addRow(String.format("%s", mojo.getEntry()), mojo.getTime());
              });
           }
       });
       if (data.isDownloadSectionDisplayed()) {
           at.addStrongRule();
           at.addRow(null,String.format("Artifact Downloading %s", data.getTotalDownloadTime()));
           at.addRule();
           at.addRow("Artifact", "Duration");

           data.getDownloads().forEach(download -> {
              at.addRule();
              at.addRow(download.getEntry(), download.getTime());
           });
       }
       at.addStrongRule();

       RenderedTable renderedTable = renderer.render(at);
       return renderedTable.toString();
    }
}
