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
package unit731.boxon.coders;

import unit731.boxon.annotations.exceptions.CodecException;
import unit731.boxon.coders.queclink.ACKMessageHex;
import unit731.boxon.helpers.ByteHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;


class LoaderTest{

	@Test
	void loadFromMap(){
		Loader loader = new Loader();
		loader.loadCoders();

		Assertions.assertFalse(loader.getInitialized());
		List<Codec<?>> codecs = Collections.emptyList();
		loader.init(codecs);
		Assertions.assertTrue(loader.getInitialized());
	}

	@Test
	void loadFromScan(){
		Loader loader = new Loader();
		loader.loadCoders();

		Assertions.assertFalse(loader.getInitialized());
		loader.init();
		Assertions.assertTrue(loader.getInitialized());
	}

	@Test
	void loadFromScanWithBasePackage(){
		Loader loader = new Loader();
		loader.loadCoders();

		Assertions.assertFalse(loader.getInitialized());
		loader.init(LoaderTest.class);
		Assertions.assertTrue(loader.getInitialized());
	}

	@Test
	void loadCodec(){
		Loader loader = new Loader();
		loader.loadCoders();
		loader.init(LoaderTest.class);

		byte[] payload = ByteHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitBuffer reader = BitBuffer.wrap(payload);
		Codec<?> codec = loader.getCodec(reader);

		Assertions.assertNotNull(codec);
		Assertions.assertEquals(ACKMessageHex.class, codec.getType());
	}

	@Test
	void cannotLoadCodec(){
		Loader loader = new Loader();
		loader.loadCoders();
		loader.init(LoaderTest.class);

		byte[] payload = ByteHelper.toByteArray("3b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitBuffer reader = BitBuffer.wrap(payload);
		Assertions.assertThrows(CodecException.class, () -> {
			loader.getCodec(reader);
		});
	}

	@Test
	void findNextCodec(){
		Loader loader = new Loader();
		loader.loadCoders();
		loader.init(LoaderTest.class);

		byte[] payload = ByteHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitBuffer reader = BitBuffer.wrap(payload);
		int position = loader.findNextMessageIndex(reader);

		Assertions.assertEquals(36, position);
	}

	@Test
	void cannotFindNextCodec(){
		Loader loader = new Loader();
		loader.loadCoders();
		loader.init(LoaderTest.class);

		byte[] payload = ByteHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitBuffer reader = BitBuffer.wrap(payload);
		int position = loader.findNextMessageIndex(reader);

		Assertions.assertEquals(-1, position);
	}

}
