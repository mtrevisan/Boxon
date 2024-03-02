/*
 * Copyright (c) 2021-2024 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.boxon.core.similarity.evolutionarytree;

import io.github.mtrevisan.boxon.core.similarity.evolutionarytree.distances.DistanceDataInterface;
import io.github.mtrevisan.boxon.core.similarity.evolutionarytree.distances.GenomeDistanceData;
import io.github.mtrevisan.boxon.core.similarity.evolutionarytree.distances.LevenshteinDistance;


public final class TemplateSpecies implements SpeciesInterface{

	//a unique name associated with the species (template class)
	private final String name;
	//the biological sequence describing this species (uuid of each parameter)
	private final DistanceDataInterface sequence;


	/**
	 * @param name	Intended name of the species.
	 * @param sequence	A list of single character in the genetic sequence.
	 */
	public static TemplateSpecies create(final String name, final String[] sequence){
		return new TemplateSpecies(name, sequence);
	}

	private TemplateSpecies(final String name, final String[] sequence){
		this.name = name;
		this.sequence = GenomeDistanceData.of(sequence);
	}


	@Override
	public String getName(){
		return name;
	}

	@Override
	public DistanceDataInterface getSequence(){
		return sequence;
	}

	@Override
	public double distance(final SpeciesInterface other){
		return LevenshteinDistance.similarity(sequence, other.getSequence());

//		final String[] seq1 = getSequence();
//		final String[] seq2 = other.getSequence();
//		if(seq1.length != seq2.length){
//			System.err.println("Error: Sequences must already be aligned");
//			System.exit(5);
//		}
//
//		int numDiffs = 0;
//		for(int i = 0; i < seq1.length; i ++)
//			if(!seq1[i].equals(seq2[i]))
//				numDiffs ++;
//		final double salt = 1. / (name.hashCode() ^ other.getName().hashCode());
//
//		return ((double)numDiffs) / seq1.length + salt;
	}

}
