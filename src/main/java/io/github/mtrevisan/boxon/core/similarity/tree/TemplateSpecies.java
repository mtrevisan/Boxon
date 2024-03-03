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
package io.github.mtrevisan.boxon.core.similarity.tree;

import io.github.mtrevisan.boxon.core.similarity.distances.DamerauLevenshteinDistance;
import io.github.mtrevisan.boxon.core.similarity.distances.DistanceDataInterface;
import io.github.mtrevisan.boxon.core.similarity.distances.GenomeDistanceData;


public final class TemplateSpecies implements SpeciesInterface{

	private static final DamerauLevenshteinDistance DISTANCE = DamerauLevenshteinDistance.create(1, 1, 1, 1000);

	//a unique name associated with the species (template class)
	private final String name;
	//the biological sequence describing this species (uuid of each parameter)
	private final DistanceDataInterface<?> sequence;


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
	public DistanceDataInterface<?> getSequence(){
		return sequence;
	}

	@Override
	public int distance(final SpeciesInterface other){
		return DISTANCE.distance(sequence, other.getSequence());
	}

	@Override
	public double similarity(final SpeciesInterface other){
		return DISTANCE.similarity(sequence, other.getSequence());
	}

}
