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
    extends ServerMojoSupport
{
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
     * The file or resource to use for default user-extensions.js.
     *
     * @parameter default-value="org/codehaus/mojo/selenium/default-user-extensions.js"
     */
    String defaultUserExtensions

    /**
     * Enable or disable default user-extensions.js
     *
     * @parameter default-value="true"
     */
    boolean defaultUserExtensionsEnabled

    /**
     * Location of the user-extensions.js to load into the server.
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
     * Flag to control if we start Selenium RC in alwaysProxy mode or not. The alwaysProxy mode
     * forces all browser traffic through the proxy.
     *
     * @parameter default-value="false"
     */
    boolean alwaysProxy
    
    /**
     * Normally a fresh empty Firefox profile is created every time we launch.
     * You can specify a directory to make us copy your profile directory instead.
     *
     * @parameter
     */
    File firefoxProfileTemplate
    
    /**
     * Stops re-initialization and spawning of the browser between tests.
     *
     * @parameter default-value="false"
     */
    boolean browserSessionReuse
    
    /**
     * The location of the file to read the display properties.
     *
     * @parameter default-value="${project.build.directory}/selenium/display.properties"
     */
    File displayPropertiesFile
    
    /**
     * Sets the browser mode (e.g. "*iexplore" for all sessions, no matter what is passed to getNewBrowserSession).
     *
     * @parameter
     */
    String forcedBrowserMode
    
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
        
        def launcher = new ProcessLauncher(name: 'Selenium Server', background: background)
        
        launcher.process = {
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
                
                if (alwaysProxy) {
                    arg(value: '-alwaysProxy')
                }
                
                if (firefoxProfileTemplate) {
                    if (!firefoxProfileTemplate.exists()) {
                        log.warn("Missing Firefox profile template directory: $firefoxProfileTemplate")
                    }
                    arg(value: '-firefoxProfileTemplate')
                    arg(file: firefoxProfileTemplate)
                }
                
                if (browserSessionReuse) {
                    arg(value: '-browserSessionReuse')
                }
                
                if (forcedBrowserMode) {
                    arg(value: '-forcedBrowserMode')
                    arg(value: forcedBrowserMode)
                }
                
                // Maybe configure user extensions
                def file = createUserExtensionsFile()
                if (file) {
                    log.info("User extensions: $file")
                    arg(value: '-userExtensions')
                    arg(file: file)
                }
            }
        }
        
        URL url = new URL("http://localhost:$port/selenium-server")
        
        launcher.verifier = {
            log.debug("Trying connection to: $url")
            
            try {
                url.openConnection().content
                
                //
                // Use the Java client API to try and validate that it can actually
                // fire up a browser.  As just launching the server won't really
                // provide feedback if firefox (or whatever browser) isn't on the path/runnable.
                //
                
                if (verifyBrowser) {
                    log.info("Verifying broweser configuration for: $verifyBrowser")
                    
                    try {
                        def selenium = new DefaultSelenium('localhost', port, verifyBrowser, "http://localhost:$port/selenium-server")
                        
                        try {
                            selenium.start()
                        }
                        finally {
                            selenium.stop()
                        }
                    }
                    catch (Exception e) {
                        fail("Failed to verify browser: $verifyBrowser", e)
                    }
                }
                
                return true
            }
            catch (Exception e) {
                return false
            }
        }
        
        launcher.launch()
    }

    /**
     * Create the user-extensions.js file to use, or null if it should not be installed.
     */
    private File createUserExtensionsFile() {
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
            writer.println("// Default user extensions from: $url")
            writer.println('//')
            writer << url.openStream()
        }

        if (userExtensions) {
            def url = resolveResource(userExtensions)
            log.debug("Using user extensions: $url")

            writer.println('//')
            writer.println("// User extensions from: $url")
            writer.println('//')
            writer << url.openStream()
        }

        writer.flush()
        writer.close()

        return file
    }
}
