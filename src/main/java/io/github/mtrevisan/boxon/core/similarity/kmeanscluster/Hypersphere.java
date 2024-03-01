package io.github.mtrevisan.boxon.core.similarity.kmeanscluster;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;


public class Hypersphere extends Point{

	static int COUNT = 0;
	static int ALL_COUNT = 0;


	private double radius;
	private List<Integer> instances;
	private Hypersphere[] children;
	private double[] sumOfPoints;


	Hypersphere(Point center, double r, List<Integer> ins){
		super(center.pos);

		this.radius = r;
		this.instances = ins;
		sumOfPoints = new double[Process.dimension];
	}

	Hypersphere(){
		super(new double[Process.dimension]);

		instances = new LinkedList<>();
		sumOfPoints = new double[Process.dimension];
	}

	void addInstance(int index){
		instances.add(index);
		double[] pos = Process.instances.get(index).getPosition();
		for(int i = 0; i < Process.dimension; i ++)
			sumOfPoints[i] += pos[i];
	}

	void endAdding(){
		int size = instances.size();
		for(int i = 0; i < Process.dimension; i ++)
			this.pos[i] = this.sumOfPoints[i] / size;
		this.radius = Point.euclideanDistance(this, Process.instances.get(this.getFarestPoint(this)));
	}

	int size(){
		return instances.size();
	}

	double maxDistance(Point p){
		return radius + Point.euclideanDistance(p, this);
	}

	double minDistance(Point p){
		return Point.euclideanDistance(p, this) - radius;
	}

	//如果不落在单独的cluster中，就返回-1 否则返回cluster center的index
	int isInSingleCluster(){
		ALL_COUNT++;
		PriorityQueue<Map.Entry<Integer, Double>> maxpq = new PriorityQueue<Map.Entry<Integer, Double>>(Process.centers.size(), new Comparator<Map.Entry<Integer, Double>>(){
			public int compare(Map.Entry<Integer, Double> e1, Map.Entry<Integer, Double> e2){
				double d1 = e1.getValue(), d2 = e2.getValue();
				if(d1 > d2)
					return 1;
				if(d1 < d2)
					return -1;
				return 0;
			}
		});
		PriorityQueue<Map.Entry<Integer, Double>> minpq = new PriorityQueue<Map.Entry<Integer, Double>>(Process.centers.size(), new Comparator<Map.Entry<Integer, Double>>(){
			public int compare(Map.Entry<Integer, Double> e1, Map.Entry<Integer, Double> e2){
				double d1 = e1.getValue(), d2 = e2.getValue();
				if(d1 > d2)
					return 1;
				if(d1 < d2)
					return -1;
				return 0;
			}
		});
		int index = 0;
		for(ClusterCenter cen : Process.centers){
			maxpq.add(new AbstractMap.SimpleEntry<Integer, Double>(index, this.maxDistance(cen)));
			minpq.add(new AbstractMap.SimpleEntry<Integer, Double>(index, this.minDistance(cen)));
			index ++;
		}
		Map.Entry<Integer, Double> the = maxpq.poll(), comp;
		index = the.getKey();
		double theDist = the.getValue();
		while((comp = minpq.poll()) != null){
			int ind = comp.getKey();
			double dis = comp.getValue();
			if(theDist < dis){
				if(ind != index){
					COUNT ++;
					return index;
				}
				else
					continue;
			}
			else{
				if(ind == index)
					continue;
				return - 1;
			}
		}
		return - 1;
	}

	private int getFarestPoint(Point p){
		double maxDist = 0.0;
		int maxIndex = - 1;
		for(int i : this.instances){
			Point pp = Process.instances.get(i);
			double dist = Point.euclideanDistance(p, pp);
			if(dist >= maxDist){
				maxDist = dist;
				maxIndex = i;
			}
		}
		return maxIndex;
	}

	//split and store it to this node's children field, & return the children.
	Hypersphere[] split(){
		int firstCenter = this.getFarestPoint(this);
		Point fir = Process.instances.get(firstCenter);
		int secondCenter = this.getFarestPoint(fir);
		Point sec = Process.instances.get(secondCenter);
		this.children = new Hypersphere[2];
		this.children[0] = new Hypersphere();
		this.children[1] = new Hypersphere();
		this.children[0].addInstance(firstCenter);
		this.children[1].addInstance(secondCenter);
		for(int i : this.instances){
			if(i == firstCenter || i == secondCenter)
				continue;
			Point p = Process.instances.get(i);
			double dist1 = Point.euclideanDistance(p, fir), dist2 = Point.euclideanDistance(p, sec);
			if(dist1 < dist2){
				this.children[0].addInstance(i);
			}
			else{
				this.children[1].addInstance(i);
			}
		}
		this.children[0].endAdding();
		this.children[1].endAdding();
		return this.children;
	}

	Hypersphere[] getChildren(){
		return this.children;
	}

	static void locateAndAssign(Hypersphere hp){
		int clusterIndex = hp.isInSingleCluster();
		if(clusterIndex != - 1){
			ClusterCenter cc = Process.centers.get(clusterIndex);
			for(int pi : hp.instances){
				cc.addPointToCluster(pi);
			}
			return;
		}
		if(hp.children == null){
			for(int pi : hp.instances){
				Point p = Process.instances.get(pi);
				double minDist = Double.MAX_VALUE;
				int minCenIndex = 0, index = 0;
				for(ClusterCenter cc : Process.centers){
					double dist = Point.euclideanDistance(p, cc);
					if(dist < minDist){
						minDist = dist;
						minCenIndex = index;
					}
					index++;
				}
				ClusterCenter cen = Process.centers.get(minCenIndex);
				cen.addPointToCluster(pi);
			}
		}
		else{
			for(Hypersphere chp : hp.children){
				Hypersphere.locateAndAssign(chp);
			}
		}
	}

}
