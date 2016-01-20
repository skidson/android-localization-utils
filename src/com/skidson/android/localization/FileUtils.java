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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by skidson on 2016-01-19.
 */
public class FileUtils {

    public static final Pattern REGEX_VALUES = Pattern.compile("values(-[a-z]{2})?");
    private static final Pattern REGEX_TRANSLATED_FILE = Pattern.compile("strings_([a-z]){2}\\.xml");
    public static final String STRINGS_FILENAME = "strings.xml";private static final String NODE_STRING = "string";
    public static final String NODE_PLURALS = "plurals";
    public static final String NODE_RESOURCES = "resources";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_TRANSLATABLE = "translatable";
    private static final String OUTPUT_KEY_INDENT_AMOUNT = "{http://xml.apache.org/xslt}indent-amount";
    private static final String OUTPUT_VALUE_INDENT_AMOUNT = "4";
    private static final String YES = "yes";

    /**
     * Returns a map of XX --> values-XX/strings.xml where XX is the locale. The map will include an entry for the
     * default strings.xml file with a key of {@code null}. If a values-XX directory exists but does not contain a
     * strings.xml file, the key will map to {@code null}.
     * @param resDir
     * @return
     */
    public static Map<String, File> findProjectStringFiles(File resDir) {
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
            System.out.println("Found match: " + file.getName() + " ==> " + (locale == null ? "default" : locale));

            File stringsFile = new File(file, STRINGS_FILENAME);
            stringFiles.put(locale, stringsFile.exists() ? stringsFile : null);
        }

        return stringFiles;
    }

    public static Map<String, File> findTranslationStringFiles(File inputDir) {
        File[] files = inputDir.listFiles();
        if (files == null || files.length < 1) {
            System.err.println("No strings_XX.xml found");
            System.exit(1);
        }

        // build a Map of locale --> strings.xml file for that locale
        Map<String, File> stringFiles = new HashMap<>();
        for (File file : files) {
            Matcher matcher = REGEX_TRANSLATED_FILE.matcher(file.getName());
            try {
                String locale = matcher.group(1);
                System.out.println("Found match: " + file.getName() + " ==> " + (locale == null ? "default" : locale));
                stringFiles.put(locale, file);
            } catch (IndexOutOfBoundsException e) {
                // no locale (e.g. just values/)
            }
        }
        return stringFiles;
    }

    /**
     * Parses a strings.xml file into a map of name --> value.
     * @param stringsFile
     * @return
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static Map<String, String> parseStrings(File stringsFile) throws ParserConfigurationException, IOException, SAXException {
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
     * Outputs the map of strings to the specified file.
     * @param outputFile
     * @param strings
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    public static void output(File outputFile, Map<String, String> strings) throws ParserConfigurationException, TransformerException {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Node root = document.appendChild(document.createElement(NODE_RESOURCES));
        for (Map.Entry<String, String> entry : strings.entrySet()) {
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

}
