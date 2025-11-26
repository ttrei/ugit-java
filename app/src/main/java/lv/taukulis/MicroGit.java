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
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "ugit", mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class,
        InitCommand.class, HashObjectCommand.class, CatFilesCommand.class, WriteTreeCommand.class,
        ReadTreeCommand.class, CommitCommand.class, LogCommand.class})
public class MicroGit {
    @SuppressWarnings("unused")
    @Spec
    CommandSpec spec;

    @Option(names = {"-r", "--root"}, description = "Root directory of ugit repository")
    Path root;

    public static void main(String[] args) {
        var cmd = new CommandLine(new MicroGit());
        cmd.setExecutionStrategy((parseResult) -> {
            MicroGit microGit = (MicroGit) parseResult.commandSpec().userObject();
            GitContext.setRoot(microGit.root);
            return new CommandLine.RunLast().execute(parseResult);
        });
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}

@Command(name = "init", mixinStandardHelpOptions = true)
class InitCommand implements Callable<Integer> {
    @SuppressWarnings("unused")
    @ParentCommand
    private MicroGit parent;

    @Override
    public Integer call() throws IOException {
        Data.init();
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
        System.out.println(Data.hashObject(Files.readAllBytes(file), ObjectType.BLOB));
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
        System.out.write(Data.getObject(objectId, null));
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
        System.out.println(Base.writeTree(""));
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
        Base.readTree(treeObjectId);
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
        System.out.println(Base.commit(String.join(" ", messageArgs)));
        return 0;
    }
}

@Command(name = "log", mixinStandardHelpOptions = true)
class LogCommand implements Callable<Integer> {
    @SuppressWarnings("unused")
    @ParentCommand
    private MicroGit parent;

    @Parameters(arity = "0..1", description = "Commit ID")
    String commitId;

    @Override
    public Integer call() throws IOException {
        String commitId = this.commitId != null ? this.commitId : Data.getHead().orElse(null);
        if (commitId == null) {
            return 0;
        }
        while (commitId != null) {
            Base.Commit commit = Base.getCommit(commitId);
            System.out.println("commit " + commitId);
            if (commit.message() != null) {
                String indentedMessage = commit.message().lines()
                        .map(line -> "    " + line)
                        .collect(Collectors.joining("\n"));
                System.out.println(indentedMessage);
            }
            commitId = commit.parentId();
        }
        return 0;
    }
}
