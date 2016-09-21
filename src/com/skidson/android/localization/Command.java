package com.skidson.android.localization;

import com.skidson.android.localization.action.Action;
import com.skidson.android.localization.action.Apply;
import com.skidson.android.localization.action.Diff;
import com.skidson.android.localization.action.Identify;

import java.util.*;

/**
 * Enumeration of command line arguments
 * Created by skidson on 2016-01-19.
 */
public enum Command {

    APPLY(new Apply(), "apply"),
    IDENTIFY(new Identify(), "identify", "find", "generate"),
    DIFF(new Diff(), "diff");

    private static final Map<String, Command> STRING_MAP;
    static {
        Map<String, Command> stringMap = new HashMap<>();
        for (Command command : values()) {
            for (String match : command.matches)
                stringMap.put(match, command);
        }
        STRING_MAP = Collections.unmodifiableMap(stringMap);
    }

    private final Action action;
    private final String[] matches;

    Command(Action action, String... matches) {
        this.action = action;
        this.matches = matches;
    }

    public void execute(String[] args) throws Exception {
        action.execute(args);
    }

    public static Command fromString(String string) {
        return STRING_MAP.get(string);
    }

}
