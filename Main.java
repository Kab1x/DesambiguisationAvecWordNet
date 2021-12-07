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

    private static String pathToWordnet = "/home/kabix/Desktop/FromLinux/PFE/Disambiguation/Dependencies/WordNet-3.0";
    private static String pathToXml = "/home/kabix/Desktop/FromLinux/PFE/Disambiguation/Dependencies/jwnl14-rc2/config/file_properties.xml";

    public static String text;
    static Dictionary dico;
    static CoreDocument doc;

    private static int ambiguousWordsCount;
    private static float scoreLeftRight, scoreTranslation;

    private static LinkedList<XMLToken> xmlTokens = new LinkedList<>(); //Liste des tokens depuis semcor

    private static LinkedList<CoreLabel> nomlist = new LinkedList<>();

    private static LinkedList<SynsetId> resultatLeftRight = new LinkedList<>();
    private static LinkedList<SynsetId> resultatTranslation = new LinkedList<>();
    private static LinkedList<XMLToken> resultatSemCore = new LinkedList<>();


    public static void main(String[] args) throws JWNLException, IOException, ParserConfigurationException, SAXException, rita.wordnet.jwnl.JWNLException {

        text = TextParser.getFullText(0);

        initApplication();

        xmlTokens = TextParser.getSemCore();

        dico = getWNDictionary();

        System.out.println("\t\t\t Text:"+text+" \n\n\n");

        int lastWordIndex = doc.tokens().size() -1;

        for (int i = 0; i < lastWordIndex; i++) { //Afficher la liste des noms
            CoreLabel cl = doc.tokens().get(i);
            if (cl.tag().contains("NN")) {
                nomlist.add(cl);
                IndexWord iw = dico.lookupIndexWord(POS.NOUN, cl.lemma());
                if (iw != null) {
                    System.out.print(cl + " \t \t \t");
                    System.out.print(" Nombre de synsets de " + cl.word() + " est " + iw.getSenseCount() + " " + " \t\n");
                    System.out.println("Liste des synsets du mot: " + cl.word());
                    for (int j = 0; j < iw.getSenses().length; j++) {
                        System.out.println(iw.getSenses()[j]);
                    }
                } else {
                    System.out.println(doc.tokens().get(i).word() + " est introuvable dans WordNet");
                }
            }


        }
        System.out.println("les nom sont " + nomlist);


        System.out.println("\n\n\n");
        System.out.println("les mot ambigues sont :");
        for (int i = 0; i < doc.tokens().size(); i++) { //Afficher la liste des mots embigues
            CoreLabel cl = doc.tokens().get(i);
            if (cl.tag().contains("NN")) {
                IndexWord word = dico.lookupIndexWord(POS.NOUN, cl.lemma());
                if (word != null) {
                    if (word.getSenseCount() >= 2) { //MOT AMBIGUéE
                        System.out.println(getFullIndex(cl) + " " + cl + cl.tag());
                    }
                }
            }
        }


        for (int i = 0; i < lastWordIndex; i++) {
            CoreLabel cl = doc.tokens().get(i);
            if (cl.tag().contains("NN")) {
                IndexWord word = dico.lookupIndexWord(POS.NOUN, cl.lemma());
                if (word != null) {
                    if (word.getSenseCount() >= 2) { //MOT AMBIGUéE

                        System.out.println("\n\t\t\tDesambiguation de " + cl.word() + ":\n");


                        ambiguousWordsCount++;
                        SynsetId translation = TranslationParFenetre(i);
                        resultatTranslation.add(translation);
                        System.out.println("\n\n\n");

                        SynsetId leftright = leftRightAlgo(i);
                        resultatLeftRight.add(leftright);
                        resultatSemCore.add(xmlTokens.get(i));


                        if (xmlTokens.get(i).wnSynsetIndex == 0) {//si semcor ne retourn pas id  syns
                            ambiguousWordsCount--;
                        } else {
                            if (translation.id == xmlTokens.get(i).wnSynsetIndex) {
                                scoreTranslation += 1.0f;
                            }
                            if (leftright.id == xmlTokens.get(i).wnSynsetIndex) {
                                scoreLeftRight += 1.0f;
                            }
                        }

                        System.out.println("\nSEMCORE                :  " + (xmlTokens.get(i).wnSynsetIndex == 0 ? "INTROUVABLE DANS SEMCOR" : "Synset ID: " + xmlTokens.get(i).wnSynsetIndex));

                    }
                }
            }
        }
        System.out.println("Résultats generales: ");

        int tempIndex = 0;
        for (int i = 0; i < doc.tokens().size(); i++) {
            CoreLabel cl = doc.tokens().get(i);
            if (cl.tag().contains("NN")) {
                IndexWord word = dico.lookupIndexWord(POS.NOUN, cl.lemma());
                if (word != null) {
                    if (word.getSenseCount() >= 2) {
                        System.out.println("TRANSLATION:   " + resultatTranslation.get(tempIndex));
                        tempIndex++;
                    }
                }
            }
        }
        tempIndex = 0;
        for (int i = 0; i < doc.tokens().size(); i++) {
            CoreLabel cl = doc.tokens().get(i);
            if (cl.tag().contains("NN")) {
                IndexWord word = dico.lookupIndexWord(POS.NOUN, cl.lemma());
                if (word != null) {
                    if (word.getSenseCount() >= 2) {
                        System.out.println("LEFT TO RIGHT: " + resultatLeftRight.get(tempIndex));
                        tempIndex++;
                    }
                }
            }
        }
        tempIndex = 0;
        for (int i = 0; i < doc.tokens().size(); i++) {
            CoreLabel cl = doc.tokens().get(i);
            if (cl.tag().contains("NN")) {
                IndexWord word = dico.lookupIndexWord(POS.NOUN, cl.lemma());
                if (word != null) {
                    if (word.getSenseCount() >= 2) {
                        System.out.println("SEMCORE:       " + resultatSemCore.get(tempIndex));
                        tempIndex++;
                    }
                }
            }
        }

        System.out.println("\n\t\tRésultats finales: \n");
        System.out.println("Score       LEFT TO RIGHT: " + (scoreLeftRight / ambiguousWordsCount * 100f) + "%");
        System.out.println("Score FENETRE TRANSLATION: " + (scoreTranslation / ambiguousWordsCount * 100f) + "%");

        lastLeftRightSynset = null;
    }



    private static SynsetId lastTranslationSynset = null; //Utilisé par l'algorithm Left Right

    private static SynsetId initTranslation(int i) throws rita.wordnet.jwnl.JWNLException, JWNLException {

        SynsetId resultat = new SynsetId();

        CoreLabel beforeLabel, afterLabel, currentLabel;

        beforeLabel = getPreviousWord(i);
        currentLabel = doc.tokens().get(i);
        afterLabel = getNextWord(i);

        resultat.word = xmlTokens.get(i).word;

        if (beforeLabel == null) {
            beforeLabel = getAfterNextWord(i);
        }

        if (afterLabel == null) {
            afterLabel = getBeforePreviousWord(i);
        }

        if (afterLabel == null || beforeLabel == null) {
            return null;
        }

        RiWordNet riwordnet;
        try {
            riwordnet = new RiWordNet(pathToWordnet);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }

        IndexWord beforeIW = dico.lookupIndexWord(POS.NOUN, beforeLabel.lemma());
        IndexWord currentIW = dico.lookupIndexWord(POS.NOUN, currentLabel.lemma());
        IndexWord afterIW = dico.lookupIndexWord(POS.NOUN, afterLabel.lemma());


        Synset[] firstSynsets = beforeIW.getSenses();
        Synset[] middleSynsets = currentIW.getSenses();
        Synset[] secondSynsets = afterIW.getSenses();

        float distance;
        float smallestDistance = 10;

        rita.wordnet.jwnl.wndata.IndexWord firstIW, secondIW, middleIW;
        int firstI = 0, secondI = 0, middleI = 0, smallestFirst = 0, smallestSecond = 0;


        for (Synset middleWordSynset : middleSynsets) {
            middleIW = new rita.wordnet.jwnl.wndata.IndexWord(currentIW.getLemma(), rita.wordnet.jwnl.wndata.POS.NOUN, new long[]{middleWordSynset.getOffset()});
            for (Synset firstWordSynset : firstSynsets) {
                firstIW = new rita.wordnet.jwnl.wndata.IndexWord(currentIW.getLemma(), rita.wordnet.jwnl.wndata.POS.NOUN, new long[]{firstWordSynset.getOffset()});
                for (Synset secondWordSynset : secondSynsets) {
                    secondIW = new rita.wordnet.jwnl.wndata.IndexWord(afterIW.getLemma(), rita.wordnet.jwnl.wndata.POS.NOUN, new long[]{secondWordSynset.getOffset()});

                    distance = riwordnet.getWordDistance(firstIW, middleIW) +
                            riwordnet.getWordDistance(secondIW, middleIW);
                    System.out.format("DISTANCE(%15s Synset %d, %15s Synset %d) + DISTANCE(%15s Synset %d, %15s %d) = %f\n", afterLabel.lemma(), secondI + 1, currentLabel.lemma(), middleI + 1, beforeLabel.word(), firstI + 1, currentLabel.lemma(), middleI + 1, distance);

                    if (distance <= smallestDistance) {
                        smallestDistance = distance;
                        resultat.synset = middleWordSynset;
                        resultat.id = middleI + 1;
                        resultat.smallestDistance = distance;
                        resultat.lastLemma = currentIW.getLemma();

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
        System.out.format(" La petite distance est : la DISTANCE(%15s Synset %d, %15s Synset %d) + DISTANCE(%15s Synset %d, %15s %d) = %f\n", afterLabel.lemma(), smallestSecond, currentLabel.lemma(), resultat.id, beforeLabel.word(), smallestFirst, currentLabel.lemma(), resultat.id, smallestDistance);


        System.out.println("\n\t\t\tle synset retenue par agorithm de Translation Par Fenetre est : " + resultat.id);
        System.out.println("Gloss du synset: " + resultat.synset.getGloss());


        return resultat;
    }

    private static SynsetId TranslationParFenetre(int i) throws JWNLException, rita.wordnet.jwnl.JWNLException {

        SynsetId resultat = new SynsetId();

        CoreLabel beforeLabel, afterLabel, currentLabel;

        beforeLabel = getPreviousWord(i);
        currentLabel = doc.tokens().get(i);
        afterLabel = getNextWord(i);

        resultat.word = xmlTokens.get(i).word;

        if (beforeLabel == null) {
            beforeLabel = getAfterNextWord(i);
        }

        if (afterLabel == null) {
            afterLabel = getBeforePreviousWord(i);
        }

        if (afterLabel == null || beforeLabel == null) {
            System.out.println("Impossible de continuer par ce que il ny'a pas assez de mots dans le text pour désambiguiser les mots");
            return null;
        }


        RiWordNet riwordnet;
        try {
            riwordnet = new RiWordNet(pathToWordnet);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }

        if (lastTranslationSynset == null) {
            lastTranslationSynset = initTranslation(i);
            return lastTranslationSynset;
        }


        IndexWord firstIndexWord = dico.lookupIndexWord(POS.NOUN, beforeLabel.lemma());
        IndexWord middleIndexWord = dico.lookupIndexWord(POS.NOUN, currentLabel.lemma());
        IndexWord secondIndexWord = dico.lookupIndexWord(POS.NOUN, afterLabel.lemma());

        if(firstIndexWord.getSenseCount() == 1){
            lastTranslationSynset.synset = firstIndexWord.getSenses()[0];
            lastTranslationSynset.id     = 1;
            lastTranslationSynset.lastLemma = firstIndexWord.getLemma();
            lastTranslationSynset.word = beforeLabel.word();
        }

        Synset[] middleSynsets = middleIndexWord.getSenses();
        Synset[] secondSynsets = secondIndexWord.getSenses();

        float distance;
        float smallestDistance = 10;

        rita.wordnet.jwnl.wndata.IndexWord firstIW, secondIW, middleIW;
        int firstI = lastTranslationSynset.id, secondI = 0, middleI = 0, smallestFirst = 0, smallestSecond = 0;


        for (Synset middleWordSynset : middleSynsets) {
            middleIW = new rita.wordnet.jwnl.wndata.IndexWord(middleIndexWord.getLemma(), rita.wordnet.jwnl.wndata.POS.NOUN, new long[]{middleWordSynset.getOffset()});
            firstIW = new rita.wordnet.jwnl.wndata.IndexWord(firstIndexWord.getLemma(), rita.wordnet.jwnl.wndata.POS.NOUN, new long[]{lastTranslationSynset.synset.getOffset()});
            for (Synset secondWordSynset : secondSynsets) {
                secondIW = new rita.wordnet.jwnl.wndata.IndexWord(secondIndexWord.getLemma(), rita.wordnet.jwnl.wndata.POS.NOUN, new long[]{secondWordSynset.getOffset()});

                distance = riwordnet.getWordDistance(firstIW, middleIW) +
                        riwordnet.getWordDistance(secondIW, middleIW);
                System.out.format("DISTANCE(%15s Synset %d, %15s Synset %d) + DISTANCE(%15s Synset %d, %15s %d) = %f\n", lastTranslationSynset.word, firstI, currentLabel.lemma(), middleI + 1, afterLabel.word(), secondI + 1, currentLabel.lemma(), middleI + 1, distance);

                if (distance <= smallestDistance) {
                    smallestDistance = distance;
                    resultat.synset = middleWordSynset;
                    resultat.id = middleI + 1;
                    resultat.smallestDistance = distance;
                    resultat.lastLemma = middleIndexWord.getLemma();

                    smallestFirst = firstI;
                    smallestSecond = secondI + 1;
                }
                secondI++;
            }
            secondI = 0;
            middleI++;
        }

        lastTranslationSynset = resultat;

        System.out.format(" La petite distance est : la DISTANCE(%15s Synset %d, %15s Synset %d) + DISTANCE(%15s Synset %d, %15s %d) = %f\n", afterLabel.lemma(), smallestSecond, currentLabel.lemma(), resultat.id, beforeLabel.word(), smallestFirst, currentLabel.lemma(), resultat.id, smallestDistance);


        System.out.println("\n\t\t\tle synset retenue par agorithm de Translation Par Fenetre est : " + resultat.id);
        System.out.println("Gloss du synset: " + resultat.synset.getGloss());


        return resultat;
    }

    private static SynsetId lastLeftRightSynset = null; //Utilisé par l'algorithm Left Right

    private static SynsetId initLeftRight(int ambiguousindex, int previousWordIndex) throws JWNLException, rita.wordnet.jwnl.JWNLException {

        SynsetId resultat = new SynsetId();


        CoreLabel middleLabel = doc.tokens().get(ambiguousindex);
        CoreLabel avantLabel = doc.tokens().get(previousWordIndex);

        RiWordNet riwordnet;
        try {
            riwordnet = new RiWordNet(pathToWordnet);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }

        IndexWord middleIndexWord = dico.lookupIndexWord(POS.NOUN, middleLabel.lemma());
        IndexWord avantIndexWord = dico.lookupIndexWord(POS.NOUN, avantLabel.lemma());

        if (avantIndexWord.getSenseCount() == 1) {
            resultat.id = 1;
            resultat.synset = avantIndexWord.getSenses()[0];
            resultat.lastLemma = avantIndexWord.getLemma();
            return resultat;
        }


        Synset[] middleSynsets = middleIndexWord.getSenses();
        Synset[] avantSynsets = avantIndexWord.getSenses();

        float distance;
        float smallestDistance = 10;

        for (int i = 0; i < middleSynsets.length; i++) {
            rita.wordnet.jwnl.wndata.IndexWord middleIW = new rita.wordnet.jwnl.wndata.IndexWord(middleIndexWord.getLemma(), rita.wordnet.jwnl.wndata.POS.NOUN, new long[]{middleSynsets[i].getOffset()});
            for (int j = 0; j < avantSynsets.length; j++) {
                rita.wordnet.jwnl.wndata.IndexWord avantIW = new rita.wordnet.jwnl.wndata.IndexWord(avantIndexWord.getLemma(), rita.wordnet.jwnl.wndata.POS.NOUN, new long[]{avantSynsets[j].getOffset()});
                distance = riwordnet.getWordDistance(middleIW, avantIW);
                System.out.format("  DISTANCE(%15s Synset %d , %15s Synset %d) = %f\n", middleIndexWord.getLemma(), i + 1, avantIW.getLemma(), j + 1, distance);
                if (distance < smallestDistance) {
                    resultat.id = j + 1;
                    smallestDistance = distance;
                    resultat.synset = avantSynsets[j];
                    resultat.lastLemma = avantIndexWord.getLemma();
                }
            }
        }

        System.out.println("\nRésultat de l'algo de init left right: " + resultat.id + "\n\n");

        return resultat;
    }

    private static SynsetId leftRightAlgo(int indexMotAmbiguee) throws JWNLException, rita.wordnet.jwnl.JWNLException {

        SynsetId resultat = new SynsetId();

        CoreLabel middleLabel = doc.tokens().get(indexMotAmbiguee);

        resultat.word = xmlTokens.get(indexMotAmbiguee).word;

        RiWordNet riwordnet;
        try {
            riwordnet = new RiWordNet(pathToWordnet);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }


        if (lastLeftRightSynset == null) {//dernier synset trouvé
            System.out.println("Ran 1");
            CoreLabel previousWord = getPreviousWord(indexMotAmbiguee);
            if (previousWord == null) {
                previousWord = getNextWord(indexMotAmbiguee);
            }
            if (previousWord == null) {
                System.out.println("Impossible de continuer par ce que il ny'a pas assez de mots dans le text pour désambiguiser les mots");
                return null;
            }
            lastLeftRightSynset = initLeftRight(indexMotAmbiguee, getFullIndex(previousWord));
        } else {
            System.out.println("Ran 2");
            CoreLabel previousWord = getPreviousWord(indexMotAmbiguee);
            if (previousWord == null) {
                previousWord = getNextWord(indexMotAmbiguee);
            }
            if (previousWord == null) {
                System.out.println("Impossible de continuer par ce que il ny'a pas assez de mots dans le text pour désambiguiser les mots");
                return null;
            }
            IndexWord iw = dico.lookupIndexWord(POS.NOUN, previousWord.lemma());
            if (iw.getSenseCount() == 1) {
                lastLeftRightSynset.synset = iw.getSenses()[0];
                lastLeftRightSynset.id = 1;
                lastLeftRightSynset.lastLemma = iw.getLemma();
            }
        }

        IndexWord middleIndexWord = dico.lookupIndexWord(POS.NOUN, middleLabel.lemma());
        Synset[] middleSynsets = middleIndexWord.getSenses();

        float currentDistance = 10, smallestDistance = 10;
//
        for (int index = 0; index < middleSynsets.length; index++) {
            rita.wordnet.jwnl.wndata.IndexWord middleIndex = new rita.wordnet.jwnl.wndata.IndexWord(middleIndexWord.getLemma(),
                    rita.wordnet.jwnl.wndata.POS.NOUN,
                    new long[]{middleSynsets[index].getOffset()});
            rita.wordnet.jwnl.wndata.IndexWord lastWordIndex = new rita.wordnet.jwnl.wndata.IndexWord(lastLeftRightSynset.lastLemma,
                    rita.wordnet.jwnl.wndata.POS.NOUN,
                    new long[]{lastLeftRightSynset.synset.getOffset()});
            currentDistance = riwordnet.getWordDistance(middleIndex, lastWordIndex);

            System.out.format("  DISTANCE(%15s Synset %d , %15s Synset %d) = %f\n", middleIndexWord.getLemma(), index + 1, lastLeftRightSynset.lastLemma, lastLeftRightSynset.id, currentDistance);


            if (currentDistance < smallestDistance) {
                smallestDistance = currentDistance;
                resultat.id = index + 1;
                resultat.synset = middleSynsets[index];
                resultat.smallestDistance = smallestDistance;
                resultat.lastLemma = middleIndex.getLemma();
            }
        }
        System.out.format(" la petite distance est la DISTANCE(%20s Synset %d, %20s Synset %d) = %f\n", resultat.lastLemma, resultat.id, lastLeftRightSynset.lastLemma, lastLeftRightSynset.id, smallestDistance);

        System.out.println("\n\t\t\tle synset retenu par algorithme  de Left to Right est : " + resultat.id);
        System.out.println("Gloss du synset: " + resultat.synset.getGloss());


        lastLeftRightSynset.synset = resultat.synset;
        lastLeftRightSynset.lastLemma = resultat.lastLemma;
        lastLeftRightSynset.id = resultat.id;
        return resultat;
    }

    private static void initApplication() {
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

    public static int getFullIndex(CoreLabel lbl) {
        int index = 0;
        for (int i = 0; i < lbl.sentIndex(); i++) {
            index += doc.sentences().get(i).lemmas().size();
        }
        index += lbl.index();
        return index - 1;
    }

    private static CoreLabel getPreviousWord(int i) throws JWNLException {
        i--;
        if (i <= 0) {
            return null;
        } else {
            if (doc.tokens().get(i).tag().contains("NN")) {
                if (dico.lookupIndexWord(POS.NOUN, doc.tokens().get(i).lemma()) == null) {
                    //System.out.println("Mot " + doc.tokens().get(i).word() + " trouvée mais n'a pas de synset recherche du prochain...");
                    return getPreviousWord(i);
                } else {
                    if (dico.lookupIndexWord(POS.NOUN, doc.tokens().get(i).lemma()).getSenseCount() == 0) {
                        //System.out.println("Mot " + doc.tokens().get(i).word() + " est trouvée mais a 0 synsets");
                        return getPreviousWord(i);
                    } else {
                        return doc.tokens().get(i);
                    }
                }
            } else {
                return getPreviousWord(i);
            }
        }
    }

    private static CoreLabel getBeforePreviousWord(int i) throws JWNLException {
        CoreLabel currentLabel = doc.tokens().get(i);
        CoreLabel previousLabel = getPreviousWord(getFullIndex(currentLabel));
        CoreLabel beforePreviousLabel = getPreviousWord(getFullIndex(previousLabel));
        return beforePreviousLabel;

    }

    private static CoreLabel getNextWord(int i) throws JWNLException {
        i++;
        if (i >= doc.tokens().size() - 1) {
            return null;
        } else {
            if (doc.tokens().get(i).tag().contains("NN")) {
                if (dico.lookupIndexWord(POS.NOUN, doc.tokens().get(i).lemma()) == null) {
                    //System.out.println("Mot " + doc.tokens().get(i).word() + " trouvée mais n'a pas de synset recherche du prochain...");
                    return getNextWord(i);
                } else {
                    if (dico.lookupIndexWord(POS.NOUN, doc.tokens().get(i).lemma()).getSenseCount() == 0) {
                        //System.out.println("Mot " + doc.tokens().get(i).word() + " est trouvée mais a 0 synsets");
                        return getNextWord(i);
                    } else {
                        return doc.tokens().get(i);
                    }
                }
            } else {
                return getNextWord(i);
            }
        }
    }

    private static CoreLabel getAfterNextWord(int i) throws JWNLException {
        CoreLabel currentLabel = doc.tokens().get(i);
        CoreLabel nextLabel = getNextWord(getFullIndex(currentLabel));
        CoreLabel afterNextLabel = getNextWord(getFullIndex(nextLabel));
        return afterNextLabel;
    }

    private static Dictionary getWNDictionary() {

        FileInputStream xmlFile;
        try {
            xmlFile = new FileInputStream(pathToXml);
        } catch (FileNotFoundException fnfe) {
            System.out.println("le chemin vers file_properties.xml est faux!");
            return null;
        }

        try {
            JWNL.initialize(xmlFile);
        } catch (Exception e) {
            System.out.println("Problème d'initialisation de JWNL! \n" + e);
            return null;
        }
        if (Dictionary.getInstance() != null) {
            return Dictionary.getInstance();
        }
        System.out.println("Impossible d'ouvrire WordNet:");
        return null;
    }

    private static void testLemma(String lemma) throws JWNLException, rita.wordnet.jwnl.JWNLException { //TODO

        RiWordNet riwordnet;
        try {
            riwordnet = new RiWordNet(pathToWordnet);
        } catch (Exception e) {
            System.out.println(e);
            return;
        }


        if (dico.lookupIndexWord(POS.NOUN, lemma) != null) {
            System.out.println("Synset count of " + lemma + " is: " + dico.lookupIndexWord(POS.NOUN, lemma).getLemma());
        } else {
            System.out.println("Le mot " + lemma + " est introuvable dans RitaWordNet");
        }

        if (riwordnet.getDictionary().lookupIndexWord(rita.wordnet.jwnl.wndata.POS.NOUN, lemma) != null) {
            System.out.println("Synset count of " + lemma + " is: " + riwordnet.getDictionary().lookupIndexWord(rita.wordnet.jwnl.wndata.POS.NOUN, lemma).getLemma());
        } else {
            System.out.println("Le mot " + lemma + " est introuvable dans RitaWordNet");
        }
    }

}






class SynsetId {
    public String lastLemma;
    public String word;
    Synset synset;
    int id;
    float smallestDistance;

    public String toString() {
        return String.format("%20s Synset id: %d", word, id);
    }
};

