/*
 * Copyright (c) 2020-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.core.codecs.queclink.REGConfigurationASCII;
import io.github.mtrevisan.boxon.core.keys.ConfigurationKey;
import io.github.mtrevisan.boxon.utils.MultithreadingHelper;
import io.github.mtrevisan.boxon.utils.PrettyPrintMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;


class ConfiguratorThreadedTest{

	@Test
	void concurrencySingleParserSingleCore() throws Exception{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withConfiguration(REGConfigurationASCII.class)
			.build();
		Configurator configurator = Configurator.create(core);

		MultithreadingHelper.testMultithreading(configurator::getConfigurations,
			configurations -> {
				Map<String, Object> configuration = configurations.getFirst();
				String jsonHeader = PrettyPrintMap.toString(configuration.get(ConfigurationKey.HEADER.toString()));
				String jsonFields = PrettyPrintMap.toString(configuration.get(ConfigurationKey.FIELDS.toString()));
				String jsonProtocolVersionBoundaries = PrettyPrintMap.toString(configuration.get(
					ConfigurationKey.PROTOCOL_VERSION_BOUNDARIES.toString()));

//				Assertions.assertEquals("{longDescription:The command AT+GTREG is used to do things.,maxProtocol:2.8,shortDescription:AT+GTREG}", jsonHeader);
				Assertions.assertEquals(102, jsonHeader.length());
//				Assertions.assertEquals("{Operation mode report interval:{minValue:3600,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.21,fieldType:int},Maximum download retry count:{alternatives:[{maxProtocol:1.20,minValue:0,fieldType:int,maxValue:3,defaultValue:0},{minValue:0,fieldType:int,maxValue:3,minProtocol:1.21,defaultValue:1}]},Download timeout:{alternatives:[{maxProtocol:1.18,minValue:5,unitOfMeasure:min,fieldType:int,maxValue:30,defaultValue:10},{minValue:5,unitOfMeasure:min,fieldType:int,maxValue:30,minProtocol:1.19,defaultValue:20}]},Random field:{defaultValue:27,fieldType:java.math.BigInteger},Weekday:{defaultValue:[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY],enumeration:[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY],fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.Weekday[]},Update Over-The-Air:{defaultValue:FALSE,mutuallyExclusive:true,enumeration:[TRUE, FALSE],fieldType:io.github.mtrevisan.boxon.annotations.configurations.BooleanType},Header:{charset:UTF-8,defaultValue:GTREG,fieldType:java.lang.String},Download protocol:{alternatives:[{maxProtocol:1.35,enumeration:[HTTP, HTTPS],fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.DownloadProtocol,defaultValue:HTTP,mutuallyExclusive:true},{enumeration:[HTTP, HTTPS],fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.DownloadProtocol,minProtocol:1.36,defaultValue:HTTPS,mutuallyExclusive:true}]},Download URL:{pattern:.{0,100},fields:{URL:{pattern:https?://.{0,92},fieldType:java.lang.String},password:{pattern:.{1,32},fieldType:java.lang.String},username:{pattern:.{1,32},fieldType:java.lang.String}},charset:UTF-8},Update mode:{minValue:0,maxValue:1,defaultValue:1,fieldType:int},Message counter:{charset:UTF-8,pattern:[0-9A-F]{4},fieldType:java.lang.String},Operation mode:{minValue:0,maxValue:3,defaultValue:0,fieldType:java.lang.Integer},Motion report interval:{maxProtocol:1.20,minValue:90,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.19,fieldType:int},Password:{charset:UTF-8,defaultValue:gb200s,pattern:[0-9a-zA-Z]{4,20},fieldType:java.lang.String},Decimal field:{defaultValue:3.1415927,fieldType:float},Motionless report interval:{maxProtocol:1.20,minValue:90,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.19,fieldType:int}}", jsonFields);
				Assertions.assertEquals(2284, jsonFields.length());
				Assertions.assertEquals("[1.18,1.19,1.20,1.21,1.35,1.36,2.8]", jsonProtocolVersionBoundaries);
			},
			10
		);
	}

	@Test
	void concurrencyMultipleParserSingleCore() throws Exception{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withConfiguration(REGConfigurationASCII.class)
			.build();

		MultithreadingHelper.testMultithreading(
			() -> {
				Configurator configurator = Configurator.create(core);
				return configurator.getConfigurations();
			},
			configurations -> {
				Map<String, Object> configuration = configurations.getFirst();
				String jsonHeader = PrettyPrintMap.toString(configuration.get(ConfigurationKey.HEADER.toString()));
				String jsonFields = PrettyPrintMap.toString(configuration.get(ConfigurationKey.FIELDS.toString()));
				String jsonProtocolVersionBoundaries = PrettyPrintMap.toString(configuration.get(
					ConfigurationKey.PROTOCOL_VERSION_BOUNDARIES.toString()));

//				Assertions.assertEquals("{longDescription:The command AT+GTREG is used to do things.,maxProtocol:2.8,shortDescription:AT+GTREG}", jsonHeader);
				Assertions.assertEquals(102, jsonHeader.length());
//				Assertions.assertEquals("{Operation mode report interval:{minValue:3600,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.21,fieldType:int},Maximum download retry count:{alternatives:[{maxProtocol:1.20,minValue:0,fieldType:int,maxValue:3,defaultValue:0},{minValue:0,fieldType:int,maxValue:3,minProtocol:1.21,defaultValue:1}]},Download timeout:{alternatives:[{maxProtocol:1.18,minValue:5,unitOfMeasure:min,fieldType:int,maxValue:30,defaultValue:10},{minValue:5,unitOfMeasure:min,fieldType:int,maxValue:30,minProtocol:1.19,defaultValue:20}]},Random field:{defaultValue:27,fieldType:java.math.BigInteger},Weekday:{defaultValue:[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY],enumeration:[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY],fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.Weekday[]},Update Over-The-Air:{defaultValue:FALSE,mutuallyExclusive:true,enumeration:[TRUE, FALSE],fieldType:io.github.mtrevisan.boxon.annotations.configurations.BooleanType},Header:{charset:UTF-8,defaultValue:GTREG,fieldType:java.lang.String},Download protocol:{alternatives:[{maxProtocol:1.35,enumeration:[HTTP, HTTPS],fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.DownloadProtocol,defaultValue:HTTP,mutuallyExclusive:true},{enumeration:[HTTP, HTTPS],fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.DownloadProtocol,minProtocol:1.36,defaultValue:HTTPS,mutuallyExclusive:true}]},Download URL:{pattern:.{0,100},fields:{URL:{pattern:https?://.{0,92},fieldType:java.lang.String},password:{pattern:.{1,32},fieldType:java.lang.String},username:{pattern:.{1,32},fieldType:java.lang.String}},charset:UTF-8},Update mode:{minValue:0,maxValue:1,defaultValue:1,fieldType:int},Message counter:{charset:UTF-8,pattern:[0-9A-F]{4},fieldType:java.lang.String},Operation mode:{minValue:0,maxValue:3,defaultValue:0,fieldType:java.lang.Integer},Motion report interval:{maxProtocol:1.20,minValue:90,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.19,fieldType:int},Password:{charset:UTF-8,defaultValue:gb200s,pattern:[0-9a-zA-Z]{4,20},fieldType:java.lang.String},Decimal field:{defaultValue:3.1415927,fieldType:float},Motionless report interval:{maxProtocol:1.20,minValue:90,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.19,fieldType:int}}", jsonFields);
				Assertions.assertEquals(2284, jsonFields.length());
				Assertions.assertEquals("[1.18,1.19,1.20,1.21,1.35,1.36,2.8]", jsonProtocolVersionBoundaries);
			},
			10
		);
	}

	@Test
	void concurrencyMultipleParserMultipleCore() throws Exception{
		MultithreadingHelper.testMultithreading(
			() -> {
				Core core = CoreBuilder.builder()
					.withDefaultCodecs()
					.withConfiguration(REGConfigurationASCII.class)
					.build();
				Configurator configurator = Configurator.create(core);
				return configurator.getConfigurations();
			},
			configurations -> {
				Map<String, Object> configuration = configurations.getFirst();
				String jsonHeader = PrettyPrintMap.toString(configuration.get(ConfigurationKey.HEADER.toString()));
				String jsonFields = PrettyPrintMap.toString(configuration.get(ConfigurationKey.FIELDS.toString()));
				String jsonProtocolVersionBoundaries = PrettyPrintMap.toString(configuration.get(
					ConfigurationKey.PROTOCOL_VERSION_BOUNDARIES.toString()));

//				Assertions.assertEquals("{longDescription:The command AT+GTREG is used to do things.,maxProtocol:2.8,shortDescription:AT+GTREG}", jsonHeader);
				Assertions.assertEquals(102, jsonHeader.length());
//				Assertions.assertEquals("{Operation mode report interval:{minValue:3600,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.21,fieldType:int},Maximum download retry count:{alternatives:[{maxProtocol:1.20,minValue:0,fieldType:int,maxValue:3,defaultValue:0},{minValue:0,fieldType:int,maxValue:3,minProtocol:1.21,defaultValue:1}]},Download timeout:{alternatives:[{maxProtocol:1.18,minValue:5,unitOfMeasure:min,fieldType:int,maxValue:30,defaultValue:10},{minValue:5,unitOfMeasure:min,fieldType:int,maxValue:30,minProtocol:1.19,defaultValue:20}]},Random field:{defaultValue:27,fieldType:java.math.BigInteger},Weekday:{defaultValue:[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY],enumeration:[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY],fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.Weekday[]},Update Over-The-Air:{defaultValue:FALSE,mutuallyExclusive:true,enumeration:[TRUE, FALSE],fieldType:io.github.mtrevisan.boxon.annotations.configurations.BooleanType},Header:{charset:UTF-8,defaultValue:GTREG,fieldType:java.lang.String},Download protocol:{alternatives:[{maxProtocol:1.35,enumeration:[HTTP, HTTPS],fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.DownloadProtocol,defaultValue:HTTP,mutuallyExclusive:true},{enumeration:[HTTP, HTTPS],fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.DownloadProtocol,minProtocol:1.36,defaultValue:HTTPS,mutuallyExclusive:true}]},Download URL:{pattern:.{0,100},fields:{URL:{pattern:https?://.{0,92},fieldType:java.lang.String},password:{pattern:.{1,32},fieldType:java.lang.String},username:{pattern:.{1,32},fieldType:java.lang.String}},charset:UTF-8},Update mode:{minValue:0,maxValue:1,defaultValue:1,fieldType:int},Message counter:{charset:UTF-8,pattern:[0-9A-F]{4},fieldType:java.lang.String},Operation mode:{minValue:0,maxValue:3,defaultValue:0,fieldType:java.lang.Integer},Motion report interval:{maxProtocol:1.20,minValue:90,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.19,fieldType:int},Password:{charset:UTF-8,defaultValue:gb200s,pattern:[0-9a-zA-Z]{4,20},fieldType:java.lang.String},Decimal field:{defaultValue:3.1415927,fieldType:float},Motionless report interval:{maxProtocol:1.20,minValue:90,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.19,fieldType:int}}", jsonFields);
				Assertions.assertEquals(2284, jsonFields.length());
				Assertions.assertEquals("[1.18,1.19,1.20,1.21,1.35,1.36,2.8]", jsonProtocolVersionBoundaries);
			},
			10
		);
	}

}
