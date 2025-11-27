package lv.taukulis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DataTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        GitContext.setRoot(tempDir);
    }

    @Test
    void testInitCreatesDirectories() throws IOException {
        Data.init();
        assertTrue(Files.exists(GitContext.gitDir()));
        assertTrue(Files.exists(GitContext.gitDir().resolve("objects")));
    }

    @Test
    void testHashObject() throws IOException {
        Data.init();
        byte[] data = "test data".getBytes();
        String objectId = Data.hashObject(data, ObjectType.BLOB);
        assertNotNull(objectId);
        assertEquals(40, objectId.length()); // SHA-1 hex length
        Path objectPath = GitContext.gitDir().resolve("objects").resolve(objectId);
        assertTrue(Files.exists(objectPath));
    }

    @Test
    void testGetObject() throws IOException {
        Data.init();
        byte[] data = "test data".getBytes();
        String objectId = Data.hashObject(data, ObjectType.BLOB);
        byte[] retrieved = Data.getObject(objectId, ObjectType.BLOB);
        assertArrayEquals(data, retrieved);
    }

    @Test
    void testGetObjectWrongType() throws IOException {
        Data.init();
        byte[] data = "test data".getBytes();
        String objectId = Data.hashObject(data, ObjectType.BLOB);
        assertThrows(RuntimeException.class, () -> Data.getObject(objectId, ObjectType.TREE));
    }

    @Test
    void testUpdateRefAndGetRef() throws IOException {
        Data.init();
        String ref = "refs/heads/main";
        String commitId = "abc123";
        Data.updateRef(ref, commitId);
        Optional<String> retrieved = Data.getRef(ref);
        assertTrue(retrieved.isPresent());
        assertEquals(commitId, retrieved.get());
    }

    @Test
    void testGetRefNonExistent() throws IOException {
        Data.init();
        Optional<String> retrieved = Data.getRef("nonexistent");
        assertFalse(retrieved.isPresent());
    }
}
