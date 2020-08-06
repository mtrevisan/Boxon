/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.exceptions.ProtocolMessageException;
import io.github.mtrevisan.boxon.codecs.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.helpers.ByteHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class LoaderTest{

	@Test
	void loadFromMap(){
		Loader loader = new Loader();
		loader.loadDefaultCodecs();

		loader.addProtocolMessages();
	}

	@Test
	void loadFromScan(){
		Loader loader = new Loader();
		loader.loadDefaultCodecs();

		loader.loadDefaultProtocolMessages();
	}

	@Test
	void loadFromScanWithBasePackage(){
		Loader loader = new Loader();
		loader.loadDefaultCodecs();

		loader.loadProtocolMessages(LoaderTest.class);
	}

	@Test
	void loadCodecsAfterProtocolMessages(){
		Loader loader = new Loader();
		Exception e = Assertions.assertThrows(ProtocolMessageException.class,
			() -> loader.loadProtocolMessages(LoaderTest.class));
		Assertions.assertEquals("Cannot create a raw message from data: cannot scan protocol message", e.getMessage());
	}

	@Test
	void loadProtocolMessage(){
		Loader loader = new Loader();
		loader.loadDefaultCodecs();
		loader.loadProtocolMessages(LoaderTest.class);

		byte[] payload = ByteHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReader reader = BitReader.wrap(payload);
		ProtocolMessage<?> protocolMessage = loader.getProtocolMessage(reader);

		Assertions.assertNotNull(protocolMessage);
		Assertions.assertEquals(ACKMessageHex.class, protocolMessage.getType());
	}

	@Test
	void cannotLoadProtocolMessage(){
		Loader loader = new Loader();
		loader.loadDefaultCodecs();
		loader.loadProtocolMessages(LoaderTest.class);

		byte[] payload = ByteHelper.toByteArray("3b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReader reader = BitReader.wrap(payload);
		Assertions.assertThrows(ProtocolMessageException.class, () -> loader.getProtocolMessage(reader));
	}

	@Test
	void findNextProtocolMessage(){
		Loader loader = new Loader();
		loader.loadDefaultCodecs();
		loader.loadProtocolMessages(LoaderTest.class);

		byte[] payload = ByteHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReader reader = BitReader.wrap(payload);
		int position = loader.findNextMessageIndex(reader);

		Assertions.assertEquals(36, position);
	}

	@Test
	void cannotFindNextProtocolMessage(){
		Loader loader = new Loader();
		loader.loadDefaultCodecs();
		loader.loadProtocolMessages(LoaderTest.class);

		byte[] payload = ByteHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReader reader = BitReader.wrap(payload);
		int position = loader.findNextMessageIndex(reader);

		Assertions.assertEquals(-1, position);
	}

}
