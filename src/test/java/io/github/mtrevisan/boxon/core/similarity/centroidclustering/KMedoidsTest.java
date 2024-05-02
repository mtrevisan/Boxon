/*
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.similarity.centroidclustering;

import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.core.Core;
import io.github.mtrevisan.boxon.core.CoreBuilder;
import io.github.mtrevisan.boxon.core.Describer;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.core.similarity.distances.StringArrayDistanceData;
import io.github.mtrevisan.boxon.core.similarity.tree.TemplateSpecies;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class KMedoidsTest{

	@TemplateHeader(start = "m0", end = "\r\n")
	private static class Xero{
		@BindString(size = "2")
		private String aa;
		@BindString(size = "3")
		private String ab;
		@BindString(size = "4")
		private String c;
	}

	@TemplateHeader(start = "m1", end = "\r\n")
	private static class Un{
		@BindString(size = "2")
		private String aa;
		@BindString(size = "5")
		private String bb;
		@BindString(size = "4")
		private String c;
	}

	@TemplateHeader(start = "m2", end = "\r\n")
	private static class Do{
		@BindString(size = "2")
		private String aa;
		@BindString(size = "5")
		private String bb;
		@BindString(size = "6")
		private String cc;
	}


	@Test
	void testRandom() throws BoxonException{
		Core core = CoreBuilder.builder()
			.withTemplate(Xero.class)
			.withTemplate(Un.class)
			.withTemplate(Do.class)
			.withDefaultCodecs()
			.create();
		TemplateSpecies[] species = extractTemplateGenome(core);

		Collection<Collection<String>> clusters = KMedoids.cluster(species, 2, 1);

		Assertions.assertEquals(2, clusters.size());
		for(final Collection<String> value : clusters)
			Assertions.assertFalse(value.isEmpty());
	}

	@Test
	void testEnumerated() throws BoxonException{
		Core core = CoreBuilder.builder()
			.withTemplate(Xero.class)
			.withTemplate(Un.class)
			.withTemplate(Do.class)
			.withDefaultCodecs()
			.create();
		TemplateSpecies[] species = extractTemplateGenome(core);

		Collection<Collection<String>> assignments = KMedoids.cluster(species, 2, 3);

		Assertions.assertEquals(2, assignments.size());
		for(final Collection<String> value : assignments)
			Assertions.assertFalse(value.isEmpty());
	}

	private static TemplateSpecies[] extractTemplateGenome(final Core core) throws BoxonException{
		final Describer descriptor = Describer.create(core);
		final List<Map<String, Object>> descriptions = descriptor.describeTemplate();
		final TemplateSpecies[] species = new TemplateSpecies[descriptions.size()];
		for(int s = 0; s < species.length; s ++){
			final Map<String, Object> description = descriptions.get(s);
			final List<Map<String, Object>> parameters = new ArrayList<>((Collection<Map<String, Object>>)description.get(DescriberKey.FIELDS.toString()));
			final String[] genome = new String[parameters.size()];
			for(int g = 0; g < parameters.size(); g ++){
				final Map<String, Object> parameter = new HashMap<>(parameters.get(g));
				parameter.remove(DescriberKey.FIELD_NAME.toString());
				parameter.remove("condition");
				parameter.remove("validator");
				genome[g] = parameter.toString();
			}
			species[s] = TemplateSpecies.create((String)description.get(DescriberKey.TEMPLATE.toString()), StringArrayDistanceData.of(genome));
		}
		return species;
	}

	private static int sameNumber(final int[] array){
		int count = 0;
		for(int i = 0; i < array.length - 1; i ++)
			for(int j = i + 1; j < array.length; j ++)
				if(array[i] == array[j])
					count ++;
		return count;
	}

}