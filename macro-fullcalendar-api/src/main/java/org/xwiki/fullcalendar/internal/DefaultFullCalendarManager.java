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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.fullcalendar.FullCalendarManager;
import org.xwiki.velocity.tools.JSONTool;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.util.CompatibilityHints;

/**
 * Default implementation for {@link FullCalendarManager}.
 * 
 * @version $Id$
 * @since 2.1
 */
@Component
@Singleton
public class DefaultFullCalendarManager implements FullCalendarManager, Initializable
{
    private static final String T_VALUE = "T";

    private CalendarBuilder builder;

    @Inject
    private Logger logger;

    private TimeZoneRegistry timeZoneRegistry;

    @Override
    public void initialize() throws InitializationException
    {
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);
        builder = new CalendarBuilder();
        timeZoneRegistry = builder.getRegistry();
    }

    @Override
    public String iCalToJSON(String iCalStringURL) throws Exception
    {
        Calendar calendar = getCalendar(iCalStringURL);

        TimeZone timeZone = getTimeZone(calendar);

        ArrayList<Object> jsonArrayList = new ArrayList<Object>();

        // FullCalendar will accept ISO8601 date strings written with hours, minutes, seconds, and milliseconds.
        DateFormat jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");

        ComponentList<VEvent> events = calendar.getComponents(net.fortuna.ical4j.model.Component.VEVENT);

        for (VEvent event : events) {
            Map<String, Object> jsonMap = new HashMap<String, Object>();
            jsonMap.put("id", event.getUid() == null ? "" : event.getUid().getValue());
            jsonMap.put("title", event.getSummary() == null ? "" : event.getSummary().getValue());

            String startDateValue = event.getStartDate() == null ? "" : event.getStartDate().getValue();
            String endDateValue = event.getEndDate() == null ? "" : event.getEndDate().getValue();

            // If either the start or end value has a "T" as part of the ISO8601 date string, allDay will become
            // false. Otherwise, it will be true.
            boolean allDay = startDateValue.contains(T_VALUE) || endDateValue.contains(T_VALUE);
            jsonMap.put("allDay", !allDay);

            DateTime startDateTime = new DateTime(startDateValue, timeZone);
            jsonMap.put("start", jsonDateFormat.format(startDateTime));

            DateTime endDateTime = new DateTime(endDateValue, timeZone);
            jsonMap.put("end", jsonDateFormat.format(endDateTime));

            // Non-standard fields in each Event Object. FullCalendar will not modify or delete these fields.
            jsonMap.put("description", event.getDescription() == null ? "" : event.getDescription().getValue());
            jsonMap.put("location", event.getLocation() == null ? "" : event.getLocation().getValue());
            jsonMap.put("status", event.getStatus() == null ? "" : event.getStatus().getValue());

            jsonArrayList.add(jsonMap);
        }

        return new JSONTool().serialize(jsonArrayList);
    }

    private Calendar getCalendar(String iCalStringURL) throws Exception
    {
        URL iCalURL = new URL(iCalStringURL);
        URLConnection conn = iCalURL.openConnection();
        InputStream is = conn.getInputStream();
        Calendar calendar = builder.build(is);

        if (logger.isDebugEnabled()) {
            logger.debug("InputStream: {}", IOUtils.toString(is, StandardCharsets.UTF_8.name()));
            logger.debug("Calendar: {}", calendar);
        }

        return calendar;
    }

    private String getCalendarValue(Calendar calendar, String propertyName)
    {
        return calendar.getProperty(propertyName) == null ? "" : calendar.getProperty(propertyName).getValue();
    }

    private TimeZone getTimeZone(Calendar calendar)
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
        return timeZoneRegistry.getTimeZone(timeZoneValue);
    }
}
