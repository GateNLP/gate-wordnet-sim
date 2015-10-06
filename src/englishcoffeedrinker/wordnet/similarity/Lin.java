/*
 * Lin.java
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
import net.didion.jwnl.data.Synset;

/**
 * An implementation of the WordNet similarity measure developed by Lin. For
 * full details of the measure see:
 * <blockquote>Lin D. 1998. An information-theoretic definition of similarity.
 * In
 * Proceedings of the 15th International Conference on Machine
 * Learning, Madison, WI.</blockquote>
 * @author Mark A. Greenwood
 */
public class Lin extends ICMeasure
{
	/**
	 * Instances of this similarity measure should be generated using the
	 * factory methods of {@link SimilarityMeasure}.
	 */
	public Lin()
	{
		// Don't force the use of newInstance.
	}

	@Override
	public double getSimilarity(Synset s1, Synset s2) throws JWNLException
	{
		//if the POS tags are not the same then return 0 as this measure
		//only works with 2 nouns or 2 verbs.
		if (!s1.getPOS().equals(s2.getPOS())) return 0;

		//see if the similarity is already cached and...
		Double cached = getFromCache(s1, s2);

		//if it is then simply return it
		if (cached != null) return cached.doubleValue();

		//Get the Information Content (IC) values for the two supplied synsets
		double ic1 = getIC(s1);
		double ic2 = getIC(s2);

		//if either IC value is zero then cache and return a sim of 0
		if (ic1 == 0 || ic2 == 0) return addToCache(s1, s2, 0);

		//Get the Lowest Common Subsumer (LCS) of the two synsets
		Synset lcs = getLCSbyIC(s1, s2);

		//if there isn't an LCS then cache and return a sim of 0
		if (lcs == null) return addToCache(s1, s2, 0);

		//get the IC value of the LCS
		double icLCS = getIC(lcs);

		//calculate the similarity score
		double sim = (2 * icLCS) / (ic1 + ic2);

		//cache and return the calculated similarity
		return addToCache(s1, s2, sim);
	}
}
