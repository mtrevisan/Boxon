package io.github.mtrevisan.boxon.core.similarity;

import io.github.mtrevisan.boxon.core.similarity.evolutionarytree.distances.CharSequenceDistanceData;
import io.github.mtrevisan.boxon.core.similarity.evolutionarytree.distances.LevenshteinDistance;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;


/**
 * @see <a href="https://github.com/servbaset/K-medoids">K-medoids</a>
 * @see <a href="https://www.geeksforgeeks.org/ml-k-medoids-clustering-with-example/">K-Medoids clustering</a>
 */
public final class KMedoid{

	private static final Random RANDOM = new Random(System.currentTimeMillis());


	private KMedoid(){}


	/**
	 * Cluster the input.
	 *
	 * @param dataset	The list of strings to be clustered.
	 * @param numberOfClusters	Number of clusters to generate.
	 * @param maxIterations	The maximum number of iterations the algorithm is allowed to run.
	 * @return	The association for each data to the corresponding centroid.
	 */
	public static int[] cluster(final List<String> dataset, final int numberOfClusters, final int maxIterations){
		if(dataset == null || dataset.isEmpty())
			throw new IllegalArgumentException("Dataset cannot be empty");
		if(numberOfClusters < 1)
			throw new IllegalArgumentException("Number of clusters cannot be less than 1");
		if(maxIterations < 1)
			throw new IllegalArgumentException("Maximum number of iterations cannot be less than 1");

		final int m = dataset.size();
		final int k = Math.min(numberOfClusters, m);

		//initialize the medoids: select `k` random points out of the `n` data points
		final Set<Integer> medoidsRandom = new HashSet<>(k);
		selectMedoids(medoidsRandom, m, k);
		final int[] medoids = extractMedoids(medoidsRandom, k);

		final int[] assignment = new int[m];
		final int[] newAssignment = new int[m];
		final Set<Integer> newMedoidsRandom = new HashSet<>(k << 1);

		//partitioning: assign each data object to the closest medoid
		double score = assign(assignment, medoids, dataset);

		int count = 0;
		while(count < maxIterations){
			count ++;

			//randomly select one non-medoid point
			selectNewMedoids(newMedoidsRandom, medoidsRandom, m, k);
			final int[] newMedoids = extractMedoids(medoidsRandom, k);
			//recalculate the cost
			final double newScore = assign(newAssignment, newMedoids, dataset);

			//update: if the cost decreases, accept the new solution
			if(newScore < score){
				replaceMedoids(medoidsRandom, newMedoidsRandom);
				replaceArray(medoids, newMedoids);
				replaceArray(assignment, newAssignment);
				score = newScore;
			}
		}
		return assignment;
	}

	private static Set<Integer> selectMedoids(final Set<Integer> medoidsRandom, final int m, final int max){
		while(medoidsRandom.size() < Math.min(max, m))
			medoidsRandom.add(RANDOM.nextInt(m));
		return medoidsRandom;
	}

	private static void selectNewMedoids(final Set<Integer> newMedoidsRandom, final Collection<Integer> medoidsRandom, final int m, final int k){
		newMedoidsRandom.addAll(medoidsRandom);
		selectMedoids(newMedoidsRandom, m, k << 1);
		newMedoidsRandom.removeAll(medoidsRandom);
		final int newMedoidsRandomSize = newMedoidsRandom.size();
		if(newMedoidsRandomSize < k){
			final Iterator<Integer> itr = medoidsRandom.iterator();
			for(int i = newMedoidsRandomSize; i < k; i ++)
				newMedoidsRandom.add(itr.next());
		}
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
	private static double assign(final int[] assignment, final int[] medoids, final List<String> dataset){
		double score = 0.;
		final int n = dataset.size();
		for(int j = 0; j < n; j ++){
			final String data = dataset.get(j);

			int minDistance = Integer.MAX_VALUE;
			int minIndex = Integer.MAX_VALUE;
			for(int i = 0; i < medoids.length; i ++){
				final int k = medoids[i];
				if(k == j){
					minDistance = 0;
					minIndex = k;
					break;
				}

				final int distance = LevenshteinDistance.distance(CharSequenceDistanceData.of(data),
					CharSequenceDistanceData.of(dataset.get(k)));
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

	private static void replaceMedoids(final Collection<Integer> set, final Collection<Integer> newSet){
		set.clear();
		set.addAll(newSet);
	}

}
