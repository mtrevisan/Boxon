package io.github.mtrevisan.boxon.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class EventLogger implements EventListener{

	static{
		try{
			//check whether an optional SLF4J binding is available
			Class.forName("org.slf4j.impl.StaticLoggerBinder");
		}
		catch(final LinkageError | ClassNotFoundException ignored){
			System.out.println("[WARN] SLF4J: No logger is defined, NO LOG will be printed!");
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(EventLogger.class);


	private static class SingletonHelper{
		private static final EventLogger INSTANCE = new EventLogger();
	}


	public static EventLogger getInstance(){
		return EventLogger.SingletonHelper.INSTANCE;
	}

	private EventLogger(){}

//	public void event(final EventData data){
//		if(LOGGER != NOPLogger.NOP_LOGGER){
//			final Level level = data.getLevel();
//			final String message = data.composeMessage();
//			final Throwable throwable = data.getThrowable();
//			if(level == Level.TRACE)
//				LOGGER.trace(message, throwable);
//			else if(level == Level.DEBUG)
//				LOGGER.debug(message, throwable);
//			else if(level == Level.INFO)
//				LOGGER.info(message, throwable);
//			else if(level == Level.WARN)
//				LOGGER.warn(message, throwable);
//			else if(level == Level.ERROR)
//				LOGGER.error(message, throwable);
//		}
//	}

}
