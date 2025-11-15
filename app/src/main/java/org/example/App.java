package org.example;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name="ugit", mixinStandardHelpOptions = true)
public class App implements Runnable {

    @Option(names = { "-s", "--font-size" }, description = "Font size")
    int fontSize = 19;

    @Override
    public void run() {
        // Business logic of the command goes here...
        System.out.println("Hello, from picoli!");
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }
}
