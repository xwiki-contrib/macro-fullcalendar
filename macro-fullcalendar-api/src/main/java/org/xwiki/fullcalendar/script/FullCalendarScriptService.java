/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.fullcalendar.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.fullcalendar.FullCalendarManager;
import org.xwiki.script.service.ScriptService;

/**
 * Exposes simplified APIs to perform calendar operations.
 * 
 * @version $Id$
 * @since 2.1
 */
@Component
@Named("fullcalendar")
@Singleton
public class FullCalendarScriptService implements ScriptService
{
    @Inject
    private FullCalendarManager fullCalendarManager;

    public String iCalToJSON(String iCalStringURL) throws Exception
    {
        return fullCalendarManager.iCalToJSON(iCalStringURL);
    }
}
