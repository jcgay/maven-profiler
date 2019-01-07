package fr.jcgay.maven.profiler.reporting;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

public final class Files {

    private static final Logger LOGGER = getLogger(Files.class);

    private Files() {
    }

    public static void write(File target, String content) {
        try (FileWriter writer = new FileWriter(target)) {
            writer.write(content);
        } catch (IOException e) {
            LOGGER.error("Cannot write profiler report.", e);
        }
    }
}
