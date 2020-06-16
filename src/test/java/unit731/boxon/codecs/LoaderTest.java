package unit731.boxon.codecs;

import unit731.boxon.codecs.queclink.ACKMessage;
import unit731.boxon.utils.ByteHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;


class LoaderTest{

	@Test
	void loadFromMap(){
		Loader loader = new Loader();

		Assertions.assertFalse(loader.isInitialized());
		Map<String, Codec<?>> codecs = new HashMap<>();
		loader.init(codecs);
		Assertions.assertTrue(loader.isInitialized());
	}

	@Test
	void loadFromScan(){
		Loader loader = new Loader();

		Assertions.assertFalse(loader.isInitialized());
		loader.init();
		Assertions.assertTrue(loader.isInitialized());
	}

	@Test
	void loadFromScanWithBasePackage(){
		Loader loader = new Loader();

		Assertions.assertFalse(loader.isInitialized());
		loader.init(LoaderTest.class);
		Assertions.assertTrue(loader.isInitialized());
	}

	@Test
	void loadCodec(){
		Loader loader = new Loader();
		loader.init(LoaderTest.class);

		byte[] payload = ByteHelper.hexStringToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitBuffer reader = BitBuffer.wrap(payload);
		Codec<?> codec = loader.getCodec(reader);

		Assertions.assertNotNull(codec);
		Assertions.assertEquals(ACKMessage.class, codec.getType());
	}

	@Test
	void cannotLoadCodec(){
		Loader loader = new Loader();
		loader.init(LoaderTest.class);

		byte[] payload = ByteHelper.hexStringToByteArray("3b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitBuffer reader = BitBuffer.wrap(payload);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			loader.getCodec(reader);
		});
	}

	@Test
	void findNextCodec(){
		Loader loader = new Loader();
		loader.init(LoaderTest.class);

		byte[] payload = ByteHelper.hexStringToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitBuffer reader = BitBuffer.wrap(payload);
		int position = loader.findNextMessageIndex(reader);

		Assertions.assertEquals(36, position);
	}

	@Test
	void cannotFindNextCodec(){
		Loader loader = new Loader();
		loader.init(LoaderTest.class);

		byte[] payload = ByteHelper.hexStringToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitBuffer reader = BitBuffer.wrap(payload);
		int position = loader.findNextMessageIndex(reader);

		Assertions.assertEquals(-1, position);
	}

}
