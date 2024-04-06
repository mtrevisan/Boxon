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
package io.github.mtrevisan.boxon.core.helpers.configurations.validators;

import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.semanticversioning.Version;
import io.github.mtrevisan.boxon.semanticversioning.VersionException;


/**
 * A collection of convenience methods for working with validations.
 */
final class ProtocolValidator{

	private ProtocolValidator(){}


	/**
	 * Validate the protocol.
	 *
	 * @param minProtocolVersion The minimum protocol version (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @param maxProtocolVersion The maximum protocol version (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @param configData         The configuration field data.
	 * @throws AnnotationException If a validation error occurs.
	 */
	static void validateProtocol(final Version minProtocolVersion, final Version maxProtocolVersion, final ConfigFieldData configData)
			throws AnnotationException{
		final Version minimum = validateProtocol(configData.getMinProtocol(), configData,
			"Invalid minimum protocol version in {}; found {}");
		final Version maximum = validateProtocol(configData.getMaxProtocol(), configData,
			"Invalid maximum protocol version in {}; found {}");

		//`maxProtocol` must be after or equal to `minProtocol`
		validateMaxLessThanMin(minimum, maximum, configData);

		//`minProtocol` must be after or equal to `minProtocolVersion`
		validateMinLessThanMinProtocol(minimum, minProtocolVersion, configData);
		//`maxProtocol` must be before or equal to `maxProtocolVersion`
		validateMaxProtocolLessThanMax(maximum, maxProtocolVersion, configData);
	}

	private static Version validateProtocol(final String protocolVersion, final ConfigFieldData configData, final String errorMessage)
			throws AnnotationException{
		Version protocol = null;
		if(!StringHelper.isBlank(protocolVersion)){
			try{
				protocol = Version.of(protocolVersion);
			}
			catch(final VersionException ve){
				throw AnnotationException.create(ve, errorMessage, configData.getAnnotationName(), protocolVersion);
			}
		}
		return protocol;
	}

	private static void validateMaxLessThanMin(final Version minimum, final Version maximum, final ConfigFieldData configData)
			throws AnnotationException{
		if(minimum != null && maximum != null && maximum.isLessThan(minimum))
			throw AnnotationException.create("Minimum protocol version is greater than maximum protocol version in {}; found {} >= {}",
				configData.getAnnotationName(), configData.getMinProtocol(), configData.getMaxProtocol());
	}

	private static void validateMinLessThanMinProtocol(final Version minimum, final Version minProtocolVersion,
			final ConfigFieldData configData) throws AnnotationException{
		//NOTE: `minimum.isLessThan(minProtocolVersion)` return false if `minProtocolVersion` is empty
		if(minimum != null && minimum.isLessThan(minProtocolVersion))
			throw AnnotationException.create("Minimum protocol version is less than whole message minimum protocol version in {}; expected {} >= {}",
				configData.getAnnotationName(), minimum, minProtocolVersion);
	}

	private static void validateMaxProtocolLessThanMax(final Version maximum, final Version maxProtocolVersion,
			final ConfigFieldData configData) throws AnnotationException{
		if(maximum != null && !maxProtocolVersion.isEmpty() && maxProtocolVersion.isLessThan(maximum))
			throw AnnotationException.create("Maximum protocol version is greater than whole message maximum protocol version in {}; expected {} <= {}",
				configData.getAnnotationName(), maximum, maxProtocolVersion);
	}

}
