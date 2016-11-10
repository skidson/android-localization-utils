package com.skidson.android.localization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        Command command = Command.fromString(args[0]);
        if (command == null) {
            LOGGER.error("Unknown command '" + args[0] + "'");
            System.exit(1);
        }
        command.execute(Arrays.copyOfRange(args, 1, args.length));
    }

}
