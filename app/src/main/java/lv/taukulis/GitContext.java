package lv.taukulis;

import java.nio.file.Path;

public class GitContext {
    private static Path root;

    public static void setRoot(Path rootPath) {
        root = rootPath;
    }

    public static Path roodDir() {
        if (root == null) {
            throw new RuntimeException("root not set");
        }
        return root;
    }

    public static Path gitDir() {
        return roodDir().resolve(".ugit");
    }
}