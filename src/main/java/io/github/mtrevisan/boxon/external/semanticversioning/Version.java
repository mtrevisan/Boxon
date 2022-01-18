/*
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.external.semanticversioning;

import io.github.mtrevisan.boxon.external.codecs.ParserDataType;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;


/**
 * @see <a href="https://semver.org/">Semantic Versioning 2.0.0</a>
 */
@SuppressWarnings("WeakerAccess")
public final class Version implements Comparable<Version>{

	private static final String EMPTY_STRING = "";
	/** An empty immutable {@code String} array. */
	private static final String[] EMPTY_ARRAY = new String[0];

	/** An empty instance (see {@link #isEmpty()}). */
	public static final Version EMPTY = of("");

	private static final String DOT = ".";
	/** A separator that separates the pre-release version from the normal version. */
	private static final String PRE_RELEASE_PREFIX = "-";
	/** A separator that separates the build metadata from the normal version or the pre-release version. */
	private static final String BUILD_PREFIX = "+";

	private static final Pattern PATTERN_DOT = Pattern.compile("\\.");
	private static final Pattern PATTERN_DOT_PREFIX = Pattern.compile("[" + DOT + BUILD_PREFIX + PRE_RELEASE_PREFIX + "]");
	//split and keep delimiters
	private static final Pattern PATTERN_PREFIX = Pattern.compile("((?=[" + BUILD_PREFIX + PRE_RELEASE_PREFIX + "])|(?<=[" + BUILD_PREFIX + PRE_RELEASE_PREFIX + "]))");


	private final Integer major;
	private final Integer minor;
	private final Integer patch;
	private final String[] preRelease;
	private final String[] build;


	/**
	 * Creates a new instance of {@code Version} as a result of parsing the specified version string.
	 *
	 * @param version	The string representation of the version.
	 * @return	An instance of this class.
	 */
	public static Version of(final String version){
		return new Version(version);
	}

	/**
	 * Constructs a {@code Version} with the major, minor and patch version numbers.
	 *
	 * @param major	The major version number
	 * @param minor	The minor version number
	 * @param patch	The patch version number
	 * @return	An instance of this class.
	 * @throws IllegalArgumentException	If one of the version numbers is a negative integer
	 */
	public static Version of(final int major, final int minor, final int patch){
		return new Version(major, minor, patch, EMPTY_ARRAY, EMPTY_ARRAY);
	}

	/**
	 * Constructs a {@code Version} with the major, minor and patch version numbers.
	 *
	 * @param major	The major version number
	 * @param minor	The minor version number
	 * @param patch	The patch version number
	 * @param preRelease	The pre-release identifiers
	 * @return	An instance of this class.
	 * @throws IllegalArgumentException	If one of the version numbers is a negative integer
	 */
	public static Version of(final int major, final int minor, final int patch, final String[] preRelease){
		return new Version(major, minor, patch, preRelease, EMPTY_ARRAY);
	}

	/**
	 * Constructs a {@code Version} with the major, minor and patch version numbers.
	 *
	 * @param major	The major version number
	 * @param minor	The minor version number
	 * @param patch	The patch version number
	 * @param preRelease	The pre-release identifiers
	 * @param build	The build identifiers
	 * @return	An instance of this class.
	 * @throws IllegalArgumentException	If one of the version numbers is a negative integer
	 */
	public static Version of(final int major, final int minor, final int patch, final String[] preRelease, final String[] build){
		return new Version(major, minor, patch, preRelease, build);
	}

	private Version(final int major, final int minor, final int patch, final String[] preRelease, final String[] build){
		if(major < 0 || minor < 0 || patch < 0)
			throw new IllegalArgumentException("Major, minor and patch versions MUST be non-negative integers.");

		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.preRelease = (preRelease != null? preRelease: EMPTY_ARRAY);
		this.build = (build != null? build: EMPTY_ARRAY);
	}

