/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License") you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.codehaus.mojo.selenium

import org.codehaus.mojo.groovy.GroovyMojoSupport

import org.apache.commons.lang.SystemUtils

import com.thoughtworks.selenium.DefaultSelenium

/**
 * Start the Selenium server.
 *
 * @goal start-server
 *
 * @version $Id$
 */
class StartServerMojo
    extends GroovyMojoSupport
{
    /**
     * The port number the server will use.
     *
     * @parameter expression="${port}" default-value="4444"
     */
    int port

    /**
     * Timeout for the server in seconds.
     *
     * @parameter expression="${timeout}" default-value="-1"
     */
    int timeout

    /**
     * Enable the server's debug mode..
     *
     * @parameter expression="${debug}" default-value="false"
     */
    boolean debug
    
    /**
     * The file or resource to use for default user-extentions.js.
     *
     * @parameter default-value="org/codehaus/mojo/selenium/default-user-extentions.js"
     */
    String defaultUserExtensions

    /**
     * Enable or disable default user-extentions.js
     *
     * @parameter default-value="true"
     */
    boolean defaultUserExtensionsEnabled

    /**
     * Location of the user-extentions.js to load into the server.
     * If defaultUserExtensionsEnabled is true, then this file will be appended to the defaults.
     *
     * @parameter
     */
    String userExtensions
    
    /**
     * Working directory where Selenium server will be started from.
     *
     * @parameter expression="${project.build.directory}/selenium"
     * @required
     */
    File workingDirectory

    /**
     * Enable logging mode.
     *
     * @parameter expression="${logOutput}" default-value="false"
     */
    boolean logOutput

    /**
     * The file that Selenium server logs will be written to.
     *
     * @parameter expression="${logFile}" default-value="${project.build.directory}/selenium/server.log"
     * @required
     */
    File logFile

    /**
     * Flag to control if we background the server or block Maven execution.
     *
     * @parameter default-value="false"
     * @required
     */
    boolean background

    /**
     * Attempt to verify the named browser configuration.  Must be one of the
     * standard valid browser names (and must start with a *), e.g. *firefox, *iexplore, *custom.
     *
     * @parameter
     */
    String verifyBrowser
    
    /**
     * Flag to control if we start Selenium RC in multiWindow mode or not. The multiWindow mode
     * is useful for applications using frames/iframes which otherwise cannot be tested as the
     * same window is used for displaying both the Selenium tests and the AUT.
     *
     * @parameter default-value="false"
     */
    boolean multiWindow

    /**
     * The location of the file to read the display properties.
     *
     * @parameter default-value="${project.build.directory}/selenium/display.properties"
     */
    File displayPropertiesFile
    
    //
    // Components
    //
    
    /**
     * @parameter expression="${plugin.artifactMap}"
     * @required
     * @readonly
     */
    Map pluginArtifactMap

    //
    // Mojo
    //

    void execute() {
        log.info('Starting Selenium server...')
        
        ant.mkdir(dir: workingDirectory)
        
        if (logOutput) {
            ant.mkdir(dir: logFile.parentFile)
        }
        
        def pluginArifact = { id ->
            def artifact = pluginArtifactMap[id]
            if (!artifact) {
                fail("Unable to locate '$id' in the list of plugin artifacts")
            }
            
            return artifact.file
        }
        
        // Holds any exception that was thrown during startup
        def errors = []
        
        def runner = {
            try {
                ant.java(classname: 'org.openqa.selenium.server.SeleniumServer',
                         fork: true,
                         dir: workingDirectory,
                         failonerror: true)
                {
                    classpath() {
                        // Add our plugin artifact to pick up log4j configuration
                        pathelement(location: getClass().protectionDomain.codeSource.location.file)
                        pathelement(location: pluginArifact('log4j:log4j'))
                        pathelement(location: pluginArifact('org.openqa.selenium.server:selenium-server'))
                    }
                    
                    // Set display properties if the properties file exists
                    if (displayPropertiesFile && displayPropertiesFile.exists()) {
                        log.info("Including display properties from: $displayPropertiesFile")
                        
                        def props = new Properties()
                        props.load(displayPropertiesFile.newInputStream())
                        props.each { key, value ->
                            env(key: key, value: value)
                        }
                    }
                    // If the system looks like Unix (and not Mac OS X) then complain if DISPLAY is not set
                    else if (SystemUtils.IS_OS_UNIX && !SystemUtils.IS_OS_MAC_OSX) {
                        def tmp = System.getenv('DISPLAY')
                        if (!tmp) {
                            log.warn('OS appears to be Unix and no DISPLAY environment variable has been detected. ' + 
                                     'Browser maybe unable to function correctly. ' + 
                                     'Consider using the selenium:xvfb goal to enable headless operation.')
                        }
                    }
                    
                    if (logOutput) {
                        log.info("Redirecting output to: $logFile")
                        redirector(output: logFile)
                    }
                    
                    // Configure Selenium's logging
                    sysproperty(key: 'selenium.log', value: logFile)
                    sysproperty(key: 'selenium.loglevel', value: debug ? 'DEBUG' : 'INFO')
                    sysproperty(key: 'log4j.configuration', value: 'org/codehaus/mojo/selenium/log4j.properties')
                    
                    arg(value: '-port')
                    arg(value: "$port")
                    
                    if (debug) {
                        arg(value: '-debug')
                    }
                    
                    if (timeout > 0) {
                        arg(value: '-timeout')
                        arg(value: "$timeout")
                    }
                    
                    if (multiWindow) {
                        arg(value: '-multiwindow')
                    }
                    
                    // Maybe configure user extentions
                    def file = createUserExtentionsFile()
                    if (file) {
                        log.info("User extensions: $file")
                        arg(value: '-userExtensions')
                        arg(file: file)
                    }
                }
            }
            catch (Exception e) {
                errors << e
            }
        }
        
        // Start the server int a seperate thread
        Thread t = new Thread(runner, 'Selenium Server Runner')
        t.start()

        log.debug('Waiting for Selenium server...')

        // Verify server started
        URL url = new URL("http://localhost:$port/selenium-server")
        boolean started = false
        while (!started) {
            if (errors) {
                fail('Failed to start Selenium server', errors[0])
            }

            log.debug("Trying connection to: $url")

            try {
                url.openConnection().content
                started = true
            }
            catch (Exception e) {
                // ignore
            }

            Thread.sleep(1000)
        }

        //
        // Use the Java client API to try and validate that it can actually
        // fire up a browser.  As just launching the server won't really
        // provide feedback if firefox (or whatever browser) isn't on the path/runnable.
        //
        
        if (verifyBrowser) {
            log.info("Verifying broweser configuration for: $verifyBrowser")
            
            def selenium = new DefaultSelenium('localhost', port, verifyBrowser, "http://localhost:$port/selenium-server")
            
            try {
                selenium.start()
                
                //
                // TODO: Try open?
                //
            }
            finally {
                selenium.stop()
            }
        }
        
        log.info('Selenium server started')
        
        if (!background) {
            log.info('Waiting for Selenium to shutdown...')
            t.join()
        }
    }

    /**
     * Create the user-extentions.js file to use, or null if it should not be installed.
     */
    private File createUserExtentionsFile() {
        if (!defaultUserExtensionsEnabled && userExtensions == null) {
            return null
        }
        
        def resolveResource = { name ->
            if (name == null) return null
            
            def url
            def file = new File(name)
            if (file.exists()) {
                url = file.toURL()
            }
            else {
                try {
                    url = new URL(name)
                }
                catch (MalformedURLException e) {
                    url = Thread.currentThread().contextClassLoader.getResource(name)
                }
            }
            
            if (!url) {
                fail("Could not resolve resource: $name")
            }
            
            log.debug("Resolved resource '$name' as: $url")
            
            return url
        }
        
        // File needs to be named 'user-extensions.js' or Selenium server will puke
        def file = new File(workingDirectory, 'user-extensions.js')
        if (file.exists()) {
            log.debug("Reusing previously generated file: $file")
            return file
        }

        def writer = file.newPrintWriter()
        
        if (defaultUserExtensionsEnabled) {
            def url = resolveResource(defaultUserExtensions)
            log.debug("Using defaults: $url")

            writer.println('//')
            writer.println("// Default user extentions from: $url")
            writer.println('//')
            writer << url.openStream()
        }

        if (userExtensions) {
            def url = resolveResource(userExtensions)
            log.debug("Using user extentions: $url")

            writer.println('//')
            writer.println("// User extentions from: $url")
            writer.println('//')
            writer << url.openStream()
        }

        writer.flush()
        writer.close()

        return file
    }
}
