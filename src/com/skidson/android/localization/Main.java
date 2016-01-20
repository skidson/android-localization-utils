package com.skidson.android.localization;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final Pattern REGEX_VALUES = Pattern.compile("values(-[a-z]{2})?");
    private static final String STRINGS_FILENAME = "strings.xml";
    private static final String FORMAT_OUTPUT_FILENAME = "strings_%s.xml";
    private static final String NODE_STRING = "string";
    private static final String NODE_PLURALS = "plurals";
    private static final String NODE_RESOURCES = "resources";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_TRANSLATABLE = "translatable";
    private static final String OUTPUT_KEY_INDENT_AMOUNT = "{http://xml.apache.org/xslt}indent-amount";
    private static final String OUTPUT_VALUE_INDENT_AMOUNT = "4";
    private static final String YES = "yes";

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        // TODO support "apply" command as well
        findMissing(args[0], args[1]);
    }

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
    public static void findMissing(String resPath, String outputPath) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        Map<String, File> stringFiles = findStringFiles(new File(resPath));
        if (stringFiles.get(null) == null) {
            System.err.println("No default " + STRINGS_FILENAME + " found");
            System.exit(1);
        }

        File outputDirectory = new File(outputPath);
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                System.err.println("Failed to create directory: " + outputDirectory.getAbsolutePath());
                System.exit(1);
            }
        }

        // parse the default strings.xml and iterate through all others. If a string that is defined in the default
        // is not present in a locale-specific file, print it to <outputDirectory>/strings_<locale>.xml
        Map<String, String> translatableStrings = parseStrings(stringFiles.get(null));
        for (Map.Entry<String, File> entry : stringFiles.entrySet()) {
            String locale = entry.getKey();
            if (locale == null)
                continue;

            if (entry.getValue() == null) {
                // the whole strings.xml file for this locale is missing, mark all strings as missing
                output(outputDirectory, locale, translatableStrings);
                continue;
            }

            // using a TreeMap to sort by keys
            Map<String, String> missingStrings = new TreeMap<>();
            Map<String, String> localeStrings = parseStrings(entry.getValue());
            for (String key : translatableStrings.keySet()) {
                if (!localeStrings.containsKey(key))
                    missingStrings.put(key, translatableStrings.get(key));
            }

            if (!missingStrings.isEmpty())
                output(outputDirectory, locale, missingStrings);
        }
    }

    /**
     * Applies translations from a directory containing strings_XX.xml files to the corresponding
     * <resDir>/values-XX/strings.xml files for each locale. Existing values without a new translation will be preserved
     * but incoming values whose 'name' attributes are present in the destination files will be overwritten.
     * @param inputDir
     * @param resDir
     */
    private static void apply(File inputDir, File resDir) {
        // TODO
    }

    /**
     * Parses a strings.xml file into a map of name --> value.
     * @param stringsFile
     * @return
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    private static Map<String, String> parseStrings(File stringsFile) throws ParserConfigurationException, IOException, SAXException {
        Map<String, String> stringMap = new HashMap<>();
        Document xmlStringsFile = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsFile);
        xmlStringsFile.getDocumentElement().normalize();
        NodeList nodes = xmlStringsFile.getElementsByTagName(NODE_STRING);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) node;
                String translatable = e.getAttribute(ATTR_TRANSLATABLE);
                if (translatable != null && !translatable.isEmpty() && !Boolean.valueOf(translatable))
                    continue;

                stringMap.put(e.getAttribute(ATTR_NAME), e.getTextContent());
            }
        }
        // TODO plurals
        return stringMap;
    }

    /**
     * Returns a map of XX --> values-XX/strings.xml where XX is the locale. The map will include an entry for the
     * default strings.xml file with a key of {@code null}. If a values-XX directory exists but does not contain a
     * strings.xml file, the key will map to {@code null}.
     * @param resDir
     * @return
     */
    private static Map<String, File> findStringFiles(File resDir) {
        File[] directories = resDir.listFiles();
        if (directories == null || directories.length < 1) {
            System.err.println("No directories found, please verify '" + resDir.getAbsolutePath() + "' is a valid resource directory");
            System.exit(1);
        }

        // build a Map of locale --> strings.xml file for that locale
        Map<String, File> stringFiles = new HashMap<>();
        for (File file : directories) {
            Matcher matcher = REGEX_VALUES.matcher(file.getName());
            if (!file.isDirectory() || !matcher.matches())
                continue;

            String locale = null;
            try {
                String group = matcher.group(1);
                if (group != null)
                    locale = group.replace("-", "");
            } catch (IndexOutOfBoundsException e) {
                // no locale (e.g. just values/)
            }
            System.out.println("Found match: " + file.getName() + " ==> " + locale);

            File stringsFile = new File(file, STRINGS_FILENAME);
            stringFiles.put(locale, stringsFile.exists() ? stringsFile : null);
        }

        return stringFiles;
    }

    /**
     * Outputs the map of strings to a <outputDir>/strings_XX.xml file, where XX is the locale.
     * @param outputDir where the strings_XX.xml file should be generated.
     * @param locale
     * @param missingStrings
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    private static void output(File outputDir, String locale, Map<String, String> missingStrings)
            throws ParserConfigurationException, TransformerException {
        File outputFile = new File(outputDir, String.format(Locale.US, FORMAT_OUTPUT_FILENAME, locale));

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Node root = document.appendChild(document.createElement(NODE_RESOURCES));
        for (Map.Entry<String, String> entry : missingStrings.entrySet()) {
            Element node = document.createElement(NODE_STRING);
            node.setAttribute(ATTR_NAME, entry.getKey());
            node.setTextContent(entry.getValue());
            root.appendChild(node);
        }
        // TODO plurals

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, YES);
        transformer.setOutputProperty(OutputKeys.INDENT, YES);
        transformer.setOutputProperty(OUTPUT_KEY_INDENT_AMOUNT, OUTPUT_VALUE_INDENT_AMOUNT);
        transformer.transform(new DOMSource(document), new StreamResult(outputFile));
    }

    // TODO just for testing
    private static void printMap(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            // TODO testing
            System.out.println(entry.getKey() + " ==> " + entry.getValue());
        }
    }

}
