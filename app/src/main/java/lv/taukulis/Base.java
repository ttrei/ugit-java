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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

public class Base {

    private static final String UGIT_DIR = ".ugit";

    public static String writeTree() throws IOException {
        return writeTree(".");
    }

    public static String writeTree(String directory) throws IOException {
        Path path = Paths.get(directory);
        if (!Files.isDirectory(path)) {
            throw new IOException("write-tree called on non-directory");
        }
        Map<Path, List<Entry>> entries = new HashMap<>();
        AtomicReference<String> rootObjectId = new AtomicReference<>();

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
                String objectId = Data.hashObject(Files.readAllBytes(file));
                entries.get(file.getParent()).add(new Entry("blob", objectId, file.getFileName().toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                String objectId = Data.hashObject(buildTree(entries.remove(dir)).getBytes(StandardCharsets.UTF_8));
                Path parent = dir.getParent();
                if (parent != null && entries.containsKey(parent)) {
                    entries.get(parent).add(new Entry("tree", objectId, dir.getFileName().toString()));
                } else {
                    rootObjectId.set(objectId);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return rootObjectId.get();
    }

    private static String buildTree(List<Entry> entries) {
        StringBuilder sb = new StringBuilder();
        entries.stream().sorted(Comparator.comparing(Entry::type).thenComparing(Entry::name)).forEach(entry -> sb.append(entry).append("\n"));
        return sb.toString();
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
