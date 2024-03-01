package io.github.mtrevisan.boxon.core.similarity.kmedoids2;

import java.util.ArrayList;
import java.util.List;


public class Medoid{

	private List<Item> items = new ArrayList<>();
	private Item centerOfMedoid;
	private int cost;


	Medoid(Item centerOfMedoid, int cost){
		this.centerOfMedoid = centerOfMedoid;
		this.cost = cost;
	}

	public List<Item> getItems(){
		return items;
	}

	public Item getCenterOfMedoid(){
		return centerOfMedoid;
	}

	int getCost(){
		return cost;
	}

	void setCost(int cost){
		this.cost = cost;
	}

	@Override
	public String toString(){
		return "\nMedoid{" + "items=" + items + ", centerOfMedoid=" + centerOfMedoid + ", cost=" + cost + '}';
	}

}
