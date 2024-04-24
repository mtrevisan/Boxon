/*
 * Copyright (c) 2019-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.semanticversioning;

import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.nio.charset.StandardCharsets;
import java.util.Objects;


/**
 * This class is responsible for constructing instances of the {@link Version} class.
 * <p>
 * It provides several overloaded static "of" methods that create a new `Version` instance based on different combinations of version
 * numbers, pre-release identifiers, and build identifiers.</p>
 */
public final class VersionBuilder{

	private static final String KEY_MAJOR = "major";
	private static final String KEY_MINOR = "minor";
	private static final String KEY_PATCH = "patch";


	/**
	 * Creates a new instance of {@code Version} as a result of parsing the specified version string.
	 *
	 * @param version	The string representation of the version.
	 * @return	An instance of this class.
	 * @throws VersionException	If the given version is not a valid semver version.
	 */
	public static Version of(final String version) throws VersionException{
		return createVersion(version);
	}

	/**
	 * Constructs a {@code Version} with the major, minor and patch version numbers.
	 *
	 * @param major	The major version number.
	 * @return	An instance of this class.
	 * @throws VersionException	If one of the version numbers is a negative integer
	 */
	public static Version of(final int major) throws VersionException{
		return createVersion(major, null, null, Version.EMPTY_STRING_ARRAY, Version.EMPTY_STRING_ARRAY);
	}

	/**
	 * Constructs a {@code Version} with the major, minor and patch version numbers.
	 *
	 * @param major	The major version number.
	 * @param minor	The minor version number.
	 * @return	An instance of this class.
	 * @throws VersionException	If the given version is not valid.
	 */
	public static Version of(final int major, final int minor) throws VersionException{
		return createVersion(major, minor, null, Version.EMPTY_STRING_ARRAY, Version.EMPTY_STRING_ARRAY);
	}

	/**
	 * Constructs a {@code Version} with the major, minor and patch version numbers.
	 *
	 * @param major	The major version number.
	 * @param minor	The minor version number.
	 * @param patch	The patch version number.
	 * @return	An instance of this class.
	 * @throws VersionException	If the given version is not valid.
	 */
	public static Version of(final int major, final int minor, final int patch) throws VersionException{
		return createVersion(major, minor, patch, Version.EMPTY_STRING_ARRAY, Version.EMPTY_STRING_ARRAY);
	}

	/**
	 * Constructs a {@code Version} with the major, minor and patch version numbers.
	 *
	 * @param major	The major version number.
	 * @param minor	The minor version number.
	 * @param patch	The patch version number.
	 * @param preRelease	The pre-release identifiers.
	 * @return	An instance of this class.
	 * @throws VersionException	If the given version is not valid.
	 */
	public static Version of(final int major, final int minor, final int patch, final String[] preRelease) throws VersionException{
		Objects.requireNonNull(preRelease, "Pre-release identifier cannot be null");

		return createVersion(major, minor, patch, preRelease, Version.EMPTY_STRING_ARRAY);
	}

	/**
	 * Constructs a {@code Version} with the major, minor and patch version numbers.
	 *
	 * @param major	The major version number.
	 * @param minor	The minor version number.
	 * @param patch	The patch version number.
	 * @param preRelease	The pre-release identifiers.
	 * @param build	The build identifiers.
	 * @return	An instance of this class.
	 * @throws VersionException	If the given version is not valid.
	 */
	public static Version of(final int major, final int minor, final int patch, final String[] preRelease, final String[] build)
			throws VersionException{
		Objects.requireNonNull(preRelease, "Pre-release identifier cannot be null");
		Objects.requireNonNull(build, "Build identifier cannot be null");

		return createVersion(major, minor, patch, preRelease, build);
	}


	private VersionBuilder(){}


