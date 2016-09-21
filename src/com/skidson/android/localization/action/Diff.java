package com.skidson.android.localization.action;

import com.skidson.android.localization.FileUtils;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by skidson on 2016-09-20.
 */
public class Diff implements Action {

    /**
     * Searches all locale strings.xml files for translations missing from the default strings.xml and generates
     * separate strings_XX.xml files at the specified output directory for each locale.
     * @param args
     *          0: the path of the project's default strings.xml (e.g. <MyProject>/app/src/main/res/strings.xml)
     *          1: the path to the updated strings.xml
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     * @throws javax.xml.transform.TransformerException
     */
    @Override
    public void execute(String[] args) throws Exception {
        Map<String, String> oldStrings = FileUtils.parseStrings(new File(args[0]));
        Map<String, String> newStrings = FileUtils.parseStrings(new File(args[1]));

        Map<String, String> modifiedStrings = new TreeMap<>();
        Map<String, String> removedStrings = new TreeMap<>();
        Map<String, String> unmatchedStrings = new TreeMap<>();

        // Find Modified and Removed Strings
        for (Map.Entry<String, String> oldString : oldStrings.entrySet()) {
            String newStringValue = newStrings.get(oldString.getKey());
            if (newStringValue == null) {
                // the existing string does not have a mapping in the new file
                removedStrings.put(oldString.getKey(), oldString.getValue());
            } else if (!newStringValue.equals(oldString.getValue())) {
                // String with this key has changed
                modifiedStrings.put(oldString.getKey(), newStringValue);
            }
        }

        for (Map.Entry<String, String> newString : newStrings.entrySet()) {
            if (!oldStrings.containsKey(newString.getKey())) {
                // String is new (key is not in existing file)
                unmatchedStrings.put(newString.getKey(), newString.getValue());
            }
        }

        System.out.println("-------------------- MODIFIED STRINGS --------------------");
        for (Map.Entry<String, String> modifiedString : modifiedStrings.entrySet()) {
            String key = modifiedString.getKey();
            System.out.println(key + ": " + oldStrings.get(key) + " --> " + modifiedString.getValue());
        }

        System.out.println("\n\n-------------------- NEW STRINGS --------------------");
        for (Map.Entry<String, String> unmatchedString : unmatchedStrings.entrySet()) {
            System.out.println(unmatchedString.getKey() + ": " + unmatchedString.getValue());
        }

        System.out.println("\n\n-------------------- REMOVED STRINGS --------------------");
        for (Map.Entry<String, String> removedString : removedStrings.entrySet()) {
            System.out.println(removedString.getKey() + ": " + removedString.getValue());
        }
    }

}
