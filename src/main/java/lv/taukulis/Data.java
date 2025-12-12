package lv.taukulis;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Data {

    public static final String HEAD = "HEAD";

    private static final byte NULL_BYTE = 0x00;

    public static void init() throws IOException {
        Path gitDir = Path.of(System.getProperty("user.dir")).resolve(GitContext.GIT_DIR);
        Path objectsPath = gitDir.resolve("objects");
        Files.createDirectory(gitDir);
        Files.createDirectory(objectsPath);
        System.out.println("Initialized empty ugit repository at " + gitDir.toAbsolutePath());
    }

    public static String hashObject(byte[] dataBytes, ObjectType type) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }

        byte[] typeBytes = type.getValue().getBytes();
        ByteBuffer typeAndDataBuffer = ByteBuffer.allocate(typeBytes.length + 1 + dataBytes.length);
        typeAndDataBuffer.put(typeBytes);
        typeAndDataBuffer.put(NULL_BYTE);
        typeAndDataBuffer.put(dataBytes);
        byte[] typeAndDataBytes = typeAndDataBuffer.array();

        String objectId = Utils.bytesToHex(md.digest(typeAndDataBytes));
        Path objectPath = GitContext.gitDir().resolve("objects").resolve(objectId);
        Files.write(objectPath, typeAndDataBytes);
        return objectId;
    }

    public static String getObjectString(String objectId, ObjectType expectedType) throws IOException {
        return new String(getObject(objectId, expectedType));
    }

    public static byte[] getObject(String objectId, ObjectType expectedType) throws IOException {
        byte[] objectBytes = Files.readAllBytes(GitContext.gitDir().resolve("objects/").resolve(objectId));

        int separatorIndex = -1;
        for (int i = 0; i < objectBytes.length; i++) {
            if (objectBytes[i] == NULL_BYTE) {
                separatorIndex = i;
                break;
            }
        }

        byte[] typeBytes;
        byte[] dataBytes;
        if (separatorIndex != -1) {
            typeBytes = Arrays.copyOfRange(objectBytes, 0, separatorIndex);
            dataBytes = Arrays.copyOfRange(objectBytes, separatorIndex + 1, objectBytes.length);
        } else {
            typeBytes = objectBytes;
            dataBytes = new byte[0];
        }

        var type = ObjectType.fromValue(new String(typeBytes));
        if (expectedType != null && !type.equals(expectedType)) {
            throw new RuntimeException("Expected object of type " + expectedType + ", got " + type);
        }

        return dataBytes;
    }

    public static void updateRef(String ref, String commitId) throws IOException {
        Path path = GitContext.gitDir().resolve(ref);
        Files.createDirectories(path.getParent());
        Files.write(path, (commitId + "\n").getBytes());
    }

    public static Optional<String> getRef(String ref) {
        try {
            Path file = GitContext.gitDir().resolve("@".equals(ref) ? HEAD : ref);
            return Optional.of(Files.readString(file).strip());
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String resolveCommitId(String name) {
        return Stream.of(name, "refs/" + name, "refs/tags/" + name, "refs/heads/" + name)
                .map(Data::getRef)
                .flatMap(Optional::stream)
                .findFirst()
                .orElseGet(() -> {
                    if (isSha1Hash(name)) {
                        return name;
                    }
                    throw new RuntimeException("Unknown name: " + name);
                });
    }

    public static boolean isSha1Hash(String input) {
        if (input == null) {
            return false;
        }
        // SHA-1 is exactly 40 hexadecimal characters
        return input.matches("[a-fA-F0-9]{40}");
    }

    public static Iterable<Path> iterRefs() {
        try (var stream = Files.walk(GitContext.gitDir().resolve("refs"))) {
            return stream.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
