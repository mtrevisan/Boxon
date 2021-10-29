package io.github.mtrevisan.boxon.internal.semanticversioning;

import java.util.ArrayList;
import java.util.List;


public class StringUtils{

	/** Represents a failed index search. */
	public static final int INDEX_NOT_FOUND = -1;

	/** An empty immutable {@code String} array. */
	public static final String[] EMPTY_STRING_ARRAY = new String[0];


	private StringUtils(){}

	/**
	 * <p>Checks if a CharSequence is empty (""), null or whitespace only.</p>
	 *
	 * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
	 *
	 * <pre>
	 * StringUtils.isBlank(null)      = true
	 * StringUtils.isBlank("")        = true
	 * StringUtils.isBlank(" ")       = true
	 * StringUtils.isBlank("bob")     = false
	 * StringUtils.isBlank("  bob  ") = false
	 * </pre>
	 *
	 * @param cs the CharSequence to check, may be null
	 * @return {@code true} if the CharSequence is null, empty or whitespace only
	 * @since 2.0
	 * @since 3.0 Changed signature from isBlank(String) to isBlank(CharSequence)
	 */
	public static boolean isBlank(final CharSequence cs){
		final int strLen = length(cs);
		for(int i = 0; i < strLen; i ++)
			if(!Character.isWhitespace(cs.charAt(i)))
				return false;
		return true;
	}

	/**
	 * Gets a CharSequence length or {@code 0} if the CharSequence is
	 * {@code null}.
	 *
	 * @param cs a CharSequence or {@code null}
	 * @return CharSequence length or {@code 0} if the CharSequence is
	 * {@code null}.
	 * @since 2.4
	 * @since 3.0 Changed signature from length(String) to length(CharSequence)
	 */
	public static int length(final CharSequence cs){
		return (cs == null? 0: cs.length());
	}

	/**
	 * <p>Splits the provided text into an array, separators specified.
	 * This is an alternative to using StringTokenizer.</p>
	 *
	 * <p>The separator is not included in the returned String array.
	 * Adjacent separators are treated as one separator.
	 * For more control over the split use the StrTokenizer class.</p>
	 *
	 * <p>A {@code null} input String returns {@code null}.
	 * A {@code null} separatorChars splits on whitespace.</p>
	 *
	 * <pre>
	 * StringUtils.split(null, *)         = null
	 * StringUtils.split("", *)           = []
	 * StringUtils.split("abc def", null) = ["abc", "def"]
	 * StringUtils.split("abc def", " ")  = ["abc", "def"]
	 * StringUtils.split("abc  def", " ") = ["abc", "def"]
	 * StringUtils.split("ab:cd:ef", ":") = ["ab", "cd", "ef"]
	 * </pre>
	 *
	 * @param str            the String to parse, may be null
	 * @param separatorChars the characters used as the delimiters,
	 *                       {@code null} splits on whitespace
	 * @return an array of parsed Strings, {@code null} if null String input
	 */
	public static String[] split(final String str, final String separatorChars){
		return splitWorker(str, separatorChars, -1);
	}

	/**
	 * <p>Splits the provided text into an array with a maximum length,
	 * separators specified.</p>
	 *
	 * <p>The separator is not included in the returned String array.
	 * Adjacent separators are treated as one separator.</p>
	 *
	 * <p>A {@code null} input String returns {@code null}.
	 * A {@code null} separatorChars splits on whitespace.</p>
	 *
	 * <p>If more than {@code max} delimited substrings are found, the last
	 * returned string includes all characters after the first {@code max - 1}
	 * returned strings (including separator characters).</p>
	 *
	 * <pre>
	 * StringUtils.split(null, *, *)            = null
	 * StringUtils.split("", *, *)              = []
	 * StringUtils.split("ab cd ef", null, 0)   = ["ab", "cd", "ef"]
	 * StringUtils.split("ab   cd ef", null, 0) = ["ab", "cd", "ef"]
	 * StringUtils.split("ab:cd:ef", ":", 0)    = ["ab", "cd", "ef"]
	 * StringUtils.split("ab:cd:ef", ":", 2)    = ["ab", "cd:ef"]
	 * </pre>
	 *
	 * @param str            the String to parse, may be null
	 * @param separatorChars the characters used as the delimiters,
	 *                       {@code null} splits on whitespace
	 * @param max            the maximum number of elements to include in the
	 *                       array. A zero or negative value implies no limit
	 * @return an array of parsed Strings, {@code null} if null String input
	 */
	public static String[] split(final String str, final String separatorChars, final int max){
		return splitWorker(str, separatorChars, max);
	}