	private Version(String version){
		if(StringHelper.isBlank(version)){
			major = null;
			minor = null;
			patch = null;
			preRelease = EMPTY_ARRAY;
			build = EMPTY_ARRAY;
			return;
		}

		version = version.trim();
		if(!startsWithNumber(version))
			throw new IllegalArgumentException("Argument is not a valid version");

		final String[] tokens = PATTERN_DOT.split(version, 3);
		validateValues(version, tokens);

		major = Integer.valueOf(tokens[0]);
		minor = (tokens.length > 1? Integer.valueOf(tokens[1]): null);
		if(tokens.length > 2){
			final String[] patchPreReleaseBuild = PATTERN_PREFIX.split(tokens[2]);

			patch = Integer.valueOf(patchPreReleaseBuild[0]);

			int offset = 1;
			String nextToken = (patchPreReleaseBuild.length > offset? patchPreReleaseBuild[offset]: null);
			if(PRE_RELEASE_PREFIX.equals(nextToken) && patchPreReleaseBuild.length > ++ offset){
				preRelease = PATTERN_DOT.split(patchPreReleaseBuild[offset ++]);

				validatePreRelease();

				nextToken = (patchPreReleaseBuild.length > offset? patchPreReleaseBuild[offset]: null);
			}
			else
				preRelease = EMPTY_ARRAY;

			if(BUILD_PREFIX.equals(nextToken) && patchPreReleaseBuild.length > ++ offset){
				build = PATTERN_DOT.split(patchPreReleaseBuild[offset ++]);

				validateBuild();
			}
			else
				build = EMPTY_ARRAY;

			if(patchPreReleaseBuild.length > offset + 1)
				throw new IllegalArgumentException("Argument is not a valid version");
		}
		else{
			patch = null;
			preRelease = EMPTY_ARRAY;
			build = EMPTY_ARRAY;
		}
	}

	private static void validateValues(final CharSequence version, final String[] tokens){
		final String[] tokensWithPatch = PATTERN_DOT_PREFIX.split(version);
		if(hasLeadingZeros(tokens[0])
				|| tokensWithPatch.length > 1 && hasLeadingZeros(tokens[1])
				|| tokensWithPatch.length > 2 && hasLeadingZeros(tokensWithPatch[2]))
			throw new IllegalArgumentException("Numeric identifier MUST NOT contain leading zeros");
	}

	private void validatePreRelease(){
		for(int i = 0; i < preRelease.length; i ++){
			final String pr = preRelease[i];
			final boolean numeric = ParserDataType.isDecimalNumber(pr);
			if(numeric && pr.charAt(0) == '0')
				throw new IllegalArgumentException("Numeric identifier MUST NOT contain leading zeros");
			if(!numeric && !containsOnlyValidChars(pr))
				throw new IllegalArgumentException("Argument is not a valid version");
		}
	}

	private void validateBuild(){
		for(int i = 0; i < build.length; i ++){
			final String b = build[i];
			if(!ParserDataType.isDecimalNumber(b) && !containsOnlyValidChars(b))
				throw new IllegalArgumentException("Argument is not a valid version");
		}
	}

	/**
	 * Checks if this version is greater than the other version.
	 *
	 * @param other	The other version to compare to
	 * @return	{@code true} if this version is greater than the other version
	 * @see #compareTo(Version other)
	 */
	public boolean isGreaterThan(final Version other){
		return (compareTo(other) > 0);
	}

	/**
	 * Checks if this version is greater than or equal to the other version.
	 *
	 * @param other	The other version to compare to
	 * @return	{@code true} if this version is greater than or equal to the other version
	 * @see #compareTo(Version other)
	 */
	public boolean isGreaterThanOrEqualTo(final Version other){
		return (compareTo(other) >= 0);
	}

	/**
	 * Checks if this version is less than the other version.
	 *
	 * @param other	The other version to compare to
	 * @return	{@code true} if this version is less than the other version
	 * @see #compareTo(Version other)
	 */
	public boolean isLessThan(final Version other){
		return (compareTo(other) < 0);
	}

	/**
	 * Checks if this version is less than or equal to the other version.
	 *
	 * @param other	The other version to compare to
	 * @return	{@code true} if this version is less than or equal to the other version
	 * @see #compareTo(Version other)
	 */
	public boolean isLessThanOrEqualTo(final Version other){
		return (compareTo(other) <= 0);
	}

	/**
	 * Whether this instance represents a void version.
	 *
	 * @return	Whether this instance was generated by an empty string.
	 */
	public boolean isEmpty(){
		return (major == null);
	}

