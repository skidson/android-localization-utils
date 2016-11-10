package com.skidson.android.localization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by skidson on 2016-01-19.
 */
public class FileUtils {

    public static final Pattern REGEX_VALUES = Pattern.compile("values(-[a-z]{2})?");
    private static final Pattern REGEX_TRANSLATED_FILE = Pattern.compile("strings_([a-z]{2})\\.xml");
    public static final String STRINGS_FILENAME = "strings.xml";
    private static final String NODE_STRING = "string";
    public static final String NODE_PLURALS = "plurals";
    public static final String NODE_RESOURCES = "resources";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_TRANSLATABLE = "translatable";
    private static final String OUTPUT_KEY_INDENT_AMOUNT = "{http://xml.apache.org/xslt}indent-amount";
    private static final String OUTPUT_VALUE_INDENT_AMOUNT = "4";
    private static final String YES = "yes";
    private static final String ENCODING = "UTF-8";
    private static final Logger LOGGER = LogManager.getLogger(FileUtils.class);

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
            LOGGER.error("No directories found, please verify '" + resDir.getAbsolutePath() + "' is a valid resource directory");
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

            File stringsFile = new File(file, STRINGS_FILENAME);
            stringFiles.put(locale, stringsFile.exists() ? stringsFile : null);
        }

        return stringFiles;
    }

    public static Map<String, File> findTranslationStringFiles(File inputDir) {
        File[] files = inputDir.listFiles();
        if (files == null || files.length < 1) {
            LOGGER.error("No strings_XX.xml found");
            System.exit(1);
        }

        // build a Map of locale --> strings.xml file for that locale
        Map<String, File> stringFiles = new HashMap<>();
        for (File file : files) {
            Matcher matcher = REGEX_TRANSLATED_FILE.matcher(file.getName());
            if (matcher.matches()) {
                try {
                    String locale = matcher.group(1);
                    stringFiles.put(locale, file);
                } catch (IndexOutOfBoundsException e) {
                    // no locale (e.g. just values/)
                }
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
        return parseStrings(stringsFile, true);
    }

    /**
     * Parses a strings.xml file into a map of name --> value.
     * @param stringsFile
     * @param sorted whether the map implementation should be sorted alphabetically
     * @return
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static Map<String, String> parseStrings(File stringsFile, boolean sorted) throws ParserConfigurationException, IOException, SAXException {
        Map<String, String> stringMap = sorted ? new TreeMap<>() : new LinkedHashMap<>();
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
     * Updates a strings.xml file with new values without altering the order or comments.
     * @param stringsFile
     * @param updates the map of string keys to updated values
     * @return
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static int update(File stringsFile, Map<String, String> updates) throws ParserConfigurationException, TransformerException, IOException, SAXException {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsFile);
        document.getDocumentElement().normalize();

        Set<String> newTranslations = new HashSet<>();
        newTranslations.addAll(updates.keySet());

        int updated = 0;
        NodeList nodes = document.getElementsByTagName(NODE_STRING);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) node;
                String key = e.getAttribute(ATTR_NAME);
                String updatedValue = updates.get(key);
                if (updatedValue != null) {
                    updated++;
                    e.setTextContent(updatedValue);
                }

                newTranslations.remove(key);
            }
        }

        // append new translations to the bottom
        if (!newTranslations.isEmpty()) {
            Element root = document.getDocumentElement();
            root.appendChild(document.createTextNode("\t"));
            for (String key : newTranslations) {
                Element e = document.createElement(NODE_STRING);
                e.setAttribute(ATTR_NAME, key);
                e.appendChild(document.createTextNode(updates.get(key)));
                root.appendChild(e);
                updated++;
            }
        }

        // TODO plurals

        getStringsXmlTransformer().transform(new DOMSource(document), new StreamResult(stringsFile));
        return updated;
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
        if (!strings.isEmpty()) {
            Node root = document.appendChild(document.createElement(NODE_RESOURCES));
            for (Map.Entry<String, String> entry : strings.entrySet()) {
                Element node = document.createElement(NODE_STRING);
                node.setAttribute(ATTR_NAME, entry.getKey());
                node.setTextContent(entry.getValue());
                root.appendChild(node);
            }
        }
        // TODO plurals
        getStringsXmlTransformer().transform(new DOMSource(document), new StreamResult(outputFile));
    }

    private static Transformer getStringsXmlTransformer() throws ParserConfigurationException, TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, YES);
        transformer.setOutputProperty(OutputKeys.INDENT, YES);
        transformer.setOutputProperty(OutputKeys.ENCODING, ENCODING);
        transformer.setOutputProperty(OUTPUT_KEY_INDENT_AMOUNT, OUTPUT_VALUE_INDENT_AMOUNT);
        return transformer;
    }

}
