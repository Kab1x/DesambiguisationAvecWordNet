package MainClass;


import edu.mit.jsemcor.element.IContext;
import edu.mit.jsemcor.element.ISentence;
import edu.mit.jsemcor.element.IWordform;
import edu.mit.jsemcor.main.IConcordance;
import edu.mit.jsemcor.main.IConcordanceSet;
import edu.mit.jsemcor.main.Semcor;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Properties;

public class TextParser {

    static String pathToText = "/home/kabix/Desktop/FromLinux/PFE/Disambiguation/Dependencies/semcor/brown1/tagfiles/br-c04.xml";

    public static String getFullText(int textId) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(pathToText);

        NodeList paragraphs = doc.getElementsByTagName("p");

        StringBuilder fullText = new StringBuilder();

        for(int i = 0; i < paragraphs.getLength(); i++){
            fullText.append("\n\n\t\t");
            Element paragraph = (Element)paragraphs.item(i);
            NodeList sList = paragraph.getElementsByTagName("s");
            for(int s = 0; s < sList.getLength(); s++){
                Element ss = (Element)sList.item(s);
                NodeList tokens = ss.getElementsByTagName("*");
                for(int j = 0; j < tokens.getLength(); j++){
                    Element token = (Element) tokens.item(j);
                    if(token.getTagName().equals("punc")){
                        fullText.append(" " +  token.getTextContent().trim() + " ");
                    }
                    if(token.getTagName().equals("wf")){
                        fullText.append(" " + token.getTextContent().trim());
                    }
                }
            }
        }

        return fullText.toString();
    }

    private static CoreDocument doc;
    private static String text;

    private static void initApplication(){
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        props.setProperty("coref.algorithm", "neural");
        props.setProperty("tokenize.options", "ptb3Escaping=true");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        doc = new CoreDocument(text);
        pipeline.annotate(doc);
    }

    public static LinkedList<XMLToken> tokens = null;

    public static LinkedList<XMLToken> getSemCore() throws ParserConfigurationException, IOException, SAXException {

        System.out.println("Lecture depuis le fichier XML en cours...");

        if(tokens != null){
            return tokens;
        }else{
            tokens = new LinkedList<>();
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document xmlDoc = builder.parse(pathToText);

        NodeList paragraphs = xmlDoc.getElementsByTagName("p");

        int fullTokenIndex = 0;

        for(int i = 0; i < paragraphs.getLength(); i++){
            Element paragraph = (Element)paragraphs.item(i);
            NodeList sList = paragraph.getElementsByTagName("s");

            for(int s = 0; s < sList.getLength(); s++){
                Element ss = (Element)sList.item(s);
                NodeList xmlElements = ss.getElementsByTagName("*");

                for(int j = 0; j < xmlElements.getLength(); j++){
                    Element xmlElement = (Element) xmlElements.item(j);
                    text = " " + xmlElement.getTextContent().trim() + " ";
                    initApplication();
                    int wnSynsetIndex = 0;
                    if(xmlElement.hasAttribute("wnsn")){
                        try{
                            wnSynsetIndex = Integer.parseInt(xmlElement.getAttribute("wnsn").trim().split(";")[0]);
                        }catch (Exception e){
                            wnSynsetIndex = 0;
                        }
                    }
                    for(int nlpIndex = 0; nlpIndex < doc.tokens().size(); nlpIndex++){
                        tokens.add(new XMLToken(doc.tokens().get(nlpIndex).word(),fullTokenIndex+1,wnSynsetIndex));
                        fullTokenIndex++;
                    }
                }
            }
        }
        return tokens;
    }
}

class XMLToken{
    public XMLToken(String word, int index, int _wnSynsetIndex){
        this.word = word;
        this.tokenIndex = index;
        wnSynsetIndex = _wnSynsetIndex;
    }

    public String toString(){
        return String.format("%20s Synset id: %d", word, wnSynsetIndex);
    }

    public XMLToken(){};
    String word;
    int    wnSynsetIndex;
    int tokenIndex;
}

