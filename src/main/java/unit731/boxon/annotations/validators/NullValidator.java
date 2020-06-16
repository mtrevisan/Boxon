package unit731.boxon.annotations.validators;


public class NullValidator<T> implements Validator<T>{

	@Override
	public boolean validate(final T value){
		return true;
	}

}
