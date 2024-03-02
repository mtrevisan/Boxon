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
package io.github.mtrevisan.boxon.core.similarity.kmedoids;

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.core.Core;
import io.github.mtrevisan.boxon.core.CoreBuilder;
import io.github.mtrevisan.boxon.core.Descriptor;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.core.similarity.evolutionarytree.TemplateSpecies;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


class KMedoidsTest{

	@MessageHeader(start = "m0", end = "\r\n")
	private static class Xero{
		@BindString(size = "2")
		private String aa;
		@BindString(size = "3")
		private String ab;
		@BindString(size = "4")
		private String c;
	}

	@MessageHeader(start = "m1", end = "\r\n")
	private static class Un{
		@BindString(size = "2")
		private String aa;
		@BindString(size = "5")
		private String bb;
		@BindString(size = "4")
		private String c;
	}

	@MessageHeader(start = "m2", end = "\r\n")
	private static class Do{
		@BindString(size = "2")
		private String aa;
		@BindString(size = "5")
		private String bb;
		@BindString(size = "6")
		private String cc;
	}


	@Test
	void test() throws TemplateException, ConfigurationException, AnnotationException{
		Core core = CoreBuilder.builder()
			.withTemplatesFrom(KMedoidsTest.class)
			.withDefaultCodecs()
			.create();
		TemplateSpecies[] species = extractTemplateGenome(core);

		int[] assignments = KMedoids.cluster(species, 2, 10);

		Assertions.assertEquals(3, assignments.length);
		Set<Integer> numbers = new HashSet<>(3);
		for(int i = 0; i < assignments.length; i ++)
			numbers.add(assignments[i]);
		Assertions.assertEquals(2, numbers.size());
		Iterator<Integer> itr = numbers.iterator();
		int num1 = itr.next();
		int num2 = itr.next();
		Assertions.assertTrue(0 <= num1 && num1 <= 2);
		Assertions.assertTrue(0 <= num2 && num2 <= 2);
	}

	private static TemplateSpecies[] extractTemplateGenome(final Core core) throws TemplateException{
		final Descriptor descriptor = Descriptor.create(core);
		final List<Map<String, Object>> descriptions = descriptor.describe();
		final TemplateSpecies[] species = new TemplateSpecies[descriptions.size()];
		for(int s = 0; s < species.length; s ++){
			final Map<String, Object> description = descriptions.get(s);
			final List<Map<String, Object>> parameters = (List<Map<String, Object>>)description.get(DescriberKey.FIELDS.toString());
			final String[] genome = new String[parameters.size()];
			for(int g = 0; g < parameters.size(); g ++){
				final Map<String, Object> parameter = parameters.get(g);
				parameter.remove(DescriberKey.FIELD_NAME.toString());
				parameter.remove(DescriberKey.BIND_CONDITION.toString());
				parameter.remove(DescriberKey.BIND_VALIDATOR.toString());
				genome[g] = parameter.toString();
			}
			species[s] = TemplateSpecies.create((String)description.get(DescriberKey.TEMPLATE.toString()), genome);
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