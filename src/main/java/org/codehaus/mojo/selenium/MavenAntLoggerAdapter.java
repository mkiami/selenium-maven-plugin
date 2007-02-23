/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
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

package org.codehaus.mojo.selenium;

import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;

import java.io.PrintStream;

/**
 * Adapts Ant logging to Maven Logging.
 *
 * @version $Id$
 */
public class MavenAntLoggerAdapter
    extends DefaultLogger
{
    protected Log log;

    public MavenAntLoggerAdapter( final Log log )
    {
        super();

        this.log = log;
    }

    protected void printMessage( final String message, final PrintStream stream, final int priority )
    {
        switch ( priority )
        {
            case Project.MSG_ERR:
                log.error( message );
                break;

            case Project.MSG_WARN:
                log.warn( message );
                break;

            case Project.MSG_INFO:
                log.info( message );
                break;

            case Project.MSG_VERBOSE:
            case Project.MSG_DEBUG:
                log.debug( message );
                break;
        }
    }
}
