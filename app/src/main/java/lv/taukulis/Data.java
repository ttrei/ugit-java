package lv.taukulis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Data {

    private static final String GIT_DIR = ".ugit";

    public static void init() {

        Path ugitPath = Paths.get(GIT_DIR);
        try {
            Files.createDirectory(ugitPath);
            System.out.println("Initialized ugit repository at " + ugitPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to create directory: " + e.getMessage());
        }
    }
}
