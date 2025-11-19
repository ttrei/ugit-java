package lv.taukulis;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Data {

    private static final String GIT_DIR = ".ugit";

    private static final byte NULL_BYTE = 0x00;

    public static void init(Path root) throws IOException {
        Path ugitPath = root.resolve(GIT_DIR);
        Path objectsPath = ugitPath.resolve("objects");
        Files.createDirectory(ugitPath);
        Files.createDirectory(objectsPath);
        System.out.println("Initialized empty ugit repository at " + ugitPath.toAbsolutePath());
    }

    public static String hashObject(Path root, byte[] dataBytes) throws IOException {
        return hashObject(root, dataBytes, "blob");
    }

    public static String hashObject(Path root, byte[] dataBytes, String type) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }

        byte[] typeBytes = type.getBytes(StandardCharsets.UTF_8);
        ByteBuffer typeAndDataBuffer = ByteBuffer.allocate(typeBytes.length + 1 + dataBytes.length);
        typeAndDataBuffer.put(typeBytes);
        typeAndDataBuffer.put(NULL_BYTE);
        typeAndDataBuffer.put(dataBytes);
        byte[] typeAndDataBytes = typeAndDataBuffer.array();

        String objectId = Utils.bytesToHex(md.digest(typeAndDataBytes));
        Path objectPath = root.resolve(Paths.get(GIT_DIR, "objects", objectId));
        Files.write(objectPath, typeAndDataBytes);
        return objectId;
    }

    @SuppressWarnings("unused")
    public static byte[] getObject(Path root, String objectId) throws IOException {
        return getObject(root, objectId, "blob");
    }

    public static byte[] getObject(Path root, String objectId, String expectedType) throws IOException {
        byte[] objectBytes = Files.readAllBytes(root.resolve(Paths.get(GIT_DIR, "objects", objectId)));

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

        var type = new String(typeBytes, StandardCharsets.UTF_8);
        if (expectedType != null && !type.equals(expectedType)) {
            throw new RuntimeException("Expected type '" + expectedType + "', got '" + type + "'");
        }

        return dataBytes;
    }

}
