package MainClass;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;
import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.dictionary.Dictionary;
import org.xml.sax.SAXException;
import rita.RiWordNet;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

public class Main {

    public static String text;
    static Dictionary dico;
    static CoreDocument doc;

    private static int ambiguousWordsCount;
    private static float scoreLeftRight, scoreTranslation;

    private static LinkedList<XMLToken> xmlTokens = new LinkedList<>();

    private static void testLemma(String lemma) throws JWNLException, rita.wordnet.jwnl.JWNLException {
        String pathToWordnet = "/home/kabix/Desktop/FromLinux/PFE/Disambiguation/Dependencies/WordNet-3.0";

        RiWordNet riwordnet;
        try{
            riwordnet = new RiWordNet(pathToWordnet);
        }catch(Exception e){
            System.out.println(e);
            return;
        }


        if(dico.getIndexWord (POS.NOUN, lemma) != null){
            System.out.println("Synset count of "+ lemma + " is: " + dico.getIndexWord(POS.NOUN, lemma).getLemma());
        }else{
            System.out.println("Le mot " + lemma + " est introuvable dans RitaWordNet");
        }

        if(riwordnet.getDictionary().getIndexWord (rita.wordnet.jwnl.wndata.POS.NOUN, lemma) != null){
            System.out.println("Synset count of "+ lemma + " is: " + riwordnet.getDictionary().getIndexWord(rita.wordnet.jwnl.wndata.POS.NOUN, lemma).getLemma());
        }else{
            System.out.println("Le mot " + lemma + " est introuvable dans RitaWordNet");
        }
    }

    public static void main(String[] args) throws JWNLException, IOException, ParserConfigurationException, SAXException, rita.wordnet.jwnl.JWNLException {

        text = TextParser.getFullText(0);

        initApplication();

        xmlTokens = TextParser.getSemCore();

        dico = getWNDictionary();


        testLemma("Praisegod_Piepsam");
        testLemma("in_order");
        testLemma("Amra");
        testLemma("utner");
        testLemma("Jacoby");
        testLemma("as_well");
        testLemma("in_fact");
        testLemma("operating_procedures");



        for(int i = 0; i < doc.tokens().size()-1; i++) {
            CoreLabel cl = doc.tokens().get(i);
            System.out.print(getFullIndex(cl) + " " + cl +" "+ cl.tag() + " \t");
        }
        System.out.println("\n\n\n");

        for(int i = 0; i < doc.tokens().size(); i++) {
            CoreLabel cl = doc.tokens().get(i);
            if (cl.tag().contains("NN")) {
                IndexWord word = dico.getIndexWord(POS.NOUN, cl.lemma());
                if (word != null) {
                    if (word.getSenseCount() >= 2) { //MOT AMBIGUéE
                        CoreLabel beforeLabel = getPreviousWord(i);
                        CoreLabel afterLabel = getNextWord(i);

                        if(beforeLabel == null){
                            beforeLabel = getAfterNextWord(i);
                        }

                        if(afterLabel == null){
                            afterLabel = getBeforePreviousWord(i);
                        }

                        System.out.println(beforeLabel + " \t");
                        System.out.println(getFullIndex(cl) + " " + cl + " \t");
                        System.out.println(afterLabel + " \t\n\n\n");
                    }
                }
            }
        }


        for(int i = 0; i < doc.tokens().size(); i++){
            CoreLabel cl = doc.tokens().get(i);
            if(cl.tag().contains("NN")){
                IndexWord word = dico.getIndexWord(POS.NOUN, cl.lemma());
                if(word != null) {
                    if (word.getSenseCount() >= 2) { //MOT AMBIGUéE

                        System.out.println("\n\t\t\tDesambiguation de " + word.getLemma() + ":\n");


                        ambiguousWordsCount++;
                        SynsetId translation = TranslationParFenetre(i);
                        SynsetId leftright = leftRightAlgo(i);

                        if(xmlTokens.get(i).wnSynsetIndex == 0){
                            ambiguousWordsCount--;
                        }else{
                            if(translation.id == xmlTokens.get(i).wnSynsetIndex){
                                scoreTranslation += 1.0f;
                            }
                            if(leftright.id == xmlTokens.get(i).wnSynsetIndex){
                                scoreLeftRight += 1.0f;
                            }
                        }

                        System.out.println("\nSEMCORE                :  " + (xmlTokens.get(i).wnSynsetIndex == 0? "INTROUVABLE DANS SEMCOR" : "Synset ID: "+ xmlTokens.get(i).wnSynsetIndex));

                    }
                }
            }
        }

        System.out.println("\n\t\tRésultats finales: \n");
        System.out.println("Score       LEFT TO RIGHT: " + (  scoreLeftRight/ambiguousWordsCount*100f) + "%");
        System.out.println("Score FENETRE TRANSLATION: " + (scoreTranslation/ambiguousWordsCount*100f) + "%");

        lastSyn = null;
    }

