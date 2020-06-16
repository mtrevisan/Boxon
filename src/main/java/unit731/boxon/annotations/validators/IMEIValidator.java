package unit731.boxon.annotations.validators;


public class IMEIValidator implements Validator<String>{

	@Override
	public boolean validate(final String value){
		return (value != null && luhnValidate(value));
	}

	private static boolean luhnValidate(final String text){
		int sum = 0;
		boolean isEven = false;
		for(int i = text.length() - 1; i >= 0; i --, isEven = !isEven){
			int k = Character.getNumericValue(text.charAt(i));
			if(isEven){
				k <<= 1;
				if(k > 9)
					k -= 9;
			}

			sum += k;
		}
		return (sum % 10 == 0);
	}

}
