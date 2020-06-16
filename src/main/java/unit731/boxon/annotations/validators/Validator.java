package unit731.boxon.annotations.validators;


public interface Validator<T>{

	boolean validate(final T value);

}