    private static void initApplication(){
        RedwoodConfiguration.current().clear().apply();
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        props.setProperty("coref.algorithm", "neural");
        props.setProperty("tokenize.options", "ptb3Escaping=true");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        doc = new CoreDocument(text);
        pipeline.annotate(doc);

        dico = getWNDictionary();
    }

    public static int getFullIndex(CoreLabel lbl){
        int index = 0;
        for(int i = 0; i < lbl.sentIndex();i++){
            index += doc.sentences().get(i).lemmas().size() ;
        }
        index += lbl.index();
        return index-1;
    }

    private static CoreLabel getPreviousWord(int i) throws JWNLException {
        i--;
        if(i <= 0){
            return null;
        }else{
            if(doc.tokens().get(i).tag().contains("NN")){
                if(dico.getIndexWord(POS.NOUN, doc.tokens().get(i).lemma()) == null){
                    System.out.println("Mot " + doc.tokens().get(i).word() + " trouvée mais n'a pas de synset recherche du prochain...");
                    return getPreviousWord(i);
                }else{
                    if(dico.getIndexWord(POS.NOUN, doc.tokens().get(i).lemma()).getSenseCount() == 0){
                        System.out.println("Mot " + doc.tokens().get(i).word() + " est trouvée mais a 0 synsets");
                        return getPreviousWord(i);
                    }else{
                        return doc.tokens().get(i);
                    }
                }
            }else{
                return getPreviousWord(i);
            }
        }
    }




    private static CoreLabel getNextWord(int i) throws JWNLException {
        i++;
        if(i >= doc.tokens().size()-1){
            return null;
        }else    {
            if(doc.tokens().get(i).tag().contains("NN")){
                if(dico.getIndexWord(POS.NOUN, doc.tokens().get(i).lemma()) == null){
                    System.out.println("Mot " + doc.tokens().get(i).word() + " trouvée mais n'a pas de synset recherche du prochain...");
                    return getNextWord(i);
                }else{
                    if(dico.getIndexWord(POS.NOUN, doc.tokens().get(i).lemma()).getSenseCount() == 0){
                        System.out.println("Mot " + doc.tokens().get(i).word() + " est trouvée mais a 0 synsets");
                        return getNextWord(i);
                    }else{
                        return doc.tokens().get(i);
                    }
                }
            }else{
                return getNextWord(i);
            }
        }
    }

//    private static CoreLabel getNextWord(int i) throws JWNLException {
//        i++;
//        if(i >= doc.tokens().size()-1){
//            return null;
//        }else{
//            if(doc.tokens().get(i).tag().contains("NN")){
//                return doc.tokens().get(i);
//            }else{
//                return getNextWord(i);
//            }
//        }
//    }

    private static CoreLabel getBeforePreviousWord(int i) throws JWNLException {
        CoreLabel currentLabel  = doc.tokens().get(i);
        CoreLabel previousLabel = getPreviousWord(getFullIndex(currentLabel));
        CoreLabel beforePreviousLabel = getPreviousWord(getFullIndex(previousLabel));
        return beforePreviousLabel;

    }

