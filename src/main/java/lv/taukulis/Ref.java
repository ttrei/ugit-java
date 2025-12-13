package lv.taukulis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;


// name - path of the ref relative to gitDir
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@ToString
public class Ref {

    String name;
    String commitId;

    public static final String HEAD = "HEAD";
    public static final String REFS = "refs/";
    public static final String TAGS = "refs/tags/";
    public static final String HEADS = "refs/heads/";

    public static Ref of(String name, String commitId) {
        return new Ref(name, commitId);
    }

    public static Optional<Ref> get(String name) {
        return get(resolve(name));
    }

    public static Optional<Ref> get(Path path) {
        try {
            return Optional.of(new Ref(GitContext.gitDir().relativize(path).toString(), Files.readString(path).strip()));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Iterable<Ref> iterRefs() {
        try (var stream = Files.walk(GitContext.gitDir().resolve(REFS))) {
            return stream.filter(Files::isRegularFile).map(Ref::get).flatMap(Optional::stream).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<Ref> find(String name) {
        return Stream.of(name, REFS + name, TAGS + name, HEADS + name)
                .map(Ref::get)
                .flatMap(Optional::stream)
                .findFirst();
    }


    public static void update(Ref ref) throws IOException {
        Path path = GitContext.gitDir().resolve(ref.name);
        Files.createDirectories(path.getParent());
        Files.write(path, (ref.commitId + "\n").getBytes());
    }

    private static Path resolve(String name) {
        return GitContext.gitDir().resolve("@".equals(name) ? HEAD : name);
    }

}
