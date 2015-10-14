package englishcoffeedrinker.wordnet.util;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.*;
import net.didion.jwnl.data.list.PointerTargetNode;
import net.didion.jwnl.data.list.PointerTargetNodeList;
import net.didion.jwnl.data.list.PointerTargetTree;
import net.didion.jwnl.dictionary.Dictionary;
import net.didion.jwnl.dictionary.file.DictionaryCatalog;

import java.io.PrintWriter;
import java.util.*;

/**
 * Utility class to count terms in order to produce the information content file.
 *
 * Created by Dominic Rout on 13/10/15.
 */
public class ICCounter {
    private static String[] POS_TAGS = {"n", "v"};
    private static int MAX_COMPOUND_WORDS = 3;
    private final Dictionary dictionary;
    private final PointerUtils pointerUtils;

    private boolean resnik;
    private HashSet<String> compoundTerms;
    private TreeMap<String, HashMap<Long, Float>> offsetFreqMap; // Maps from a synset offset to a frequency score

    /**
     *
     * @param resnik Use the counting method of resnik et al in which IC is divided amongst all senses
     */
    public ICCounter(boolean resnik) throws JWNLException {
        this.resnik = resnik;
        pointerUtils = PointerUtils.getInstance();
        dictionary = Dictionary.getInstance();

        offsetFreqMap = new TreeMap<String, HashMap<Long, Float>>();

        for (String pos_tag : POS_TAGS) {
            offsetFreqMap.put(pos_tag, new HashMap<Long, Float>());
        }

        loadCompounds();
    }

    public void countSentence(List<String> sentence) throws JWNLException {
        // Remove unwanted characters
        LinkedList<String> cleanSentence = new LinkedList<String>();
        for (String term : sentence) {
            String cleanTerm = term;

            cleanTerm = cleanTerm.toLowerCase();
            cleanTerm = cleanTerm.replaceAll("'", "");
            cleanTerm = cleanTerm.replaceAll("[^a-z0-9]", " ");
            cleanTerm = cleanTerm.trim();

            cleanSentence.addAll(Arrays.asList(cleanTerm.split("\\s")));
        }

        List<String> compoundSentence = compoundify(cleanSentence);

        // Count each word in the sentence.
        for (String word : compoundSentence) {
            countWord(word);
        }
    }

    /**
     * Resolves synsets for the word and adds to the hashmap for each.
     *
     * @param word The lemma of the term to count
     * @throws JWNLException If senses could not be fetched from the WordNet database.
     */
    public void countWord(String word) throws JWNLException {
        for (String posString : POS_TAGS) {
            POS pos = POS.getPOSForKey(posString);

            // Get the hashmap for this POS tag.
            HashMap<Long, Float> offsetMap = offsetFreqMap.get(posString);

            // Find synsets for the word from the dictionary.
            IndexWord indexWord = dictionary.getIndexWord(pos, word);

            if (indexWord != null) {
                long[] offsets = indexWord.getSynsetOffsets();

                for (long offset : offsets) {

                    // Initialise the offset to zero in the map if needed because Java is ridiculous.
                    if (!offsetMap.containsKey(offset)) {
                        offsetMap.put(offset, 0f);
                    }

                    if (resnik) {
                        // Spread the value over all senses by dividing by the number of senses.
                        offsetMap.put(offset, offsetMap.get(offset) + (1.0f / offsets.length));
                    } else {
                        offsetMap.put(offset, offsetMap.get(offset) + 1.0f);
                    }
                }
            }
        }
    }

    /**
     * Propagates the frequencies up through WordNet.
     *
     */
    public void propagateFrequency() throws JWNLException {
        for (String posString : POS_TAGS) {
            POS pos = POS.getPOSForKey(posString);

            HashMap<Long, Float> offsetMap = offsetFreqMap.get(posString);
            // This will hold the intermediate result to prevent summing twice.
            HashMap<Long, Float> resultOffsetMap = new HashMap<Long, Float>(offsetFreqMap.get(posString));

            // First, set the root offsets to 0.
            if (!resultOffsetMap.containsKey(0l)) { resultOffsetMap.put(0l, 0.0f); }

            // Get the sum of hyponyms for each term in the dictionary.
            Iterator synsetIterator = dictionary.getSynsetIterator(pos);

            while (synsetIterator.hasNext()) {
                // Get the actual index term.
                Synset synset = (Synset) synsetIterator.next();

                // Only start with root nodes.
                if (pointerUtils.getDirectHypernyms(synset).isEmpty()) {
                    float frequency = _propagateFrequency(synset, offsetMap, resultOffsetMap);
                    resultOffsetMap.put(0l, resultOffsetMap.get(0l) + frequency);
                }
            }

            offsetMap = resultOffsetMap;
        }
    }

    public void smoothFrequency() throws JWNLException {
        for (String posString : POS_TAGS) {
            POS pos = POS.getPOSForKey(posString);

            HashMap<Long, Float> offsetMap = offsetFreqMap.get(posString);

            // Iterate over every sense in the dictionary.
            Iterator synsetIterator = dictionary.getSynsetIterator(pos);

            while (synsetIterator.hasNext()) {
                // Get the actual index term.
                Synset synset = (Synset) synsetIterator.next();

                long offset = synset.getOffset();

                offsetMap.put(offset, (offsetMap.containsKey(offset) ? offsetMap.get(offset) : 0l) + 1l);
            }
        }

    }

