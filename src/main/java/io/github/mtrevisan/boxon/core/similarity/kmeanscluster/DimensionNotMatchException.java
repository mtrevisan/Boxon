package io.github.mtrevisan.boxon.core.similarity.kmeanscluster;


public class DimensionNotMatchException extends Exception{

	private static final long serialVersionUID = 74463019850062185L;


	public String toString(){
		return "The two operators' dimension does not match.";
	}

}
