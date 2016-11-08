package com.skidson.android.localization.action;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by skidson on 2016-10-05.
 */
public class ParseLint implements Action {

    private static final String NODE_ISSUE = "issue";
    private static final String NODE_ISSUES = "issues";

    private static final String ATTR_ID = "id";
    private static final String ATTR_ERROR_LINE_1 = "errorLine1";

    @Override
    public void execute(String[] args) throws Exception {
        String lintFile = args[0];

        /*
        Map<String, String> stringMap = sorted ? new TreeMap<String, String>() : new LinkedHashMap<String, String>();
        Document xmlStringsFile = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(lintFile);
        xmlStringsFile.getDocumentElement().normalize();
        NodeList nodes = xmlStringsFile.getElementsByTagName(NODE_ISSUE);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) node;
                if (translatable != null && !translatable.isEmpty() && !Boolean.valueOf(translatable))
                    continue;

                stringMap.put(e.getAttribute(ATTR_NAME), e.getTextContent());
            }
        }
        // TODO plurals
        return stringMap;
        */
    }

    private static class Issue {
        private String id;
        private String message;
        private String category;
        private String summary;
        private String explanation;
        private String errorLine2;
        private int priority;

    }

}
