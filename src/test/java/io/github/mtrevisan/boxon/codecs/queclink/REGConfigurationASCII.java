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
package io.github.mtrevisan.boxon.codecs.queclink;

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationMessage;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSkip;


@ConfigurationMessage(start = "AT+", end = "$", shortDescription = "AT+GTREG", longDescription = "The command AT+GTREG is used to register the network.")
public class REGConfigurationASCII{

	@ConfigurationField(shortDescription = "Header", longDescription = "", terminator = '=', defaultValue = "GTREG", writable = false)
	private String messageHeader;

	@ConfigurationField(terminator = ',', format = "[0-9a-zA-Z]{4,20}", defaultValue = "gb200s")
	private String password;

	@ConfigurationField(terminator = ',', format = "[0-3]", defaultValue = "0")
	private int operationMode;

	@ConfigurationField(terminator = ',', format = "[0-1]", defaultValue = "0")
	private int updateOverTheAir;

	@ConfigurationField(terminator = ',', format = "[0-1]", defaultValue = "1")
	private int updateMode;

	@ConfigurationField(terminator = ',', maxProtocol = "1.20", format = "[0-3]", defaultValue = "0")
	private int maxDownloadRetryCount1_20;
	@ConfigurationField(terminator = ',', minProtocol = "1.21", format = "[0-3]", defaultValue = "1")
	private int maxDownloadRetryCount1_21;

	@ConfigurationField(terminator = ',', maxProtocol = "1.18", minValue = "5", maxValue = "30", defaultValue = "10", unitOfMeasure = "min")
	private int downloadTimeout1_18;
	@ConfigurationField(terminator = ',', minProtocol = "1.19", minValue = "5", maxValue = "30", defaultValue = "20", unitOfMeasure = "min")
	private int downloadTimeout1_19;

	@ConfigurationField(terminator = ',', maxProtocol = "1.35", defaultValue = "0", writable = false)
	private int downloadProtocol1_35;
	@ConfigurationField(terminator = ',', minProtocol = "1.36", format = "0|2", defaultValue = "0")
	private int downloadProtocol;

	@ConfigurationField(terminator = ',', format = ".{0,100}")
	private String downloadURL;

	@ConfigurationSkip(terminator = ',', maxProtocol = "1.18")
	@ConfigurationField(terminator = ',', minProtocol = "1.19", maxProtocol = "1.20", minValue = "90", maxValue = "86400", defaultValue = "3600", unitOfMeasure = "s")
	private int motionReportInterval;
	@ConfigurationSkip(terminator = ',', minProtocol = "1.21")

	@ConfigurationSkip(terminator = ',', maxProtocol = "1.18")
	@ConfigurationField(terminator = ',', minProtocol = "1.19", maxProtocol = "1.20", minValue = "90", maxValue = "86400", defaultValue = "3600", unitOfMeasure = "s")
	private int motionlessReportInterval;
	@ConfigurationField(terminator = ',', minProtocol = "1.21", minValue = "3600", maxValue = "86400", defaultValue = "3600", unitOfMeasure = "s")
	private int operationModeReportInterval;

	@ConfigurationField(terminator = ',', minValue = "0x0000", maxValue = "0xFFFF", writable = false)
	private int messageCounter;

}
