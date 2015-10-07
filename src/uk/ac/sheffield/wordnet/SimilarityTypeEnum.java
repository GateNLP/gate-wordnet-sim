package uk.ac.sheffield.wordnet;

/**
 * Defines the possible kinds of similarity that can be calculated.
 *
 * Created by dominic on 07/10/15.
 */
public enum SimilarityTypeEnum {
    LIN, JCN;

    public String toClassName() {
        switch (this) {
            case LIN: return "englishcoffeedrinker.wordnet.similarity.Lin";
            case JCN: return "englishcoffeedrinker.wordnet.similarity.JCn";
        }

        throw new RuntimeException("Similarity type enum did not have valid value");
    }

}
