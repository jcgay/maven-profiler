package fr.jcgay.maven.profiler.template;

import com.google.common.base.Stopwatch;
import org.apache.maven.plugin.MojoExecution;

import java.util.ArrayList;
import java.util.List;

public class Project {

    private final String name;
    private final Stopwatch time;
    private final List<EntryAndTime<MojoExecution>> mojosWithTime = new ArrayList<EntryAndTime<MojoExecution>>();

    public Project(String name, Stopwatch time) {
        this.name = name;
        this.time = time;
    }

    public void addMojoTime(EntryAndTime<MojoExecution> mojoWithTime) {
        mojosWithTime.add(mojoWithTime);
    }

    public String getName() {
        return name;
    }

    public List<EntryAndTime<MojoExecution>> getMojosWithTime() {
        return mojosWithTime;
    }

    public Stopwatch getTime() {
        return time;
    }

    public String getMillisTimeStamp() {
        return String.valueOf(time.elapsedMillis()) + " ms";
    }

    @Override
    public String toString() {
        return String.format("{%s, %s, totalMojos = %d}", name, getMillisTimeStamp(), mojosWithTime.size());
    }

    public boolean isEqual(Project project) {
        if (name.equals(project.getName())) {
            if (time.equals(project.getTime())) {
                if (mojosWithTime.size() == project.getMojosWithTime().size()) {
                    int index = 0;
                    boolean hasSameMojos = true;
                    for (EntryAndTime<MojoExecution> entryAndTime : mojosWithTime) {
                        EntryAndTime<MojoExecution> entryAndTime2 = project.getMojosWithTime().get(index);

                        if (!entryAndTime.getEntry().equals(entryAndTime2.getEntry()) ||
                            !entryAndTime.getTime().equals(entryAndTime2.getTime())) {
                            hasSameMojos = false;
                            break;
                        }
                        index++;
                    }
                    return hasSameMojos;
                }
            }
        }

        return false;
    }
}
