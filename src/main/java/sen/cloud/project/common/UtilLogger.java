package sen.cloud.project.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtilLogger
{
	private static final Logger log = LoggerFactory.getLogger("logger");

	public static void info(String format, Object... args)
	{
		StringBuilder str = new StringBuilder();
		str.append(String.format(format, args));
		log.info(str.toString());
	}

	public static void error(String format, Object... args)
	{
		StringBuilder str = new StringBuilder();
		str.append(String.format(format, args));
		log.info(str.toString());
	}
}
