package fr.jcgay.maven.profiler.reporting.json;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import fr.jcgay.maven.profiler.reporting.Files;
import fr.jcgay.maven.profiler.reporting.ReportDirectory;
import fr.jcgay.maven.profiler.reporting.Reporter;
import fr.jcgay.maven.profiler.reporting.template.Data;
import fr.jcgay.maven.profiler.reporting.template.EntryAndTime;
import fr.jcgay.maven.profiler.reporting.template.Project;
import org.slf4j.Logger;

import java.io.File;

import static fr.jcgay.maven.profiler.reporting.Format.ms;
import static fr.jcgay.maven.profiler.reporting.ReportFormat.JSON;
import static org.slf4j.LoggerFactory.getLogger;

public class JsonReporter implements Reporter {

    private static final Logger LOGGER = getLogger(JsonReporter.class);

    @Override
    public void write(Data data, ReportDirectory directory) {
        String reportString = getJSONRepresentation(data);

        File file = directory.fileName(data.getDate(), JSON);
        Files.write(file, reportString);
        LOGGER.info("JSON profiling report has been saved in: {}", file);
    }

    private String getJSONRepresentation(Data context) {
        JsonObject obj = new JsonObject();
        obj.add("name", context.getName());
        obj.add("time", ms(context.getBuildTime()));
        obj.add("goals", context.getGoals());
        obj.add("date", context.getFormattedDate());
        obj.add("parameters", context.getParameters().toString());

        JsonArray projectsArr = new JsonArray();
        for (Project project : context.getProjects()) {
            JsonObject projectObj = new JsonObject();
            projectObj.add("project", project.getName());
            projectObj.add("time", ms(project.getTime()));
            JsonArray projectMojosArr = new JsonArray();
            for (EntryAndTime entry : project.getMojosWithTime()) {
                JsonObject projectMojoObj = new JsonObject();
                projectMojoObj.add("mojo", entry.getEntry().toString());
                projectMojoObj.add("time", ms(entry.getTime()));
                projectMojosArr.add(projectMojoObj);
            }
            projectObj.add("mojos", projectMojosArr);
            projectsArr.add(projectObj);
        }
        obj.add("projects", projectsArr);

        if (context.isDownloadSectionDisplayed()) {
            JsonArray downloadsArr = new JsonArray();
            for (EntryAndTime download : context.getDownloads()) {
                JsonObject downloadObj = new JsonObject();
                downloadObj.add("download", download.getEntry().toString());
                downloadObj.add("time", ms(download.getTime()));
                downloadsArr.add(downloadObj);
            }
            obj.add("downloads", downloadsArr);
        }

        return obj.toString();
    }
}