    private static CoreLabel getAfterNextWord(int i) throws JWNLException {
        CoreLabel currentLabel = doc.tokens().get(i);
        CoreLabel nextLabel    = getNextWord(getFullIndex(currentLabel));
        CoreLabel afterNextLabel = getNextWord(getFullIndex(nextLabel));
        return afterNextLabel;
    }

    private static SynsetId TranslationParFenetre(int i) throws JWNLException, rita.wordnet.jwnl.JWNLException {

        SynsetId resultat = new SynsetId();
        String pathToWordnet = "/home/kabix/Desktop/FromLinux/PFE/Disambiguation/Dependencies/WordNet-3.0";

        CoreLabel beforeLabel, afterLabel, currentLabel;

        beforeLabel = getPreviousWord(i);
        currentLabel = doc.tokens().get(i);
        afterLabel = getNextWord(i);


        if(beforeLabel == null){
            beforeLabel = getAfterNextWord(i);
        }

        if(afterLabel == null){
            afterLabel = getBeforePreviousWord(i);
        }

        if(afterLabel == null || beforeLabel == null){
            System.out.println("Impossible de continuer par ce que il ny'a pas assez de mots dans le text pour désambiguiser les mots");
            return null;
        }

        RiWordNet riwordnet;
        try{
            riwordnet = new RiWordNet(pathToWordnet);
        }catch(Exception e){
            System.out.println(e);
            return null;
        }

        IndexWord  firstIndexWord = dico.getIndexWord(POS.NOUN,  beforeLabel.lemma());
        IndexWord middleIndexWord = dico.getIndexWord(POS.NOUN, currentLabel.lemma());
        IndexWord secondIndexWord = dico.getIndexWord(POS.NOUN, afterLabel.lemma());


        Synset[] firstSynsets  =  firstIndexWord.getSenses();
        Synset[] middleSynset  = middleIndexWord.getSenses();
        Synset[] secondSynsets = secondIndexWord.getSenses();

        //System.out.format("%30s %30s %30s \n", firstLabel.word(), middleLabel.word(), secondLabel.word());

        float distance;
        float smallestDistance = 10;

        rita.wordnet.jwnl.wndata.IndexWord firstIW,  secondIW,  middleIW;
        int                                firstI=0, secondI=0, middleI=0, smallestFirst=0, smallestSecond=0;

        for(Synset middleWordSynset : middleSynset){
            middleIW = new rita.wordnet.jwnl.wndata.IndexWord(middleIndexWord.getLemma(), rita.wordnet.jwnl.wndata.POS.NOUN,new long[] {middleWordSynset.getOffset()});
            for(Synset firstWordSynset : firstSynsets){
                firstIW = new rita.wordnet.jwnl.wndata.IndexWord(firstIndexWord.getLemma(),rita.wordnet.jwnl.wndata.POS.NOUN,new long[] {firstWordSynset.getOffset()});
                for(Synset secondWordSynset : secondSynsets){
                    secondIW = new rita.wordnet.jwnl.wndata.IndexWord(secondIndexWord.getLemma(),rita.wordnet.jwnl.wndata.POS.NOUN,new long[] {secondWordSynset.getOffset()});

                    distance =  riwordnet.getWordDistance(firstIW,middleIW) +
                                riwordnet.getWordDistance(secondIW,middleIW);
                    System.out.format("DISTANCE(%15s Synset %d, %15s Synset %d) + DISTANCE(%15s Synset %d, %15s %d) = %f\n", afterLabel.lemma(), secondI+1,currentLabel.lemma(),middleI+1,beforeLabel.word(),firstI+1,currentLabel.lemma(),middleI+1,  distance);

                    if(distance <= smallestDistance){
                        smallestDistance =                     distance;
                        resultat.synset =             middleWordSynset;
                        resultat.id =                          secondI + 1;
                        resultat.smallestDistance =            distance;
                        resultat.lastLemma = middleIndexWord.getLemma();

                        smallestFirst = firstI + 1;
                        smallestSecond = secondI + 1;

                    }
                    secondI++;
                }
                secondI = 0;
                firstI++;
            }
            firstI = 0;
            middleI++;
        }

        System.out.println("\n\t\t\tRésultat de Translation Par Fenetre: ");
        System.out.format("\nDISTANCE(%15s Synset %02d, %15s Synset %02d) + DISTANCE(%15s Synset %02d, %15s %02d) = %f\n\n", afterLabel.lemma(), smallestSecond,currentLabel.lemma(), resultat.id,beforeLabel.word(),smallestFirst,currentLabel.lemma(),resultat.id, smallestDistance);


        return resultat;
    }

