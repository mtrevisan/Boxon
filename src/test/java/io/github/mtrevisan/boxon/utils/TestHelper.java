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
package io.github.mtrevisan.boxon.utils;

import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.core.similarity.distances.StringArrayDistanceData;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public final class TestHelper{

	public static final Random RANDOM = new Random(System.currentTimeMillis());


	private TestHelper(){}


	/**
	 * Checks whether the given {@code mask} has the bit at {@code index} set.
	 *
	 * @param mask	The value to check the bit into.
	 * @param index	The index of the bit (rightmost is zero). The value can range between {@code 0} and {@link Byte#SIZE}.
	 * @return	The state of the bit at a given index in the given byte.
	 */
	public static boolean hasBit(final byte mask, final int index){
		final int bitMask = 1 << (index % Byte.SIZE);
		return ((mask & bitMask) != 0);
	}

	public static byte[] toByteArray(final String payload){
		return payload.getBytes(StandardCharsets.ISO_8859_1);
	}


	public static StringArrayDistanceData extractTemplateGenome(final Map<String, Object> description){
		final List<Map<String, Object>> parameters = new ArrayList<>((Collection<Map<String, Object>>)description
			.get(DescriberKey.FIELDS.toString()));
		final String[] genome = new String[parameters.size()];
		int gene = 0;
		for(int g = 0, length = genome.length; g < length; g ++){
			final Map<String, Object> parameter = new LinkedHashMap<>(parameters.get(g));
			parameter.remove(DescriberKey.FIELD_NAME.toString());
			parameter.remove("condition");
			parameter.remove("validator");

			if(parameter.containsKey(DescriberKey.COLLECTION_TYPE.toString()))
				genome[gene] += parameter.toString();
			else
				genome[gene ++] = parameter.toString();
		}
		return StringArrayDistanceData.of(Arrays.copyOfRange(genome, 0, gene));
	}

}
