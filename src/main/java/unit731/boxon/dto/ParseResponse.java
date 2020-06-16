package unit731.boxon.dto;

import unit731.boxon.codecs.ParseException;

import java.util.ArrayList;
import java.util.List;


public class ParseResponse{

	private final List<Object> parsedMessages = new ArrayList<>();
	private final List<ParseException> errors = new ArrayList<>();


	public void addParsedMessage(final Object decodedMessage){
		parsedMessages.add(decodedMessage);
	}

	public List<Object> getParsedMessages(){
		return parsedMessages;
	}

	public void addError(final ParseException exception){
		errors.add(exception);
	}

	public boolean hasErrors(){
		return !errors.isEmpty();
	}

	public List<ParseException> getErrors(){
		return errors;
	}

}
