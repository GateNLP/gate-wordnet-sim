/************************************************************************
 *         Copyright (C) 2006-2007 The University of Sheffield          *
 *                            2011 Mark A. Greenwood                    *
 *      Developed by Mark A. Greenwood <m.greenwood@dcs.shef.ac.uk>     *
 *                                                                      *
 * This program is free software; you can redistribute it and/or modify *
 * it under the terms of the GNU General Public License as published by *
 * the Free Software Foundation; either version 2 of the License, or    *
 * (at your option) any later version.                                  *
 *                                                                      *
 * This program is distributed in the hope that it will be useful,      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of       *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        *
 * GNU General Public License for more details.                         *
 *                                                                      *
 * You should have received a copy of the GNU General Public License    *
 * along with this program; if not, write to the Free Software          *
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.            *
 ************************************************************************/

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.dictionary.Dictionary;

import org.junit.BeforeClass;
import org.junit.Test;

import englishcoffeedrinker.wordnet.similarity.SimilarityInfo;
import englishcoffeedrinker.wordnet.similarity.SimilarityMeasure;

/**
 * A simple test of this WordNet similarity library.
 * @author Mark A. Greenwood
 */
public class TestJCn
{
	private static SimilarityMeasure sim;
	
	@BeforeClass
	public static void setUp() throws Exception {
		
		//Initialize WordNet - this must be done before you try
		//and create a similarity measure otherwise nasty things
		//might happen!
		JWNL.initialize(new FileInputStream("test/wordnet.xml"));
		
		//Create a map to hold the similarity config params
		Map<String,String> params = new HashMap<String,String>();
		
		//the simType parameter is the class name of the measure to use
		params.put("simType","englishcoffeedrinker.wordnet.similarity.JCn");
		
		//this param should be the URL to an infocontent file (if required
		//by the similarity measure being loaded)
		params.put("infocontent","file:test/ic-bnc-resnik-add1.dat");
		
		//this param should be the URL to a mapping file if the
		//user needs to make synset mappings
		params.put("mapping","file:test/domain_independent.txt");
		
		//set the encoding of the two input files
		params.put("encoding", "us-ascii");
		
		//create the similarity measure
		sim = SimilarityMeasure.newInstance(params);
		
	}
	
	@Test
	public void test() throws Exception {
		
		//Get two words from WordNet
		Dictionary dict = Dictionary.getInstance();		
		IndexWord word1 = dict.getIndexWord(POS.NOUN, "dog");
		IndexWord word2 = dict.getIndexWord(POS.NOUN,"cat");
		
		//and get the similarity between the first senses of each word	
		assertEquals(0.38732210985266985, sim.getSimilarity(word1.getSense(1), word2.getSense(1)), 0.00001);
	}
	
	@Test
	public void testStrings() throws Exception {
		//get similarity using the string methods (note this also makes use
		//of the fake root node)
		SimilarityInfo info = sim.getSimilarity("time#n","cat#n");
		
		assertNotNull(info);

		assertEquals(5, info.getSenseNumber1());
		assertEquals(1, info.getSenseNumber2());
				
		assertEquals(0.07042660413866274, info.getSimilarity(), 0.00001);
	}
	
	@Test
	public void testMappings() throws Exception {
		//get a similarity that involves a mapping
		SimilarityInfo info = sim.getSimilarity("namperson", "organization");
				
		assertNotNull(info);

		assertEquals(1, info.getSenseNumber2());
		
		assertEquals(0.1687995825035664, info.getSimilarity(), 0.00001);
	}
}
