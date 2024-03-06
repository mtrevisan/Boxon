package io.github.mtrevisan.boxon.helpers;


/**
 * Utility class for computing a histogram of the bytes seen in a stream.
 *
 * @since Apache Tika 1.2
 *
 * @see <a href="https://github.com/apache/tika/">Apache Tika</a>.
 */
public class TextStatistics{

	private final int[] counts = new int[256];

	private int total = 0;


	public void addData(final byte[] buffer, final int offset, final int length){
		for(int i = 0; i < length; i ++){
			counts[buffer[offset + i] & 0xff] ++;
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
		final int control = count(0, 0x20);
		final int ascii = count(0x20, 128);
		final int safe = countSafeControl();
		return (total > 0
			&& (control - safe) * 100 < total * 2
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
		final int control = count(0, 0x20);
		int utf8 = count(0x20, 0x80);
		final int safe = countSafeControl();

		int expectedContinuation = 0;
		final int[] leading = new int[]{count(0xC0, 0xE0), count(0xE0, 0xF0), count(0xF0, 0xF8)};
		for(int i = 0; i < leading.length; i ++){
			utf8 += leading[i];
			expectedContinuation += (i + 1) * leading[i];
		}

		final int continuation = count(0x80, 0xC0);
		return (utf8 > 0
			&& continuation <= expectedContinuation
			&& continuation >= expectedContinuation - 3
			&& count(0xF8, 0x100) == 0
			&& (control - safe) * 100 < utf8 * 2);
	}

	/**
	 * Returns the total number of bytes seen so far.
	 *
	 * @return	Count of all bytes.
	 */
	public int count(){
		return total;
	}

	/**
	 * Returns the number of occurrences of the given byte.
	 *
	 * @param b	Byte.
	 * @return	Count of the given byte.
	 */
	public int count(final int b){
		return counts[b & 0xFF];
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
		return (count(0, 0x20) - countSafeControl());
	}

	/**
	 * Counts "safe" (i.e. seven-bit non-control) ASCII characters.
	 *
	 * @return	Count of safe ASCII characters.
	 * @see #countControl()
	 */
	public int countSafeAscii(){
		return (count(0x20, 128) + countSafeControl());
	}

	/**
	 * Counts eight bit characters, i.e. bytes with their highest bit set.
	 *
	 * @return	Count of eight bit characters.
	 */
	public int countEightBit(){
		return count(128, 256);
	}

	private int count(final int from, final int to){
		assert 0 <= from && to <= counts.length;

		int count = 0;
		for(int i = from; i < to; i ++)
			count += counts[i];
		return count;
	}

	private int countSafeControl(){
			// tab, LF, CR
		return count('\t') + count('\n') + count('\r')
			//new page, escape
			+ count(0x0c) + count(0x1b);
	}

}
