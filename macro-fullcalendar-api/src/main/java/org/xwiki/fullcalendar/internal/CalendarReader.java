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
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.stability.Unstable;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.util.CompatibilityHints;

/**
 * Importer helper class used to process a {@link Calendar} from a URL or a file.
 *
 * @version $Id$
 * @since 2.4.0
 */
@Unstable
public class CalendarReader
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CalendarReader.class);

    private Calendar calendar;

    private CalendarBuilder builder;

    /**
     * Read the {@link Calendar} from a given URL.
     *
     * @param iCalURL the String representation of an iCal URL.
     * @throws Exception if the retrieval of the iCal fails or if it contains malformed dates.
     */
    CalendarReader(URL iCalURL) throws Exception
    {
        builder = new CalendarBuilder();
        this.processCalendarFromURL(iCalURL);
    }

    /**
     * Read the {@link Calendar} from a file as a byte array.
     *
     * @param iCalFile content of an iCal file.
     * @throws Exception if the file format is incorrect, or if it contains malformed dates.
     */
    CalendarReader(byte[] iCalFile) throws Exception
    {
        builder = new CalendarBuilder();
        this.processCalendarFromFile(iCalFile);
    }

    /**
     * Get the calendar events.
     *
     * @return a {@link List} with the calendar events.
     */
    public List<CalendarComponent> getEvents()
    {
        return calendar.getComponents(net.fortuna.ical4j.model.Component.VEVENT);
    }

    /**
     * Get the calendar {@link TimeZone}.
     *
     * @return the {@link TimeZone} of the calendar.
     */
    public TimeZone getTimeZone()
    {
        String timeZoneValue = getTimeZoneValue(calendar);
        if (timeZoneValue.isEmpty()) {
            return builder.getRegistry().getTimeZone(TimeZone.getDefault().getID());
        }
        return builder.getRegistry().getTimeZone(timeZoneValue);
    }

    private void processCalendarFromURL(URL iCalURL) throws Exception
    {
        this.calendar = getCalendarFromURL(iCalURL, builder);
    }

    private void processCalendarFromFile(byte[] iCalFile) throws Exception
    {
        String importedFileContent = new String(iCalFile);
        StringReader stringReader = new StringReader(importedFileContent);
        this.calendar = builder.build(stringReader);
    }

    private Calendar getCalendarFromURL(URL iCalURL, CalendarBuilder builder) throws Exception
    {
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);

        URLConnection conn = iCalURL.openConnection();
        InputStream is = conn.getInputStream();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("InputStream: {}", IOUtils.toString(is, StandardCharsets.UTF_8));
        }

        Calendar calendarFromURL = builder.build(is);
        LOGGER.debug("Calendar: {}", calendarFromURL);

        return calendarFromURL;
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

    private String getCalendarValue(Calendar calendar, String propertyName)
    {
        return calendar.getProperty(propertyName) == null ? "" : calendar.getProperty(propertyName).getValue();
    }
}
