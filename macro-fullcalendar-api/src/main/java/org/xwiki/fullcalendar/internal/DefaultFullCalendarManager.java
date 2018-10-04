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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.fullcalendar.FullCalendarManager;
import org.xwiki.velocity.tools.JSONTool;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.component.CalendarComponent;
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

    @Override
    public String getJSON(String iCalStringURL) throws Exception
    {
        URL iCalURL = new URL(iCalStringURL);

        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);
        CalendarBuilder builder = new CalendarBuilder();
        URLConnection conn = iCalURL.openConnection();
        InputStream in = conn.getInputStream();
        Calendar calendar = builder.build(in);

        TimeZoneRegistry timeZoneRegistry = builder.getRegistry();
        TimeZone timeZone = timeZoneRegistry.getTimeZone(calendar.getProperty("X-WR-TIMEZONE").getValue());

        ArrayList<Object> jsonArrayList = new ArrayList<Object>();

        DateFormat jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");

        for (CalendarComponent component : calendar.getComponents()) {
            Map<String, Object> jsonMap = new HashMap<String, Object>();
            jsonMap.put("id", component.getProperty("UID").getValue());
            jsonMap.put("title", component.getProperty("SUMMARY").getValue());

            DateTime startDateTime = new DateTime(component.getProperty("DTSTART").getValue(), timeZone);
            jsonMap.put("start", jsonDateFormat.format(startDateTime));

            DateTime endDateTime = new DateTime(component.getProperty("DTEND").getValue(), timeZone);
            jsonMap.put("end", jsonDateFormat.format(endDateTime));
            jsonArrayList.add(jsonMap);
        }

        return new JSONTool().serialize(jsonArrayList);
    }
}
