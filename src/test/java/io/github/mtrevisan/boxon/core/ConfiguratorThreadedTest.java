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

import io.github.mtrevisan.boxon.core.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.core.codecs.queclink.REGConfigurationASCII;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.core.keys.ConfigurationKey;
import io.github.mtrevisan.boxon.utils.MultithreadingHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ExecutionException;


@SuppressWarnings("ALL")
class ConfiguratorThreadedTest{

	@Test
	void concurrencySingleParserSingleCore() throws AnnotationException, ConfigurationException, CodecException, TemplateException,
			ExecutionException, InterruptedException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with("QUECLINK_GB200S", (byte)0x46);
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withConfiguration(REGConfigurationASCII.class)
			.create();
		Configurator configurator = Configurator.create(core);

		MultithreadingHelper.testMultithreading(
			() -> configurator.getConfigurations(),
			configurations -> {
				Map<String, Object> configuration = configurations.get(0);
				String jsonHeader = PrettyPrintMap.toString(configuration.get(ConfigurationKey.CONFIGURATION_HEADER.toString()));
				String jsonFields = PrettyPrintMap.toString(configuration.get(ConfigurationKey.CONFIGURATION_FIELDS.toString()));
				String jsonProtocolVersionBoundaries = PrettyPrintMap.toString(configuration.get(
					ConfigurationKey.CONFIGURATION_PROTOCOL_VERSION_BOUNDARIES.toString()));

				Assertions.assertEquals("{longDescription:The command AT+GTREG is used to do things.,maxProtocol:2.8,shortDescription:AT+GTREG}", jsonHeader);
				Assertions.assertEquals("{Operation mode report interval:{minValue:3600,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.21,fieldType:int},Maximum download retry count:{alternatives:[{maxProtocol:1.20,minValue:0,fieldType:int,maxValue:3,defaultValue:0},{minValue:0,fieldType:int,maxValue:3,minProtocol:1.21,defaultValue:1}]},Download timeout:{alternatives:[{maxProtocol:1.18,minValue:5,unitOfMeasure:min,fieldType:int,maxValue:30,defaultValue:10},{minValue:5,unitOfMeasure:min,fieldType:int,maxValue:30,minProtocol:1.19,defaultValue:20}]},Weekday:{defaultValue:[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY],enumeration:[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY]},Update Over-The-Air:{defaultValue:FALSE,mutuallyExclusive:true,enumeration:[TRUE, FALSE]},Header:{charset:UTF-8,defaultValue:GTREG,fieldType:String},Download protocol:{alternatives:[{maxProtocol:1.35,enumeration:[HTTP, HTTPS],defaultValue:HTTP,mutuallyExclusive:true},{enumeration:[HTTP, HTTPS],minProtocol:1.36,defaultValue:HTTP,mutuallyExclusive:true}]},Download URL:{pattern:.{0,100},charset:UTF-8,fields:{URL:{pattern:https?://.{0,92},fieldType:String},password:{pattern:.{1,32},fieldType:String},username:{pattern:.{1,32},fieldType:String}}},Update mode:{minValue:0,maxValue:1,defaultValue:1,fieldType:int},Message counter:{minValue:0,maxValue:65535,fieldType:int},Operation mode:{minValue:0,maxValue:3,defaultValue:0,fieldType:int},Motion report interval:{maxProtocol:1.20,minValue:90,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.19,fieldType:int},Password:{charset:UTF-8,defaultValue:gb200s,pattern:[0-9a-zA-Z]{4,20},fieldType:String},Motionless report interval:{maxProtocol:1.20,minValue:90,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.19,fieldType:int}}", jsonFields);
				Assertions.assertEquals("[1.18,1.19,1.20,1.21,1.35,1.36,2.8]", jsonProtocolVersionBoundaries);
			},
			10
		);
	}

	@Test
	void concurrencyMultipleParserSingleCore() throws AnnotationException, ConfigurationException, CodecException, TemplateException,
			ExecutionException, InterruptedException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with("QUECLINK_GB200S", (byte)0x46);
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withConfiguration(REGConfigurationASCII.class)
			.create();

		MultithreadingHelper.testMultithreading(
			() -> {
				Configurator configurator = Configurator.create(core);
				return configurator.getConfigurations();
			},
			configurations -> {
				Map<String, Object> configuration = configurations.get(0);
				String jsonHeader = PrettyPrintMap.toString(configuration.get(ConfigurationKey.CONFIGURATION_HEADER.toString()));
				String jsonFields = PrettyPrintMap.toString(configuration.get(ConfigurationKey.CONFIGURATION_FIELDS.toString()));
				String jsonProtocolVersionBoundaries = PrettyPrintMap.toString(configuration.get(
					ConfigurationKey.CONFIGURATION_PROTOCOL_VERSION_BOUNDARIES.toString()));

				Assertions.assertEquals("{longDescription:The command AT+GTREG is used to do things.,maxProtocol:2.8,shortDescription:AT+GTREG}", jsonHeader);
				Assertions.assertEquals("{Operation mode report interval:{minValue:3600,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.21,fieldType:int},Maximum download retry count:{alternatives:[{maxProtocol:1.20,minValue:0,fieldType:int,maxValue:3,defaultValue:0},{minValue:0,fieldType:int,maxValue:3,minProtocol:1.21,defaultValue:1}]},Download timeout:{alternatives:[{maxProtocol:1.18,minValue:5,unitOfMeasure:min,fieldType:int,maxValue:30,defaultValue:10},{minValue:5,unitOfMeasure:min,fieldType:int,maxValue:30,minProtocol:1.19,defaultValue:20}]},Weekday:{defaultValue:[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY],enumeration:[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY]},Update Over-The-Air:{defaultValue:FALSE,mutuallyExclusive:true,enumeration:[TRUE, FALSE]},Header:{charset:UTF-8,defaultValue:GTREG,fieldType:String},Download protocol:{alternatives:[{maxProtocol:1.35,enumeration:[HTTP, HTTPS],defaultValue:HTTP,mutuallyExclusive:true},{enumeration:[HTTP, HTTPS],minProtocol:1.36,defaultValue:HTTP,mutuallyExclusive:true}]},Download URL:{pattern:.{0,100},charset:UTF-8,fields:{URL:{pattern:https?://.{0,92},fieldType:String},password:{pattern:.{1,32},fieldType:String},username:{pattern:.{1,32},fieldType:String}}},Update mode:{minValue:0,maxValue:1,defaultValue:1,fieldType:int},Message counter:{minValue:0,maxValue:65535,fieldType:int},Operation mode:{minValue:0,maxValue:3,defaultValue:0,fieldType:int},Motion report interval:{maxProtocol:1.20,minValue:90,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.19,fieldType:int},Password:{charset:UTF-8,defaultValue:gb200s,pattern:[0-9a-zA-Z]{4,20},fieldType:String},Motionless report interval:{maxProtocol:1.20,minValue:90,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.19,fieldType:int}}", jsonFields);
				Assertions.assertEquals("[1.18,1.19,1.20,1.21,1.35,1.36,2.8]", jsonProtocolVersionBoundaries);
			},
			10
		);
	}

	@Test
	void concurrencyMultipleParserMultipleCore() throws AnnotationException, ConfigurationException, CodecException, TemplateException,
			ExecutionException, InterruptedException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with("QUECLINK_GB200S", (byte)0x46);

		MultithreadingHelper.testMultithreading(
			() -> {
				Core core = CoreBuilder.builder()
					.withDefaultCodecs()
					.withConfiguration(REGConfigurationASCII.class)
					.create();
				Configurator configurator = Configurator.create(core);
				return configurator.getConfigurations();
			},
			configurations -> {
				Map<String, Object> configuration = configurations.get(0);
				String jsonHeader = PrettyPrintMap.toString(configuration.get(ConfigurationKey.CONFIGURATION_HEADER.toString()));
				String jsonFields = PrettyPrintMap.toString(configuration.get(ConfigurationKey.CONFIGURATION_FIELDS.toString()));
				String jsonProtocolVersionBoundaries = PrettyPrintMap.toString(configuration.get(
					ConfigurationKey.CONFIGURATION_PROTOCOL_VERSION_BOUNDARIES.toString()));

				Assertions.assertEquals("{longDescription:The command AT+GTREG is used to do things.,maxProtocol:2.8,shortDescription:AT+GTREG}", jsonHeader);
				Assertions.assertEquals("{Operation mode report interval:{minValue:3600,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.21,fieldType:int},Maximum download retry count:{alternatives:[{maxProtocol:1.20,minValue:0,fieldType:int,maxValue:3,defaultValue:0},{minValue:0,fieldType:int,maxValue:3,minProtocol:1.21,defaultValue:1}]},Download timeout:{alternatives:[{maxProtocol:1.18,minValue:5,unitOfMeasure:min,fieldType:int,maxValue:30,defaultValue:10},{minValue:5,unitOfMeasure:min,fieldType:int,maxValue:30,minProtocol:1.19,defaultValue:20}]},Weekday:{defaultValue:[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY],enumeration:[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY]},Update Over-The-Air:{defaultValue:FALSE,mutuallyExclusive:true,enumeration:[TRUE, FALSE]},Header:{charset:UTF-8,defaultValue:GTREG,fieldType:String},Download protocol:{alternatives:[{maxProtocol:1.35,enumeration:[HTTP, HTTPS],defaultValue:HTTP,mutuallyExclusive:true},{enumeration:[HTTP, HTTPS],minProtocol:1.36,defaultValue:HTTP,mutuallyExclusive:true}]},Download URL:{pattern:.{0,100},charset:UTF-8,fields:{URL:{pattern:https?://.{0,92},fieldType:String},password:{pattern:.{1,32},fieldType:String},username:{pattern:.{1,32},fieldType:String}}},Update mode:{minValue:0,maxValue:1,defaultValue:1,fieldType:int},Message counter:{minValue:0,maxValue:65535,fieldType:int},Operation mode:{minValue:0,maxValue:3,defaultValue:0,fieldType:int},Motion report interval:{maxProtocol:1.20,minValue:90,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.19,fieldType:int},Password:{charset:UTF-8,defaultValue:gb200s,pattern:[0-9a-zA-Z]{4,20},fieldType:String},Motionless report interval:{maxProtocol:1.20,minValue:90,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.19,fieldType:int}}", jsonFields);
				Assertions.assertEquals("[1.18,1.19,1.20,1.21,1.35,1.36,2.8]", jsonProtocolVersionBoundaries);
			},
			10
		);
	}

}
