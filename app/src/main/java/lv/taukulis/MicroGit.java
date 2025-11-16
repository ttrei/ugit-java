package lv.taukulis;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "ugit", mixinStandardHelpOptions = true, subcommands = {InitCommand.class, HashObjectCommand.class,
        CatFilesCommand.class, CommandLine.HelpCommand.class})
public class MicroGit {
    @SuppressWarnings("unused")
    @Spec
    CommandSpec spec;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MicroGit()).execute(args);
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

    @Parameters(arity = "1", description = "File to hash")
    Path file;

    @Override
    public Integer call() throws IOException {
        System.out.println(Data.hashObject(file));
        return 0;
    }

}

@Command(name = "cat-file", mixinStandardHelpOptions = true)
class CatFilesCommand implements Callable<Integer> {

    @Parameters(arity = "1", description = "Object ID")
    String objectId;

    @Override
    public Integer call() throws IOException {
        System.out.println(Data.getObject(objectId));
        return 0;
    }

}