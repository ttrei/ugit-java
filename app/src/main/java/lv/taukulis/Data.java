package lv.taukulis;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;

public class Data {

    private static final byte NULL_BYTE = 0x00;

    public static void init() throws IOException {
        Path gitDir = GitContext.gitDir();
        Path objectsPath = gitDir.resolve("objects");
        Files.createDirectory(gitDir);
        Files.createDirectory(objectsPath);
        System.out.println("Initialized empty ugit repository at " + gitDir.toAbsolutePath());
    }

    public static String hashObject(byte[] dataBytes, String type) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }

        byte[] typeBytes = type.getBytes();
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

    public static byte[] getObject(String objectId, String expectedType) throws IOException {
        byte[] objectBytes = Files.readAllBytes(GitContext.gitDir().resolve("objects").resolve(objectId));

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

        var type = new String(typeBytes);
        if (expectedType != null && !type.equals(expectedType)) {
            throw new RuntimeException("Expected type '" + expectedType + "', got '" + type + "'");
        }

        return dataBytes;
    }

    public static void setHead(String commitObjectId) throws IOException {
        Files.write(GitContext.gitDir().resolve("HEAD"), (commitObjectId + "\n").getBytes());
    }

    public static Optional<String> getHead() throws IOException {
        try {
            return Optional.of(Files.readString(GitContext.gitDir().resolve("HEAD")));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        }
    }

}
