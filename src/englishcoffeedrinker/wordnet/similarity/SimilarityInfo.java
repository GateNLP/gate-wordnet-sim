/*
 * SimilarityInfo.java
 * 
 * Copyright (c) 2006-2007, The University of Sheffield.
 * Copyright (c) 2011-2012, Mark A. Greenwood
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */

package englishcoffeedrinker.wordnet.similarity;

import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.dictionary.Dictionary;

/**
 * Simple encapsulation of the similarity between two synsets.
 * @author Mark A. Greenwood
 */
public class SimilarityInfo
{
	private Synset s1, s2;
	private IndexWord iw1, iw2;
	private double sim = 0;
	private String d1, d2;
	private int sn1, sn2;

	/**
	 * @param w1 the first word (or it's encoded form)
	 * @param s1 the first synset
	 * @param w2 the second word (or it's encoded form)
	 * @param s2 the second synset
	 * @param sim the similarity between the two synsets
	 * @throws JWNLException
	 */
	protected SimilarityInfo(String w1, Synset s1, String w2, Synset s2, double sim) throws JWNLException
	{
		//store the synsets and the similarity between them
		this.s1 = s1;
		this.s2 = s2;
		this.sim = sim;

		//The following is just for display purposes and as this class
		//is immutable we just generate this stuff once

		//get access to WordNet
		Dictionary dict = Dictionary.getInstance();

		//get the two index words
		iw1 = dict.getIndexWord(s1.getPOS(), w1.split("#")[0]);
		iw2 = dict.getIndexWord(s2.getPOS(), w2.split("#")[0]);

		sn1 = getSenseNumber(iw1, s1);
		sn2 = getSenseNumber(iw2, s2);

		//build the descriptions of the two words
		d1 = (iw1 == null ? w1 : iw1.getLemma() + "#" + s1.getPOS().getKey() + "#" + sn1);
		d2 = (iw2 == null ? w2 : iw2.getLemma() + "#" + s2.getPOS().getKey() + "#" + sn2);
	}

	/**
	 * Given an index word and synset works out which sense index we are looking
	 * at
	 * @param iw the index word
	 * @param s a synset that includes the index word
	 * @return the sense indes of the sysnet for the given word,
	 *         or -1 if the word is not in the synset
	 * @throws JWNLException if an error occurs accessing WordNet
	 */
	private static final int getSenseNumber(IndexWord iw, Synset s) throws JWNLException
	{
		if (iw == null || s == null) return -1;

		//get all the senses of the word
		Synset[] senses = iw.getSenses();

		for (int i = 0; i < senses.length; ++i)
		{
			//if the sense we are looking at is the one we
			//want then return it's index
			if (senses[i].equals(s)) return (i + 1);
		}

		//we didn't find the sense so return -1 to denote failure
		return -1;
	}

	/**
	 * Get the first synset used to compute similarity
	 * @return the first synset used to compute similarity.
	 */
	public Synset getSynset1()
	{
		return s1;
	}

	/**
	 * Get the second synset used to compute similarity
	 * @return the second synset used to compute similarity.
	 */
	public Synset getSynset2()
	{
		return s2;
	}

	/**
	 * Get the first word being used to compute similarity
	 * @return the first word being used to compute similarity, may be null if a
	 *         mapping was used
	 */
	public IndexWord getIndexWord1()
	{
		return iw1;
	}

	/**
	 * Get the second word being used to compute similarity
	 * @return the second word being used to compute similarity, may be null if
	 *         a
	 *         mapping was used
	 */
	public IndexWord getIndexWord2()
	{
		return iw2;
	}

	/**
	 * Get the sense number of the first word used to compute similarity
	 * @return the sense number of the first word used to compute similarity,
	 *         may be -1 if a mapping was used
	 */
	public int getSenseNumber1()
	{
		return sn1;
	}

	/**
	 * Get the sense number of the second word used to compute similarity
	 * @return the sense number of the second word used to compute similarity,
	 *         may be -1 if a mapping was used
	 */
	public int getSenseNumber2()
	{
		return sn2;
	}

	/**
	 * Get the similarity between the two synsets
	 * @return the similarity between the two synsets.
	 */
	public double getSimilarity()
	{
		return sim;
	}

	@Override
	public String toString()
	{
		return d1 + "  " + d2 + "  " + sim;
	}
}
