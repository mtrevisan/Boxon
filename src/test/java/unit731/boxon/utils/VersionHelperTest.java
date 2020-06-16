package unit731.boxon.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class VersionHelperTest{

	@Test
	void compare(){
		Assertions.assertEquals(1, VersionHelper.compare("1.1", "1.0"));
		Assertions.assertEquals(0, VersionHelper.compare("1.0", "1.0"));
		Assertions.assertEquals(-1, VersionHelper.compare("1.0", "1.1"));

		Assertions.assertEquals(Integer.MAX_VALUE, VersionHelper.compare("1.0a", "1.0"));
		Assertions.assertEquals(0, VersionHelper.compare("1.0a", "1.0a"));
		Assertions.assertEquals(-Integer.MAX_VALUE, VersionHelper.compare("1.0", "1.0a"));

		Assertions.assertEquals(1, VersionHelper.compare("1.0b", "1.0a"));
		Assertions.assertEquals(0, VersionHelper.compare("1.0a", "1.0A"));
		Assertions.assertEquals(-1, VersionHelper.compare("1.0a", "1.0b"));

		Assertions.assertEquals(1, VersionHelper.compare("1.1b", "1.0a"));
		Assertions.assertEquals(-1, VersionHelper.compare("1.0a", "1.1b"));
	}

}