	private static Version createVersion(final int major, final Integer minor, final Integer patch, final String[] preRelease,
			final String[] build) throws VersionException{
		validateToken(KEY_MAJOR, String.valueOf(major));
		if(minor != null)
			validateToken(KEY_MINOR, minor.toString());
		if(patch != null)
			validateToken(KEY_PATCH, patch.toString());
		if(preRelease != null)
			validatePreRelease(preRelease);
		if(build != null)
			validateBuild(build);

		return Version.create(major, minor, patch, preRelease, build);
	}

	private static Version createVersion(String version) throws VersionException{
		if(StringHelper.isBlank(version))
			return Version.createEmpty();

		version = version.trim();

		final int buildIndex = version.indexOf(Version.BUILD_PREFIX);
		String[] build = Version.EMPTY_STRING_ARRAY;
		if(buildIndex >= 0){
			build = StringHelper.split(version, buildIndex + 1, Version.DOT);
			version = version.substring(0, buildIndex);

			validateBuild(build);
		}

		final int preReleaseIndex = version.indexOf(Version.PRE_RELEASE_PREFIX);
		String[] preRelease = Version.EMPTY_STRING_ARRAY;
		if(preReleaseIndex >= 0){
			preRelease = StringHelper.split(version, preReleaseIndex + 1, Version.DOT);
			version = version.substring(0, preReleaseIndex);

			validatePreRelease(preRelease);
		}

		final String[] tokens = StringHelper.split(version, Version.DOT);
		final Integer major = parseIdentifier(tokens, 0, KEY_MAJOR);
		final Integer minor = parseIdentifier(tokens, 1, KEY_MINOR);
		final Integer patch = parseIdentifier(tokens, 2, KEY_PATCH);

		return Version.create(major, minor, patch, preRelease, build);
	}


	private static Integer parseIdentifier(final String[] tokens, final int index, final String type) throws VersionException{
		Integer value = null;
		if(tokens.length > index){
			final String token = tokens[index];

			validateToken(type, token);
			value = Integer.valueOf(token);
		}
		return value;
	}

	private static void validateToken(final String type, final String token) throws VersionException{
		try{
			final int number = Integer.parseInt(token);
			if(number < 0)
				throw VersionException.create("The {} identifier MUST be a non-negative integer", type);
		}
		catch(final NumberFormatException nfe){
			throw VersionException.create(nfe, "The {} identifier `{}` could not be parsed as an integer", type, token);
		}
	}

	private static void validatePreRelease(final String[] preRelease) throws VersionException{
		for(int i = 0, length = preRelease.length; i < length; i ++)
			validatePreRelease(preRelease[i]);
	}

	private static void validatePreRelease(final String preRelease){
		final boolean numeric = JavaHelper.isDecimalIntegerNumber(preRelease);
		if(numeric && preRelease.charAt(0) == '0')
			throw VersionException.create("The pre-release identifier MUST NOT contain leading zeros");
		if(!numeric && !containsOnlyValidChars(preRelease))
			throw VersionException.create("Argument is not a valid pre-release identifier");
	}

	private static void validateBuild(final String[] build) throws VersionException{
		for(int i = 0, length = build.length; i < length; i ++)
			validateBuild(build[i]);
	}

	private static void validateBuild(final String build){
		if(!JavaHelper.isDecimalIntegerNumber(build) && !containsOnlyValidChars(build))
			throw VersionException.create("Argument is not a valid build identifier");
	}

	/**
	 * <p>Checks if the text contains only alphabetic characters or dash.</p>
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
	private static boolean containsOnlyValidChars(final String text){
		final int length = JavaHelper.sizeOrZero(text);
		if(length == 0)
			return true;

		final byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
		for(int i = 0; i < length; i ++){
			final byte chr = bytes[i];

			if(chr != '-' && !isAlphabetCharacter(chr))
				return false;
		}
		return true;
	}

	private static boolean isAlphabetCharacter(final byte chr){
		return (chr >= 'a' && chr <= 'z'
			|| chr >= 'A' && chr <= 'Z');
	}

}
