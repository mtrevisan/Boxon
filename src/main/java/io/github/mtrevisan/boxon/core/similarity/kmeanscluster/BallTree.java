package io.github.mtrevisan.boxon.core.similarity.kmeanscluster;


public class BallTree{

	/** NOTE: when call this function, the parameter should be <code>null</code>. */
	static Hypersphere buildAnInstance(Hypersphere cur){
		if(cur == null){
			cur = new Hypersphere();
			for(int i = 0; i < Process.instances.size(); ++ i){
				cur.addInstance(i);
			}
			cur.endAdding();
		}
		Hypersphere[] ch = cur.split();
		for(Hypersphere hp : ch){
			if(hp.size() <= Process.maxInstancesNumNotSplit){
				continue;
			}
			buildAnInstance(hp);
		}
		return cur;
	}

}
