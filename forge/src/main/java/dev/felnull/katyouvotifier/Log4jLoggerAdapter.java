package dev.felnull.katyouvotifier;

import com.vexsoftware.votifier.platform.LoggingAdapter;
import org.apache.logging.log4j.Logger;

public class Log4jLoggerAdapter implements LoggingAdapter {
    private final Logger logger;

    public Log4jLoggerAdapter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void error(String s) {
        this.logger.error(s);
    }

    @Override
    public void error(String s, Object... o) {
        this.logger.error(s, o);
    }

    @Override
    public void error(String s, Throwable e, Object... o) {
        this.logger.error(s, e, o);
    }

    @Override
    public void warn(String s) {
        this.logger.warn(s);
    }

    @Override
    public void warn(String s, Object... o) {
        this.logger.warn(s, o);
    }

    @Override
    public void info(String s) {
        this.logger.info(s);
    }

    @Override
    public void info(String s, Object... o) {
        this.logger.info(s, o);
    }
}
