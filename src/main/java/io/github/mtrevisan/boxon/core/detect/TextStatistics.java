/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.mtrevisan.boxon.core.detect;


/**
 * Utility class for computing a histogram of the bytes seen in a stream.
 *
 * @since	Apache Tika 1.2
 *
 * @see <a href="https://github.com/apache/tika/">Apache Tika</a>.
 */
@SuppressWarnings("unused")
public final class TextStatistics{

	private static final int BYTE_MASK = 0xFF;
	private static final int ASCII_START = 0x20;
	private static final int ASCII_END = 0x80;
	private static final int TAB_CHARACTER = '\t';
	private static final int LINE_FEED = '\n';
	private static final int CARRIAGE_RETURN = '\r';
	private static final int NEW_PAGE_CHARACTER = 0x0C;
	private static final int ESCAPE_CHARACTER = 0x1B;

	private final int[] counts = new int[1 << Byte.SIZE];

	/** Total number of bytes seen so far. */
	private int total;


	/**
	 * Creates a new instance using the given byte array buffer.
	 *
	 * @param buffer	The byte array buffer.
	 * @return	The created TextStatistics instance.
	 */
	public static TextStatistics create(final byte[] buffer){
		return new TextStatistics(buffer, 0, buffer.length);
	}

	/**
	 * Creates a new instance using the given byte array buffer, offset, and length values.
	 *
	 * @param buffer	The byte array buffer.
	 * @param offset	The offset in the byte array.
	 * @param length	The length of the bytes to be considered.
	 * @return	The created TextStatistics instance.
	 */
	public static TextStatistics create(final byte[] buffer, final int offset, final int length){
		return new TextStatistics(buffer, offset, length);
	}


	private TextStatistics(final byte[] buffer, final int offset, final int length){
		for(int i = 0; i < length; i ++){
			counts[buffer[offset + i] & BYTE_MASK] ++;
			total ++;
		}
	}


	/**
	 * Checks whether at least one byte was seen and that the bytes that were seen were mostly plain text (i.e. < 2% control, > 90% ASCII
	 * range).
	 *
	 * @return	Whether the seen bytes were mostly safe ASCII.
	 *
	 * @see <a href="https://issues.apache.org/jira/browse/TIKA-483">TIKA-483</a>
	 * @see <a href="https://issues.apache.org/jira/browse/TIKA-688">TIKA-688</a>
	 */
	public boolean isMostlyAscii(){
		final int control = count(0, ASCII_START);
		final int ascii = count(ASCII_START, ASCII_END);
		final int safe = countSafeControl();
		return (total > 0
			&& (control - safe) * 100 < total << 1
			&& (ascii + safe) * 100 > total * 90);
	}

	/**
	 * Checks whether the observed byte stream looks like UTF-8 encoded text.
	 *
	 * @return	Whether the seen bytes look like UTF-8.
	 *
	 * @since	Apache Tika 1.3
	 */
	public boolean looksLikeUTF8(){
		final int control = count(0, ASCII_START);
		int utf8 = count(ASCII_START, ASCII_END);
		final int safe = countSafeControl();

		int expectedContinuation = 0;
		final int[] leading = {count(0xC0, 0xE0), count(0xE0, 0xF0), count(0xF0, 0xF8)};
		for(int i = 0, length = leading.length; i < length; i ++){
			final int chr = leading[i];

			utf8 += chr;
			expectedContinuation += (i + 1) * chr;
		}

		final int continuation = count(ASCII_END, 0xC0);
		return (utf8 > 0
			&& expectedContinuation - 3 <= continuation && continuation <= expectedContinuation
			&& count(0xF8, 0x100) == 0
			&& (control - safe) * 100 < utf8 << 1);
	}

	/**
	 * Returns the number of occurrences of the given byte.
	 *
	 * @param b	Byte.
	 * @return	Count of the given byte.
	 */
	public int count(final int b){
		return counts[b & BYTE_MASK];
	}

	/**
	 * Counts control characters (i.e. < 0x20, excluding tab, CR, LF, page feed and escape).
	 * <p>
	 * This definition of control characters is based on section 4 of the "Content-Type Processing Model" Internet-draft
	 * (<a href="http://webblaze.cs.berkeley.edu/2009/mime-sniff/mime-sniff.txt">draft-abarth-mime-sniff-01</a>).
	 * <pre>
	 * +-------------------------+
	 * | Binary data byte ranges |
	 * +-------------------------+
	 * | 0x00 -- 0x08            |
	 * | 0x0B                    |
	 * | 0x0E -- 0x1A            |
	 * | 0x1C -- 0x1F            |
	 * +-------------------------+
	 * </pre>
	 *
	 * @return	Count of control characters.
	 *
	 * @see <a href="https://issues.apache.org/jira/browse/TIKA-154">TIKA-154</a>
	 */
	public int countControl(){
		return (count(0, ASCII_START) - countSafeControl());
	}

	/**
	 * Counts "safe" (i.e. seven-bit non-control) ASCII characters.
	 *
	 * @return	Count of safe ASCII characters.
	 * @see #countControl()
	 */
	public int countSafeAscii(){
		return (count(ASCII_START, ASCII_END) + countSafeControl());
	}

	/**
	 * Counts eight bit characters, i.e. bytes with their highest bit set.
	 *
	 * @return	Count of eight bit characters.
	 */
	public int countEightBit(){
		return count(ASCII_END, 256);
	}

	private int count(final int from, final int to){
		assert 0 <= from && to <= counts.length;

		int count = 0;
		for(int i = from; i < to; i ++)
			count += counts[i];
		return count;
	}

	private int countSafeControl(){
		return count(TAB_CHARACTER) + count(LINE_FEED) + count(CARRIAGE_RETURN)
			+ count(NEW_PAGE_CHARACTER) + count(ESCAPE_CHARACTER);
	}

}
