package com.dslplatform.compiler.client.cmdline.params;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.dslplatform.compiler.client.api.logging.Logger;
import com.dslplatform.compiler.client.api.params.ArgumentsValidator;

public class ArgumentsParser extends ArgumentsValidator {
    private Logger logger;

    public ArgumentsParser(final Logger logger, final String[] args) throws IOException {
        super(logger);
        this.logger = logger;
        parseArguments(args);
    }

    public void parseArguments(final String[] args) throws IOException {
        if (args.length == 0) {
            logger.trace("There were no arguments provided");
            exitWithHelp();
        }

        logger.debug(String.format("There were %d arguments provided", args.length));

        logger.trace("Running preliminary scan for the help switch");
        for (final String arg : args) {
            if (is(arg, "--")) {
                logger.trace("Stopping preliminary parser (encountered '--').");
                break;
            }

            if (is(arg, "-h", "--help")) {
                logger.trace("Found help switch, rendering help and exiting");
                exitWithHelp();
            }
        }

        for (int index = 0; index < args.length; index ++) {
            final String arg = args[index];
            final boolean last = index == args.length - 1;

            if (is(arg, "--")) {
                logger.debug("Stopping parser (encountered '--').");
                break;
            }

            // skipping diff can only be disabled (enabled by default)
            if (is(arg, "--skip-diff")) {
                logger.trace("Parsed --skip-diff parameter, setting skip diff to 'true'");
                setSkipDiff(true);
                continue;
            }

            // confirming unsafe operations will not be required
            if (is(arg, "--confirm-unsafe")) {
                logger.trace("Parsed --confirm-unsafe parameter, setting confirm unsafe required to 'false'");
                setConfirmUnsafeRequired(false);
                continue;
            }

            // ---------------------------------------------------------------------------------------------------------

            {   // parse username, new arguments overwrite old ones
                String username = startsWith(arg, "-u", "--username=");
                if (username != null) {
                    logger.trace("Parsed username parameter, overwriting old username: " + username);
                    if (username.isEmpty()) {
                        if (last || (username = args[++index]).isEmpty())
                            throw new IllegalArgumentException("Username cannot be empty!");
                        logger.trace("Username argument was empty, read next argument: " + username);
                    }
                    setUsername(username);
                    continue;
                }
            }

            {   // parse password, new arguments overwrite old ones
                String password = startsWith(arg, "-p", "--password=");
                if (password != null) {
                    logger.trace("Parsed password parameter, overwriting old password: ****");
                    if (password.isEmpty()) {
                        if (last || (password = args[++index]).isEmpty())
                            throw new IllegalArgumentException("Password cannot be empty!");
                        logger.trace("Password argument was empty, reading next argument: ****");
                    }
                    setPassword(password);
                    continue;
                }
            }

            {   // parse projectID, new arguments overwrite old ones
                String projectID = startsWith(arg, "-i", "--project-id=");
                if (projectID != null) {
                    logger.trace("Parsed project ID parameter, overwriting old project ID: " + projectID);
                    if (projectID.isEmpty()) {
                        if (last || (projectID = args[++index]).isEmpty())
                            throw new IllegalArgumentException("Project ID cannot be empty!");
                        logger.trace("Project ID argument was empty, reading next argument: " + projectID);
                    }
                    setProjectID(projectID);
                    continue;
                }
            }

            // ---------------------------------------------------------------------------------------------------------

            {   // parse language, new arguments are joined with old ones (multiple languages supported)
                String languages = startsWith(arg, "-l", "--language=");
                if (languages != null) {
                    logger.trace("Parsed language parameter, adding languages to the list: " + languages);
                    if (languages.isEmpty()) {
                        if (last || (languages = args[++index]).isEmpty())
                            throw new IllegalArgumentException("Language cannot be empty!");
                        logger.trace("Language argument was empty, reading next argument: " + languages);
                    }
                    addLanguages(languages);
                    continue;
                }
            }

            {   // parse package name (namespace), new arguments overwrite old ones
                String packageName = startsWith(arg, "-n", "--namespace=");
                if (packageName != null) {
                    logger.trace("Parsed namespace parameter, overwriting old namespace: " + packageName);
                    if (packageName.isEmpty()) {
                        if (!last) {
                            packageName = args[++index];
                            logger.trace("Namespace argument was empty, reading next argument: " + packageName);
                            if (packageName.isEmpty()) {
                                logger.debug("Files will be compiled without a namespace");
                            }
                        }
                    }
                    setPackageName(packageName);
                    continue;
                }
            }

            // ---------------------------------------------------------------------------------------------------------

            {   // parse DSL path, new arguments are joined with old ones (multiple folders)
                String dslPath = startsWith(arg, "-d", "--dsl-path=");
                if (dslPath != null) {
                    logger.trace("Parsed DSL path parameter, adding DSL path to the list: " + dslPath);
                    if (dslPath.isEmpty()) {
                        if (last || (dslPath = args[++index]).isEmpty())
                            throw new IllegalArgumentException("Missing DSL path argument!");
                        logger.trace("Dsl path was empty, reading next argument: " + dslPath);
                    }
                    addDslPath(dslPath);
                    continue;
                }
            }

            {   // parse output path, new arguments overwrite old ones
                String outputPath = startsWith(arg, "-o", "--output-path=");
                if (outputPath != null) {
                    logger.trace("Parsed output path parameter, overwriting old output path: " + outputPath);
                    if (outputPath.isEmpty()) {
                        if (last || (outputPath = args[++index]).isEmpty())
                            throw new IllegalArgumentException("Missing output path argument!");
                        logger.trace("Output path was empty, reading next argument: " + outputPath);
                    }
                    setOutputPath(outputPath);
                    continue;
                }
            }

            {   // parse cache path, new arguments overwrite old ones
                String cachePath = startsWith(arg, "-o", "--cache-path=");
                if (cachePath != null) {
                    logger.trace("Parsed cache path parameter, overwriting old cache path: " + cachePath);
                    if (cachePath.isEmpty()) {
                        if (last || (cachePath = args[++index]).isEmpty())
                            throw new IllegalArgumentException("Missing cache path argument!");
                        logger.trace("Cache path was empty, reading next argument: " + cachePath);
                    }
                    setCachePath(cachePath);
                    continue;
                }
            }

            {   // parse logging level, new arguments overwrite old ones
                String loggingLevel = startsWith(arg, "-v", "--logging-level=");
                if (loggingLevel != null) {
                    logger.trace("Parsed logging level parameter, overwriting old logging level: " + loggingLevel);
                    if (loggingLevel.isEmpty()) {
                        if (last || (loggingLevel = args[++index]).isEmpty())
                            throw new IllegalArgumentException("Missing logging level argument!");
                        logger.trace("Cache path was empty, reading next argument: " + loggingLevel);
                    }
                    setLoggingLevel(loggingLevel);
                    continue;
                }
            }

            // =========================================================================================================

            {   // parse project ini, immediately expanded with new .ini arguments overwriting old ones
                String projectIniPath = startsWith(arg, "-f", "--project-ini-path=");
                if (projectIniPath != null) {
                    logger.trace("Parsed project ini path parameter, processing: " + projectIniPath);
                    if (projectIniPath.isEmpty()) {
                        if (last || (projectIniPath = args[++index]).isEmpty())
                            throw new IllegalArgumentException("Missing project ini path argument!");
                        logger.trace("Project ini path as empty, reading next argument: " + projectIniPath);
                    }

                    setProjectIniParams(projectIniPath);
                    continue;
                }
            }

            // =========================================================================================================

            // parse action, new arguments are joined with old ones (specific combinations of multiple actions are allowed)
            logger.trace("No specific handler has been found, assuming that this parameter is an action: " + arg);
            addActions(arg);
        }
    }

