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

import java.util.Arrays;
import java.util.Objects;


/**
 * An immutable representation of a semantic version number.
 *
 * @see <a href="https://semver.org/">Semantic Versioning</a>
 */
@SuppressWarnings("unused")
public class Version implements Comparable<Version>{

	/** An empty {@code String} array. */
	static final String[] EMPTY_STRING_ARRAY = new String[0];

	/** An empty instance (see {@link #isEmpty()}). */
	public static final Version EMPTY = createEmpty();

	static final char DOT = '.';
	private static final String DOT_AS_STRING = String.valueOf(DOT);
	/** A separator that separates the pre-release version from the normal version. */
	static final String PRE_RELEASE_PREFIX = "-";
	/** A separator that separates the build metadata from the normal version or the pre-release version. */
	static final String BUILD_PREFIX = "+";


	private final Integer major;
	private final Integer minor;
	private final Integer patch;
	private final String[] preRelease;
	private final String[] build;


	static Version createEmpty(){
		return new Version();
	}

	static Version create(final Integer major, final Integer minor, final Integer patch, final String[] preRelease, final String[] build){
		return new Version(major, minor, patch, preRelease, build);
	}


	private Version(){
		major = null;
		minor = null;
		patch = null;
		preRelease = EMPTY_STRING_ARRAY;
		build = EMPTY_STRING_ARRAY;
	}

	private Version(final Integer major, final Integer minor, final Integer patch, final String[] preRelease, final String[] build){
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.preRelease = preRelease;
		this.build = build;
	}


	/**
	 * Major parameter.
	 *
	 * @return	Major parameter.
	 */
	public Integer getMajor(){
		return major;
	}

	/**
	 * Minor parameter.
	 *
	 * @return	Minor parameter.
	 */
	public Integer getMinor(){
		return minor;
	}

	/**
	 * Patch parameter.
	 *
	 * @return	Patch parameter.
	 */
	public Integer getPatch(){
		return patch;
	}

	/**
	 * Pre-release parameter.
	 *
	 * @return	Pre-release parameter.
	 */
	public String[] getPreRelease(){
		return preRelease.clone();
	}

	/**
	 * Build parameter.
	 *
	 * @return	Build parameter.
	 */
	public String[] getBuild(){
		return build.clone();
	}


	/**
	 * Increments the major version number by a given amount, clearing all other values.
	 * <p>
	 * When the major version number is incremented, the minor and patch version numbers are reset to {@code 0}, and
	 * the pre-release and build metadata are cleared.
	 * </p>
	 *
	 * @param amount	The amount to increment by.
	 * @return	The new instance.
	 * @throws VersionException	If the resulting version number is not valid.
	 */
	public Version incrementMajor(final int amount) throws VersionException{
		return VersionBuilder.of(JavaHelper.nonNullOrDefault(major, 0) + amount);
	}

	/**
	 * Increments the minor version number by a given amount, clearing lesser values.
	 * <p>
	 * When the minor version number is incremented, the patch version number is reset to {@code 0}, and the pre-release
	 * and build metadata are cleared.
	 * </p>
	 *
	 * @param amount	The amount to increment by.
	 * @return	The new instance.
	 * @throws VersionException	If the resulting version number is not valid.
	 */
	public Version incrementMinor(final int amount) throws VersionException{
		if(major == null)
			throw new IllegalArgumentException("Cannot increment minor part of an invalid version: " + this);

		return VersionBuilder.of(major, JavaHelper.nonNullOrDefault(minor, 0) + amount);
	}

	/**
	 * Increments the patch version number by a given amount, clearing all metadata.
	 * <p>
	 * When the patch version number is incremented, the pre-release and build metadata are cleared.
	 * </p>
	 *
	 * @param amount	The amount to increment by.
	 * @return	The new instance.
	 * @throws VersionException	If the resulting version number is not valid.
	 */
	public Version incrementPatch(final int amount) throws VersionException{
		if(major == null || minor == null)
			throw new IllegalArgumentException("Cannot increment patch part of an invalid version: " + this);

		return VersionBuilder.of(major, minor, JavaHelper.nonNullOrDefault(patch, 0) + amount);
	}

	/**
	 * Sets the pre-release metadata.
	 *
	 * @param preRelease	The pre-release metadata.
	 * @return	The new instance.
	 * @throws VersionException	If the pre-release metadata is not valid.
	 */
	public Version setPreRelease(final String... preRelease) throws VersionException{
		if(major == null || minor == null || patch == null)
			throw new IllegalArgumentException("Cannot set release part of an invalid version: " + this);

		return VersionBuilder.of(major, minor, patch, preRelease, build);
	}

	/**
	 * Sets the build metadata.
	 *
	 * @param build	The build metadata.
	 * @return	The new instance.
	 * @throws VersionException	If the build metadata is not valid.
	 */
	public Version setBuild(final String... build) throws VersionException{
		if(major == null || minor == null || patch == null)
			throw new IllegalArgumentException("Cannot set build part of an invalid version: " + this);

		return VersionBuilder.of(major, minor, patch, preRelease, build);
	}


	/**
	 * Checks if the version number is a pre-release version number.
	 *
	 * @return	Whether the version is pre-release.
	 */
	public boolean isPreRelease(){
		return (major != null && (major == 0 || preRelease.length > 0));
	}

	/**
	 * Checks if the version number is a stable version number.
	 *
	 * @return	Whether the version is stable.
	 */
	public boolean isStable(){
		return (major != null && (major > 0 && preRelease.length == 0));
	}