	/**
	 * Performs the logic for the {@code split} and
	 * {@code splitPreserveAllTokens} methods that return a maximum array
	 * length.
	 *
	 * @param str               the String to parse, may be {@code null}
	 * @param separatorChars    the separate character
	 * @param max               the maximum number of elements to include in the
	 *                          array. A zero or negative value implies no limit.
	 * @return an array of parsed Strings, {@code null} if null String input
	 */
	private static String[] splitWorker(final String str, final String separatorChars, final int max){
		if(str == null)
			return null;

		final int len = str.length();
		if(len == 0)
			return EMPTY_STRING_ARRAY;

		final List<String> list = new ArrayList<>();
		int sizePlus1 = 1;
		int i = 0, start = 0;
		boolean match = false;
		if(separatorChars == null){
			//null separator means use whitespace
			while(i < len){
				if(Character.isWhitespace(str.charAt(i))){
					if(match){
						if(sizePlus1 ++ == max)
							i = len;
						list.add(str.substring(start, i));
						match = false;
					}
					start = ++ i;
					continue;
				}
				match = true;
				i ++;
			}
		}
		else if(separatorChars.length() == 1){
			// Optimise 1 character case
			final char sep = separatorChars.charAt(0);
			while(i < len){
				if(str.charAt(i) == sep){
					if(match){
						if(sizePlus1 ++ == max)
							i = len;
						list.add(str.substring(start, i));
						match = false;
					}
					start = ++ i;
					continue;
				}
				match = true;
				i ++;
			}
		}
		else{
			// standard case
			while(i < len){
				if(separatorChars.indexOf(str.charAt(i)) >= 0){
					if(match){
						if(sizePlus1 ++ == max)
							i = len;
						list.add(str.substring(start, i));
						match = false;
					}
					start = ++ i;
					continue;
				}
				match = true;
				i ++;
			}
		}
		if(match)
			list.add(str.substring(start, i));
		return list.toArray(EMPTY_STRING_ARRAY);
	}

	/**
	 * <p>Checks if the CharSequence contains only Unicode digits.
	 * A decimal point is not a Unicode digit and returns false.</p>
	 *
	 * <p>{@code null} will return {@code false}.
	 * An empty CharSequence (length()=0) will return {@code false}.</p>
	 *
	 * <p>Note that the method does not allow for a leading sign, either positive or negative.
	 * Also, if a String passes the numeric test, it may still generate a NumberFormatException
	 * when parsed by Integer.parseInt or Long.parseLong, e.g. if the value is outside the range
	 * for int or long respectively.</p>
	 *
	 * <pre>
	 * StringUtils.isNumeric(null)   = false
	 * StringUtils.isNumeric("")     = false
	 * StringUtils.isNumeric("  ")   = false
	 * StringUtils.isNumeric("123")  = true
	 * StringUtils.isNumeric("\u0967\u0968\u0969")  = true
	 * StringUtils.isNumeric("12 3") = false
	 * StringUtils.isNumeric("ab2c") = false
	 * StringUtils.isNumeric("12-3") = false
	 * StringUtils.isNumeric("12.3") = false
	 * StringUtils.isNumeric("-123") = false
	 * StringUtils.isNumeric("+123") = false
	 * </pre>
	 *
	 * @param cs the CharSequence to check, may be null
	 * @return {@code true} if only contains digits, and is non-null
	 */
	public static boolean isNumeric(final CharSequence cs){
		if(isEmpty(cs))
			return false;

		final int sz = cs.length();
		for(int i = 0; i < sz; i ++)
			if(!Character.isDigit(cs.charAt(i)))
				return false;
		return true;
	}

	/**
	 * <p>Checks if a CharSequence is empty ("") or null.</p>
	 *
	 * <pre>
	 * StringUtils.isEmpty(null)      = true
	 * StringUtils.isEmpty("")        = true
	 * StringUtils.isEmpty(" ")       = false
	 * StringUtils.isEmpty("bob")     = false
	 * StringUtils.isEmpty("  bob  ") = false
	 * </pre>
	 *
	 * <p>NOTE: This method changed in Lang version 2.0.
	 * It no longer trims the CharSequence.
	 * That functionality is available in isBlank().</p>
	 *
	 * @param cs the CharSequence to check, may be null
	 * @return {@code true} if the CharSequence is empty or null
	 */
	public static boolean isEmpty(final CharSequence cs){
		return (cs == null || cs.length() == 0);
	}

