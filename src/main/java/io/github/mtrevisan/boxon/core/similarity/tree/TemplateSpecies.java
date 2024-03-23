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

import io.github.mtrevisan.boxon.core.similarity.distances.metrics.DamerauLevenshteinMetric;
import io.github.mtrevisan.boxon.core.similarity.distances.DistanceDataInterface;


public final class TemplateSpecies<S extends SpeciesInterface<S, D>, D extends DistanceDataInterface<D>> implements SpeciesInterface<S, D>{

	private final DamerauLevenshteinMetric<D> distance = DamerauLevenshteinMetric.create(1, 1, 1, 1000);

	//a unique name associated with the species (template class)
	private final String name;
	//the biological sequence describing this species (UUID of each parameter)
	private final D sequence;


	/**
	 * @param name	Intended name of the species.
	 * @param sequence	A list of single character in the genetic sequence.
	 */
	public static <S extends SpeciesInterface<S, D>, D extends DistanceDataInterface<D>> TemplateSpecies<S, D> create(final String name,
			final D sequence){
		return new TemplateSpecies<>(name, sequence);
	}

	private TemplateSpecies(final String name, final D sequence){
		this.name = name;
		this.sequence = sequence;
	}


	@Override
	public String getName(){
		return name;
	}

	@Override
	public D getSequence(){
		return sequence;
	}

	@Override
	public int distance(final SpeciesInterface<S, D> other){
		return distance.distance(sequence, other.getSequence());
	}

	@Override
	public double similarity(final SpeciesInterface<S, D> other){
		return distance.similarity(sequence, other.getSequence());
	}

}
