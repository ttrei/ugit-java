package lv.taukulis;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "ugit", mixinStandardHelpOptions = true, subcommands = {CommandLine.HelpCommand.class,
        InitCommand.class, HashObjectCommand.class, CatFilesCommand.class, WriteTreeCommand.class,
        ReadTreeCommand.class, CommitCommand.class, LogCommand.class, CheckoutCommand.class, TagCommand.class,
        KCommand.class})
public class UgitCommand {
    @SuppressWarnings("unused")
    @Spec
    CommandSpec spec;

    public static void main(String[] args) {
        var cmd = new CommandLine(new UgitCommand());
        cmd.setExecutionStrategy((parseResult) -> {
            GitContext.setRoot();
            return new CommandLine.RunLast().execute(parseResult);
        });
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}

@Command(name = "init", mixinStandardHelpOptions = true)
class InitCommand implements Callable<Integer> {
    @Override
    public Integer call() throws IOException {
        Data.init();
        return 0;
    }
}

@Command(name = "hash-object", mixinStandardHelpOptions = true)
class HashObjectCommand implements Callable<Integer> {
    @Parameters(description = "File to hash")
    Path file;

    @Override
    public Integer call() throws IOException {
        System.out.println(Data.hashObject(Files.readAllBytes(file), ObjectType.BLOB));
        return 0;
    }
}

@Command(name = "cat-file", mixinStandardHelpOptions = true)
class CatFilesCommand implements Callable<Integer> {
    @Parameters(description = "Object ID")
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
    @Override
    public Integer call() throws IOException {
        System.out.println(Base.writeTree(""));
        return 0;
    }
}

@Command(name = "read-tree", mixinStandardHelpOptions = true)
class ReadTreeCommand implements Callable<Integer> {
    @Parameters(description = "Object ID of a tree")
    String treeObjectId;

    @Override
    public Integer call() throws IOException {
        Base.readTree(treeObjectId);
        return 0;
    }
}

@Command(name = "commit", mixinStandardHelpOptions = true)
class CommitCommand implements Callable<Integer> {
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
    @Parameters(defaultValue = "@", description = "Commit reference")
    String ref;

    @Override
    public Integer call() throws IOException {
        String commitId = Data.resolveCommitId(ref);
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

@Command(name = "checkout", mixinStandardHelpOptions = true)
class CheckoutCommand implements Callable<Integer> {
    @Parameters(description = "Commit reference")
    String ref;

    @Override
    public Integer call() throws IOException {
        Base.checkout(Data.resolveCommitId(ref));
        return 0;
    }
}

@Command(name = "tag", mixinStandardHelpOptions = true)
class TagCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Tag name")
    String name;

    @Parameters(index = "1", defaultValue = "@", description = "Commit reference")
    String ref;

    @Override
    public Integer call() throws IOException {
        Base.tag(name, Data.resolveCommitId(ref));
        return 0;
    }
}

@Command(name = "k", mixinStandardHelpOptions = true)
class KCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        for (Data.Ref ref : Data.iterRefs()) {
            System.out.println(ref);
        }
        return 0;
    }
}
