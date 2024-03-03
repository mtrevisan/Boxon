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
package io.github.mtrevisan.boxon.core.similarity;

import io.github.mtrevisan.boxon.core.similarity.tree.PhylogeneticTreeNode;
import io.github.mtrevisan.boxon.core.similarity.tree.SpeciesInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//https://en.wikipedia.org/wiki/Complete-linkage_clustering
//https://en.wikipedia.org/wiki/UPGMA
//https://en.wikipedia.org/wiki/UPGMA
public class CompleteLinkageClustering{

	private static final Matcher MATCHER_KEY = Pattern.compile("^\\(([^|]+?)\\|(.+?)\\)$")
		.matcher("");


	private CompleteLinkageClustering(){}


	/**
	 * @param species	Set of species from which to infer the phylogenetic tree.
	 */
	public static PhylogeneticTreeNode build(final SpeciesInterface[] species){
		//create a tree for each species
		final List<PhylogeneticTreeNode> forest = initializeForest(species);

		//store distance matrix from each species to every other species
		Map<String, Integer> distanceMatrix = createDistanceMatrix(species);

		while(findMostSimilarSpecies(forest, distanceMatrix))
			distanceMatrix = createDistanceMatrix(forest, species);

		return forest.get(0);
	}

	private static List<PhylogeneticTreeNode> initializeForest(final SpeciesInterface[] species){
		final List<PhylogeneticTreeNode> forest = new ArrayList<>(0);
		for(int i = 0; i < species.length; i ++)
			forest.add(PhylogeneticTreeNode.create(species[i].getName()));
		return forest;
	}

	private static Map<String, Integer> createDistanceMatrix(final SpeciesInterface[] species){
		final int speciesCount = species.length;
		final Map<String, Integer> distanceMatrix = new HashMap<>(speciesCount * (speciesCount - 1) / 2);
		for(int i = 0; i < speciesCount; i ++){
			final SpeciesInterface speciesI = species[i];
			for(int j = i + 1; j < speciesCount; j ++){
				final SpeciesInterface speciesJ = species[j];
				final String key = composeKey(speciesI.getName(), speciesJ.getName());
				distanceMatrix.put(key, speciesI.distance(speciesJ));
			}
		}
		return distanceMatrix;
	}

	private static Map<String, Integer> createDistanceMatrix(final List<PhylogeneticTreeNode> forest, final SpeciesInterface[] species){
		final int speciesCount = forest.size();
		final Map<String, Integer> distanceMatrix = new HashMap<>(speciesCount * (speciesCount - 1) / 2);
		for(int i = 0; i < speciesCount; i ++){
			final String speciesILabel = forest.get(i).getLabel();
			final List<SpeciesInterface> speciesI = getSpecies(speciesILabel, species);
			for(int j = i + 1; j < speciesCount; j ++){
				final String speciesJLabel = forest.get(j).getLabel();
				final List<SpeciesInterface> speciesJ = getSpecies(speciesJLabel, species);
				final String key = composeKey(speciesILabel, speciesJLabel);

				//use group-average linkage
				double distance = 0.;
				for(final SpeciesInterface spI : speciesI)
					for(final SpeciesInterface spJ : speciesJ)
						distance += spI.distance(spJ);
				distance /= speciesI.size() + speciesJ.size();

				distanceMatrix.put(key, (int)distance);
			}
		}
		return distanceMatrix;
	}

	private static List<SpeciesInterface> getSpecies(final String treeLabel, final SpeciesInterface[] species){
		final List<SpeciesInterface> out = new ArrayList<>(1);
		for(int i = 0; i < species.length; i ++)
			if(treeLabel.contains(species[i].getName()))
				out.add(species[i]);
		return out;
	}

	private static String composeKey(final String key1, final String key2){
		return "(" + key1 + "|" + key2 + ")";
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
		final Matcher matcher = MATCHER_KEY.reset(minKey);
		matcher.find();
		final String[] children = {matcher.group(1), matcher.group(2)};

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
			if(key.equals(rightChildLabel)){
				itr.remove();
				nodeChildren[1] = child;
			}
		}
		return nodeChildren;
	}

}
