package uk.ac.sheffield.wordnet;

import englishcoffeedrinker.wordnet.similarity.SimilarityMeasure;
import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.RunTime;
import gate.creole.metadata.Optional;
import gate.creole.ExecutionException;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


@CreoleResource(name = "Similarity to WordNet sense", comment = "Calculates the maximum similarity from each token to a given sense in WordNet")
/**
 * Calculates the maximum distance from each token to a given sense in WordNet
 *  @author Dominic Rout
 *
 */
public class SimilarityToWordNet extends AbstractLanguageAnalyser implements
        ProcessingResource {


    @SuppressWarnings("unchecked")
    @Override
    public void execute() throws ExecutionException {
        //create the similarity measure
        try {
            SimilarityMeasure sim = SimilarityMeasure.newInstance(getSimParams());
        } catch (IOException e) {
            throw new ExecutionException(e);
        }

        AnnotationSet inputSet = document.getAnnotations(inputAS);

//        for (Annotation token : inputSet.get(tokenType)) {
//            String tokenText = (String) token.getFeatures().get(textFeature);
//
//            Collection<Concept> tokenConcepts = db.getAllConcepts(tokenText, "n"); // only interested in nouns
//
//            double maxSimilarity = 0;
//
//            // We only care about the sense with the highest similarity.
//            for (Concept c1 : tokenConcepts) {
//                double similarity = calculator.calcRelatednessOfSynset(c1, c).getScore();
//
//                if (similarity > maxSimilarity) { maxSimilarity = similarity; }
//            }
//
//            token.getFeatures().put(outputFeature, maxSimilarity);
//        }
    }

    private Map<String, String> getSimParams() {
        //Create a map to hold the similarity config params
        Map<String,String> params = new HashMap<String,String>();

        //the simType parameter is the class name of the measure to use
        params.put("simType","englishcoffeedrinker.wordnet.similarity.Lin");

        //this param should be the URL to an infocontent file (if required
        //by the similarity measure being loaded)
        params.put("infocontent","file:test/ic-bnc-resnik-add1.dat");

        //this param should be the URL to a mapping file if the
        //user needs to make synset mappings
        params.put("mapping","file:test/domain_independent.txt");

        //set the encoding of the two input files
        params.put("encoding", "us-ascii");

        return params;
    }

    /**
     * Creole Parameters below this point.
     */
    private String inputAS;
    private String tokenType;
    private String textFeature;
    private String outputFeature;
    private String targetSynset;

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
    @CreoleParameter(comment = "The feature for the token containing its text.")
    public void setTextFeature(String textFeature) {
        this.textFeature = textFeature;
    }

    public String getOutputFeature() {
        return outputFeature;
    }

    @RunTime
    @CreoleParameter(comment = "The feature to add to the token for the similarity score.")
    public void setOutputFeature(String outputFeature) {
        this.outputFeature = outputFeature;
    }

    public String getTargetSynset() {
        return targetSynset;
    }

    @RunTime
    @CreoleParameter(comment = "The target synset to which tokens should be compared.")
    public void setTargetSynset(String targetSynset) {
        this.targetSynset = targetSynset;
    }


}
