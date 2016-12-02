/*
 * Conditions Of Use
 *
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), an agency of the Federal Government.
 * Pursuant to title 15 Untied States Code Section 105, works of NIST
 * employees are not subject to copyright protection in the United States
 * and are considered to be in the public domain.  As a result, a formal
 * license is not needed to use the software.
 *
 * This software is provided by NIST as a service and is expressly
 * provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
 * AND DATA ACCURACY.  NIST does not warrant or make any representations
 * regarding the use of the software or the results thereof, including but
 * not limited to the correctness, accuracy, reliability or usefulness of
 * the software.
 *
 * Permission to use this software is contingent upon your acceptance
 * of the terms of this agreement.
 *
 */
/*
 ****************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).    *
 ****************************************************************************
 */

package gov.nist.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * A wrapper around log4j that is used for logging debug and errors. You can
 * replace this file if you want to change the way in which messages are logged.
 *
 * @author M. Ranganathan <br/>
 * @author M.Andrews
 * @author Jeroen van Bemmel
 * @author Jean Deruelle
 * @version 1.2
 */

public class LogWriter implements StackLogger {

    /**
     * The LOG to which we will write our logging output.
     */
    private Logger logger;

    /**
     * Flag to indicate that logging is enabled.
     */
    private volatile boolean needsLogging = false;

    private int lineCount;

    /**
     * trace level
     */

    protected int traceLevel = TRACE_NONE;

    private String buildTimeStamp;

    private Properties configurationProperties;
    private Level level;

    /**
     * log a stack trace. This helps to look at the stack frame.
     */
    public void logStackTrace() {
        this.logStackTrace(TRACE_DEBUG);
    }

