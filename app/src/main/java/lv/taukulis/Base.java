package lv.taukulis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static final String GIT_DIR = ".ugit";

    public static String writeTree(Path root, String directory) throws IOException {
        Path path = root.resolve(directory);
        if (!Files.isDirectory(path)) {
            throw new IOException("write-tree called on non-directory");
        }
        Map<Path, List<TreeEntry>> entries = new HashMap<>();
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
                String objectId = Data.hashObject(root, Files.readAllBytes(file));
                entries.get(file.getParent()).add(new TreeEntry("blob", objectId, file.getFileName()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                String objectId = Data.hashObject(root,
                        buildTreeString(entries.remove(dir)).getBytes(StandardCharsets.UTF_8));
                Path parent = dir.getParent();
                if (parent != null && entries.containsKey(parent)) {
                    entries.get(parent).add(new TreeEntry("tree", objectId, dir.getFileName()));
                } else {
                    rootObjectId.set(objectId);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return rootObjectId.get();
    }

    public static void readTree(Path root, String treeObjectId) throws IOException {
        List<TreeEntry> entries = new ArrayList<>();
        parseTree(root, new TreeEntry("tree", treeObjectId, root), entries);
        for (TreeEntry entry : entries) {
            Files.createDirectories(entry.path.getParent());
            Files.write(entry.path, Data.getObject(root, entry.objectId));
        }
    }

    private static void parseTree(Path root, TreeEntry tree, List<TreeEntry> entries) throws IOException {
        var treeString = new String(Data.getObject(root, tree.objectId), StandardCharsets.UTF_8);
        for (String entryString : treeString.split("\n")) {
            var entry = TreeEntry.fromStringRelativeToPath(entryString, tree.path);
            if ("blob".equals(entry.type)) {
                entries.add(entry);
            } else if ("tree".equals(entry.type)) {
                parseTree(root, entry, entries);
            } else {
                throw new RuntimeException(String.format("Unexpected tree entry type '%s'", entry.type));
            }
        }
    }

    private static String buildTreeString(List<TreeEntry> entries) {
        StringBuilder sb = new StringBuilder();
        entries.stream().sorted(Comparator.comparing(TreeEntry::type).thenComparing(TreeEntry::path)).forEach(entry -> sb.append(entry).append("\n"));
        return sb.toString();
    }

    private static boolean isIgnored(Path path) {
        return StreamSupport.stream(path.spliterator(), true).anyMatch(p -> p.toString().equals(GIT_DIR));
    }

    private record TreeEntry(String type, String objectId, Path path) {
        @Override
        public String toString() {
            return type + " " + objectId + " " + path;
        }

        public static TreeEntry fromStringRelativeToPath(String entry, Path baseDir) {
            String[] parts = entry.split(" ", 3);
            if (parts.length < 3) {
                throw new RuntimeException(String.format("Invalid tree entry: '%s'", entry));
            }
            return new TreeEntry(parts[0], parts[1], baseDir.resolve(parts[2]));
        }
    }

}
