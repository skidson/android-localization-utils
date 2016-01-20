package com.skidson.android.localization;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) throws Exception {
        Command command = Command.fromString(args[0]);
        if (command == null) {
            System.err.println("Unknown command '" + args[0] + "'");
            System.exit(1);
        }
        command.execute(Arrays.copyOfRange(args, 1, args.length));
    }

}
