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

//https://github.com/Dhananjaya07/BioinformaticsPhylo/blob/master/src/Driver.java
//https://github.com/audreyclark/phylotree/blob/master/PhyloTree.java
//https://github.com/ccarter11/phytree/blob/main/phytree.java
//https://github.com/rngSwoop/EvolutionaryTree/blob/main/PhyloTree.java
//https://github.com/ajaykc7/Evolutionary-Tree/blob/master/BinaryTree.java
//
//https://guava.physics.uiuc.edu/~nigel/courses/598BIO/498BIOonline-essays/hw2/files/hw2_li.pdf

/*
characters-based, maximum likelihood (https://en.wikipedia.org/wiki/Computational_phylogenetics#Maximum_likelihood)
Uses each position in an alignment and evaluates all possible trees. It calculates the likelihood for each tree and seeks the one with the
maximum likelihood.
For a given tree, at each site, the likelihood is determined by evaluating the probability that a certain evolutionary model has generated
the observed data. The likelihood’s for each site are then multiplied to provide likelihood for each tree.

also https://scholarship.claremont.edu/cgi/viewcontent.cgi?article=1047&context=scripps_theses
*/
public class EvolutionaryTree{
}
