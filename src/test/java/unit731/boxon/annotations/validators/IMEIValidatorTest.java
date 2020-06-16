package unit731.boxon.annotations.validators;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class IMEIValidatorTest{

	@Test
	void valid(){
		IMEIValidator validator = new IMEIValidator();
		Assertions.assertTrue(validator.validate("79927398713"));
		Assertions.assertFalse(validator.validate("79927398710"));
		Assertions.assertFalse(validator.validate("79927398711"));
		Assertions.assertFalse(validator.validate("79927398712"));
		Assertions.assertFalse(validator.validate("79927398714"));
		Assertions.assertFalse(validator.validate("79927398715"));
		Assertions.assertFalse(validator.validate("79927398716"));
		Assertions.assertFalse(validator.validate("79927398717"));
		Assertions.assertFalse(validator.validate("79927398718"));
		Assertions.assertFalse(validator.validate("79927398719"));
	}

}
