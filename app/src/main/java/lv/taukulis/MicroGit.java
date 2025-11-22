package lv.taukulis;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@Command(name = "ugit", mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class,
        InitCommand.class, HashObjectCommand.class, CatFilesCommand.class, WriteTreeCommand.class,
        ReadTreeCommand.class, CommitCommand.class})
public class MicroGit {
    @SuppressWarnings("unused")
    @Spec
    CommandSpec spec;

    @Option(names = {"-r", "--root"}, description = "Root directory of ugit repository")
    Path root;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MicroGit()).execute(args);
        System.exit(exitCode);
    }

    public Path getRoot() {
        return root != null ? root : Paths.get(".");
    }
}

@Command(name = "init", mixinStandardHelpOptions = true)
class InitCommand implements Callable<Integer> {
    @SuppressWarnings("unused")
    @ParentCommand
    private MicroGit parent;

    @Override
    public Integer call() throws IOException {
        Data.init(parent.getRoot());
        return 0;
    }
}

@Command(name = "hash-object", mixinStandardHelpOptions = true)
class HashObjectCommand implements Callable<Integer> {
    @SuppressWarnings("unused")
    @ParentCommand
    private MicroGit parent;

    @Parameters(arity = "1", description = "File to hash")
    Path file;

    @Override
    public Integer call() throws IOException {
        System.out.println(Data.hashObject(parent.getRoot(), Files.readAllBytes(file), "blob"));
        return 0;
    }
}

@Command(name = "cat-file", mixinStandardHelpOptions = true)
class CatFilesCommand implements Callable<Integer> {
    @SuppressWarnings("unused")
    @ParentCommand
    private MicroGit parent;

    @Parameters(arity = "1", description = "Object ID")
    String objectId;

    @Override
    public Integer call() throws IOException {
        System.out.write(Data.getObject(parent.getRoot(), objectId, null));
        System.out.flush();
        return 0;
    }
}

@Command(name = "write-tree", mixinStandardHelpOptions = true)
class WriteTreeCommand implements Callable<Integer> {
    @SuppressWarnings("unused")
    @ParentCommand
    private MicroGit parent;

    @Override
    public Integer call() throws IOException {
        System.out.println(Base.writeTree(parent.getRoot(), ""));
        return 0;
    }
}

@Command(name = "read-tree", mixinStandardHelpOptions = true)
class ReadTreeCommand implements Callable<Integer> {
    @SuppressWarnings("unused")
    @ParentCommand
    private MicroGit parent;

    @Parameters(arity = "1", description = "Object ID of a tree")
    String treeObjectId;

    @Override
    public Integer call() throws IOException {
        Base.readTree(parent.getRoot(), treeObjectId);
        return 0;
    }
}

@Command(name = "commit", mixinStandardHelpOptions = true)
class CommitCommand implements Callable<Integer> {
    @SuppressWarnings("unused")
    @ParentCommand
    private MicroGit parent;

    @Parameters(arity = "1..*", description = "Commit message")
    String[] messageArgs;

    @Override
    public Integer call() throws IOException {
        System.out.println(Base.commit(parent.getRoot(), String.join(" ", messageArgs)));
        return 0;
    }
}
