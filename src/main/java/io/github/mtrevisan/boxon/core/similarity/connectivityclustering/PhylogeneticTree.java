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


/**
 * Defines a phylogenetic tree, which is a strictly binary tree that represents inferred hierarchical relationships between species.
 * <p>
 * There are weights along each edge; the weight from parent to left child is the same as parent to right child.
 * </p>
 *
 * @see <a href="https://github.com/rngSwoop/EvolutionaryTree/tree/main">EvolutionaryTree</a>
 * @see <a href="https://github.com/audreyclark/phylotree/tree/master">phylotree</a>
 */
public final class PhylogeneticTree{

	private PhylogeneticTree(){}


	/**
	 * @param species	Set of species from which to infer the phylogenetic tree.
	 * @return	The root of the tree.
	 */
	public static <S extends SpeciesInterface<S, D>, D extends DistanceDataInterface<D>> PhylogeneticTreeNode build(
			final SpeciesInterface<S, D>[] species){
		//create a tree for each species
		final List<PhylogeneticTreeNode> forest = initializeForest(species);

		//store distance matrix from each species to every other species
		final Map<String, Double> similarityMatrix = createSimilarityMatrix(species);
		while(forest.size() > 1){
			final PhylogeneticTreeNode newNode = mergeNodes(species, forest);

			//remove both children from forest
			final PhylogeneticTreeNode leftChild = newNode.getLeftChild();
			final PhylogeneticTreeNode rightChild = newNode.getRightChild();
			final String leftChildLabel = leftChild.getLabel();
			final String rightChildLabel = rightChild.getLabel();
			removeChildren(forest, leftChildLabel, rightChildLabel);

			//add new node to map
			addNewNode(forest, leftChild, rightChild, similarityMatrix, newNode);

			//remove both children from map
			removeChildren(forest, leftChildLabel, rightChildLabel, similarityMatrix);

			//add new tree to forest
			forest.add(newNode);
		}

		return forest.getFirst();
	}

	/**
	 * @param species	Set of species from which to infer the phylogenetic tree.
	 * @return	A set of sets where each data is grouped into.
	 */
	public static <S extends SpeciesInterface<S, D>, D extends DistanceDataInterface<D>> Collection<Collection<String>> cluster(
			final SpeciesInterface<S, D>[] species){
		final PhylogeneticTreeNode root = build(species);

		return PhylogeneticTreeNode.getSpeciesByLevel(root);
	}

	private static <S extends SpeciesInterface<S, D>, D extends DistanceDataInterface<D>> PhylogeneticTreeNode mergeNodes(
			final SpeciesInterface<S, D>[] species, final List<PhylogeneticTreeNode> forest){
		//find the smallest distance in forest
		final int forestSize = forest.size();
		double minDistance = Double.MAX_VALUE;
		PhylogeneticTreeNode child1 = null;
		PhylogeneticTreeNode child2 = null;
		for(int i = 0; i < forestSize; i ++)
			for(int j = i + 1; j < forestSize; j ++){
				final double distance = species[i].distance(species[j]);
				if(distance < minDistance){
					//enforce determinism (left child is alphabetically earlier)
					if(forest.get(i).getLabel().compareTo(forest.get(j).getLabel()) <= 0){
						child1 = forest.get(i);
						child2 = forest.get(j);
					}
					else{
						child1 = forest.get(j);
						child2 = forest.get(i);
					}
					minDistance = distance;
				}
			}

		//create new node
		return createNewNode(child1, child2, minDistance);
	}

	private static <S> List<PhylogeneticTreeNode> initializeForest(final SpeciesInterface<?, ?>[] species){
		final List<PhylogeneticTreeNode> forest = new ArrayList<>(0);
		for(int i = 0, length = species.length; i < length; i ++)
			forest.add(PhylogeneticTreeNode.create(species[i].getName()));
		return forest;
	}

	private static <S extends SpeciesInterface<S, D>, D extends DistanceDataInterface<D>> Map<String, Double> createSimilarityMatrix(
			final SpeciesInterface<S, D>[] species){
		final int speciesSize = species.length;
		final Map<String, Double> similarityMatrix = new HashMap<>((speciesSize - 1) * speciesSize / 2);
		for(int i = 0; i < speciesSize; i ++){
			final SpeciesInterface<S, D> speciesI = species[i];
			for(int j = i + 1; j < speciesSize; j ++){
				final SpeciesInterface<S, D> speciesJ = species[j];
				final String key = composeKey(speciesI.getName(), speciesJ.getName());
				similarityMatrix.put(key, speciesI.similarity(speciesJ));
			}
		}
		return similarityMatrix;
	}

	private static PhylogeneticTreeNode createNewNode(final PhylogeneticTreeNode leftChild,
			final PhylogeneticTreeNode rightChild, final double minDistance){
		final String newKey = composeKey(leftChild.getLabel(), rightChild.getLabel());
		return PhylogeneticTreeNode.create(newKey, leftChild, rightChild, minDistance / 2.);
	}

	private static void removeChildren(final Iterable<PhylogeneticTreeNode> forest, final String leftChildLabel,
			final String rightChildLabel){
		final Iterator<PhylogeneticTreeNode> itr = forest.iterator();
		int count = 0;
		while(count < 2 && itr.hasNext()){
			final String label = itr.next().getLabel();
			if(label.equals(leftChildLabel) || label.equals(rightChildLabel)){
				itr.remove();
				count ++;
			}
		}
	}

	private static void addNewNode(final List<PhylogeneticTreeNode> forest, final PhylogeneticTreeNode leftChild,
			final PhylogeneticTreeNode rightChild, final Map<String, Double> similarityMatrix, final PhylogeneticTreeNode newNode){
		final int forestSize = forest.size();
		final String leftChildLabel = leftChild.getLabel();
		final String rightChildLabel = rightChild.getLabel();
		final int leftLeafCount = leftChild.getLeafCount();
		final int rightLeafCount = rightChild.getLeafCount();
		final int denominator = leftLeafCount + rightLeafCount;
		double t1ToOther;
		double t2ToOther;
		double distanceToOther;
		for(int i = 0; i < forestSize; i ++){
			final PhylogeneticTreeNode tree = forest.get(i);
			final String leftKey = composeKey(leftChildLabel, tree.getLabel());
			final String rightKey = composeKey(rightChildLabel, tree.getLabel());
			if(similarityMatrix.get(leftKey) != null && similarityMatrix.get(rightKey) != null){
				t1ToOther = similarityMatrix.get(leftKey);
				t2ToOther = similarityMatrix.get(rightKey);
				final String newKey = composeKey(newNode.getLabel(), tree.getLabel());
				distanceToOther = (leftLeafCount * t1ToOther + rightLeafCount * t2ToOther) / denominator;
				similarityMatrix.put(newKey, distanceToOther);
			}
		}
	}

	private static <S> void removeChildren(final List<PhylogeneticTreeNode> forest, final String leftChildLabel,
			final String rightChildLabel, final Map<String, Double> similarityMatrix){
		final int forestSize = forest.size();
		for(int i = 0; i < forestSize; i ++){
			final String treeLabel = forest.get(i).getLabel();
			similarityMatrix.remove(composeKey(leftChildLabel, treeLabel));
			similarityMatrix.remove(composeKey(rightChildLabel, treeLabel));
		}
	}

	private static String composeKey(final String key1, final String key2){
		return key1 + "|" + key2;
	}

}
