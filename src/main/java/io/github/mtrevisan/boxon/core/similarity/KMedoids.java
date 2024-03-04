/*
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.similarity;

import io.github.mtrevisan.boxon.core.similarity.tree.SpeciesInterface;
import org.apache.commons.numbers.combinatorics.BinomialCoefficient;
import org.apache.commons.numbers.combinatorics.Combinations;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;


/**
 * @see <a href="https://github.com/servbaset/K-medoids">K-medoids</a>
 * @see <a href="https://www.geeksforgeeks.org/ml-k-medoids-clustering-with-example/">K-Medoids clustering</a>
 */
public final class KMedoids{

	private static final Random RANDOM = new Random(System.currentTimeMillis());


	private KMedoids(){}


	/**
	 * Cluster the input.
	 *
	 * @param dataset	The list of strings to be clustered.
	 * @param numberOfClusters	Number of clusters to generate.
	 * @param maxIterations	The maximum number of iterations the algorithm is allowed to run.
	 * @return	A set of sets where each data is grouped into.
	 */
	public static Collection<Collection<String>> cluster(final SpeciesInterface[] dataset, final int numberOfClusters,
			final int maxIterations){
		if(dataset == null || dataset.length == 0)
			throw new IllegalArgumentException("Dataset cannot be empty");
		if(numberOfClusters < 1)
			throw new IllegalArgumentException("Number of clusters cannot be less than 1");
		if(maxIterations < 1)
			throw new IllegalArgumentException("Maximum number of iterations cannot be less than 1");

		final int m = dataset.length;
		final int k = Math.min(numberOfClusters, m);

		final int[] assignment;
		final int combinationsCount = (int)BinomialCoefficient.value(m, k);
		if(combinationsCount > 0 && combinationsCount <= maxIterations)
			assignment = clusterEnumerated(dataset, k);
		else
			assignment = clusterRandom(dataset, k, maxIterations);

		final Map<String, Collection<String>> clusters = new HashMap<>(1);
		for(int i = 0; i < assignment.length; i ++){
			final String clusterValue = dataset[i].getName();
			final String clusterKey = dataset[assignment[i]].getName();
			clusters.computeIfAbsent(clusterKey, key -> new HashSet<>(1))
				.add(clusterValue);
		}
		return clusters.values();
	}

	private static int[] clusterEnumerated(final SpeciesInterface[] dataset, final int k){
		final int m = dataset.length;

		double minScore = Double.MAX_VALUE;
		int[] minMedoids = new int[k];
		final int[] minAssignment = new int[m];
		final int[] assignment = new int[m];
		final Iterator<int[]> itr = Combinations.of(m, k).iterator();
		while(itr.hasNext()){
			final int[] combination = itr.next();

			//partitioning: assign each data object to the closest medoid
			final double score = assign(assignment, combination, dataset);
			//update: if the cost decreases, accept the new solution
			if(score < minScore){
				replaceArray(minMedoids, combination);
				replaceArray(minAssignment, assignment);
				minScore = score;
			}
		}
		return minAssignment;
	}

	private static int[] clusterRandom(final SpeciesInterface[] dataset, final int k, final int maxIterations){
		final int m = dataset.length;

		//initialize the medoids: select `k` random points out of the `n` data points
		final Set<Integer> medoidsRandom = new HashSet<>(k);
		selectMedoids(medoidsRandom, m, k);
		final int[] medoids = extractMedoids(medoidsRandom, k);

		final int[] assignment = new int[m];
		final int[] newAssignment = new int[m];
		final Set<Integer> newMedoidsRandom = new HashSet<>(k);

		//partitioning: assign each data object to the closest medoid
		double score = assign(assignment, medoids, dataset);

		int count = 0;
		while(count < maxIterations){
			count ++;

			//randomly select one non-medoid point
			selectNewMedoids(newMedoidsRandom, medoidsRandom, m, k);
			final int[] newMedoids = extractMedoids(newMedoidsRandom, k);
			//recalculate the cost
			final double newScore = assign(newAssignment, newMedoids, dataset);

			//update: if the cost decreases, accept the new solution
			if(newScore < score){
				replaceCollection(medoidsRandom, newMedoidsRandom);
				replaceArray(medoids, newMedoids);
				replaceArray(assignment, newAssignment);
				score = newScore;
			}
		}

		return assignment;
	}

	private static void selectMedoids(final Set<Integer> medoidsRandom, final int m, final int k){
		while(medoidsRandom.size() < Math.min(m, k))
			medoidsRandom.add(RANDOM.nextInt(m));
	}

	private static void selectNewMedoids(final Set<Integer> newMedoidsRandom, final Collection<Integer> medoidsRandom, final int m,
			final int k){
		do{
			newMedoidsRandom.clear();
			selectMedoids(newMedoidsRandom, m, k);
		}while(newMedoidsRandom.equals(medoidsRandom));
	}

	private static int[] extractMedoids(final Iterable<Integer> medoidsRandom, final int k){
		final int[] medoids = new int[k];
		final Iterator<Integer> itr = medoidsRandom.iterator();
		for(int i = 0; i < k; i ++)
			medoids[i] = itr.next();
		return medoids;
	}

	/**
	 * Assign all instances from the data set to the medoids.
	 *
	 * @param assignment	Best cluster indices for each instance in the data set.
	 * @param medoids	Candidate medoids.
	 * @param dataset	The data to assign to the medoids.
	 */
	private static double assign(final int[] assignment, final int[] medoids, final SpeciesInterface[] dataset){
		double score = 0.;
		final int n = dataset.length;
		for(int j = 0; j < n; j ++){
			final SpeciesInterface data = dataset[j];

			double minDistance = Double.MAX_VALUE;
			int minIndex = Integer.MAX_VALUE;
			for(int i = 0; i < medoids.length; i ++){
				final int k = medoids[i];
				if(k == j){
					minDistance = 0;
					minIndex = k;
					break;
				}

				final double distance = data.similarity(dataset[k]);
				if(distance < minDistance){
					minDistance = distance;
					minIndex = k;
				}
			}

			assignment[j] = minIndex;
			score += minDistance;
		}
		return score;
	}

	private static void replaceArray(final int[] array, final int[] newArray){
		System.arraycopy(newArray, 0, array, 0, array.length);
	}

	private static void replaceCollection(final Collection<Integer> collection, final Collection<Integer> newCollection){
		collection.clear();
		collection.addAll(newCollection);
	}

}
