package utilities;

import org.apache.logging.log4j.Logger;

public class BaseLogger {
	protected final Logger log;

	protected BaseLogger() {
		// Automatically picks the subclass's logger
		this.log = LoggerHelper.getLogger(getClass());
	}
}
