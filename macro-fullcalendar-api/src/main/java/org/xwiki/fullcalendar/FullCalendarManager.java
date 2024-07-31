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
package org.xwiki.fullcalendar;

import java.net.URL;
import java.util.Date;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.fullcalendar.model.MoccaCalendarEvent;
import org.xwiki.stability.Unstable;

/**
 * Manages Full calendar macro.
 *
 * @version $Id$
 * @since 2.1
 */
@Role
public interface FullCalendarManager
{
    /**
     * Convert an iCal to a JSON.
     *
     * @param iCalStringURL the String representation of an iCal URL.
     * @return the JSON representation of a calendar.
     * @throws Exception if the retrieval of the iCal fails or if it contains malformed dates.
     */
    String iCalToJSON(String iCalStringURL) throws Exception;

    /**
     * Get the events from an iCal in a specified date interval. This method expands the recurring events.
     *
     * @param iCalURL the String representation of an iCal URL.
     * @param intervalStart the start of the interval of the returned calendar events.
     * @param intervalEnd the end of the interval.
     * @return a JSON that contains a list of FullCalendar Event Objects.
     * @throws Exception if the retrieval of the iCal fails or if it contains malformed dates.
     * @since 2.3
     */
    String getICalEvents(URL iCalURL, Date intervalStart, Date intervalEnd) throws Exception;

    /**
     * Get the events from an iCal in a specified date interval.
     *
     * @param iCalURL the String representation of an iCal URL.
     * @param intervalStart the start of the interval of the returned calendar events.
     * @param intervalEnd the end of the interval.
     * @param collapse expands the recurring events if {@code true}, or collapses them into the parent otherwise.
     * @return a JSON that contains a list of FullCalendar Event Objects.
     * @throws Exception if the retrieval of the iCal fails or if it contains malformed dates.
     * @since 2.4.0
     */
    @Unstable
    String getICalEvents(URL iCalURL, Date intervalStart, Date intervalEnd, boolean collapse)
        throws Exception;

    /**
     * Get the events from an iCal file.
     * @param iCalFile content of an iCal file.
     * @param collapse expands the recurring events if {@code true}, or collapses them into the parent otherwise.
     * @return the {@link List} of events from the iCal file.
     * @throws Exception if the file format is incorrect, or if it contains malformed dates.
     * @since 2.4.0
     */
    @Unstable
    List<MoccaCalendarEvent> getICalEventsFromFile(byte[] iCalFile, boolean collapse) throws Exception;
}