    private static SynsetId lastSyn; //Utilisé par l'algorithm Left Right

    private static SynsetId leftRightAlgo(int indexMotAmbiguee) throws JWNLException, rita.wordnet.jwnl.JWNLException {

        SynsetId resultat = new SynsetId();
        String pathToWordnet = "/home/kabix/Desktop/FromLinux/PFE/Disambiguation/DependenciesWordNet-3.0";

        CoreLabel middleLabel = doc.tokens().get(indexMotAmbiguee);

        RiWordNet riwordnet;
        try{
            riwordnet = new RiWordNet(pathToWordnet);
        }catch(Exception e){
            System.out.println(e);
            return null;
        }

        if(lastSyn == null){
            CoreLabel previousWord = getPreviousWord(indexMotAmbiguee);
            if(previousWord == null){
                previousWord = getNextWord(indexMotAmbiguee);
            }
            if(previousWord == null){
                System.out.println("Impossible de continuer par ce que il ny'a pas assez de mots dans le text pour désambiguiser les mots");
                return null;
            }
            lastSyn = algoWassila(indexMotAmbiguee, getFullIndex(previousWord));
        }

        IndexWord middleIndexWord = dico.getIndexWord(POS.NOUN, middleLabel.lemma());
        Synset[] middleSynsets  = middleIndexWord.getSenses();

        float currentDistance = 10, smallestDistance = 10;
//        for(int index = 0; index < middleSynsets.length; index++){
//            currentDistance = riwordnet.getDistance(middleSynsets[index].getWord(0).getLemma(),
//                                                    lastSyn.getWord(0).getLemma(), "n");
//            if(currentDistance < smallestDistance){
//                smallestDistance = currentDistance;
//                foundSynset.id = index;
//                foundSynset.synset = middleSynsets[index];
//                foundSynset.smallestDistance = smallestDistance;
//            }
//        }
        for(int index = 0; index < middleSynsets.length; index++){
            rita.wordnet.jwnl.wndata.IndexWord middleIndex = new rita.wordnet.jwnl.wndata.IndexWord(middleIndexWord.getLemma(),
                                                                                                    rita.wordnet.jwnl.wndata.POS.NOUN,
                                                                                                    new long[]{middleSynsets[index].getOffset()});
            rita.wordnet.jwnl.wndata.IndexWord lastWordIndex = new rita.wordnet.jwnl.wndata.IndexWord(lastSyn.lastLemma,
                                                                                                    rita.wordnet.jwnl.wndata.POS.NOUN,
                                                                                                    new long[]{lastSyn.synset.getOffset()});
            currentDistance = riwordnet.getWordDistance(middleIndex, lastWordIndex);

            System.out.format("DISTANCE(%15s Synset %d , %15s Synset %d) = %f\n", middleIndexWord.getLemma(), index+1 ,lastSyn.lastLemma, lastSyn.id, currentDistance);


            if(currentDistance < smallestDistance){
                smallestDistance = currentDistance;
                resultat.id = index + 1;
                resultat.synset = middleSynsets[index];
                resultat.smallestDistance = smallestDistance;
                resultat.lastLemma = middleIndex.getLemma();
            }
        }
        System.out.println("\n\t\t\tRésultat de Left to Right: ");
        System.out.format("\nDISTANCE(%20s Synset %d, %20s Synset %d) = %f\n", resultat.lastLemma, resultat.id ,lastSyn.lastLemma, lastSyn.id, smallestDistance);


        lastSyn.synset = resultat.synset;
        lastSyn.lastLemma = resultat.lastLemma;
        return resultat;
    }

