package org.codehaus.mojo.selenium;

/*
 * Copyright 2006 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Simple helper that holds an object.
 *
 * @author Emmanuel Venisse <evenisse@codehaus.org>
 * @version $Id$
 */
public class ObjectHolder
{
    private Object object;

    public ObjectHolder()
    {
        super();
    }

    public ObjectHolder( final Object object )
    {
        set( object );
    }

    public String toString()
    {
        return String.valueOf( get() );
    }

    public void set( final Object object )
    {
        this.object = object;
    }

    public Object get()
    {
        return object;
    }

    public boolean isSet()
    {
        return get() != null;
    }
}
