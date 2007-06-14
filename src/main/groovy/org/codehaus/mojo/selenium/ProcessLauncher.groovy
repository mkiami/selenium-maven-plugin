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

import java.util.Timer

//
// TODO: Move to groovy-mojo-support, as this is the type of thing that I use all over the place
//

/**
 * Helper to execute a process and perform some verification logic to determine if the process is up or not.
 *
 * @version $Id$
 */
class ProcessLauncher
{
    //
    // TODO: Use logging
    //
    
    String name
    
    Closure process
    
    Closure verifier
    
    int verifyWaitDelay = 1000
    
    int timeout = -1
    
    boolean background = false
    
    def launch() {
        assert process
        assert name
        
        def errors = []
        
        def runner = {
            try {
                process.call()
            }
            catch (Exception e) {
                errors << e
            }
        }
        
        Thread t = new Thread(runner, "$name Runner")
        
        println "Launching $name"
        t.start()
        
        if (verifier) {
            def timer = new Timer("$name Timer", true)
            
            def timedOut = false
            
            if (timeout > 0) {
                timer.runAfter(timeout * 1000, {
                    timedOut = true
                })
            }
            
            def started = false
            
            println "Waiting for ${name}..."
            
            while (!started) {
                if (timedOut) {
                    throw new Exception("Unable to verify if $name was started in the given time ($timeout seconds)")
                }
                
                if (errors) {
                    throw new Exception("Failed to start: $name", errors[0])
                }
                
                if (verifier.call()) {
                    started = true
                }
                else {
                    Thread.sleep(verifyWaitDelay)
                }
            }
            
            //
            // TODO: Should call TimerTask.cancel() here, but we don't have easy access to it via Timer.runAfter()
            //
        }
        
        println "$name started"
        
        if (!background) {
            println "Waiting for $name to shutdown..."
            
            t.join()
            
            println "$name has shutdown"
        }
    }
}
