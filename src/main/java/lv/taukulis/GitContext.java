package lv.taukulis;

import java.nio.file.Files;
import java.nio.file.Path;

public class GitContext {
    public static final String GIT_DIR = ".ugit";

    private static Path root;

    public static void setRoot() {
        root = Path.of(System.getProperty("user.dir"));
        while (root != null) {
            if (Files.exists(root.resolve(GIT_DIR))) {
                return;
            }
            root = root.getParent();
        }
    }

    public static Path rootDir() {
        return root;
    }

    public static Path gitDir() {
        if (root == null) {
            throw new RuntimeException("Not in ugit repository");
        }
        return root.resolve(GIT_DIR);
    }
}