import englishcoffeedrinker.wordnet.util.ICCounter;
import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Methods to help test the information content counting
 *
 * Created by Dominic Rout on 14/10/15.
 */
public class TestICCounter {
    public static void main(String[] args) throws IOException, JWNLException {
        JWNL.initialize(new FileInputStream("test/wordnet.xml"));

        FileInputStream testCorpus = new FileInputStream("test/test-ic-corpus.txt");

        BufferedReader br = new BufferedReader(new InputStreamReader(testCorpus));

        String line;
        ICCounter testCounter = new ICCounter(false);

        while ((line = br.readLine()) != null) {
            String[] sentences = line.split("\\.");
            for (String sentence : sentences) {
                String[] words = line.split("\\s");

                List<String> sentenceWords = new LinkedList<String>();
                for (String word : words) {
                    word = word.replaceAll("\\W$|^\\W", "");

                    // Don't bother with words that originally only had punctuation in them
                    if (!word.isEmpty())  {
                        sentenceWords.add(word);
                    }
                }

                testCounter.countSentence(sentenceWords);
            }
        }

        testCounter.propagateFrequency();
        PrintWriter exportWriter = new PrintWriter(new FileOutputStream("test/test-java-ic.dat"));
        testCounter.export(exportWriter);

        exportWriter.close();
    }

}
