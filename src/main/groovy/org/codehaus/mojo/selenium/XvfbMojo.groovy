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

/**
 * Starts an Xvfb instance suitable for handling X11 displays for headless systems.
 * Use this in combonation with the <tt>start-server</tt> goal to allow browsers
 * to be launched on headless unix systems.
 *
 * @goal xvfb
 *
 * @version $Id$
 */
class XvfbMojo
    extends GroovyMojoSupport
{
    /**
     * The Xvfb command to execute.
     *
     * @parameter default-value="Xvfb"
     * @required
     */
    String executable
    
    /**
     * The X11 display to use.  Default value is <tt>:1</tt>.
     *
     * @parameter
     */
    String display
    
    //
    // TODO: See if we need default options for Firefox [ '-screen', '0', '1024x768x24' ]
    //
    
    /**
     * A list of additional options to pass to the Xvfb process.
     *
     * @parameter
     */
    String[] options
    
    /**
     * The location of the file to write the display properties which will be picked up
     * by the <tt>start-server</tt> goal.
     *
     * @parameter default-value="${project.build.directory}/selenium/display.properties"
     * @required
     */
    File displayPropertiesFile
    
    /**
     * Enable logging mode.
     *
     * @parameter default-value="true"
     */
    boolean logOutput

    /**
     * The file that Xvfb output will be written to.
     *
     * @parameter default-value="${project.build.directory}/selenium/xvfb.log"
     */
    File logFile
    
    /**
     * Flag to control if we background the process or block Maven execution.
     *
     * @parameter default-value="true"
     */
    boolean background

    //
    // Mojo
    //

    void execute() {
        log.info('Starting Xvfb...')
        
        // Figure out what the display number is, and generate the properties file
        if (!display) {
            //
            // TODO: Try and detect which screen to use if not explicity configured,
            //
            
            //
            // Normally, the first X11 display is on port 6000, the next on port 6001,
            // which get abbreviated as :0, :1 and so on.
            //
            
            //
            // HACK: For now just default to :1
            //
            display = ':1'
        }
        log.info("Using display: $display")
        
        // Write out the display properties so that the start-server goal can pick it up
        ant.mkdir(dir: displayPropertiesFile.parentFile)
        def props = new Properties()
        props['DISPLAY'] = "$display"
        props.store(displayPropertiesFile, 'Xvfb Display Properties')
        
        if (logOutput) {
            ant.mkdir(dir: logFile.parentFile)
        }
        
        // Holds any exception that was thrown during startup
        def errors = []
        
        def runner = {
            try {
                ant.exec(executable: executable, failonerror: true) {
                    if (logOutput) {
                        log.info("Redirecting output to: $logFile")
                        redirector(output: logFile)
                    }
                    
                    // Set the display
                    arg(value: display)
                    
                    // Add extra options
                    if (options) {
                        options.each {
                            arg(value: it)
                        }
                    }
                }
            }
            catch (Exception e) {
                errors << e
            }
        }
        
        // Start the server int a seperate thread
        Thread t = new Thread(runner, 'Xvfb Runner')
        t.start()
        
        //
        // TODO: Verify that Xvfb is up and running... how?
        //
        //       Could potentialy use the escher X11 library, though its GPL (making this plugin GPL)
        //       and doesn't appear to be in any Maven repo as of yet (that I could find).
        //
        
        log.info('Xvfb started')
        
        if (!background) {
            log.info('Waiting for Xvfb to shutdown...')
            t.join()
        }
    }
}