    private static SynsetId algoWassila(int ambiguousindex, int previousWordIndex) throws JWNLException, rita.wordnet.jwnl.JWNLException {

        SynsetId resultat = new SynsetId();
        String pathToWordnet = "/home/kabix/Desktop/FromLinux/PFE/Disambiguation/Dependencies/WordNet-3.0";

        CoreLabel middleLabel = doc.tokens().get(ambiguousindex);
        CoreLabel avantLabel  = doc.tokens().get(previousWordIndex);

        RiWordNet riwordnet;
        try{
            riwordnet = new RiWordNet(pathToWordnet);
        }catch(Exception e){
            System.out.println(e);
            return null;
        }

        IndexWord middleIndexWord = dico.getIndexWord(POS.NOUN, middleLabel.lemma());
        IndexWord avantIndexWord = dico.getIndexWord(POS.NOUN, avantLabel.lemma());

        if(avantIndexWord.getSenseCount() == 1){
            resultat.id = 1;
            resultat.synset = avantIndexWord.getSense(0);
            resultat.lastLemma = avantIndexWord.getLemma();
            System.out.println("ID SYNSET DE "+ avantIndexWord.getLemma() + " = " + 1);
            return resultat;
        }


        Synset[] middleSynsets  = middleIndexWord.getSenses();
        Synset[] avantSynsets = avantIndexWord.getSenses();

        float distance;
        float smallestDistance = 10;

        for(int i = 0; i < middleSynsets.length; i++){
            rita.wordnet.jwnl.wndata.IndexWord middleIW = new rita.wordnet.jwnl.wndata.IndexWord(middleIndexWord.getLemma(), rita.wordnet.jwnl.wndata.POS.NOUN,new long[]{middleSynsets[i].getOffset()});
            for(int j = 0; j < avantSynsets.length; j++){
                rita.wordnet.jwnl.wndata.IndexWord avantIW = new rita.wordnet.jwnl.wndata.IndexWord(avantIndexWord.getLemma(), rita.wordnet.jwnl.wndata.POS.NOUN,new long[]{avantSynsets[j].getOffset()});
                distance = riwordnet.getWordDistance(middleIW, avantIW);
                if(distance < smallestDistance){
                    resultat.id = j+1;
                    smallestDistance = distance;
                    resultat.synset = middleSynsets[i];
                    resultat.lastLemma = avantIndexWord.getLemma();
                }
            }
        }
        System.out.println("ID SYNSET DE "+ avantIndexWord.getLemma() + " = " + resultat.id);


        return resultat;
    }

    private static Dictionary getWNDictionary() {
        String pathToXml = "/home/kabix/Desktop/FromLinux/PFE/Disambiguation/Dependencies/jwnl14-rc2/config/file_properties.xml";
        FileInputStream xmlFile;
        try{
            xmlFile = new FileInputStream(pathToXml);
        }catch(FileNotFoundException fnfe){
            System.out.println("le chemin vers file_properties.xml est faux!");
            return null;
        }

        try{
            JWNL.initialize(xmlFile);
        }catch(Exception e){
            System.out.println("Problème d'initialisation de JWNL! \n" + e);
            return null;
        }
        if(Dictionary.getInstance() != null){
            return Dictionary.getInstance();
        }
        System.out.println("Impossible d'ouvrire WordNet:");
        return null;
    }

}

class SynsetId{
    public String lastLemma;
    Synset synset;
    int    id;
    float  smallestDistance;
};