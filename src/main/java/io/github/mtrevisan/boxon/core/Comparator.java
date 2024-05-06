/*
 * Copyright (c) 2020-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.core.similarity.distances.StringArrayMetricData;
import io.github.mtrevisan.boxon.core.similarity.distances.metrics.LevenshteinMetric;
import io.github.mtrevisan.boxon.core.similarity.distances.metrics.Metric;
import io.github.mtrevisan.boxon.exceptions.BoxonException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * Comparator class for measuring distance and similarity between two template classes.
 */
public final class Comparator{

	private final Describer describer;

	private final Metric<StringArrayMetricData> metric = LevenshteinMetric.create();


	/**
	 * Create a comparator.
	 *
	 * @param core	The parser core.
	 * @return	A comparator.
	 */
	public static Comparator create(final Core core){
		return new Comparator(core);
	}


	private Comparator(final Core core){
		describer = Describer.create(core);
	}


	/**
	 * Calculates the distance between two template classes.
	 *
	 * @param templateClass1	The first template class.
	 * @param templateClass2	The second template class.
	 * @return	The distance between the template classes.
	 * @throws BoxonException	If an error occurs during the calculation.
	 */
	public int distance(final Class<?> templateClass1, final Class<?> templateClass2) throws BoxonException{
		final StringArrayMetricData dna1 = getTemplateGenome(templateClass1);
		final StringArrayMetricData dna2 = getTemplateGenome(templateClass2);

		return metric.distance(dna1, dna2);
	}

	/**
	 * Calculates the similarity between two template classes.
	 *
	 * @param templateClass1	The first template class.
	 * @param templateClass2	The second template class.
	 * @return	The similarity between the template classes.
	 * @throws BoxonException	If an error occurs during the calculation.
	 */
	public double similarity(final Class<?> templateClass1, final Class<?> templateClass2) throws BoxonException{
		final StringArrayMetricData dna1 = getTemplateGenome(templateClass1);
		final StringArrayMetricData dna2 = getTemplateGenome(templateClass2);

		return metric.similarity(dna1, dna2);
	}


	private StringArrayMetricData getTemplateGenome(final Class<?> templateClass) throws BoxonException{
		final Map<String, Object> description = describer.describeTemplate(templateClass);
		return extractGenomeParameters(description);
	}

	private static StringArrayMetricData extractGenomeParameters(final Map<String, Object> description){
		final Collection<Map<String, Object>> fields = (Collection<Map<String, Object>>)description.get(DescriberKey.FIELDS.toString());
		final String[] genome = new String[fields.size()];
		final int gene = populateGenome(fields, genome);
		return StringArrayMetricData.of(Arrays.copyOfRange(genome, 0, gene));
	}

	private static int populateGenome(final Collection<Map<String, Object>> fields, final String[] genome){
		int gene = 0;
		for(final Map<String, Object> field : fields){
			final Map<String, Object> parameter = removeUnwantedProperties(field);

			if(parameter.containsKey(DescriberKey.COLLECTION_TYPE.toString()))
				genome[gene] += parameter.toString();
			else
				genome[gene ++] = parameter.toString();
		}
		return gene;
	}

	private static Map<String, Object> removeUnwantedProperties(final Map<String, Object> field){
		final Map<String, Object> parameter = new HashMap<>(field);
		parameter.remove(DescriberKey.FIELD_NAME.toString());
		parameter.remove(DescriberKey.CONDITION.toString());
		parameter.remove(DescriberKey.VALIDATOR.toString());
		return parameter;
	}

}
