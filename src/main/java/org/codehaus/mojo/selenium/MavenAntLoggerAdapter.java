package org.codehaus.mojo.selenium;

import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;

import java.io.PrintStream;

/**
 * Adapts Ant logging to Maven Logging.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
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
