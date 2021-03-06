import englishcoffeedrinker.wordnet.util.ICCounter;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.dictionary.Dictionary;

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
        Dictionary dict = Dictionary.getInstance(new FileInputStream("test/wordnet.xml"));

        FileInputStream testCorpus = new FileInputStream("test/test-ic-corpus.txt");

        BufferedReader br = new BufferedReader(new InputStreamReader(testCorpus));

        String line;
        ICCounter testCounter = new ICCounter(dict, true);
        line = br.readLine();
        while (line != null) {
            String[] sentences = line.split("\\.");
            for (String sentence : sentences) {
                String[] words = sentence.split("\\s");

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
            line = br.readLine();
        }

        testCounter.propagateFrequency();
//        testCounter.smoothFrequency();
        PrintWriter exportWriter = new PrintWriter(new FileOutputStream("test/test-java-ic.dat"));
        testCounter.export(exportWriter);

        exportWriter.close();
    }

}
