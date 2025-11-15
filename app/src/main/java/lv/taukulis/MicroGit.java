package lv.taukulis;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name="ugit", mixinStandardHelpOptions = true,
        subcommands = {InitCommand.class, CommandLine.HelpCommand.class})
public class MicroGit {
    @SuppressWarnings("unused")
    @Spec CommandSpec spec;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MicroGit()).execute(args);
        System.exit(exitCode);
    }
}

@Command(name="init", mixinStandardHelpOptions = true)
class InitCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Hello from ugit init!");
    }

}