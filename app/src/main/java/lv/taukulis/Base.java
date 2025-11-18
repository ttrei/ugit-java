package lv.taukulis;

import java.io.IOException;
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
            paths.filter(Predicate.not(Base::isIgnored))
                    .forEach(System.out::println);
        }
    }

    private static boolean isIgnored(Path path) {
        return StreamSupport.stream(path.spliterator(), true)
                .anyMatch(p -> p.toString().equals(UGIT_DIR));
    }
}
