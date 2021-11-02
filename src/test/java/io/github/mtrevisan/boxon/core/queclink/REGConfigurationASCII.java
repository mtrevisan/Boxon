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

import io.github.mtrevisan.boxon.annotations.configurations.BooleanType;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationMessage;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSkip;


@SuppressWarnings("ALL")
@ConfigurationMessage(start = "AT+", end = "$", shortDescription = "AT+GTREG",
	longDescription = "The command AT+GTREG is used to do things.", maxProtocol = "2.8")
public class REGConfigurationASCII{

	@ConfigurationField(shortDescription = "Header", terminator = "=", defaultValue = "GTREG", writable = false)
	private String messageHeader;

	@ConfigurationField(shortDescription = "Password", terminator = ",", format = "[0-9a-zA-Z]{4,20}", defaultValue = "gb200s")
	private String password;

	@ConfigurationField(shortDescription = "Operation mode", terminator = ",", minValue = "0", maxValue = "3", defaultValue = "0")
	private int operationMode;

	@ConfigurationField(shortDescription = "Update Over-The-Air", terminator = ",", enumeration = BooleanType.class,
		defaultValue = "FALSE")
	private BooleanType updateOverTheAir;

	@ConfigurationField(shortDescription = "Update mode", terminator = ",", minValue = "0", maxValue = "1", defaultValue = "1")
	private int updateMode;

	@ConfigurationField(shortDescription = "Maximum download retry count", terminator = ",", maxProtocol = "1.20", minValue = "0",
		maxValue = "3", defaultValue = "0")
	private int maxDownloadRetryCount1_20;
	@ConfigurationField(shortDescription = "Maximum download retry count", terminator = ",", minProtocol = "1.21", minValue = "0",
		maxValue = "3", defaultValue = "1")
	private int maxDownloadRetryCount1_21;

	@ConfigurationField(shortDescription = "Download timeout", terminator = ",", maxProtocol = "1.18", minValue = "5", maxValue = "30",
		defaultValue = "10", unitOfMeasure = "min")
	private int downloadTimeout1_18;
	@ConfigurationField(shortDescription = "Download timeout", terminator = ",", minProtocol = "1.19", minValue = "5", maxValue = "30",
		defaultValue = "20", unitOfMeasure = "min")
	private int downloadTimeout1_19;

	@ConfigurationField(shortDescription = "Download protocol", terminator = ",", maxProtocol = "1.35", enumeration = DownloadProtocol.class,
		defaultValue = "HTTP", writable = false)
	private DownloadProtocol downloadProtocol1_35;
	@ConfigurationField(shortDescription = "Download protocol", terminator = ",", minProtocol = "1.36", enumeration = DownloadProtocol.class,
		defaultValue = "HTTP")
	private DownloadProtocol downloadProtocol;

	//NOTE: The length of downloadURL + downloadURLUsername + downloadURLPassword cannot exceed 100 characters
	@ConfigurationField(shortDescription = "Download URL", terminator = ",", format = "https?://.{0,92}")
	//NOTE: When HTTPS is used, place username and passwords (both have maximum length of 32), with a `@` as a separator, AFTER the URL, i.e. `https://test.server.comgb200s.enc@username@password`
	private String downloadURL;
//	@ConfigurationField(shortDescription = "Download URL username", prefix = "@", format = ".{1,32}")
//	private String downloadURLUsername;
//	@ConfigurationField(shortDescription = "Download URL password", prefix = "@", format = ".{1,32}")
//	private String downloadURLPassword;

	@ConfigurationSkip(terminator = ',', maxProtocol = "1.18")
	@ConfigurationField(shortDescription = "Motion report interval", terminator = ",", minProtocol = "1.19", maxProtocol = "1.20",
		minValue = "90", maxValue = "86400", defaultValue = "3600", unitOfMeasure = "s")
	private int motionReportInterval;
	@ConfigurationSkip(terminator = ',', minProtocol = "1.21")

	@ConfigurationSkip(terminator = ',', maxProtocol = "1.18")
	@ConfigurationField(shortDescription = "Motionless report interval", terminator = ",", minProtocol = "1.19", maxProtocol = "1.20",
		minValue = "90", maxValue = "86400", defaultValue = "3600", unitOfMeasure = "s")
	private int motionlessReportInterval;
	@ConfigurationField(shortDescription = "Operation mode report interval", terminator = ",", minProtocol = "1.21", minValue = "3600",
		maxValue = "86400", defaultValue = "3600", unitOfMeasure = "s")
	private int operationModeReportInterval;

	@ConfigurationField(shortDescription = "Weekday", terminator = ",", enumeration = Weekday.class, radix = 16,
		defaultValue = "MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY")
	private Weekday[] weekday;

	@ConfigurationField(shortDescription = "Message counter", minValue = "0x0000", maxValue = "0xFFFF", mandatory = true, radix = 16)
	private int messageCounter;

}
