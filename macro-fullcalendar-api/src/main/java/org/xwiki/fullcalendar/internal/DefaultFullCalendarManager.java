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
package org.xwiki.fullcalendar.internal;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.fullcalendar.FullCalendarManager;
import org.xwiki.fullcalendar.internal.util.DateProcessor;
import org.xwiki.fullcalendar.internal.util.CalendarReader;
import org.xwiki.fullcalendar.internal.util.EventProcessor;
import org.xwiki.fullcalendar.internal.util.RecurrenceProcessor;
import org.xwiki.fullcalendar.model.CalendarEvent;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;

/**
 * Default implementation for {@link FullCalendarManager}.
 *
 * @version $Id$
 * @since 2.1
 */
@Component
@Singleton
public class DefaultFullCalendarManager implements FullCalendarManager
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    private RecurrenceProcessor recurrenceProcessor;

    @Inject
    private EventProcessor eventProcessor;

    private DateProcessor util;

    @Override
    public String iCalToJSON(String iCalStringURL) throws Exception
    {
        return getICalEvents(new URL(iCalStringURL), null, null);
    }

    @Override
    public String getICalEvents(URL iCalURL, java.util.Date intervalStart, java.util.Date intervalEnd, boolean collapse)
        throws Exception
    {
        CalendarReader calendarReader = new CalendarReader(iCalURL);
        ArrayList<CalendarEvent> calendarEventsJSON =
            getCalendarEvents(intervalStart, intervalEnd, collapse, calendarReader);
        return MAPPER.writeValueAsString(calendarEventsJSON);
    }

    @Override
    public String getICalEvents(URL iCalURL, java.util.Date intervalStart, java.util.Date intervalEnd) throws Exception
    {
        return getICalEvents(iCalURL, intervalStart, intervalEnd, false);
    }

    @Override
    public List<CalendarEvent> getICalEventsFromFile(byte[] iCalFile, java.util.Date intervalStart,
        java.util.Date intervalEnd, boolean collapse) throws Exception
    {
        CalendarReader calendarReader = new CalendarReader(iCalFile);
        ArrayList<CalendarEvent> calendarEventsJSON =
            getCalendarEvents(intervalStart, intervalEnd, collapse, calendarReader);
        return calendarEventsJSON;
    }

    private ArrayList<CalendarEvent> getCalendarEvents(Date intervalStart, Date intervalEnd, boolean collapse,
        CalendarReader calendarReader) throws Exception
    {
        ZoneId zoneId = calendarReader.getTimeZone().toZoneId();
        util = new DateProcessor(zoneId);
        LocalDateTime icalIntervalStart = intervalStart == null ? null : util.toLocalDateTime(intervalStart);
        LocalDateTime icalIntervalEnd = intervalEnd == null ? null : util.toLocalDateTime(intervalEnd);
        ArrayList<CalendarEvent> calendarEventsJSON = new ArrayList<>();
        List<CalendarComponent> sortedEvents = getSortedEvents(calendarReader.getEvents());
        addEvents(sortedEvents, zoneId, icalIntervalStart, icalIntervalEnd, calendarEventsJSON, collapse);
        return calendarEventsJSON;
    }

    private void addEvents(List<CalendarComponent> events, ZoneId zoneId, LocalDateTime icalIntervalStart,
        LocalDateTime icalIntervalEnd, ArrayList<CalendarEvent> jsonArrayList, boolean collapse)
    {
        for (CalendarComponent eventComponent : events) {
            VEvent event = (VEvent) eventComponent;
            CalendarEvent jsonMap = new CalendarEvent();
            if (!eventProcessor.addEventPeriod(event, jsonMap, zoneId)) {
                continue;
            }
            eventProcessor.addBasicEventProperties(jsonMap, event);
            // If the interval dates are null and the collapse flag is false, we don't check for recurring events. Done
            // in order to maintain backwards compatibility.
            if (!collapse && (icalIntervalStart == null || icalIntervalEnd == null)) {
                jsonArrayList.add(jsonMap);
                continue;
            }
            recurrenceProcessor.checkRRule(zoneId, icalIntervalStart, icalIntervalEnd, jsonArrayList, collapse, event,
                jsonMap);
        }
    }

    /**
     * Sort the events to start with the recurrent ones, so that any modified instance of a recurrence is added
     * correctly.
     */
    private List<CalendarComponent> getSortedEvents(List<CalendarComponent> events)
    {
        return events.stream().sorted((c1, c2) -> {
            boolean c1HasRRule = c1.getProperty(Property.RRULE) != null;
            boolean c2HasRRule = c2.getProperty(Property.RRULE) != null;

            if (c1HasRRule && !c2HasRRule) {
                return -1;
            } else if (!c1HasRRule && c2HasRRule) {
                return 1;
            } else {
                return 0;
            }
        }).collect(Collectors.toList());
    }
}
