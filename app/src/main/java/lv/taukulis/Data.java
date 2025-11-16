package lv.taukulis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Data {

    private static final String GIT_DIR = ".ugit";

    public static void init() throws IOException {
        Path ugitPath = Paths.get(GIT_DIR);
        Path objectsPath = Paths.get(GIT_DIR, "objects");
        Files.createDirectory(ugitPath);
        Files.createDirectory(objectsPath);
        System.out.println("Initialized empty ugit repository at " + ugitPath.toAbsolutePath());
    }

    public static void hashObject(Path file) throws IOException {
        try {
            hashObject(file, "SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }

    public static void hashObject(Path file, String algorithm) throws NoSuchAlgorithmException, IOException {
        byte[] data = Files.readAllBytes(file);
        MessageDigest md = MessageDigest.getInstance(algorithm);
        String objectId = Utils.bytesToHex(md.digest(data));
        Path objectPath = Paths.get(GIT_DIR, "objects", objectId);
        Files.write(objectPath, data);
    }

}
