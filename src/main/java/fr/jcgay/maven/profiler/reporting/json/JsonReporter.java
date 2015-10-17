package fr.jcgay.maven.profiler.reporting.json;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import fr.jcgay.maven.profiler.reporting.Files;
import fr.jcgay.maven.profiler.reporting.ReportDirectory;
import fr.jcgay.maven.profiler.reporting.Reporter;
import fr.jcgay.maven.profiler.reporting.template.Data;
import fr.jcgay.maven.profiler.reporting.template.EntryAndTime;
import fr.jcgay.maven.profiler.reporting.template.Project;

import static fr.jcgay.maven.profiler.reporting.ReportFormat.JSON;

public class JsonReporter implements Reporter {

    @Override
    public void write(Data data, ReportDirectory directory) {
        String reportString = getJSONRepresentation(data);

        Files.write(directory.fileName(data.getDate(), JSON), reportString);
    }

    private String getJSONRepresentation(Data context) {
        JsonObject obj = new JsonObject();
        obj.add("name", context.getName());
        obj.add("goals", context.getGoals());
        obj.add("date", context.getFormattedDate());
        obj.add("parameters", context.getParameters().toString());

        JsonArray projectsArr = new JsonArray();
        for (Project project : context.getProjects()) {
            JsonObject projectObj = new JsonObject();
            projectObj.add("project", project.getName());
            projectObj.add("time", project.getMillisTimeStamp());
            JsonArray projectMojosArr = new JsonArray();
            for (EntryAndTime entry : project.getMojosWithTime()) {
                JsonObject projectMojoObj = new JsonObject();
                projectMojoObj.add("mojo", entry.getEntry().toString());
                projectMojoObj.add("time", entry.getMillisTimeStamp());
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
                downloadObj.add("time", download.getMillisTimeStamp());
                downloadsArr.add(downloadObj);
            }
            obj.add("downloads", downloadsArr);
        }

        return obj.toString();
    }
}