	@Override
	@SuppressWarnings({"AccessingNonPublicFieldOfAnotherObject", "OverlyComplexBooleanExpression"})
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final Version rhs = (Version)obj;
		return (major.equals(rhs.major)
			&& minor.equals(rhs.minor)
			&& patch.equals(rhs.patch)
			&& Arrays.equals(preRelease, rhs.preRelease)
			&& Arrays.equals(build, rhs.build));
	}

	@Override
	public int hashCode(){
		int result = (major != null? Integer.hashCode(major): 0);
		if(minor != null)
			result = 31 * result + Integer.hashCode(minor);
		if(patch != null)
			result = 31 * result + patch.hashCode();
		result = 31 * result + Arrays.hashCode(preRelease);
		result = 31 * result + Arrays.hashCode(build);
		return result;
	}

	/**
	 * Compares two {@code Version} instances.
	 * <p>
	 * This method doesn't take into account the versions' build metadata. If you want to compare the versions' build metadata
	 * use the {@code Version.compareToWithBuilds} method.</p>
	 *
	 * @param other	The object to be compared.
	 * @return	A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
	 * @see #compareToWithBuilds(Version other)
	 */
	@Override
	@SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
	public int compareTo(final Version other){
		final int result = compareToCore(other);
		return (result != 0? result: compareToIdentifiers(preRelease, other.preRelease));
	}

	/**
	 * Compares two {@code Version} instances taking into account their build metadata.
	 * <p>
	 * When compared build metadata is divided into identifiers. The numeric identifiers are compared numerically, and the alphanumeric
	 * identifiers are compared in the ASCII sort order.</p>
	 * <p>
	 * If one of the compared versions has no defined build metadata, this version is considered to have a lower
	 * precedence than that of the other.</p>
	 *
	 * @param other	The object to be compared.
	 * @return	A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
	 */
	@SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
	public int compareToWithBuilds(final Version other){
		int result = compareTo(other);
		if(result == 0)
			result = compareToIdentifiers(preRelease, other.preRelease);
		return (result != 0? result: compareToIdentifiers(build, other.build));
	}

	@SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
	private int compareToCore(final Version other){
		int comparison = compareToCore(major, other.major);
		if(comparison != 0)
			return comparison;

		comparison = compareToCore(minor, other.minor);
		if(comparison != 0)
			return comparison;

		return compareToCore(patch, other.patch);
	}

	private static int compareToCore(final Integer value1, final Integer value2){
		int comparison = 0;
		if(value1 != null || value2 != null){
			if(value1 != null && value2 == null)
				return 1;
			if(value1 == null)
				return -1;

			comparison = Integer.compare(value1, value2);
		}
		return comparison;
	}

	private static int compareToIdentifiers(final String[] preRelease, final String[] otherPreRelease){
		final int result = compareIdentifierArrays(preRelease, otherPreRelease);
		//a larger set of pre-release fields has a higher precedence than a smaller set, if all the preceding identifiers are equal
		return (result != 0? result: preRelease.length - otherPreRelease.length);
	}

	private static int compareIdentifierArrays(final String[] preRelease, final String[] otherPreRelease){
		int result = (otherPreRelease.length - preRelease.length);
		for(int i = 0; i < getLeastCommonArrayLength(preRelease, otherPreRelease); i ++){
			result = compareIdentifiers(preRelease[i], otherPreRelease[i]);
			if(result != 0)
				break;
		}
		return result;
	}

	private static int compareIdentifiers(final String identifier1, final String identifier2){
		return (ParserDataType.isDecimalNumber(identifier1) && ParserDataType.isDecimalNumber(identifier2)
			? Integer.parseInt(identifier1) - Integer.parseInt(identifier2)
			: identifier1.compareTo(identifier2));
	}


	/**
	 * <p>Checks if the text contains only certain characters.</p>
	 *
	 * <p>A {@code null} text will return {@code false}.
	 * An empty String ({@code length() = 0}) always returns {@code true}.</p>
	 *
	 * <pre>
	 * containsOnly(null, *)       = false
	 * containsOnly(*, null)       = false
	 * containsOnly("", *)         = true
	 * containsOnly("ab", "")      = false
	 * containsOnly("abab", "abc") = true
	 * containsOnly("ab1", "abc")  = false
	 * containsOnly("abz", "abc")  = false
	 * </pre>
	 *
	 * @param text	The text to check, may be {@code null}.
	 * @return	Whether if the given text only contains valid chars and is non-{@code null}.
	 */
	private static boolean containsOnlyValidChars(String text){
		text = text.toUpperCase(Locale.ROOT);
		for(int i = 0; i < text.length(); i ++){
			final char chr = text.charAt(i);
			if(chr != '-' && (chr < 'A' || chr > 'Z'))
				return false;
		}
		return true;
	}


	private static boolean hasLeadingZeros(final CharSequence token){
		return (token.length() > 1 && token.charAt(0) == '0');
	}

	private static boolean startsWithNumber(final String text){
		return (text != null && !text.isEmpty() && Character.isDigit(text.charAt(0)));
	}

	private static int getLeastCommonArrayLength(final String[] array1, final String[] array2){
		return Math.min(array1.length, array2.length);
	}

	@Override
	public String toString(){
		String message = EMPTY_STRING;
		if(major != null)
			message += major;
		if(minor != null)
			message += DOT + minor;
		if(patch != null)
			message += DOT + patch;
		if(preRelease != null && preRelease.length > 0)
			message += PRE_RELEASE_PREFIX + String.join(DOT, preRelease);
		if(build != null && build.length > 0)
			message += BUILD_PREFIX + String.join(DOT, build);
		return message;
	}

}
