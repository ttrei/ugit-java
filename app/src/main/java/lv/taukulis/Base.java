package lv.taukulis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                String objectId = Data.hashObject(root, Files.readAllBytes(file), "blob");
                entries.get(file.getParent()).add(new TreeEntry("blob", objectId, file.getFileName()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                List<TreeEntry> dirEntries = entries.remove(dir);
                if (dirEntries.isEmpty()) {
                    // Don't track empty directories.
                    return FileVisitResult.CONTINUE;
                }
                String treeObjectId = Data.hashObject(root, buildTreeString(dirEntries).getBytes(), "tree");
                Path parent = dir.getParent();
                if (parent != null && entries.containsKey(parent)) {
                    entries.get(parent).add(new TreeEntry("tree", treeObjectId, dir.getFileName()));
                } else {
                    rootObjectId.set(treeObjectId);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return rootObjectId.get();
    }

    public static void readTree(Path root, String treeObjectId) throws IOException {
        var tree = new TreeEntry("tree", treeObjectId, root);
        List<TreeEntry> entries = new ArrayList<>();
        gatherTreeEntries(root, tree, entries);
        // TODO: Remove the current tree instead (when pointer to current tree implemented).
        unreadTree(root, tree);
        for (TreeEntry entry : entries) {
            Files.createDirectories(entry.path.getParent());
            Files.write(entry.path, Data.getObject(root, entry.objectId));
        }
    }

    public static String commit(Path root, String message) throws IOException {
        String treeObjectId = writeTree(root, "");
        var sb = new StringBuilder("tree ").append(treeObjectId).append("\n");
        Data.getHead(root).ifPresent(head -> sb.append("parent ").append(head));
        sb.append("\n").append(message).append("\n");
        String commitObjectId = Data.hashObject(root, sb.toString().getBytes(), "commit");
        Data.setHead(root, commitObjectId);
        return commitObjectId;
    }

    /**
     * Remove files of given tree from filesystem.
     * Keep untracked files/directories.
     */
    private static void unreadTree(Path root, TreeEntry tree) throws IOException {
        List<TreeEntry> entries = new ArrayList<>();
        gatherTreeEntries(root, tree, entries);
        Set<Path> directories = new HashSet<>();
        // Remove files.
        for (TreeEntry entry : entries) {
            try {
                Files.delete(entry.path);
            } catch (NoSuchFileException ignored) {
            }
            directories.add(entry.path.getParent());
        }
        directories.remove(root);
        // Remove empty directories deepest-first to remove as much as we can, given there may be untracked files in
        // the tree.
        List<Path> sortedDirectories =
                directories.stream().sorted(Comparator.comparingInt(Path::getNameCount).reversed()).toList();
        for (Path directory : sortedDirectories) {
            try {
                Files.delete(directory);
            } catch (DirectoryNotEmptyException | NoSuchFileException ignored) {
            }
        }
    }

    private static List<TreeEntry> parseTree(Path root, TreeEntry tree) throws IOException {
        if (!"tree".equals(tree.type)) {
            throw new RuntimeException(String.format("Expected 'tree' object, got '%s' (%s)", tree.type,
                    tree.objectId));
        }
        var treeString = new String(Data.getObject(root, tree.objectId, "tree"), StandardCharsets.UTF_8);
        return Arrays.stream(treeString.split("\n"))
                .map(entryString -> TreeEntry.fromStringRelativeToPath(entryString, tree.path))
                .toList();
    }

    private static void gatherTreeEntries(Path root, TreeEntry tree, List<TreeEntry> entries) throws IOException {
        for (var entry : parseTree(root, tree)) {
            if ("blob".equals(entry.type)) {
                entries.add(entry);
            } else if ("tree".equals(entry.type)) {
                gatherTreeEntries(root, entry, entries);
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
