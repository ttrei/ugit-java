package lv.taukulis;

import java.nio.file.Path;

public class GitContext {
    public static final String GIT_DIR = ".ugit";

    private static Path root;

    public static void setRoot(Path rootPath) {
        root = rootPath;
    }

    public static Path rootDir() {
        if (root == null) {
            throw new RuntimeException("root unset");
        }
        return root;
    }

    public static Path gitDir() {
        return rootDir().resolve(GIT_DIR);
    }
}