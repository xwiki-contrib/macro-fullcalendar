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

import java.net.URL;
import java.util.Date;

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

    /**
     * Convert an iCal to a JSON.
     *
     * @param iCalStringURL the String representation of an iCal URL.
     * @return the JSON representation of a calendar.
     * @throws Exception if the retrieval of the iCal fails or if it contains malformed dates.
     */
    public String iCalToJSON(String iCalStringURL) throws Exception
    {
        return fullCalendarManager.iCalToJSON(iCalStringURL);
    }

    /**
     * Get the events from an iCal in a specified date interval. This method expands the recurring events.
     *
     * @param iCalStringURL the String representation of an iCal URL.
     * @param startDate the start of the interval of the returned calendar events.
     * @param endDate the end of the interval.
     * @return a JSON that contains a list of FullCalendar Event Objects.
     * @throws Exception if the retrieval of the iCal fails or if it contains malformed dates.
     * @since 2.3
     */
    public String getICalEvents(String iCalStringURL, Date startDate, Date endDate) throws Exception
    {
        return fullCalendarManager.getICalEvents(new URL(iCalStringURL), startDate, endDate);
    }
    /**
     * Get the events from an iCal in a specified date interval. This method expands the recurring events.
     *
     * @param iCalStringURL the String representation of an iCal URL.
     * @param startDate the start of the interval of the returned calendar events.
     * @param endDate the end of the interval.
     * @param collapse expands the recurring events if {@code false}, or collapses them into the parent otherwise.
     * @return a JSON that contains a list of FullCalendar Event Objects.
     * @throws Exception if the retrieval of the iCal fails or if it contains malformed dates.
     * @since 2.4.0
     */
    public String getICalEvents(String iCalStringURL, Date startDate, Date endDate, boolean collapse) throws Exception
    {
        return fullCalendarManager.getICalEvents(new URL(iCalStringURL), startDate, endDate, collapse);
    }
}
