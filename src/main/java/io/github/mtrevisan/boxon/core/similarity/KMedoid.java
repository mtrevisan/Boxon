package io.github.mtrevisan.boxon.core.similarity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


/**
 * also @see <a href="https://github.com/conndots/KMeansCluster/tree/master">KMeansCluster</a>
 * also @see <a href="https://github.com/DatabaseGroup/tree-similarity/blob/master/src/json/quickjedi_index_impl.h">tree-similarity</a>
 * https://github.com/servbaset/K-medoids
 */
public class KMedoid{

	private static final Random RANDOM = new Random(System.currentTimeMillis());


	public void cluster(final List<String> dataset, final int k, final int maxIterations){
		//select `k` random points out of the `n` data points as the medoids
		final Set<Integer> medoidsRandom = new HashSet<>();
		final int m = dataset.size();
		while(medoidsRandom.size() < k)
			medoidsRandom.add(RANDOM.nextInt(m));
		final Iterator<Integer> itr = medoidsRandom.iterator();
		final int[] medoids = new int[k];
		for(int i = 0; i < k; i ++)
			medoids[i] = itr.next();

		//associate each data point to the closest medoid
		final Map<Integer, List<Integer>> clusters = new HashMap<>(k);
		final int[] assignment = new int[m];
		int count = 0;
		boolean changed = true;
		//while the cost decreases...
		while(changed && count < maxIterations){
			changed = false;
			count ++;

			assign(assignment, medoids, dataset);

			changed = recalculateMedoids(assignment, medoids, clusters, dataset);
		}
	}

	/**
	 * Assign all instances from the data set to the medoids.
	 *
	 * @param out	Best cluster indices for each instance in the data set.
	 * @param medoids	Candidate medoids.
	 * @param dataset	The data to assign to the medoids.
	 */
	private void assign(final int[] out, final int[] medoids, final List<String> dataset){
		final int n = dataset.size();
		for(int j = 0; j < n; j ++){
			final String data = dataset.get(j);

			int minDistance = Integer.MAX_VALUE;
			int minIndex = Integer.MAX_VALUE;
			for(int i = 0; i < medoids.length; i ++){
				final int k = medoids[i];
				if(k == j){
					minIndex = i;
					break;
				}

				final int distance = DamerauLevenshteinDistance.distance(data, dataset.get(k));
				if(distance < minDistance){
					minDistance = distance;
					minIndex = k;
				}
			}

			out[j] = minIndex;
		}
	}

	/**
	 * Return a array with on each position the clusterIndex to which the Instance on that position in the dataset belongs.
	 *
	 * @param assignment	The new assignment of all instances to the different medoids.
	 * @param medoids	The current set of cluster medoids, will be modified to fit the new assignment.
	 * @param clusters	The cluster output, this will be modified at the end of the method.
	 * @return	Whether the clusters changed.
	 */
	private boolean recalculateMedoids(final int[] assignment, final int[] medoids, final Map<Integer, List<Integer>> clusters,
			final List<String> dataset){
		final int m = dataset.size();
		final int k = medoids.length;
		boolean changed = false;
		for(int i = 0; i < k; i ++){
			clusters.get(i)
				.clear();
			for(int j = 0; j < assignment.length; j ++)
				if(assignment[j] == i)
					clusters.get(i)
						.add(j);

			//new random, empty medoid
			if(clusters.get(i).isEmpty()){
				clusters.get(medoids[i])
					.add(RANDOM.nextInt(m));

				changed = true;
			}
			else{
				final List<Integer> oldMedoid = clusters.get(medoids[i]);
				final double[] centroid = average(dataset, clusters[i]);
				clusters.put(medoids[i], data.kNearest(1, centroid).iterator().next());
				if(!clusters.get(medoids[i]).equals(oldMedoid))
					changed = true;
			}
		}
		return changed;
	}

	/**
	 * Creates an instance that contains the average values for the attributes.
	 *
	 * @param data	Data set to calculate average attribute values for
	 * @return	Instance representing the average attribute values
	 */
	public static double[] average(final List<String> dataset, final String data){
		double[] tmpOut = new double[data.noAttributes()];
		for(int i = 0; i < data.noAttributes(); i ++){
			double sum = 0.;
			for(int j = 0; j < data.size(); j ++)
				sum += data.get(j)
					.value(i);
			tmpOut[i] = sum / data.size();
		}
		return tmpOut;
	}

}
