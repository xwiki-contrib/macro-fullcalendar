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

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.fullcalendar.FullCalendarManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.util.CompatibilityHints;

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
    private static final String T_VALUE = "T";

    private static final String JSON_KEY_ID = "id";

    private static final String JSON_KEY_START_DATE = "start";

    private static final String JSON_KEY_END_DATE = "end";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // FullCalendar will accept ISO8601 date strings written with hours, minutes, seconds, and milliseconds.
    private final DateFormat jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");

    @Inject
    private Logger logger;

    @Override
    public String iCalToJSON(String iCalStringURL) throws Exception
    {
        return getICalEvents(new URL(iCalStringURL), null, null);
    }

    @Override
    public String getICalEvents(URL iCalURL, java.util.Date intervalStart, java.util.Date intervalEnd)
        throws Exception
    {
        Date icalIntervalStart = intervalStart == null ? null : new Date(intervalStart);
        Date icalIntervalEnd = intervalEnd == null ? null : new Date(intervalEnd);
        CalendarBuilder builder = new CalendarBuilder();
        Calendar calendar = getCalendar(iCalURL, builder);

        TimeZoneRegistry timeZoneRegistry = builder.getRegistry();
        String timeZoneValue = getTimeZoneValue(calendar);
        TimeZone timeZone = timeZoneRegistry.getTimeZone(timeZoneValue);

        ArrayList<Object> jsonArrayList = new ArrayList<>();

        List<CalendarComponent> events = calendar.getComponents(net.fortuna.ical4j.model.Component.VEVENT);

        addEvents(events, timeZone, icalIntervalStart, icalIntervalEnd, jsonArrayList);
        return MAPPER.writeValueAsString(jsonArrayList);
    }

    private void addEvents(List<CalendarComponent> events, TimeZone timeZone, Date icalIntervalStart,
        Date icalIntervalEnd, ArrayList<Object> jsonArrayList) throws ParseException
    {
        for (CalendarComponent eventComponent : events) {
            VEvent event = (VEvent) eventComponent;
            Map<String, Object> jsonMap = new HashMap<>();

            addBasicEventProperties(jsonMap, event);

            String startDateValue = event.getStartDate() == null ? "" : event.getStartDate().getValue();
            String endDateValue = event.getEndDate() == null ? "" : event.getEndDate().getValue();

            // If either the start or end value has a "T" as part of the ISO8601 date string, allDay will become
            // false. Otherwise, it will be true.
            boolean allDay = startDateValue.contains(T_VALUE) || endDateValue.contains(T_VALUE);
            jsonMap.put("allDay", !allDay);

            DateTime startDateTime = new DateTime(startDateValue, timeZone);
            DateTime endDateTime = new DateTime(endDateValue, timeZone);
            // Do not add the event if it's not in the interval.
            long differenceInMillis = endDateTime.getTime() - startDateTime.getTime();

            // If the interval dates are null, maintain backwards compatibility.
            if (icalIntervalStart == null || icalIntervalEnd == null || areIntervalsIntersected(startDateTime,
                endDateTime, icalIntervalStart, icalIntervalEnd))
            {
                jsonMap.put(JSON_KEY_START_DATE, jsonDateFormat.format(startDateTime));
                jsonMap.put(JSON_KEY_END_DATE, jsonDateFormat.format(endDateTime));
                jsonArrayList.add(jsonMap);
            }

            // If the interval dates are null, we don't check for recurring events. Done in order to maintain backwards
            // compatibility.
            if (icalIntervalStart == null || icalIntervalEnd == null) {
                return;
            }
            addRecurringEvents(event, icalIntervalStart, icalIntervalEnd, jsonMap, timeZone, differenceInMillis,
                jsonArrayList);
        }
    }

    private static void addBasicEventProperties(Map<String, Object> jsonMap, VEvent event)
    {
        jsonMap.put(JSON_KEY_ID, event.getUid() == null ? "" : event.getUid().getValue());
        jsonMap.put("title", event.getSummary() == null ? "" : event.getSummary().getValue());

        // Non-standard fields in each Event Object. FullCalendar will not modify or delete these fields.
        jsonMap.put("description", event.getDescription() == null ? "" : event.getDescription().getValue());
        jsonMap.put("location", event.getLocation() == null ? "" : event.getLocation().getValue());
        jsonMap.put("status", event.getStatus() == null ? "" : event.getStatus().getValue());
    }

    private void addRecurringEvents(VEvent event, Date icalIntervalStart, Date icalIntervalEnd,
        Map<String, Object> jsonMap, TimeZone timeZone, long differenceInMillis, ArrayList<Object> jsonArrayList)
    {
        // Create the recurring events based on the existing RRule.
        RRule rRule = event.getProperty("rrule");
        if (rRule != null && rRule.getRecur() != null) {
            DateList recurringEventStartDates = rRule.getRecur()
                .getDates(event.getStartDate().getDate(), icalIntervalStart, icalIntervalEnd, Value.DATE);
            String groupId = String.format("%s_group", jsonMap.get(JSON_KEY_ID));
            for (int i = 0; i < recurringEventStartDates.size(); i++) {
                if (recurringEventStartDates.get(i).equals(event.getStartDate().getDate())) {
                    continue;
                }
                Map<String, Object> recurringEvent = new HashMap<>(jsonMap);

                recurringEvent.put(JSON_KEY_START_DATE,
                    jsonDateFormat.format(new DateTime(recurringEventStartDates.get(i), timeZone)));
                recurringEvent.put(JSON_KEY_END_DATE, jsonDateFormat.format(
                    new DateTime(new Date(recurringEventStartDates.get(i).getTime() + differenceInMillis), timeZone)));
                recurringEvent.put(JSON_KEY_ID, String.format("%s_%d", jsonMap.get(JSON_KEY_ID), i));
                recurringEvent.put("groupId", groupId);
                jsonArrayList.add(recurringEvent);
            }
        }
    }

    private boolean areIntervalsIntersected(Date intervalStart1, Date intervalEnd1, Date intervalStart2,
        Date intervalEnd2)
    {
        return (intervalEnd1.after(intervalStart2) && intervalEnd1.before(intervalEnd2))
            || (intervalStart1.after(intervalStart2) && intervalStart1.before(intervalEnd2));
    }

    private Calendar getCalendar(URL iCalURL, CalendarBuilder builder) throws Exception
    {
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);

        URLConnection conn = iCalURL.openConnection();
        InputStream is = conn.getInputStream();
        if (logger.isDebugEnabled()) {
            logger.debug("InputStream: {}", IOUtils.toString(is, StandardCharsets.UTF_8));
        }

        Calendar calendar = builder.build(is);
        logger.debug("Calendar: {}", calendar);

        return calendar;
    }

    private String getCalendarValue(Calendar calendar, String propertyName)
    {
        return calendar.getProperty(propertyName) == null ? "" : calendar.getProperty(propertyName).getValue();
    }

    private String getTimeZoneValue(Calendar calendar)
    {
        // Some calendars rely on X-WR-TIMEZONE property from the main component for defining the timeZone.
        String timeZoneValue = getCalendarValue(calendar, "X-WR-TIMEZONE");
        if (timeZoneValue.isEmpty()) {
            // Some calendars rely on TZID property from the VTIMEZONE component for defining the timeZone.
            VTimeZone vTimeZone = (VTimeZone) calendar.getComponent(net.fortuna.ical4j.model.Component.VTIMEZONE);
            if (vTimeZone != null) {
                timeZoneValue = vTimeZone.getTimeZoneId() == null ? "" : vTimeZone.getTimeZoneId().getValue();
            }
        }
        return timeZoneValue;
    }
}
