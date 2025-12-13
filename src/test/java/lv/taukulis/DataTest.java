package lv.taukulis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataTest {

    @BeforeEach
    void setUp() {
        GitContext.setRoot();
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
        var ref = new Ref(Ref.HEADS + "main", "abc123");
        Ref.update(Ref.of(ref.name(), ref.commitId()));
        Optional<Ref> retrieved = Ref.get(ref.name());
        assertTrue(retrieved.isPresent());
        assertEquals(ref, retrieved.get());
    }

    @Test
    void testGetRefNonExistent() throws IOException {
        Data.init();
        Optional<Ref> retrieved = Ref.get("nonexistent");
        assertFalse(retrieved.isPresent());
    }
}