	/**
	 * <p>Checks if the CharSequence contains only certain characters.</p>
	 *
	 * <p>A {@code null} CharSequence will return {@code false}.
	 * A {@code null} valid character String will return {@code false}.
	 * An empty String (length()=0) always returns {@code true}.</p>
	 *
	 * <pre>
	 * StringUtils.containsOnly(null, *)       = false
	 * StringUtils.containsOnly(*, null)       = false
	 * StringUtils.containsOnly("", *)         = true
	 * StringUtils.containsOnly("ab", "")      = false
	 * StringUtils.containsOnly("abab", "abc") = true
	 * StringUtils.containsOnly("ab1", "abc")  = false
	 * StringUtils.containsOnly("abz", "abc")  = false
	 * </pre>
	 *
	 * @param cs         the CharSequence to check, may be null
	 * @param validChars a String of valid chars, may be null
	 * @return true if it only contains valid chars and is non-null
	 * @since 2.0
	 * @since 3.0 Changed signature from containsOnly(String, String) to containsOnly(CharSequence, String)
	 */
	public static boolean containsOnly(final CharSequence cs, final String validChars){
		return (cs != null && validChars != null && containsOnly(cs, validChars.toCharArray()));
	}

	/**
	 * <p>Checks if the CharSequence contains only certain characters.</p>
	 *
	 * <p>A {@code null} CharSequence will return {@code false}.
	 * A {@code null} valid character array will return {@code false}.
	 * An empty CharSequence (length()=0) always returns {@code true}.</p>
	 *
	 * <pre>
	 * StringUtils.containsOnly(null, *)       = false
	 * StringUtils.containsOnly(*, null)       = false
	 * StringUtils.containsOnly("", *)         = true
	 * StringUtils.containsOnly("ab", '')      = false
	 * StringUtils.containsOnly("abab", 'abc') = true
	 * StringUtils.containsOnly("ab1", 'abc')  = false
	 * StringUtils.containsOnly("abz", 'abc')  = false
	 * </pre>
	 *
	 * @param cs    the String to check, may be null
	 * @param valid an array of valid chars, may be null
	 * @return true if it only contains valid chars and is non-null
	 * @since 3.0 Changed signature from containsOnly(String, char[]) to containsOnly(CharSequence, char...)
	 */
	public static boolean containsOnly(final CharSequence cs, final char... valid){
		return (valid != null && valid.length > 0 && cs != null
			&& (cs.length() == 0 || indexOfAnyBut(cs, valid) == INDEX_NOT_FOUND));
	}

	/**
	 * <p>Searches a CharSequence to find the first index of any
	 * character not in the given set of characters.</p>
	 *
	 * <p>A {@code null} CharSequence will return {@code -1}.
	 * A {@code null} or zero length search array will return {@code -1}.</p>
	 *
	 * <pre>
	 * StringUtils.indexOfAnyBut(null, *)                              = -1
	 * StringUtils.indexOfAnyBut("", *)                                = -1
	 * StringUtils.indexOfAnyBut(*, null)                              = -1
	 * StringUtils.indexOfAnyBut(*, [])                                = -1
	 * StringUtils.indexOfAnyBut("zzabyycdxx", new char[] {'z', 'a'} ) = 3
	 * StringUtils.indexOfAnyBut("aba", new char[] {'z'} )             = 0
	 * StringUtils.indexOfAnyBut("aba", new char[] {'a', 'b'} )        = -1
	 * </pre>
	 *
	 * @param cs          the CharSequence to check, may be null
	 * @param searchChars the chars to search for, may be null
	 * @return the index of any of the chars, -1 if no match or null input
	 */
	public static int indexOfAnyBut(final CharSequence cs, final char... searchChars){
		if(isEmpty(cs) || searchChars == null || searchChars.length == 0)
			return INDEX_NOT_FOUND;

		final int length = cs.length();
		final int csLast = length - 1;
		outer:
		for(int i = 0; i < length; i ++){
			final char chr = cs.charAt(i);
			for(int j = 0; j < searchChars.length; j ++)
				if(searchChars[j] == chr && (i >= csLast || j >= searchChars.length - 1 || !Character.isHighSurrogate(chr)
						|| searchChars[j + 1] == cs.charAt(i + 1)))
					continue outer;
			return i;
		}
		return INDEX_NOT_FOUND;
	}

}
