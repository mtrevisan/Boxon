/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.codecs.queclink;

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSkip;
import io.github.mtrevisan.boxon.annotations.configurations.BooleanType;


@ConfigurationHeader(longDescription = "The command AT+GTREG is used to do things.", shortDescription = "AT+GTREG", maxProtocol = "2.8",
	start = "AT+", end = "$")
public class REGConfigurationASCII{

	@ConfigurationField(shortDescription = "Header", defaultValue = "GTREG", terminator = "=")
	private String messageHeader;

	@ConfigurationField(shortDescription = "Password", pattern = "[0-9a-zA-Z]{4,20}", defaultValue = "gb200s", terminator = ",")
	private String password;

	@ConfigurationField(shortDescription = "Operation mode", minValue = "0", maxValue = "3", defaultValue = "0", terminator = ",")
	private Integer operationMode;

	@ConfigurationField(shortDescription = "Update Over-The-Air", enumeration = BooleanType.class, defaultValue = "FALSE", terminator = ",")
	private BooleanType updateOverTheAir;

	@ConfigurationField(shortDescription = "Update mode", minValue = "0", maxValue = "1", defaultValue = "1", terminator = ",")
	private int updateMode;

	@AlternativeConfigurationField(
		shortDescription = "Maximum download retry count",
		value = {
			@AlternativeSubField(maxProtocol = "1.20", minValue = "0", maxValue = "3", defaultValue = "0"),
			@AlternativeSubField(minProtocol = "1.21", minValue = "0", maxValue = "3", defaultValue = "1")
		},
		terminator = ","
	)
	private int maxDownloadRetryCount;

	@AlternativeConfigurationField(
		shortDescription = "Download timeout", unitOfMeasure = "min",
		value = {
			@AlternativeSubField(maxProtocol = "1.18", minValue = "5", maxValue = "30", defaultValue = "10"),
			@AlternativeSubField(minProtocol = "1.19", minValue = "5", maxValue = "30", defaultValue = "20")
		},
		terminator = ","
	)
	private int downloadTimeout;

	@AlternativeConfigurationField(
		shortDescription = "Download protocol", enumeration = DownloadProtocol.class,
		value = {
			@AlternativeSubField(maxProtocol = "1.35", defaultValue = "HTTP"),
			@AlternativeSubField(minProtocol = "1.36", defaultValue = "HTTP")
		},
		terminator = ","
	)
	private DownloadProtocol downloadProtocol;

	@CompositeConfigurationField(
		value = {
			@CompositeSubField(shortDescription = "URL", pattern = "https?://.{0,92}"),
			@CompositeSubField(shortDescription = "username", pattern = ".{1,32}"),
			@CompositeSubField(shortDescription = "password", pattern = ".{1,32}")
		},
		shortDescription = "Download URL",
		composition = "${URL}<#if username?has_content && password?has_content>@${username}@${password}</#if>",
		pattern = ".{0,100}",
		terminator = ","
	)
	private String downloadURL;

	@ConfigurationSkip(maxProtocol = "1.18", terminator = ",")
	@ConfigurationField(shortDescription = "Motion report interval", unitOfMeasure = "s", minProtocol = "1.19", maxProtocol = "1.20",
		minValue = "90", maxValue = "86400", defaultValue = "3600", terminator = ",")
	private int motionReportInterval;
	@ConfigurationSkip(minProtocol = "1.21", terminator = ",")

	@ConfigurationSkip(maxProtocol = "1.18", terminator = ",")
	@ConfigurationField(shortDescription = "Motionless report interval", unitOfMeasure = "s", minProtocol = "1.19", maxProtocol = "1.20",
		minValue = "90", maxValue = "86400", defaultValue = "3600", terminator = ",")
	private int motionlessReportInterval;
	@ConfigurationField(shortDescription = "Operation mode report interval", unitOfMeasure = "s", minProtocol = "1.21", minValue = "3600",
		maxValue = "86400", defaultValue = "3600", terminator = ",")
	private int operationModeReportInterval;

	@ConfigurationField(shortDescription = "Weekday", enumeration = Weekday.class, radix = 16,
		defaultValue = "MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY", terminator = ",")
	private Weekday[] weekday;

	@ConfigurationSkip(terminator = ",")
	@ConfigurationField(shortDescription = "Message counter", minValue = "0x0000", maxValue = "0xFFFF", radix = 16)
	private Integer messageCounter;

}
