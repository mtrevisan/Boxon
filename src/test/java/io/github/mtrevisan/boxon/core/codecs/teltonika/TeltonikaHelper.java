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
package io.github.mtrevisan.boxon.core.codecs.teltonika;

import io.github.mtrevisan.boxon.annotations.bindings.BindAsArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


final class TeltonikaHelper{

	private TeltonikaHelper(){}


	public static final class PreambleValidator implements Validator<Integer>{
		@Override
		public boolean isValid(final Integer preamble){
			return (preamble == 0);
		}
	}


	public static final class UnixTimestampConverter implements Converter<Long, LocalDateTime>{
		@Override
		public LocalDateTime decode(final Long value){
			return LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC);
		}

		@Override
		public Long encode(final LocalDateTime value){
			return value.toInstant(ZoneOffset.UTC)
				.toEpochMilli();
		}
	}


	public static final class CoordinateConverter implements Converter<Integer, BigDecimal>{

		private static final BigDecimal MULTIPLICAND = BigDecimal.valueOf(10000000);

		@Override
		public BigDecimal decode(final Integer value){
			return BigDecimal.valueOf(value)
				.divide(MULTIPLICAND, RoundingMode.HALF_UP);
		}

		@Override
		public Integer encode(final BigDecimal value){
			return value.multiply(MULTIPLICAND)
				.intValue();
		}
	}


	static class OneByteProperty{
		@BindInteger(size = "(codecID == -114 || codecID == 0x10? 16: 8)")
		private int key;
		@BindInteger(size = "8")
		private int value;
	}

	static class TwoBytesProperty{
		@BindInteger(size = "(codecID == -114 || codecID == 0x10? 16: 8)")
		private int key;
		@BindInteger(size = "16")
		private int value;
	}

	static class FourBytesProperty{
		@BindInteger(size = "(codecID == -114 || codecID == 0x10? 16: 8)")
		private int key;
		@BindInteger(size = "32")
		private int value;
	}

	static class EightBytesProperty{
		@BindInteger(size = "(codecID == -114 || codecID == 0x10? 16: 8)")
		private int key;
		@BindInteger(size = "64")
		private long value;
	}

	static class VariableBytesProperty{
		@BindInteger(size = "16")
		private int length;
		@BindInteger(size = "8")
		@BindAsArray(size = "#self.length")
		private byte[] value;
	}

}
