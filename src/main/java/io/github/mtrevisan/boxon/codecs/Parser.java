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

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.exceptions.DecodeException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.external.ComposeResponse;
import io.github.mtrevisan.boxon.external.ParseResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;


public class Parser{

	private final TemplateParser templateParser = new TemplateParser();


	/**
	 * Create an empty parser (context, codecs and templates MUST BE manually loaded! -- templates MUST BE loaded AFTER
	 * the codecs).
	 *
	 * @return	A basic empty parser.
	 */
	public static Parser create(){
		return new Parser();
	}


	private Parser(){}

	/**
	 * Adds a key-value pair to the context of this evaluator.
	 *
	 * @param key	The key used to reference the value.
	 * @param value	The value.
	 * @return	The {@link Parser}, used for chaining.
	 */
	public Parser addToContext(final String key, final Object value){
		Evaluator.addToContext(key, value);
		return this;
	}

	/**
	 * Loads the context for the {@link Evaluator}.
	 *
	 * @param context	The context map.
	 * @return	The {@link Parser}, used for chaining.
	 */
	public Parser withContext(final Map<String, Object> context){
		Objects.requireNonNull(context);

		context.forEach(Evaluator::addToContext);
		return this;
	}

	/**
	 * Add a method to the context for the {@link Evaluator}.
	 *
	 * @param method	The method.
	 * @return	The {@link Parser}, used for chaining.
	 */
	public Parser withContextFunction(final Method method){
		Evaluator.addToContext(method);
		return this;
	}

	/**
	 * Add a method to the context for the {@link Evaluator}.
	 *
	 * @param cls	The class in which the method resides.
	 * @param methodName	The name of the method.
	 * @param parameterTypes	The parameter array.
	 * @return	The {@link Parser}, used for chaining.
	 * @throws NoSuchMethodException	If a matching method is not found.
	 * @throws SecurityException	If a security manager, <i>s</i>, is present and any of the following conditions is met:
	 * 	<ul>
	 * 		<li>the caller's class loader is not the same as the class loader of this class and invocation of
	 * 		{@link SecurityManager#checkPermission s.checkPermission} method with
	 * 		{@code RuntimePermission("accessDeclaredMembers")} denies access to the declared method.</li>
	 *			<li>the caller's class loader is not the same as or an ancestor of the class loader for the current class and
	 *  		invocation of {@link SecurityManager#checkPackageAccess s.checkPackageAccess()} denies access to the package
	 * 		of this class.</li>
	 * 	</ul>
	 */
	public Parser withContextFunction(final Class<?> cls, final String methodName, final Class<?>... parameterTypes)
			throws NoSuchMethodException, SecurityException{
		final Method method = cls.getDeclaredMethod(methodName, parameterTypes);
		return withContextFunction(method);
	}


	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 * <p>This method SHOULD BE called from a method inside a class that lies on a parent of all the codecs.</p>
	 *
	 * @return	The {@link Parser}, used for chaining.
	 */
	public final Parser withDefaultCodecs(){
		templateParser.loader.loadDefaultCodecs();
		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs.
	 * @return	The {@link Parser}, used for chaining.
	 */
	public final Parser withCodecs(final Class<?>... basePackageClasses){
		templateParser.loader.loadCodecs(basePackageClasses);
		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param codecs	The list of codecs to be loaded.
	 * @return	The {@link Parser}, used for chaining.
	 */
	public final Parser withCodecs(final CodecInterface<?>... codecs){
		templateParser.loader.addCodecs(codecs);
		return this;
	}


	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @return	The {@link Parser}, used for chaining.
	 */
	public final Parser withDefaultTemplates(){
		templateParser.loader.loadDefaultTemplates();
		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	The {@link Parser}, used for chaining.
	 */
	public final Parser withTemplates(final Class<?>... basePackageClasses){
		templateParser.loader.loadTemplates(basePackageClasses);
		return this;
	}


	/**
	 * Parse a message from a file containing a binary stream.
	 *
	 * @param file	The file containing the binary stream.
	 * @return	The parse response.
	 * @throws FileNotFoundException	If the file does not exist, is a directory rather than a regular file,
	 * 	or for some other reason cannot be opened for reading.
	 * @throws SecurityException	If a security manager exists and its {@code checkRead} method denies read access to the file.
	 */
	public ParseResponse parse(final File file) throws IOException{
		final BitReader reader = BitReader.wrap(file);
		return parse(reader);
	}

	/**
	 * Parse a message.
	 *
	 * @param payload	The message to be parsed.
	 * @return	The parse response.
	 */
	public ParseResponse parse(final byte[] payload){
		final BitReader reader = BitReader.wrap(payload);
		return parse(reader);
	}

	/**
	 * Parse a message.
	 *
	 * @param buffer	The message to be parsed backed by a {@link ByteBuffer}.
	 * @return	The parse response.
	 */
	public ParseResponse parse(final ByteBuffer buffer){
		final BitReader reader = BitReader.wrap(buffer);
		return parse(reader);
	}

	/**
	 * Parse a message.
	 *
	 * @param reader	The message to be parsed backed by a {@link BitReader}.
	 * @return	The parse response.
	 */
	public ParseResponse parse(final BitReader reader){
		final byte[] array = reader.array();
		final ParseResponse response = new ParseResponse(array);

		int start = 0;
		while(reader.hasRemaining()){
			start = reader.position();

			//save state of the reader (restored upon a decoding error)
			reader.createFallbackPoint();

			try{
				final Template<?> template = templateParser.loader.getTemplate(reader);

				final Object partialDecodedMessage = templateParser.decode(template, reader, null);

				response.addParsedMessage(start, partialDecodedMessage);
			}
			catch(final Exception e){
				final DecodeException pe = new DecodeException(reader.position(), e);
				response.addError(start, pe);

				//restore state of the reader
				reader.restoreFallbackPoint();

				final int position = templateParser.loader.findNextMessageIndex(reader);
				if(position < 0)
					//cannot find any template for message
					break;

				reader.position(position);
			}
		}

		//check if there are unread bytes
		assertNoLeftBytes(reader, start, response);

		return response;
	}

	private void assertNoLeftBytes(final BitReader reader, final int start, final ParseResponse response){
		if(!response.hasErrors() && reader.hasRemaining()){
			final int position = reader.position();
			final IllegalArgumentException error = new IllegalArgumentException("There are remaining unread bytes");
			final DecodeException pe = new DecodeException(position, error);
			response.addError(start, pe);
		}
	}


	/**
	 * Compose a list of messages.
	 *
	 * @param data	The messages to be composed.
	 * @return	The composition response.
	 */
	public ComposeResponse compose(final Collection<Object> data){
		return compose(data.toArray(Object[]::new));
	}

	/**
	 * Compose a list of messages.
	 *
	 * @param data	The message(s) to be composed.
	 * @return	The composition response.
	 */
	public ComposeResponse compose(final Object... data){
		final ComposeResponse response = new ComposeResponse();

		final BitWriter writer = new BitWriter();
		for(int i = 0; i < data.length; i ++)
			compose(writer, data[i], response);
		writer.flush();

		response.setComposedMessage(writer.array());

		return response;
	}

	/**
	 * Compose a single message.
	 *
	 * @param data	The message to be composed.
	 */
	private void compose(final BitWriter writer, final Object data, final ComposeResponse response){
		try{
			final Template<?> template = templateParser.loader.getTemplate(data.getClass());

			templateParser.encode(template, writer, null, data);
		}
		catch(final Exception e){
			response.addError(new EncodeException(data, e));
		}
	}

}