    public void logStackTrace(int traceLevel) {
        if (needsLogging) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            StackTraceElement[] ste = new Exception().getStackTrace();
            // Skip the log writer frame and log all the other stack frames.
            for (int i = 1; i < ste.length; i++) {
                String callFrame = "[" + ste[i].getFileName() + ":"
                        + ste[i].getLineNumber() + "]";
                pw.print(callFrame);
            }
            pw.close();
            String stackTrace = sw.getBuffer().toString();
            Level level = this.getLevel(traceLevel);
            logger.log(level, stackTrace);
        }
    }

    /**
     * Get the line count in the log stream.
     *
     * @return the line count
     */
    public int getLineCount() {
        return lineCount;
    }

    /**
     * Get the LOG.
     *
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * This method allows you to add an external appender.
     * This is useful for the case when you want to log to
     * a different log stream than a file.
     *
     * @param appender the appender to add
     */
    public void addAppender(final Appender appender) {
        addAppender((LoggerContext) LogManager.getContext(false), appender, this.level);
    }

    /**
     * Log an exception.
     *
     * @param ex the throwable to log
     */
    public void logException(Throwable ex) {

        if (needsLogging) {

            this.getLogger().error(ex.getMessage(), ex);
        }
    }


    /**
     * Counts the line number so that the debug log can be correlated to the
     * message trace.
     *
     * @param message -- message to count the lines for.
     */
    private void countLines(String message) {
        char[] chars = message.toCharArray();
        for (char aChar : chars) {
            if (aChar == '\n') {
                lineCount++;
            }
        }
    }

    /**
     * Prepend the line and file where this message originated from
     *
     * @param message the message to enhance
     * @return re-written message.
     */
    private String enhanceMessage(String message) {

        StackTraceElement[] stackTrace = new Exception().getStackTrace();
        StackTraceElement elem = stackTrace[3];
        String className = elem.getClassName();
        String methodName = elem.getMethodName();
        String fileName = elem.getFileName();
        int lineNumber = elem.getLineNumber();
        return className + "." + methodName + "(" + fileName + ":"
                + lineNumber + ") [" + message + "]";
    }

    /**
     * Log a message into the log file.
     *
     * @param message message to log into the log file.
     */
    public void logDebug(String message) {
        if (needsLogging) {
            String newMessage = this.enhanceMessage(message);
            if (this.lineCount == 0) {
                getLogger().debug("BUILD TIMESTAMP = " + this.buildTimeStamp);
                getLogger().debug("Config Propeties = " + this.configurationProperties);
            }
            countLines(newMessage);
            getLogger().debug(newMessage);
        }
    }

    /*
     * (non-Javadoc)
     * @see gov.nist.core.StackLogger#logDebug(java.lang.String, java.lang.Exception)
     */
    public void logDebug(String message, Exception ex) {
        if (needsLogging) {
            String newMessage = this.enhanceMessage(message);
            if (this.lineCount == 0) {
                getLogger().debug("BUILD TIMESTAMP = " + this.buildTimeStamp);
                getLogger().debug("Config Propeties = " + this.configurationProperties);
            }
            countLines(newMessage);
            getLogger().debug(newMessage, ex);
        }
    }

    /**
     * Log a message into the log file.
     *
     * @param message message to log into the log file.
     */
    public void logTrace(String message) {
        if (needsLogging) {
            String newMessage = this.enhanceMessage(message);
            if (this.lineCount == 0) {
                getLogger().debug("BUILD TIMESTAMP = " + this.buildTimeStamp);
                getLogger().debug("Config Propeties = " + this.configurationProperties);
            }
            countLines(newMessage);
            getLogger().trace(newMessage);
        }
    }

    /**
     * Set the trace level for the stack.
     */
    private void setTraceLevel(int level) {
        traceLevel = level;
    }

    /**
     * Get the trace level for the stack.
     */
    public int getTraceLevel() {
        return traceLevel;
    }

    /**
     * Log an error message.
     *
     * @param message -- error message to log.
     */
    public void logFatalError(String message) {
        Logger logger = this.getLogger();
        String newMsg = this.enhanceMessage(message);
        countLines(newMsg);
        logger.fatal(newMsg);
    }

    /**
     * Log an error message.
     *
     * @param message -- error message to log.
     */
    public void logError(String message) {
        Logger logger = this.getLogger();
        String newMsg = this.enhanceMessage(message);
        countLines(newMsg);
        logger.error(newMsg);
    }

    public LogWriter() {
    }

    public void setStackProperties(Properties configurationProperties) {

        this.configurationProperties = configurationProperties;

        String logLevel = configurationProperties
                .getProperty("gov.nist.javax.sip.TRACE_LEVEL");

        /*
      Name of the log file in which the trace is written out (default is
      /tmp/sipserverlog.txt)
     */
        String logFileName = configurationProperties
                .getProperty("gov.nist.javax.sip.DEBUG_LOG");

        /*
      The stack name.
     */
        String stackName = configurationProperties
                .getProperty("javax.sip.STACK_NAME");

        //check whether a Log4j LOG name has been
        //specified. if not, use the stack name as the default
        //LOG name.
        String category = configurationProperties
                .getProperty("gov.nist.javax.sip.LOG4J_LOGGER_NAME", stackName);

        logger = LogManager.getLogger(category);
        if (logLevel != null) {
            if (logLevel.equals("LOG4J")) {
                CommonLogger.useLegacyLogger = false;
            } else {
                try {
                    int ll;
                    switch (logLevel) {
                        case "TRACE":
                            ll = TRACE_DEBUG;
                            Debug.debug = true;
                            Debug.setStackLogger(this);
                            break;
                        case "DEBUG":
                            ll = TRACE_DEBUG;
                            break;
                        case "INFO":
                            ll = TRACE_INFO;
                            break;
                        case "ERROR":
                            ll = TRACE_ERROR;
                            break;
                        case "NONE":
                        case "OFF":
                            ll = TRACE_NONE;
                            break;
                        default:
                            ll = Integer.parseInt(logLevel);
                            if (ll > 32) {
                                Debug.debug = true;
                                Debug.setStackLogger(this);
                            }
                            break;
                    }

                    this.setTraceLevel(ll);
                    this.needsLogging = true;

                    if (traceLevel == TRACE_TRACE) {
                        level = Level.TRACE;
                    } else if (traceLevel == TRACE_DEBUG) {
                        level = Level.DEBUG;
                    } else if (traceLevel == TRACE_INFO) {
                        level = Level.INFO;
                    } else if (traceLevel == TRACE_WARN) {
                        level = Level.WARN;
                    } else if (traceLevel == TRACE_ERROR) {
                        level = Level.ERROR;
                    } else if (traceLevel == TRACE_FATAL) {
                        level = Level.FATAL;
                    } else if (traceLevel == TRACE_NONE) {
                        level = Level.OFF;
                        this.needsLogging = false;
                    } else {
                        level = Level.ALL;
                    }

                    /*
                     * If user specifies a logging file as part of the startup
                     * properties then we try to create the appender.
                     */
                    if (this.needsLogging && logFileName != null) {
                        boolean overwrite = Boolean.valueOf(
                                configurationProperties.getProperty(
                                        "gov.nist.javax.sip.DEBUG_LOG_OVERWRITE"));

                        addFileAppender(logFileName, level, !overwrite);
                    }
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                    System.err.println("LogWriter: Bad integer " + logLevel);
                    System.err.println("logging dislabled ");
                    needsLogging = false;
                }
            }
        } else {
            this.needsLogging = false;
        }
    }

    /**
     * @return flag to indicate if logging is enabled.
     */
    public boolean isLoggingEnabled() {

        return this.needsLogging;
    }

    /**
     * Return true/false if loging is enabled at a given level.
     *
     * @param logLevel the log level to check
     */
    public boolean isLoggingEnabled(final int logLevel) {
        return this.needsLogging && logLevel <= traceLevel;
    }


    /**
     * Log an error message.
     *
     * @param message the log message
     * @param ex the exception
     */
    public void logError(String message, Exception ex) {
        Logger logger = this.getLogger();
        logger.error(message, ex);
    }

    /**
     * Log a warning message.
     *
     * @param string the log message
     */
    public void logWarning(String string) {
        getLogger().warn(string);
    }

    /**
     * Log an info message.
     *
     * @param string the log message
     */
    public void logInfo(String string) {
        getLogger().info(string);
    }

    /**
     * Disable logging altogether.
     */
    public void disableLogging() {
        this.needsLogging = false;
    }

    /**
     * Enable logging (globally).
     */
    public void enableLogging() {
        this.needsLogging = true;
    }

    public void setBuildTimeStamp(String buildTimeStamp) {
        this.buildTimeStamp = buildTimeStamp;
    }

    private void addFileAppender(final String fileName, final Level level, final boolean append) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        PatternLayout layout = PatternLayout.createLayout(PatternLayout.SIMPLE_CONVERSION_PATTERN, null, config, null, null, true, true, null, null);
        FileAppender appender = FileAppender.newBuilder().withName("FILE").withFileName(fileName).withAppend(append).withLayout(layout).build();
        addAppender(ctx, appender, level);
    }

    private void addAppender(final LoggerContext ctx, final Appender appender, final Level level) {
        Configuration config = ctx.getConfiguration();
        config.addAppender(appender);
        AppenderRef ref = AppenderRef.createAppenderRef(appender.getName(), level, null);
        LoggerConfig loggerConfig = config.getLoggerConfig("gov.nist");

        if (loggerConfig == null) {
            AppenderRef[] refs = new AppenderRef[]{ref};
            loggerConfig = LoggerConfig.createLogger(false, level, "gov.nist", "true", refs, null, config, null);
        }

        loggerConfig.getAppenderRefs().add(ref);
        loggerConfig.addAppender(appender, null, null);
        config.addLogger("org.apache.logging.log4j", loggerConfig);
        ctx.updateLoggers();
    }

    private Level getLevel(int traceLevel) {
        if (traceLevel == TRACE_INFO) {
            return Level.INFO;
        } else if (traceLevel == TRACE_ERROR) {
            return Level.ERROR;
        } else if (traceLevel == TRACE_DEBUG) {
            return Level.DEBUG;
        } else if (traceLevel == TRACE_TRACE) {
            return Level.ALL;
        } else {
            return Level.OFF;
        }
    }

    public String getLoggerName() {
        if (this.logger != null) {
            return logger.getName();
        } else {
            return null;
        }
    }
}
