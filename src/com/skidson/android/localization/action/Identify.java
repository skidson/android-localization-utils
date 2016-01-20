package com.skidson.android.localization.action;

import com.skidson.android.localization.FileUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by skidson on 2016-01-20.
 */
public class Identify implements Action {

    private static final String FORMAT_OUTPUT_FILENAME = "strings_%s.xml";

    /**
     * Searches all locale strings.xml files for translations missing from the default strings.xml and generates
     * separate strings_XX.xml files at the specified output directory for each locale.
     * @param resPath the path of the project's resource dir (e.g. <MyProject>/app/src/main/res)
     * @param outputPath the path to where the output strings_XX.xml files should be generated. Will be created if it does
     *                   not exist.
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws TransformerException
     */
    @Override
    public void execute(String[] args) throws Exception {
        Map<String, File> stringFiles = FileUtils.findProjectStringFiles(new File(args[0]));
        if (stringFiles.get(null) == null) {
            System.err.println("No default " + FileUtils.STRINGS_FILENAME + " found");
            System.exit(1);
        }

        File outputDirectory = new File(args[1]);
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                System.err.println("Failed to create directory: " + outputDirectory.getAbsolutePath());
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
                System.out.println("Writing " + missingStrings.size() + " missing translations to "
                        + String.format(FORMAT_OUTPUT_FILENAME, locale));

                File outputFile = new File(outputDirectory, String.format(Locale.US, FORMAT_OUTPUT_FILENAME, locale));
                FileUtils.output(outputFile, missingStrings);
            }
        }
    }

}
