package ssd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * TODO: Implement this content handler.
 */
public class JeopardyMoveHandler extends DefaultHandler {

    /**
     * Use this xPath variable to create xPath expression that can be evaluated
     * over the XML document.
     */
    private static XPath xPath = XPathFactory.newInstance().newXPath();

    /**
     * Store and manipulate the Jeopardy XML document here.
     */
    private Document jeopardyDoc;

    /**
     * This variable stores the text content of XML Elements.
     */
    private String eleText;

    private String sessionID;
    private String playerName;
    private int questionID;
    private ArrayList<String> answers = new ArrayList<String>();

    public JeopardyMoveHandler(Document doc) {
        jeopardyDoc = doc;
    }

    @Override
    /**
     * SAX calls this method to pass in character data
     */
    public void characters(char[] text, int start, int length)
            throws SAXException {
        eleText = new String(text, start, length);
    }

    /**
     *
     * Return the current stored Jeopardy document
     *
     * @return XML Document
     */
    public Document getDocument() {
        return jeopardyDoc;
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes atts) throws SAXException {
        if (qualifiedName.equals("move")) {
            sessionID = atts.getValue("session");
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qualifiedName) throws SAXException {
        if (qualifiedName.equals("move")) {
            doWork();
        } else if (qualifiedName.equals("player")) {
            playerName = eleText;
        } else if (qualifiedName.equals("question")) {
            questionID = Integer.valueOf(eleText);
        } else if (qualifiedName.equals("answer")) {
            answers.add(eleText);
        }
    }

    private void doWork() {
        ArrayList<Element> answerElems = new ArrayList<>();
        for (String answer : answers) {
            Element e = jeopardyDoc.createElement("givenanswer");
            e.setAttribute("player", playerName);
            e.setTextContent(answer);
            answerElems.add(e);
        }

        // find the game with the corresponding sessionID
        Element player = jeopardyDoc.createElement("player");
        player.setAttribute("ref", playerName);
        Element asked = jeopardyDoc.createElement("asked");
        asked.setAttribute("question", String.valueOf(questionID));
        for (Element e : answerElems) {
            asked.appendChild(e);
        }

        try {
            XPathExpression ex1 = xPath.compile("//game[@session = '" + sessionID + "']");
            XPathExpression ex2 = xPath.compile("//games");
            XPathExpression ex3 = xPath.compile("//game[@session = '" + sessionID + "']/asked[@question='" + questionID + "']");

            NodeList lst = (NodeList) ex1.evaluate(jeopardyDoc, XPathConstants.NODESET);

            if (lst.getLength() < 1) { // No session found! creating the game node
                lst = (NodeList) ex2.evaluate(jeopardyDoc, XPathConstants.NODESET);
                if (lst.getLength() < 1) // no games elem found !?
                {
                    Node jeopardy = jeopardyDoc.getElementsByTagName("jeopardy").item(0);
                    Element games = jeopardyDoc.createElement("games");
                    jeopardy.appendChild(games);
                    lst = (NodeList) ex2.evaluate(jeopardyDoc, XPathConstants.NODESET);
                }

                Node games = lst.item(0);
                Element game = jeopardyDoc.createElement("game");
                game.setAttribute("session", sessionID);
                game.appendChild(player);
                game.appendChild(asked);
                games.appendChild(game);

            } else { // there is only one element
                Node game = lst.item(0);
                game.insertBefore(player, game.getFirstChild());

                lst = (NodeList) ex3.evaluate(jeopardyDoc, XPathConstants.NODESET);
                Node askedNode;
                if (lst.getLength() < 1) { //No asked@question=ID found! creating one
                    askedNode = asked;

                } else { // there is only one element
                    askedNode = lst.item(0);
                    for (Element e : answerElems) {
                        askedNode.appendChild(e);
                    }
                }
                game.appendChild(askedNode);
            }

        } catch (Exception ex1) {
            System.err.println(ex1.getMessage());
        }

    }
}
