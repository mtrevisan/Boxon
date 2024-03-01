package io.github.mtrevisan.boxon.core.similarity.kmeanscluster;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class Process{

	List<ClusterCenter> centers = new ArrayList<>();
	List<Point> instances = new ArrayList<>();
	List<ClusterCenter> preCens;
	int dimension;
	int maxInstancesNumNotSplit = Integer.valueOf(LoadProperties.load("max_instances_num_not_split"));
	Hypersphere ballTree;
	int tryTimes = Integer.valueOf(LoadProperties.load("try_times"));
	//map cluster center results to its evaluation
	List<Map.Entry<List<ClusterCenter>, Double>> results = new ArrayList<>(tryTimes);


	/**
	 * @param k	The initial number of clustering centers
	 * @return	An entry: the key is the result of clustering. The label starts from 0. The value is the evaluation of the clustering result.
	 */
	public Map.Entry<Integer[], Double> cluster(final int k){
		for(int t = 0; t < tryTimes; t++){
			//random pick the cluster centers
			centers.clear();
			if(preCens != null)
				preCens = null;
			final Random rand = new Random();
			final Set<Integer> rSet = new HashSet<>();
			final int size = instances.size();
			while(rSet.size() < k)
				rSet.add(rand.nextInt(size));
			for(final int index : rSet)
				centers.add(new ClusterCenter(instances.get(index)));

			//iteration until convergence
			while(! timeToEnd()){
				Hypersphere.locateAndAssign(ballTree);
				preCens = new ArrayList<>(centers);
				List<ClusterCenter> newCenters = new ArrayList<>();
				for(final ClusterCenter cc : centers)
					newCenters.add(cc.getNewCenter());
				centers = newCenters;
			}
			results.add(new AbstractMap.SimpleEntry<>(preCens, evaluate(preCens)));
			Hypersphere.ALL_COUNT = 0;
			Hypersphere.COUNT = 0;
		}

		//找到多次试验中评分最小的
		double minEvaluate = Double.MAX_VALUE;
		int minIndex = 0, i = 0;
		for(final Map.Entry<List<ClusterCenter>, Double> entry : results){
			double e = entry.getValue();
			if(e < minEvaluate){
				minEvaluate = e;
				minIndex = i;
			}
			i++;
		}
		centers = results.get(minIndex).getKey();
		double evaluate = results.get(minIndex).getValue();
		//将instance对应的聚类编号返回
		final Integer[] ret = new Integer[instances.size()];
		for(int cNum = 0; cNum < centers.size(); cNum++){
			final ClusterCenter cc = centers.get(cNum);
			for(final int pi : cc.belongedPoints())
				ret[pi] = cNum;
		}
		return new AbstractMap.SimpleEntry<>(ret, evaluate);
	}

	private boolean timeToEnd(){
		if(preCens == null)
			return false;
		for(final ClusterCenter cc : centers)
			if(! preCens.contains(cc))
				return false;
		return true;
	}

	//gives your dataset's path and this function will build the internal data structures.
	public void loadData(String path) throws IOException{
		final BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
		String line;
		while((line = r.readLine()) != null){
			String[] fs = line.split(" +");
			double[] pos = new double[fs.length];
			int i = 0;
			for(final String s : fs)
				pos[i++] = Double.parseDouble(s + ".0");
			dimension = fs.length;
			instances.add(new Point(pos));
		}
		r.close();

		ballTree = BallTree.buildAnInstance(null);
	}

	private double evaluate(final List<ClusterCenter> cens){
		double ret = 0.;
		for(final ClusterCenter cc : cens)
			ret += cc.evaluate();
		return ret;
	}

	/**
	 * gives the evaluation and differential of each k in specific range.you can use these infos to choose a good k for your clustering
	 *
	 * @param startK gives the start point of k for the our try on k(inclusive)
	 * @param endK   gives the end point(exclusive)
	 * @return Entry's key is the evaluation of clustering of each k.The value is the differential of the evaluations--evaluation of k(i) - evaluation of k(i+1) for i in range(startK, endK - 1)
	 */
	public Map.Entry<List<Double>, List<Double>> cluster(int startK, int endK){
		List<Integer[]> results = new ArrayList<>();
		List<Double> evals = new ArrayList<>();
		for(int k = startK; k < endK; k++){
			System.out.println("now k = " + k);

			final Map.Entry<Integer[], Double> en = cluster(k);
			results.add(en.getKey());
			evals.add(en.getValue());
		}

		final List<Double> subs = new ArrayList<>();
		for(int i = 0; i < evals.size() - 1; i++)
			subs.add(evals.get(i) - evals.get(i + 1));

		return new AbstractMap.SimpleEntry<>(evals, subs);
	}

}
