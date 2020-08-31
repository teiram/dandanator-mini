package com.grelobites.romgenerator.util.multiply;

public class Command {
    private final String command;
    private final String argument;

    public Command(String command, String argument) {
        this.command = command;
        this.argument = argument;
    }

    public String getCommand() {
        return command;
    }

    public String getArgument() {
        return argument;
    }

    @Override
    public String toString() {
        return "Command{" +
                "command='" + command + '\'' +
                ", argument='" + argument + '\'' +
                '}';
    }
}
