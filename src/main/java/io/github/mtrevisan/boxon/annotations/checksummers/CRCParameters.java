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
package io.github.mtrevisan.boxon.annotations.checksummers;


public final class CRCParameters{

	/** CRC-7 parameters (x^7 + x^3 + 1) */
	public static final CRCParameters CRC7 = create(7, 0x09, 0x00, false, false, 0x00);

	/** CRC-8 CCITT parameters (x^8 + x^2 + x + 1) */
	public static final CRCParameters CRC8_CCITT = create(8, 0x07, 0x00, false, false, 0x00);
	/** CRC-8 Dallas/Maxim parameters (x^8 + x^5 + x^4 + 1) */
	public static final CRCParameters CRC8_DALLAS_MAXIM = create(8, 0x31, 0x00, true, true, 0x00);

	/** CRC-16 CCITT XMODEM parameters (x^16 + x^12 + x^5 + 1) */
	public static final CRCParameters CRC16_CCITT_XMODEM = create(16, 0x1021, 0x0000, false, false, 0x0000);
	/** CRC-16 CCITT FALSE parameters (x^16 + x^12 + x^5 + 1) */
	public static final CRCParameters CRC16_CCITT_FALSE = create(16, 0x1021, (short)0xFFFF, false, false, 0x0000);
	/** CRC-16 parameters, also known as CRC-16 IBM, CRC-16 ARC, and CRC-16 ANSI (x^16 + x^15 + x^2 + 1) */
	public static final CRCParameters CRC16 = create(16, 0x8005, 0x0000, true, true, 0x0000);

	/** CRC-32 CCITT FALSE parameters (x^32 + x^26 + x^23 + x^22 + x^16 + x^12 + x^11 + x^10 + x^8 + x^7 + x^5 + x^4 + x^2 + x + 1) */
	public static final CRCParameters CRC32 = create(32, 0x04C1_1DB7, (int)0xFFFF_FFFFl, true, true, (int)0xFFFF_FFFFl);


	/** Size of the CRC (Cyclic Redundancy Check) in bits. */
	final int width;
	/** Polynomial used in the checksum calculation. */
	final long polynomial;
	/** Initial value used in the checksum calculation. */
	long initialValue;
	/** Whether the input data should be reflected (bitwise reversed) before performing a checksum calculation. */
	boolean reflectInput;
	/** Whether the output should be reflected (bitwise reversed) after computation. */
	boolean reflectOutput;
	/** Value for final xor to be applied before returning result. */
	long xorOutputValue;


	private static CRCParameters create(final int width, final long polynomial, final long initialValue, final boolean reflectInput,
			final boolean reflectOutput, final long xorOutputValue){
		return new CRCParameters(width, polynomial, initialValue, reflectInput, reflectOutput, xorOutputValue);
	}

	/**
	 * Creates a new instance with the specified width and polynomial.
	 *
	 * @param width	The bit width of the CRC computation.
	 * @param polynomial	The polynomial used for the CRC computation.
	 * @return	A new instance configured with the specified width and polynomial.
	 */
	public static CRCParameters create(final int width, final long polynomial){
		return new CRCParameters(width, polynomial);
	}


	private CRCParameters(final int width, final long polynomial, final long initialValue, final boolean reflectInput,
			final boolean reflectOutput, final long xorOutputValue){
		this(width, polynomial);

		this.initialValue = initialValue;
		this.reflectInput = reflectInput;
		this.reflectOutput = reflectOutput;
		this.xorOutputValue = xorOutputValue;
	}

	private CRCParameters(final int width, final long polynomial){
		this.width = width;
		this.polynomial = polynomial;
	}

	/**
	 * Sets the initial value for the CRC computation.
	 *
	 * @param initialValue	The initial value to be used during the CRC computation.
	 * @return	The current instance, updated with the specified initial value.
	 */
	public CRCParameters withInitialValue(final long initialValue){
		this.initialValue = initialValue;

		return this;
	}

	/**
	 * Enables reflection of the input data during the CRC computation.
	 *
	 * @return	The current instance, updated to reflect the input data.
	 */
	public CRCParameters withReflectInput(){
		reflectInput = true;

		return this;
	}

	/**
	 * Enables reflection of the output data during the CRC computation.
	 *
	 * @return	The current instance, updated to reflect the output data.
	 */
	public CRCParameters withReflectOutput(){
		reflectOutput = true;

		return this;
	}

	/**
	 * Sets the XOR output value to be used during the CRC computation.
	 *
	 * @param xorOutputValue	The XOR output value to be applied to the final CRC result.
	 * @return	The current instance, updated with the specified XOR output value.
	 */
	public CRCParameters withXorOutputValue(final long xorOutputValue){
		this.xorOutputValue = xorOutputValue;

		return this;
	}

}
