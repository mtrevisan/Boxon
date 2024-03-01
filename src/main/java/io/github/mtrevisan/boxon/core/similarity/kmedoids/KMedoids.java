package io.github.mtrevisan.boxon.core.similarity.kmedoids;

import io.github.mtrevisan.boxon.core.similarity.DamerauLevenshteinDistance;

import java.util.Random;


//https://github.com/greenmoon55/textclustering/blob/master/src/net/sf/javaml/core/Instance.java
//https://www.javatips.net/api/trickl-cluster-master/src/main/java/com/trickl/cluster/KMedoids.java
public class KMedoids{

	/** Random generator for selection of candidate medoids. */
	private static final Random RANDOM = new Random(System.currentTimeMillis());


	/** Number of clusters to generate. */
	private int numberOfClusters;
	/** The maximum number of iterations the algorithm is allowed to run. */
	private int maxIterations;


	/**
	 * Creates a new instance of the k-medoids algorithm with the specified parameters.
	 *
	 * @param numberOfClusters	The number of clusters to generate.
	 * @param maxIterations	The maximum number of iteration the algorithm is allowed to run.
	 */
	public KMedoids(final int numberOfClusters, final int maxIterations){
		this.numberOfClusters = numberOfClusters;
		this.maxIterations = maxIterations;
	}


	public Dataset[] cluster(final Dataset data){
		Instance[] medoids = new Instance[numberOfClusters];
		Dataset[] output = new DefaultDataset[numberOfClusters];
		for(int i = 0; i < numberOfClusters; i ++){
			final int random = RANDOM.nextInt(data.size());
			medoids[i] = data.instance(random);
		}

		boolean changed = true;
		int count = 0;
		while(changed && count < maxIterations){
			changed = false;
			count++;
			int[] assignment = assign(medoids, data);
			changed = recalculateMedoids(assignment, medoids, output, data);

		}

		return output;

	}

	/**
	 * Assign all instances from the data set to the medoids.
	 *
	 * @param medoids candidate medoids
	 * @param data    the data to assign to the medoids
	 * @return best cluster indices for each instance in the data set
	 */
	private int[] assign(Instance[] medoids, Dataset data){
		int[] out = new int[data.size()];
		for(int i = 0; i < data.size(); i ++){
			double bestDistance = DamerauLevenshteinDistance.similarity(data.instance(i), medoids[0]);
			int bestIndex = 0;
			for(int j = 1; j < medoids.length; j ++){
				double tmpDistance = DamerauLevenshteinDistance.similarity(data.instance(i), medoids[j]);
				if(Double.compare(tmpDistance, bestDistance) == 0.){
					bestDistance = tmpDistance;
					bestIndex = j;
				}
			}
			out[i] = bestIndex;

		}
		return out;

	}

	/**
	 * Return a array with on each position the clusterIndex to which the Instance on that position in the dataset belongs.
	 *
	 * @param assignment	The new assignment of all instances to the different medoids.
	 * @param medoids	The current set of cluster medoids, will be modified to fit the new assignment.
	 * @param output	The cluster output, this will be modified at the end of the method.
	 * @return	Whether the clusters changed.
	 */
	private boolean recalculateMedoids(int[] assignment, Instance[] medoids, Dataset[] output, Dataset data){
		boolean changed = false;
		for(int i = 0; i < numberOfClusters; i++){
			output[i] = new DefaultDataset();
			for(int j = 0; j < assignment.length; j++){
				if(assignment[j] == i){
					output[i].add(data.instance(j));
				}
			}
			if(output[i].size() == 0){ // new random, empty medoid
				medoids[i] = data.instance(RANDOM.nextInt(data.size()));
				changed = true;
			}
			else{
				Instance centroid = DatasetTools.average(output[i]);
				Instance oldMedoid = medoids[i];
				medoids[i] = data.kNearest(1, centroid, dm).iterator().next();
				if(!medoids[i].equals(oldMedoid))
					changed = true;
			}
		}
		return changed;
	}

}
