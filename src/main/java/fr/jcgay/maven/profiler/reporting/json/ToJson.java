package fr.jcgay.maven.profiler.reporting.json;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.base.Function;
import fr.jcgay.maven.profiler.reporting.template.Data;
import fr.jcgay.maven.profiler.reporting.template.EntryAndTime;
import fr.jcgay.maven.profiler.reporting.template.Project;

import javax.annotation.Nullable;

import static fr.jcgay.maven.profiler.reporting.Format.ms;

public enum ToJson implements Function<Data, JsonObject> {
    INSTANCE;

    @Override
    public JsonObject apply(@Nullable Data context) {
        JsonObject json = new JsonObject();
        json.add("name", context.getTopProjectName());
        json.add("profile_name", context.getProfileName());
        json.add("time", ms(context.getBuildTime()));
        json.add("goals", context.getGoals());
        json.add("date", context.getFormattedDate());
        json.add("parameters", context.getParameters().toString());

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
        json.add("projects", projectsArr);

        if (context.isDownloadSectionDisplayed()) {
            JsonArray downloadsArr = new JsonArray();
            for (EntryAndTime download : context.getDownloads()) {
                JsonObject downloadObj = new JsonObject();
                downloadObj.add("download", download.getEntry().toString());
                downloadObj.add("time", ms(download.getTime()));
                downloadsArr.add(downloadObj);
            }
            json.add("downloads", downloadsArr);
        }

        return json;
    }
}
