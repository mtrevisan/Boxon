/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.queclink;

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.BooleanType;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSkip;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;


@SuppressWarnings("ALL")
@ConfigurationHeader(start = "AT+", end = "$", shortDescription = "AT+GTREG",
	longDescription = "The command AT+GTREG is used to do things.", maxProtocol = "2.8")
public class REGConfigurationASCII{

	@ConfigurationField(shortDescription = "Header", terminator = "=", defaultValue = "GTREG")
	private String messageHeader;

	@ConfigurationField(shortDescription = "Password", terminator = ",", pattern = "[0-9a-zA-Z]{4,20}", defaultValue = "gb200s")
	private String password;

	@ConfigurationField(shortDescription = "Operation mode", terminator = ",", minValue = "0", maxValue = "3", defaultValue = "0")
	private Integer operationMode;

	@ConfigurationField(shortDescription = "Update Over-The-Air", terminator = ",", enumeration = BooleanType.class,
		defaultValue = "FALSE")
	private BooleanType updateOverTheAir;

	@ConfigurationField(shortDescription = "Update mode", terminator = ",", minValue = "0", maxValue = "1", defaultValue = "1")
	private int updateMode;

	@ConfigurationField(shortDescription = "Maximum download retry count", terminator = ",", maxProtocol = "1.20", minValue = "0",
		maxValue = "3", defaultValue = "0")
	private Integer maxDownloadRetryCount1_20;
	@ConfigurationField(shortDescription = "Maximum download retry count", terminator = ",", minProtocol = "1.21", minValue = "0",
		maxValue = "3", defaultValue = "1")
	private Integer maxDownloadRetryCount1_21;

	@AlternativeConfigurationField(
		shortDescription = "Download timeout", terminator = ",", unitOfMeasure = "min",
		value = {
			@AlternativeSubField(maxProtocol = "1.18", minValue = "5", maxValue = "30", defaultValue = "10"),
			@AlternativeSubField(minProtocol = "1.19", minValue = "5", maxValue = "30", defaultValue = "20")
		}
	)
	private int downloadTimeout;

	@AlternativeConfigurationField(
		shortDescription = "Download protocol", terminator = ",", enumeration = DownloadProtocol.class,
		value = {
			@AlternativeSubField(maxProtocol = "1.35", defaultValue = "HTTP"),
			@AlternativeSubField(minProtocol = "1.36", defaultValue = "HTTP")
		}
	)
	private DownloadProtocol downloadProtocol;

	@CompositeConfigurationField(
		value = {
			@CompositeSubField(shortDescription = "URL", pattern = "https?://.{0,92}"),
			@CompositeSubField(shortDescription = "username", pattern = ".{1,32}"),
			@CompositeSubField(shortDescription = "password", pattern = ".{1,32}")
		},
		shortDescription = "Download URL",
		composition = "${URL}<#if username?? && password??>@${username}@${password}</#if>",
		terminator = ",",
		pattern = ".{0,100}"
	)
	private String downloadURL;

	@ConfigurationSkip(terminator = ",", maxProtocol = "1.18")
	@ConfigurationField(shortDescription = "Motion report interval", terminator = ",", minProtocol = "1.19", maxProtocol = "1.20",
		minValue = "90", maxValue = "86400", defaultValue = "3600", unitOfMeasure = "s")
	private int motionReportInterval;
	@ConfigurationSkip(terminator = ",", minProtocol = "1.21")

	@ConfigurationSkip(terminator = ",", maxProtocol = "1.18")
	@ConfigurationField(shortDescription = "Motionless report interval", terminator = ",", minProtocol = "1.19", maxProtocol = "1.20",
		minValue = "90", maxValue = "86400", defaultValue = "3600", unitOfMeasure = "s")
	private int motionlessReportInterval;
	@ConfigurationField(shortDescription = "Operation mode report interval", terminator = ",", minProtocol = "1.21", minValue = "3600",
		maxValue = "86400", defaultValue = "3600", unitOfMeasure = "s")
	private int operationModeReportInterval;

	@ConfigurationField(shortDescription = "Weekday", terminator = ",", enumeration = Weekday.class, radix = 16,
		defaultValue = "MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY")
	private Weekday[] weekday;

	@ConfigurationSkip(terminator = ",")
	@ConfigurationField(shortDescription = "Message counter", minValue = "0x0000", maxValue = "0xFFFF", radix = 16)
	private Integer messageCounter;

}
