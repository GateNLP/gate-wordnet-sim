package uk.ac.sheffield.wordnet;

import englishcoffeedrinker.wordnet.similarity.JCn;
import englishcoffeedrinker.wordnet.similarity.Lin;
import englishcoffeedrinker.wordnet.similarity.SimilarityInfo;
import englishcoffeedrinker.wordnet.similarity.SimilarityMeasure;
import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.RunTime;
import gate.creole.metadata.Optional;
import gate.creole.ExecutionException;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.dictionary.Dictionary;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@CreoleResource(name = "WordNet Sense Similarity", comment = "Calculates the maximum similarity from each token to a given sense in WordNet")
/**
 * Calculates the maximum similarity from each token to a given sense in WordNet
 *  @author Dominic Rout
 *
 */
public class SimilarityToWordNet extends AbstractLanguageAnalyser implements
        ProcessingResource {

    private Dictionary dict;

    @Override
    public Resource init() throws ResourceInstantiationException {
        try {
            if (wordnetConfig == null) {
                throw new NullPointerException("WordNet configuration URL is null.");
            }

            dict = Dictionary.getInstance(wordnetConfig.openStream());
        } catch (IOException e) {
            throw new ResourceInstantiationException("Couldn't find or read WordNet configuration file",e);
        } catch (JWNLException e) {
            throw new ResourceInstantiationException("Couldn't initialise JWNL to read wordnet database", e);
        }

        return super.init();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute() throws ExecutionException {
        //create the similarity measure
        SimilarityMeasure sim;
        try {
            sim = SimilarityMeasure.newInstance(dict, getSimParams());
        } catch (IOException e) {
            throw new ExecutionException(e);
        }

        AnnotationSet inputSet = document.getAnnotations(inputAS);
        try {

            Set<Synset> targetSynsets = sim.getSynsets(targetSynset);

            for (Annotation token : inputSet.get(tokenType)) {
                String tokenText = (String) token.getFeatures().get(textFeature);

                Set<Synset> tokenSynsets = sim.getSynsets(tokenText, false);
                // Only try to calculate similarity for tokens that are in wordnet
                if (tokenSynsets.size() != 0) {
                    SimilarityInfo similarity = sim.getSimilarity(tokenText, targetSynset, tokenSynsets, targetSynsets);

                    token.getFeatures().put(outputFeature, similarity.getSimilarity());
                } else {
                    // Default to 0 if the word is not in WordNet.
                    // NB that some measures cannot ordinarily produce 0 scores.
                    token.getFeatures().put(outputFeature, 0);
                }
            }
        } catch (JWNLException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> getSimParams() throws ExecutionException {
        //Create a map to hold the similarity config params
        Map<String,String> params = new HashMap<String,String>();

        //the simType parameter is the class name of the measure to use
        params.put("simType",simType.toClassName());

        //this param should be the URL to an infocontent file (if required
        //by the similarity measure being loaded)
        if (infoContentFileName != null) {
            try {
                params.put("infocontent", infoContentFileName.toURI().toString());

            } catch (URISyntaxException e) {
                throw new ExecutionException("URL supplied for infocontent file is not valid");
            }
        } else if (simType == SimilarityTypeEnum.LIN || simType == SimilarityTypeEnum.JCN) {
            throw new ExecutionException("Infocontent file is required for the selected similarity measure.");
        }

        if (mappingFileName != null) {
            //this param should be the URL to a mapping file if the
            //user needs to make synset mappings
            try {
                System.out.println(mappingFileName.toURI().toString());
                params.put("mapping", mappingFileName.toURI().toString());
            } catch (URISyntaxException e) {
                throw new ExecutionException("URL supplied for mapping file is not valid");
            }
        }
        //set the encoding of the two input files
        params.put("encoding", mappingInfoContentEncoding);

        return params;
    }

    public void runSimpleTest() throws ExecutionException {
        try {
            Dictionary dict = Dictionary.getInstance(
                    new FileInputStream("/home/dominic/Repositories/gate-wordnet-sim/test/wordnet.xml"));

            Lin sim = new Lin(dict);


            sim.loadMappings("file:/home/dominic/Repositories/gate-wordnet-sim/test/domain_independent.txt", "us-ascii");
            sim.loadInfoContent("file:/home/dominic/Repositories/gate-wordnet-sim/test/ic-bnc-resnik-add1.dat", "us-ascii");

            SimilarityInfo info = sim.getSimilarity("melbourne","organization");

            assertNotNull(info);

            assertEquals(-0.0, info.getSimilarity(), 0.00001);

        } catch (Exception e) {
            throw new ExecutionException(e);
        }


    }

    /**
     * Creole Parameters below this point.
     */
    private String inputAS;
    private String tokenType;
    private String textFeature;
    private String outputFeature;
    private String targetSynset;

    private SimilarityTypeEnum simType;
    private URL infoContentFileName;
    private URL mappingFileName;
    private String mappingInfoContentEncoding;

    private URL wordnetConfig;


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

    public SimilarityTypeEnum getSimType() {
        return simType;
    }

    @RunTime
    @CreoleParameter(comment = "The kind of similarity measure to use", defaultValue = "LIN")
    public void setSimType(SimilarityTypeEnum simType) {
        this.simType = simType;
    }

    public URL getInfoContentFileName() {
        return infoContentFileName;
    }
    @Optional
    @CreoleParameter(comment = "The file containing the information content description to use")
    public void setInfoContentFileName(URL infoContentFileName) {
        this.infoContentFileName = infoContentFileName;
    }

    public URL getMappingFileName() {
        return mappingFileName;
    }

    @Optional
    @CreoleParameter(comment = "File containing additional mappings for wordnet")
    public void setMappingFileName(URL mappingFileName) {
        this.mappingFileName = mappingFileName;
    }

    public String getMappingInfoContentEncoding() {
        return mappingInfoContentEncoding;
    }

    @CreoleParameter(comment = "Encoding for the information content and  mappings files", defaultValue = "utf-8")
    public void setMappingInfoContentEncoding(String mappingInfoContentEncoding) {
        this.mappingInfoContentEncoding = mappingInfoContentEncoding;
    }


    public URL getWordnetConfig() {
        return wordnetConfig;
    }

    @CreoleParameter(comment = "Location of the wordnet configuration file")
    public void setWordnetConfig(URL wordnetConfig) {
        this.wordnetConfig = wordnetConfig;
    }
}
