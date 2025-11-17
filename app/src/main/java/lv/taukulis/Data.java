package lv.taukulis;

import java.io.IOException;
import java.lang.reflect.Array;
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

    public static void init() throws IOException {
        Path ugitPath = Paths.get(GIT_DIR);
        Path objectsPath = Paths.get(GIT_DIR, "objects");
        Files.createDirectory(ugitPath);
        Files.createDirectory(objectsPath);
        System.out.println("Initialized empty ugit repository at " + ugitPath.toAbsolutePath());
    }

    public static String hashObject(Path file) throws IOException {
        return hashObject(file, "blob");
    }

    public static String hashObject(Path file, String type) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }

        byte[] typeBytes = type.getBytes(StandardCharsets.UTF_8);
        byte[] dataBytes = Files.readAllBytes(file);
        ByteBuffer typeAndDataBuffer = ByteBuffer.allocate(typeBytes.length + 1 + dataBytes.length);
        typeAndDataBuffer.put(typeBytes);
        typeAndDataBuffer.put(NULL_BYTE);
        typeAndDataBuffer.put(dataBytes);
        byte[] typeAndDataBytes = typeAndDataBuffer.array();

        String objectId = Utils.bytesToHex(md.digest(typeAndDataBytes));
        Path objectPath = Paths.get(GIT_DIR, "objects", objectId);
        Files.write(objectPath, typeAndDataBytes);
        return objectId;
    }

    public static byte[] getObject(String objectId) throws IOException {
        return getObject(objectId, "blob");
    }

    public static byte[] getObject(String objectId, String expectedType) throws IOException {
        byte[] objectBytes = Files.readAllBytes(Paths.get(GIT_DIR, "objects", objectId));

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
