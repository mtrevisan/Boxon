/*
 * Copyright (c) 2021-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.similarity.connectivityclustering;

import io.github.mtrevisan.boxon.core.similarity.distances.DistanceDataInterface;
import io.github.mtrevisan.boxon.core.similarity.tree.PhylogeneticTreeNode;
import io.github.mtrevisan.boxon.core.similarity.tree.SpeciesInterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


//https://en.wikipedia.org/wiki/Complete-linkage_clustering
//https://en.wikipedia.org/wiki/UPGMA
//https://en.wikipedia.org/wiki/UPGMA
public class CompleteLinkageClustering{

	private CompleteLinkageClustering(){}


	/**
	 * @param species	Set of species from which to infer the phylogenetic tree.
	 * @return	The root of the tree.
	 */
	public static <D extends DistanceDataInterface<D>> PhylogeneticTreeNode build(final SpeciesInterface<D>[] species){
		//create a tree for each species
		final List<PhylogeneticTreeNode> forest = initializeForest(species);

		//store distance matrix from each species to every other species
		Map<String, Integer> distanceMatrix = createDistanceMatrix(species);

		while(findMostSimilarSpecies(forest, distanceMatrix))
			distanceMatrix = createDistanceMatrix(forest, species);

		return forest.getFirst();
	}

	/**
	 * @param species	Set of species from which to infer the phylogenetic tree.
	 * @return	A set of sets where each data is grouped into.
	 */
	public static <D extends DistanceDataInterface<D>> Collection<Collection<String>> cluster(final SpeciesInterface<D>[] species){
		final PhylogeneticTreeNode root = build(species);

		return PhylogeneticTreeNode.getSpeciesByLevel(root);
	}

	private static List<PhylogeneticTreeNode> initializeForest(final SpeciesInterface<?>[] species){
		final List<PhylogeneticTreeNode> forest = new ArrayList<>(0);
		for(int i = 0, length = species.length; i < length; i ++)
			forest.add(PhylogeneticTreeNode.create(species[i].getName()));
		return forest;
	}

	private static <D extends DistanceDataInterface<D>> Map<String, Integer> createDistanceMatrix(final SpeciesInterface<D>[] species){
		final int speciesCount = species.length;
		final Map<String, Integer> distanceMatrix = new HashMap<>(speciesCount * (speciesCount - 1) / 2);
		for(int i = 0; i < speciesCount; i ++){
			final SpeciesInterface<D> speciesI = species[i];
			for(int j = i + 1; j < speciesCount; j ++){
				final SpeciesInterface<D> speciesJ = species[j];
				final String key = composeKey(speciesI.getName(), speciesJ.getName());
				distanceMatrix.put(key, speciesI.distance(speciesJ));
			}
		}
		return distanceMatrix;
	}

	private static <D extends DistanceDataInterface<D>> Map<String, Integer> createDistanceMatrix(final List<PhylogeneticTreeNode> forest,
			final SpeciesInterface<D>[] species){
		final int speciesCount = forest.size();
		final Map<String, Integer> distanceMatrix = new HashMap<>(speciesCount * (speciesCount - 1) / 2);
		for(int i = 0; i < speciesCount; i ++){
			final String speciesILabel = forest.get(i).getLabel();
			final List<SpeciesInterface<D>> speciesI = getSpecies(speciesILabel, species);
			for(int j = i + 1; j < speciesCount; j ++){
				final String speciesJLabel = forest.get(j).getLabel();
				final List<SpeciesInterface<D>> speciesJ = getSpecies(speciesJLabel, species);
				final String key = composeKey(speciesILabel, speciesJLabel);

				//use group-average linkage
				double distance = 0.;
				for(final SpeciesInterface<D> spI : speciesI)
					for(final SpeciesInterface<D> spJ : speciesJ)
						distance += spI.distance(spJ);
				distance /= speciesI.size() + speciesJ.size();

				distanceMatrix.put(key, (int)distance);
			}
		}
		return distanceMatrix;
	}

	private static <D extends DistanceDataInterface<D>> List<SpeciesInterface<D>> getSpecies(final String treeLabel,
			final SpeciesInterface<D>[] species){
		final List<SpeciesInterface<D>> out = new ArrayList<>(1);
		for(int i = 0, length = species.length; i < length; i ++)
			if(treeLabel.contains(species[i].getName()))
				out.add(species[i]);
		return out;
	}

	private static String composeKey(final String key1, final String key2){
		return "(" + key1 + "|" + key2 + ")";
	}

	private static String[] dismantleKey(final String key){
		int open = 0;
		for(int i = 1; i < key.length() - 1; i ++){
			if(key.charAt(i) == '(')
				open ++;
			else if(key.charAt(i) == ')')
				open --;
			else if(key.charAt(i) == '|' && open == 0)
				return new String[]{key.substring(1, i), key.substring(i + 1, key.length() - 1)};
		}
		//cannot happen
		return null;
	}

	private static boolean findMostSimilarSpecies(final List<PhylogeneticTreeNode> forest, final Map<String, Integer> distanceMatrix){
		//find the most similar pair in the current clustering
		int minValue = Integer.MAX_VALUE;
		String minKey = null;
		for(final Map.Entry<String, Integer> entry : distanceMatrix.entrySet()){
			final int value = entry.getValue();
			if(value < minValue){
				minValue = value;
				minKey = entry.getKey();
			}
		}
		final String[] children = dismantleKey(minKey);

		final PhylogeneticTreeNode[] nodeChildren = removeChildren(forest, children[0], children[1], distanceMatrix);

		//merge clusters
		forest.add(PhylogeneticTreeNode.create(composeKey(children[0], children[1]), nodeChildren[0], nodeChildren[1], minValue));

		return (forest.size() > 1);
	}

	private static PhylogeneticTreeNode[] removeChildren(final List<PhylogeneticTreeNode> forest, final String leftChildLabel,
			final String rightChildLabel, final Map<String, Integer> distanceMatrix){
		final int forestSize = forest.size();
		for(int i = 0; i < forestSize; i ++){
			final String treeLabel = forest.get(i)
				.getLabel();
			distanceMatrix.remove(composeKey(leftChildLabel, treeLabel));
			distanceMatrix.remove(composeKey(treeLabel, leftChildLabel));
			distanceMatrix.remove(composeKey(rightChildLabel, treeLabel));
			distanceMatrix.remove(composeKey(treeLabel, rightChildLabel));
		}

		final PhylogeneticTreeNode[] nodeChildren = new PhylogeneticTreeNode[2];
		final Iterator<PhylogeneticTreeNode> itr = forest.iterator();
		while(itr.hasNext()){
			final PhylogeneticTreeNode child = itr.next();
			final String key = child
				.getLabel();
			if(key.equals(leftChildLabel)){
				itr.remove();
				nodeChildren[0] = child;
			}
			else if(key.equals(rightChildLabel)){
				itr.remove();
				nodeChildren[1] = child;
			}
		}
		return nodeChildren;
	}

}
