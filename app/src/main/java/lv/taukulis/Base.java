package lv.taukulis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

public class Base {

    private static final String UGIT_DIR = ".ugit";

    public static void writeTree() throws IOException {
        writeTree(".");
    }

    public static void writeTree(String directory) throws IOException {
        Path path = Paths.get(directory);
        if (!Files.isDirectory(path)) {
            throw new IOException("write-tree called on non-directory");
        }
        Map<Path, List<Entry>> entries = new HashMap<>();

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (isIgnored(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                entries.put(dir, new ArrayList<>());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isIgnored(file)) {
                    return FileVisitResult.CONTINUE;
                }
                var entry = new Entry("blob", Data.hashObject(Files.readAllBytes(file)), file.getFileName().toString());
                entries.get(file.getParent()).add(entry);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                StringBuilder sb = new StringBuilder();
                for (var entry : entries.get(dir)) {
                    sb.append(entry);
                    sb.append("\n");
                }
                String objectId = Data.hashObject(sb.toString().getBytes(StandardCharsets.UTF_8));
                Entry entry = new Entry("tree", objectId, dir.getFileName().toString());
                Path parent = dir.getParent();
                if (parent != null) {
                    entries.get(parent).add(entry);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean isIgnored(Path path) {
        return StreamSupport.stream(path.spliterator(), true).anyMatch(p -> p.toString().equals(UGIT_DIR));
    }

    private record Entry(String type, String objectId, String name) {
        @Override
        public String toString() {
            return type + " " + objectId + " " + name;
        }
    }

}
