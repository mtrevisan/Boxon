package io.github.mtrevisan.boxon.core.similarity.kmeanscluster;

import java.util.ArrayList;
import java.util.List;


public class ClusterCenter extends Point{

	private final List<Integer> clusterPoints;
	private final double[] sumOfPoints;


	ClusterCenter(final Point p){
		super(p.pos);

		clusterPoints = new ArrayList<>();
		sumOfPoints = new double[dimension];
	}

	void addPointToCluster(final int index){
		final Point p = Process.instances.get(index);
		clusterPoints.add(index);
		final double[] po = p.getPosition();
		for(int i = 0; i < dimension; i ++)
			sumOfPoints[i] += po[i];
	}

	ClusterCenter getNewCenter(){
		final double[] pos = new double[Process.dimension];
		for(int i = 0; i < dimension; i ++)
			pos[i] = sumOfPoints[i] / clusterPoints.size();
		return new ClusterCenter(new Point(pos));
	}

	double evaluate(){
		double ret = 0.;
		for(int i = 0; i < clusterPoints.size(); i ++)
			ret += Point.squareDistance(Process.instances.get(clusterPoints.get(i)), this);
		return ret;
	}

	List<Integer> belongedPoints(){
		return new ArrayList<>(clusterPoints);
	}

}
