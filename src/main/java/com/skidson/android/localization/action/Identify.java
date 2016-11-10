package com.skidson.android.localization.action;

import com.skidson.android.localization.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Identifies missing translations.<br/><br/>
 *
 * Searches all values-XX/strings.xml files for translations missing from the default strings.xml and generates
 * separate strings_XX.xml files at the specified output directory for each locale. These files are intended to
 * be passed to a translation team and then passed back in to the {@link com.skidson.android.localization.action.Apply}
 * command after being updated.
 * @param args
 *          0: the path of the project's resource dir (e.g. <MyProject>/app/src/main/res)
 *          1: the path to a directory where the output strings_XX.xml files should be generated. Will be created if it
 *          does not exist.
 *
 * Created by skidson on 2016-01-20.
 */
public class Identify implements Action {

    private static final Logger LOGGER = LogManager.getLogger(Identify.class);
    private static final String FORMAT_OUTPUT_FILENAME = "strings_%s.xml";

    @Override
    public void execute(String[] args) throws Exception {
        Map<String, File> stringFiles = FileUtils.findProjectStringFiles(new File(args[0]));
        if (stringFiles.get(null) == null) {
            LOGGER.error("No default " + FileUtils.STRINGS_FILENAME + " found");
            System.exit(1);
        }

        File outputDirectory = new File(args[1]);
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                LOGGER.error("Failed to create directory: " + outputDirectory.getAbsolutePath());
                System.exit(1);
            }
        }

        // parse the default strings.xml and iterate through all others. If a string that is defined in the default
        // is not present in a locale-specific file, print it to <outputDirectory>/strings_<locale>.xml
        Map<String, String> translatableStrings = FileUtils.parseStrings(stringFiles.get(null));
        for (Map.Entry<String, File> entry : stringFiles.entrySet()) {
            String locale = entry.getKey();
            if (locale == null)
                continue;

            // using a TreeMap to sort by keys
            Map<String, String> missingStrings = new TreeMap<>();
            if (entry.getValue() == null) {
                // the whole strings.xml file for this locale is missing, mark all strings as missing
                missingStrings.putAll(translatableStrings);
            } else {
                Map<String, String> localeStrings = FileUtils.parseStrings(entry.getValue());
                for (String key : translatableStrings.keySet()) {
                    if (!localeStrings.containsKey(key))
                        missingStrings.put(key, translatableStrings.get(key));
                }
            }

            if (!missingStrings.isEmpty()) {
                LOGGER.info("Writing " + missingStrings.size() + " missing translations to "
                        + String.format(FORMAT_OUTPUT_FILENAME, locale));

                File outputFile = new File(outputDirectory, String.format(Locale.US, FORMAT_OUTPUT_FILENAME, locale));
                FileUtils.output(outputFile, missingStrings);
            }
        }
    }

}
