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
package io.github.mtrevisan.boxon.core.similarity.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;


/**
 * Defines a phylogenetic tree node type, which stores the information associated with a node in the tree.
 */
public final class PhylogeneticTreeNode{

	//reference variable for left child ({@code null} if empty)
	private PhylogeneticTreeNode leftChild;
	//reference variable for right child ({@code null} if empty)
	private PhylogeneticTreeNode rightChild;
	//a unique string label for the species or non-terminal
	private final String label;
	//edge weight to left child (which is also the edge weight to the right child) (Use {@code 0} for terminals)
	private double distanceToChild;
	//caches the number of leaves in the tree
	private int leafCount;


	public static PhylogeneticTreeNode create(final String label){
		return new PhylogeneticTreeNode(label);
	}

	public static  PhylogeneticTreeNode create(final String label, final PhylogeneticTreeNode leftChild,
			final PhylogeneticTreeNode rightChild, final double distanceToChild){
		return new PhylogeneticTreeNode(label, leftChild, rightChild, distanceToChild);
	}


	/**
	 * @param label	Label of the node.
	 */
	private PhylogeneticTreeNode(final String label){
		this.label = label;
		leafCount = 1;
	}

	/**
	 * @param label	Composition of left and right children labels.
	 * @param leftChild	Left child.
	 * @param rightChild	Right child.
	 * @param distanceToChild	Average edge weight from this new node to each of its children.
	 */
	private PhylogeneticTreeNode(final String label, final PhylogeneticTreeNode leftChild, final PhylogeneticTreeNode rightChild,
			final double distanceToChild){
		this.label = label;
		this.leftChild = leftChild;
		this.rightChild = rightChild;
		this.distanceToChild = distanceToChild;
		leafCount = 0;
		if(leftChild != null)
			leafCount += leftChild.leafCount;
		if(rightChild != null)
			leafCount += rightChild.leafCount;
	}


	public static Collection<Collection<String>> getSpeciesByLevel(final PhylogeneticTreeNode root){
		if(root == null)
			return Collections.emptyList();

		final List<Collection<String>> nodesByLevel = new ArrayList<>();
		final Queue<PhylogeneticTreeNode> queue = new LinkedList<>();
		queue.offer(root);
		while(!queue.isEmpty()){
			final int levelSize = queue.size();
			final Collection<String> currentLevel = new HashSet<>();
			for(int i = 0; i < levelSize; i ++){
				final PhylogeneticTreeNode node = queue.poll();

				currentLevel.add(node.label);
				if(node.leftChild != null)
					queue.offer(node.leftChild);
				if(node.rightChild != null)
					queue.offer(node.rightChild);
			}
			nodesByLevel.add(currentLevel);
		}

		//add all the nodes in the previous levels
		for(int i = 1; i < nodesByLevel.size(); i ++){
			final Collection<String> nextLevel = nodesByLevel.get(i);
			for(int j = 0; j < i; j ++)
				nextLevel.addAll(nodesByLevel.get(j));
		}

		return nodesByLevel;
	}


	public PhylogeneticTreeNode getLeftChild(){
		return leftChild;
	}

	public PhylogeneticTreeNode getRightChild(){
		return rightChild;
	}

	public String getLabel(){
		return label;
	}

	public int getLeafCount(){
		return leafCount;
	}

	public boolean isLeaf(){
		return (leftChild == null && rightChild == null);
	}

	@Override
	public String toString(){
		return (isLeaf()? label: String.format(Locale.US, "[INODE %.2f]", distanceToChild));
	}

}
