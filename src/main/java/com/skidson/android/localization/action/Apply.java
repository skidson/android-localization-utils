package com.skidson.android.localization.action;

import com.skidson.android.localization.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by skidson on 2016-01-19.
 */
public class Apply implements Action {

    private static final String FORMAT_VALUES_FOLDER = "values-%s";

    private static final Logger LOGGER = LogManager.getLogger(Apply.class);

    @Override
    public void execute(String[] args) throws TransformerException, ParserConfigurationException, IOException, SAXException {
        File inputDir = new File(args[0]);
        File resDir = new File(args[1]);

        Set<Flag> flags = new HashSet<>();
        for (int i = 2; i < args.length; i++) {
            Flag flag = Flag.fromKey(args[i]);
            if (flag != null) {
                flags.add(flag);
            }
        }

        Map<String, File> translationStringFiles = FileUtils.findTranslationStringFiles(inputDir);
        if (translationStringFiles.isEmpty()) {
            LOGGER.error("No translation files found in " + inputDir.getAbsolutePath());
            System.exit(0);
        }

        Map<String, File> projectStringFiles = FileUtils.findProjectStringFiles(resDir);
        for (Map.Entry<String, File> entry : translationStringFiles.entrySet()) {
            Map<String, String> translatedStrings = FileUtils.parseStrings(entry.getValue());
            if (translatedStrings.isEmpty()) {
                LOGGER.info("Found " + entry.getValue().getName() + " but file is empty");
                continue;
            }

            String locale = entry.getKey();
            File projectStringFile = projectStringFiles.get(locale);
            if (projectStringFile == null) {
                File projectLocaleValuesDir = new File(resDir, String.format(Locale.US, FORMAT_VALUES_FOLDER, locale));
                if (!projectLocaleValuesDir.exists() && !projectLocaleValuesDir.mkdirs()) {
                    LOGGER.info("Found translations for " + locale + " but could not create " +
                            projectLocaleValuesDir.getName());
                    continue;
                }
                projectStringFile = new File(projectLocaleValuesDir, FileUtils.STRINGS_FILENAME);
            }

            Map<String, String> projectStrings = FileUtils.parseStrings(projectStringFile);
            int newCount = 0;
            for (String translationKey : translatedStrings.keySet()) {
                if (!projectStrings.containsKey(translationKey)) {
                    LOGGER.info("Found new translation for " + translationKey);
                    newCount++;
                }
            }

            LOGGER.info("Found " + translatedStrings.size() + " total translations for " + locale + " (" + newCount + " new)");
            if (!flags.contains(Flag.DRY)) {
                int updateCount = FileUtils.update(projectStringFile, translatedStrings);
                LOGGER.info("Updated " + updateCount + " translations");
            }
        }
    }

    public enum Flag {
        DRY("--dry", "--dry-run");

        private final String[] keys;

        Flag(String... keys) {
            this.keys = keys;
        }

        public static Flag fromKey(String key) {
            for (Flag flag : values()) {
                for (String k : flag.keys) {
                    if (k.equalsIgnoreCase(key)) {
                        return flag;
                    }
                }
            }
            return null;
        }
    }

}
