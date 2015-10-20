package uk.ac.sheffield.wordnet;

import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.RunTime;
import gate.creole.metadata.Optional;
import gate.creole.ExecutionException;


@CreoleResource(name = "WordNet Information Content Counter", comment = "Calculates the information content score for a corpus which can be used in similarity measures.")
/**
 * Calculates the information content score for a corpus which can be used in similarity measures.
 *  @author Dominic Rout
 *
 */
public class ICCounterPR extends AbstractLanguageAnalyser implements
        ProcessingResource {

    @SuppressWarnings("unchecked")
    @Override
    public void execute() throws ExecutionException {

    }

    /**
     * Creole Parameters below this point.
     */
    private String inputAS;

    public String getInputAS() {
        return inputAS;
    }

    @RunTime
    @Optional
    @CreoleParameter(comment = "Input annotation set to use, blank for default")
    public void setInputAS(String inputAS) {
        this.inputAS = inputAS;
    }
}
