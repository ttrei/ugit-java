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


// name - path of the ref relative to gitDir
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@ToString
public class Ref {

    String name;
    String commitId;

    public static final String HEAD = "HEAD";

    public static Ref of(String name, String commitId) {
        return new Ref(name, commitId);
    }

    public static Optional<Ref> read(String name) {
        name = "@".equals(name) ? HEAD : name;
        return read(GitContext.gitDir().resolve(name));
    }

    public static Optional<Ref> read(Path path) {
        try {
            return Optional.of(new Ref(GitContext.gitDir().relativize(path).toString(), Files.readString(path).strip()));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Iterable<Ref> iterRefs() {
        try (var stream = Files.walk(GitContext.gitDir().resolve("refs"))) {
            return stream.filter(Files::isRegularFile).map(Ref::read).flatMap(Optional::stream).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
