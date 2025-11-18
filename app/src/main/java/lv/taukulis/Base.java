package lv.taukulis;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Base {

    private static final String UGIT_DIR = ".ugit";

    public static void writeTree() throws IOException {
        writeTree(".");
    }

    public static void writeTree(String directory) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
            paths.filter(Files::isRegularFile).filter(Predicate.not(Base::isIgnored)).forEach(p -> {
                try {
                    Data.hashObject(Files.readAllBytes(p));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static boolean isIgnored(Path path) {
        return StreamSupport.stream(path.spliterator(), true).anyMatch(p -> p.toString().equals(UGIT_DIR));
    }
}
