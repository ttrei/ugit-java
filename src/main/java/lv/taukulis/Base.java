package lv.taukulis;

import java.io.IOException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

import static lv.taukulis.Ref.HEAD;

public class Base {

    public static String writeTree(String directory) throws IOException {
        Path path = GitContext.rootDir().resolve(directory);
        if (!Files.isDirectory(path)) {
            throw new IOException("write-tree called on non-directory");
        }
        Map<Path, List<TreeEntry>> dirEntries = new HashMap<>();
        AtomicReference<String> rootTreeId = new AtomicReference<>();

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (isIgnored(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                dirEntries.put(dir, new ArrayList<>());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isIgnored(file)) {
                    return FileVisitResult.CONTINUE;
                }
                String id = Data.hashObject(Files.readAllBytes(file), ObjectType.BLOB);
                dirEntries.get(file.getParent()).add(new TreeEntry(ObjectType.BLOB, id, file.getFileName().toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                var tree = new Tree(dirEntries.remove(dir));
                if (tree.entries.isEmpty()) {
                    // Don't track empty directories.
                    return FileVisitResult.CONTINUE;
                }
                String treeId = Data.hashObject(tree.toString().getBytes(), ObjectType.TREE);
                Path parentDir = dir.getParent();
                if (parentDir != null && dirEntries.containsKey(parentDir)) {
                    dirEntries.get(parentDir).add(new TreeEntry(ObjectType.TREE, treeId, dir.getFileName().toString()));
                } else {
                    rootTreeId.set(treeId);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return rootTreeId.get();
    }

    public static void readTree(String treeId) throws IOException {
        var tree = Tree.fromId(treeId);
        Map<Path, String> blobs = tree.getAllBlobs(GitContext.rootDir());
        String headCommitId = Ref.get(HEAD).map(Ref::commitId).orElse(null);
        if (headCommitId != null) {
            unreadTree(Commit.fromId(headCommitId).treeId);
        }
        for (Map.Entry<Path, String> entry : blobs.entrySet()) {
            Path path = entry.getKey();
            String blobId = entry.getValue();
            Files.createDirectories(path.getParent());
            Files.write(path, Data.getObject(blobId, ObjectType.BLOB));
        }
    }

    public static String commit(String message) throws IOException {
        String treeObjectId = writeTree("");
        String parentCommitId = Ref.get(HEAD).map(Ref::commitId).orElse(null);
        var commit = new Commit(treeObjectId, parentCommitId, message);
        String commitId = Data.hashObject(commit.toString().getBytes(), ObjectType.COMMIT);
        Ref.update(HEAD, commitId);
        return commitId;
    }

    public static Commit getCommit(String commitId) throws IOException {
        return Commit.fromId(commitId);
    }

    public static void checkout(String commitId) throws IOException {
        Commit commit = getCommit(commitId);
        readTree(commit.treeId);
        Ref.update(HEAD, commitId);
    }

    public static void tag(String name, String commitId) throws IOException {
        Ref.update(Ref.TAGS + name, commitId);
    }

    public static void createBranch(String name, String commitId) throws IOException {
        Ref.update(Ref.HEADS + name, commitId);
    }

    public static SequencedMap<String, Commit> getReachableCommits(Iterable<String> startingCommitIds) throws IOException {
        SequencedMap<String, Commit> reachable = new LinkedHashMap<>();
        List<String> toVisit = new ArrayList<>();
        startingCommitIds.forEach(toVisit::add);
        Set<String> visited = new HashSet<>();
        while (!toVisit.isEmpty()) {
            String commitId = toVisit.removeFirst();
            if (visited.contains(commitId)) {
                continue;
            }
            visited.add(commitId);
            Commit commit = getCommit(commitId);
            reachable.put(commitId, commit);
            if (commit.parentId != null) {
                toVisit.add(commit.parentId);
            }
        }
        return reachable;
    }

    /**
     * Remove files of given tree from filesystem.
     * Keep untracked files/directories.
     */
    private static void unreadTree(String treeId) throws IOException {
        var tree = Tree.fromId(treeId);
        Map<Path, String> blobs = tree.getAllBlobs(GitContext.rootDir());
        Set<Path> directories = new HashSet<>();
        // Remove files.
        for (Path file : blobs.keySet()) {
            try {
                Files.delete(file);
            } catch (NoSuchFileException ignored) {
            }
            directories.add(file.getParent());
        }
        directories.remove(GitContext.rootDir());
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

    private static boolean isIgnored(Path path) {
        return StreamSupport.stream(path.spliterator(), true).anyMatch(p -> p.toString().equals(GitContext.GIT_DIR));
    }

    private record Tree(List<TreeEntry> entries) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            entries.stream()
                    .sorted(Comparator.comparing(TreeEntry::name))
                    .forEach(entry -> sb.append(entry).append("\n"));
            return sb.toString();
        }

        public static Tree fromId(String treeId) throws IOException {
            List<TreeEntry> entries = Arrays.stream(Data.getObjectString(treeId, ObjectType.TREE).split("\n"))
                    .map(TreeEntry::fromString)
                    .toList();
            return new Tree(entries);
        }

        /**
         * @return Map (blob path) -> (blob object id), containing all blobs in this tree and subtrees.
         * Paths are resolved relative to baseDir.
         */
        public Map<Path, String> getAllBlobs(Path baseDir) {
            Map<Path, String> blobs = new HashMap<>();
            entries.stream()
                    .filter(entry -> entry.type.equals(ObjectType.BLOB))
                    .forEach(entry -> blobs.put(baseDir.resolve(entry.name), entry.id));
            // TODO: This implementation makes too many copies.
            entries.stream()
                    .filter(entry -> entry.type.equals(ObjectType.TREE))
                    .forEach(entry -> {
                        Tree tree;
                        try {
                            tree = Tree.fromId(entry.id);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        blobs.putAll(tree.getAllBlobs(baseDir.resolve(entry.name)));
                    });
            return blobs;
        }

    }

    protected record TreeEntry(ObjectType type, String id, String name) {
        @Override
        public String toString() {
            return type.getValue() + " " + id + " " + name;
        }

        public static TreeEntry fromString(String entry) {
            String[] parts = entry.split(" ", 3);
            if (parts.length < 3) {
                throw new RuntimeException("Invalid tree entry: " + entry);
            }
            return new TreeEntry(ObjectType.fromValue(parts[0]), parts[1], parts[2]);
        }
    }

    public record Commit(String treeId, String parentId, String message) {
        @Override
        public String toString() {
            var sb = new StringBuilder();
            sb.append("tree ").append(treeId).append("\n");
            if (parentId != null) {
                sb.append("parent ").append(parentId).append("\n");
            }
            if (message != null) {
                sb.append("\n");
                sb.append(message).append("\n");
            }
            return sb.toString();
        }

        public static Commit fromId(String commitId) throws IOException {
            String commitString = new String(Data.getObject(commitId, ObjectType.COMMIT));

            String treeId = null;
            String parentId = null;
            String message = null;

            String[] lines = commitString.split("\n");
            int i = 0;

            while (i < lines.length && !lines[i].isEmpty()) {
                String line = lines[i];
                if (line.startsWith("tree ")) {
                    treeId = line.substring(5);
                } else if (line.startsWith("parent ")) {
                    parentId = line.substring(7);
                }
                i++;
            }
            i++;
            if (treeId == null) {
                throw new RuntimeException(String.format("Commit %s without tree id", commitId));
            }

            if (i < lines.length) {
                message = String.join("\n", Arrays.copyOfRange(lines, i, lines.length));
            }

            return new Commit(treeId, parentId, message);
        }
    }

}