	/**
	 * Checks if this version is API-compatible with another version, that is, the major identifier is the same.
	 *
	 * @param other	The other version to compare to.
	 * @return	Whether this version is API-compatible with the other version.
	 */
	public boolean isCompatibleWith(final Version other){
		return (major != null && major.equals(other.major));
	}

	/**
	 * Checks if this version has added functionalities with respect to another version, that is, the major identifier is the same and
	 * the minor identifier is greater.
	 *
	 * @param other	The other version to compare to.
	 * @return	Whether this version is an enhancement of the other version.
	 */
	public boolean isEnhancementOf(final Version other){
		return (isCompatibleWith(other)
			&& notNullAndGreaterThan(minor, other.minor));
	}

	/**
	 * Checks if this version is a patch of the another version, that is, the major and minor identifiers are the same and
	 * the patch identifier is greater.
	 *
	 * @param other	The other version to compare to.
	 * @return	Whether this version is an enhancement of the other version.
	 */
	public boolean isPatchOf(final Version other){
		return (isCompatibleWith(other)
			&& notNullAndEqualsTo(minor, other.minor)
			&& notNullAndGreaterThan(patch, other.patch));
	}

	private static boolean notNullAndEqualsTo(final Integer component1, final Integer component2){
		return (notNull(component1, component2) && component1.intValue() == component2.intValue());
	}

	private static boolean notNullAndGreaterThan(final Integer component1, final Integer component2){
		return (notNull(component1, component2) && component1 > component2);
	}

	private static boolean notNull(final Integer component1, final Integer component2){
		return (component1 != null && component2 != null);
	}


	/**
	 * Checks if this version is greater than the other version.
	 *
	 * @param other	The other version to compare to.
	 * @return	Whether this version is greater than the other version.
	 * @see #compareTo(Version other)
	 */
	public boolean isGreaterThan(final Version other){
		return (compareTo(other) > 0);
	}

	/**
	 * Checks if this version is greater than or equal to the other version.
	 *
	 * @param other	The other version to compare to.
	 * @return	Whether this version is greater than or equal to the other version.
	 * @see #compareTo(Version other)
	 */
	public boolean isGreaterThanOrEqualTo(final Version other){
		return (compareTo(other) >= 0);
	}

	/**
	 * Checks if this version is less than the other version.
	 *
	 * @param other	The other version to compare to.
	 * @return	Whether this version is less than the other version.
	 * @see #compareTo(Version other)
	 */
	public boolean isLessThan(final Version other){
		return (compareTo(other) < 0);
	}

	/**
	 * Checks if this version is less than or equal to the other version.
	 *
	 * @param other	The other version to compare to.
	 * @return	Whether this version is less than or equal to the other version.
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
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final Version rhs = (Version)obj;
		return (Objects.equals(major, rhs.major)
			&& Objects.equals(minor, rhs.minor)
			&& Objects.equals(patch, rhs.patch)
			&& Arrays.equals(preRelease, rhs.preRelease)
			&& Arrays.equals(build, rhs.build));
	}

	@Override
	public int hashCode(){
		int result = Objects.hashCode(major);
		result = 31 * result + Objects.hashCode(minor);
		result = 31 * result + Objects.hashCode(patch);
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
	public int compareToWithBuilds(final Version other){
		int result = compareTo(other);
		if(result == 0)
			result = compareToIdentifiers(preRelease, other.preRelease);
		return (result != 0? result: compareToIdentifiers(build, other.build));
	}

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
		return (value1 == null
			? (value2 == null? 0: -1)
			: (value2 == null? 1: value1.compareTo(value2))
		);
	}

	private static int compareToIdentifiers(final String[] preRelease, final String[] otherPreRelease){
		final int result = compareIdentifierArrays(preRelease, otherPreRelease);
		//a larger set of pre-release fields has a higher precedence than a smaller set, if all the preceding identifiers are equal
		return (result != 0? result: preRelease.length - otherPreRelease.length);
	}

	private static int compareIdentifierArrays(final String[] preRelease, final String[] otherPreRelease){
		int result = (otherPreRelease.length - preRelease.length);
		for(int i = 0, length = getLeastCommonArrayLength(preRelease, otherPreRelease); i < length; i ++){
			result = compareIdentifiers(preRelease[i], otherPreRelease[i]);
			if(result != 0)
				break;
		}
		return result;
	}

	private static int getLeastCommonArrayLength(final String[] array1, final String[] array2){
		return Math.min(array1.length, array2.length);
	}

	private static int compareIdentifiers(final String identifier1, final String identifier2){
		return (JavaHelper.isDecimalIntegerNumber(identifier1) && JavaHelper.isDecimalIntegerNumber(identifier2)
			? Integer.parseInt(identifier1) - Integer.parseInt(identifier2)
			: identifier1.compareTo(identifier2));
	}


	@Override
	public String toString(){
		if(major == null)
			return null;

		final StringBuilder sb = new StringBuilder();
		sb.append(major);
		if(minor != null)
			sb.append(DOT)
				.append(minor);
		if(patch != null)
			sb.append(DOT)
				.append(patch);
		if(preRelease != null && preRelease.length > 0)
			sb.append(PRE_RELEASE_PREFIX)
				.append(String.join(DOT_AS_STRING, preRelease));
		if(build != null && build.length > 0)
			sb.append(BUILD_PREFIX)
				.append(String.join(DOT_AS_STRING, build));
		return sb.toString();
	}

}
