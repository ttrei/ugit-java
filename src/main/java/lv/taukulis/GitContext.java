package lv.taukulis;

import java.nio.file.Files;
import java.nio.file.Path;

public class GitContext {
    public static final String GIT_DIR = ".ugit";

    private static Path root;

    public static void setRoot() {
        var currentDir = Path.of(System.getProperty("user.dir"));
        while (currentDir != null) {
            if (Files.exists(currentDir.resolve(GIT_DIR))) {
                root = currentDir;
                return;
            }
            currentDir = currentDir.getParent();
        }
    }

    public static Path rootDir() {
        return root;
    }

    public static Path gitDir() {
        return gitDir(false);
    }

    public static Path gitDir(boolean allowMissing) {
        if (!allowMissing && root == null) {
            throw new RuntimeException("Not in ugit repository");
        }
        return root.resolve(GIT_DIR);
    }
}