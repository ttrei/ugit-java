package lv.taukulis;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BaseTest {

    @Test
    void testTreeEntryToString() {
        Base.TreeEntry entry = new Base.TreeEntry(ObjectType.BLOB, "abc123", "file.txt");
        assertEquals("blob abc123 file.txt", entry.toString());
    }

    @Test
    void testTreeEntryFromString() {
        Base.TreeEntry entry = Base.TreeEntry.fromString("blob abc123 file.txt");
        assertEquals(ObjectType.BLOB, entry.type());
        assertEquals("abc123", entry.id());
        assertEquals("file.txt", entry.name());
    }

    @Test
    void testTreeEntryFromStringInvalid() {
        assertThrows(RuntimeException.class, () -> Base.TreeEntry.fromString("invalid"));
    }

    @Test
    void testCommitToString() {
        Base.Commit commit = new Base.Commit("tree123", "parent456", "message");
        String expected = "tree tree123\nparent parent456\n\nmessage\n";
        assertEquals(expected, commit.toString());
    }

    @Test
    void testCommitToStringNoParent() {
        Base.Commit commit = new Base.Commit("tree123", null, "message");
        String expected = "tree tree123\n\nmessage\n";
        assertEquals(expected, commit.toString());
    }

    @Test
    void testCommitToStringNoMessage() {
        Base.Commit commit = new Base.Commit("tree123", "parent456", null);
        String expected = "tree tree123\nparent parent456\n";
        assertEquals(expected, commit.toString());
    }
}