    // =================================================================================================================

    private void setProjectIniParams(final String projectIniPath) throws IOException {
        final Properties properties = new Properties();

        {
            final InputStream is = new FileInputStream(projectIniPath);
            try {
                properties.load(is);
                logger.trace("Successfully loaded project ini properties");
            }
            catch (final IOException e) {
                throw new IOException("An error occured whilst reading the project ini configuration", e);
            }
            finally {
                is.close();
            }
        }

        if (Boolean.parseBoolean(properties.getProperty("skip-diff"))) {
            logger.trace("Parsed --skip-diff parameter, setting skip diff to 'true'");
            setSkipDiff(true);
        }

        if (Boolean.parseBoolean(properties.getProperty("confirm-unsafe"))) {
            logger.trace("Parsed --confirm-unsafe parameter, setting confirm unsafe required to 'false'");
            setConfirmUnsafeRequired(false);
        }

        {
            final String username = properties.getProperty("username");
            logger.trace("Parsed username parameter, overwriting old username: " + username);
            if (username != null) setUsername(username);
        }{
            final String password = properties.getProperty("password");
            logger.trace("Parsed password parameter, overwriting old password: ****");
            if (password != null) setPassword(password);
        }{
            final String projectID = properties.getProperty("project-id");
            logger.trace("Parsed project ID parameter, overwriting old project ID: " + projectID);
            if (projectID != null) setProjectID(projectID);
        }{
            final String languages = properties.getProperty("language");
            logger.trace("Parsed language parameter, adding languages to the list: " + languages);
            if (languages != null) addLanguages(languages);
        }{
            final String packageName = properties.getProperty("namespace");
            logger.trace("Parsed namespace parameter, overwriting old namespace: " + packageName);
            if (packageName != null) setPackageName(packageName);
        }{
            final String dslPath = properties.getProperty("dsl-path");
            logger.trace("Parsed DSL path parameter, adding DSL path to the list: " + dslPath);
            if (dslPath != null) addDslPath(dslPath);
        }{
            final String outputPath = properties.getProperty("output-path");
            logger.trace("Parsed output path parameter, overwriting old output path: " + outputPath);
            if (outputPath != null) setOutputPath(outputPath);
        }{
            final String cachePath = properties.getProperty("cache-path");
            logger.trace("Parsed cache path parameter, overwriting old cache path: " + cachePath);
            if (cachePath != null) setCachePath(cachePath);
        }
    }

    // =================================================================================================================

    private static String startsWith(final String what, final String... switches) {
        final String trimmed = what.trim();
        for (final String sw : switches) {
            if (trimmed.startsWith(sw))
                return what.substring(sw.length());
        }
        return null;
    }

    private static boolean is(final String what, final String... switches) {
        final String startsWithResult = startsWith(what, switches);
        return startsWithResult != null && startsWithResult.isEmpty();
    }

    // =================================================================================================================

    private static void exitWithHelp() {
        try {
            final String helpText = new String(IOUtils.toByteArray(
                    ArgumentsParser.class.getResourceAsStream("/dsl-clc-help.txt")), "UTF-8");
            System.out.println(helpText);
        } catch (final Exception e) {
            System.out.println("Could not display the help file!");
        }
        System.exit(0);
    }
}