package uk.ac.sheffield.wordnet;

import englishcoffeedrinker.wordnet.similarity.Lin;
import englishcoffeedrinker.wordnet.similarity.SimilarityInfo;
import englishcoffeedrinker.wordnet.similarity.SimilarityMeasure;
import gate.Annotation;
import gate.AnnotationSet;
import gate.ProcessingResource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.dictionary.Dictionary;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@CreoleResource(name = "WordNet Sense Similarity", comment = "Calculates the maximum similarity from each token to a given sense in WordNet")
/**
 * Calculates the maximum similarity from each token to a given sense in WordNet
 *  @author Dominic Rout
 *
 */
public class CachedSimilarity extends AbstractLanguageAnalyser implements
        ProcessingResource {

    HashMap<String, HashMap<String, Float>> cache;
    LinkedList<String> targetWords;

    @SuppressWarnings("unchecked")
    @Override
    public void execute() throws ExecutionException {
        if (cache == null) {
            initCache();
        }

        AnnotationSet inputSet = document.getAnnotations(inputAS);
        for (Annotation token : inputSet.get(tokenType)) {
            String tokenText = (String) token.getFeatures().get(textFeature);

            HashMap<String, Float> tokenSims = cache.get(tokenText);

            // Only try to calculate similarity for tokens that are in wordnet
            if (tokenSims != null) {
                for (String target : targetWords) {
                    Float score = tokenSims.get(target);

                    if (score == null) {score = 0.0f;};
                    token.getFeatures().put(target, score);
                }
            } else {
                // Default to 0 if the word is not in the cache.
                for (String target : targetWords) {
                    token.getFeatures().put(target, 0);
                }
            }
        }
    }

    private void initCache() throws ExecutionException {
        cache = new HashMap<String, HashMap<String, Float>>();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(cacheLocation.openStream()));

            String line;
            targetWords = new LinkedList<String>();

            if ((line = reader.readLine()) != null) {
                targetWords.addAll(Arrays.asList(line.split(" ")));

                targetWords.removeFirst(); // First two columns aren't target words!
                targetWords.removeFirst();
            } else {
                throw new ExecutionException("Cache file for wordnet similarity was empty.");
            }

            while ((line = reader.readLine()) != null) {
                String[] splitLine = line.split(" ");

                String word = splitLine[0];
                HashMap<String, Float> cacheEntry = new HashMap<String, Float>();
                cache.put(word, cacheEntry);

                Iterator<String> targetIter = targetWords.iterator();
                for (int i = 2; i < splitLine.length; i++) {
                    String target = targetIter.next();

                    cacheEntry.put(target, Float.parseFloat(splitLine[i]));
                }


            }

        } catch ( IOException e) {
            throw new ExecutionException("Error when trying to read the cache file for wordnet similarity", e);
        }
    }


    /**
     * Creole Parameters below this point.
     */
    private String inputAS;
    private String tokenType;
    private String textFeature;

    private URL cacheLocation;


    public String getInputAS() {
        return inputAS;
    }

    @RunTime
    @Optional
    @CreoleParameter(comment = "Input annotation set to use, blank for default.")
    public void setInputAS(String inputAS) {
        this.inputAS = inputAS;
    }

    public String getTokenType() {
        return tokenType;
    }

    @RunTime
    @CreoleParameter(comment = "The token type to use for input.")
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getTextFeature() {
        return textFeature;
    }


    @RunTime
    @CreoleParameter(comment = "Feature from the token which contains the text.", defaultValue="string")
    public void setTextFeature(String textFeature) {
        this.textFeature = textFeature;
    }

    @Optional
    @CreoleParameter(comment = "File containing cached scores")
    public void setCacheLocation(URL cacheLocation) {
        this.cacheLocation = cacheLocation;
    }

    public URL getCacheLocation() {
        return cacheLocation;
    }

}