    /**
     * Actually propagates the frequency up to a subtree root. Works recursively down through the tree.
     *
     * @param synset
     * @param offsetMap
     * @param currentMap
     * @return
     * @throws JWNLException
     */
    private float _propagateFrequency(Synset synset, HashMap<Long, Float> offsetMap, HashMap<Long, Float> currentMap) throws JWNLException {
        PointerTargetNodeList hyponyms = pointerUtils.getDirectHyponyms(synset);

        long offset = synset.getOffset();
        // Return the current value if we've already encountered this node.
        if (currentMap.containsKey(synset.getOffset())) {
            return currentMap.containsKey(offset) ? currentMap.get(offset) : 0;
        }

        // Return the value from the text if the node has no hyponyms.
        if (hyponyms.isEmpty()) {
            return offsetMap.containsKey(offset) ? offsetMap.get(offset) : 0;
        }

        float sum = 0;
        // Sum the values for each hyponym.
        for (Object hyponymObject : hyponyms) {
            PointerTargetNode hyponymNode = (PointerTargetNode) hyponymObject;
            sum += _propagateFrequency(hyponymNode.getSynset(), offsetMap, currentMap);
        }

        // Add the sum to the count from the text if available.
        sum += offsetMap.containsKey(offset) ? offsetMap.get(offset) : 0;
        currentMap.put(offset, sum);
        return sum;
    }

    /**
     * Loads all compounds from the dictionary and stores in a field.
     */
    protected void loadCompounds() throws JWNLException {

        compoundTerms = new HashSet<String>();

        for (String pos_tag : POS_TAGS) {
            // Find words for the pos tag.
            Iterator indexWordIterator = dictionary.getIndexWordIterator(POS.getPOSForKey(pos_tag));
            while (indexWordIterator.hasNext()) {
                // Get the actual index term.
                IndexWord term = (IndexWord) indexWordIterator.next();

                // If the term is compound it will contain a ' ' character.
                if (term.getLemma().contains(" ")) {
                    compoundTerms.add(term.getLemma());
                }
                // WordNet::similarity uses _ here so check and see if we should also be using that.
                if (term.getLemma().contains("_")) {
                    throw new AssertionError("Did not expect to see _ symbol in term lemma.");
                }
            }
        }
    }

    /**
     * Create the structures to hold the offset frequencies.
     */
    protected void initFrequencies() {
        offsetFreqMap = new TreeMap<>();

        for (String pos : POS_TAGS) {
            offsetFreqMap.put(pos, new HashMap<Long, Float>());
        }
    }

    /**
     * Finds compound terms from WordNet in the sentence and returns a version with them added..
     *
     * Will cause words to appear both as themselves and as parts of compounds. This is necessary because
     * no disambiguation takes place here.
     * @param sentence
     * @return
     */
    protected List<String> compoundify(List<String> sentence) {
        List<String> result = new LinkedList<>(sentence);

        // Use a left and right pointer to select subsets of the tokens and compare to the compounds.
        for (int left_ptr = 0; left_ptr < sentence.size(); left_ptr++) {
            // Move the right pointer to up to MAX_COMPOUND_WORDS away, or the end of the sentence.
            String candidateString = sentence.get(left_ptr);

            for (int right_ptr = left_ptr + 1; // Not interested in length 1
                 right_ptr < left_ptr + MAX_COMPOUND_WORDS && right_ptr < sentence.size();
                    right_ptr ++) {
                // Build a candidate compound to check
                candidateString += " " + sentence.get(right_ptr);

                // See if the compound exists in WordNet
                if (compoundTerms.contains(candidateString)) {
                    // And add it to the result if so.
                    result.add(candidateString);
                }
            }
        }

        return result;
    }

    /**
     * Prints the IC counts to the supplied data file in the format required for the library.
     * @param output
     */
    public void export(PrintWriter output) throws JWNLException {
        // Print a current version number
        output.format("wnver::%f\n", JWNL.getVersion().getNumber());

        // Output one POS tag at a time
        for (Map.Entry<String, HashMap<Long, Float>> posFreqMapEntry : offsetFreqMap.entrySet()) {
            String posTag = posFreqMapEntry.getKey();
            POS pos = POS.getPOSForKey(posTag);

            // Iterate through all of the words in this dictionary.
            for (Map.Entry<Long, Float> wordFreqEntry : posFreqMapEntry.getValue().entrySet()) {
                long offset = wordFreqEntry.getKey();
                Synset synset = dictionary.getSynsetAt(pos, offset);

                output.format("%d%s %f %s\n",
                        wordFreqEntry.getKey(),
                        posTag,
                        wordFreqEntry.getValue(),
                        pointerUtils.getDirectHypernyms(synset).isEmpty() ? "ROOT" : "");
            }
        }
    }
}
