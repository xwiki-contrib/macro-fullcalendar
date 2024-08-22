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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.fullcalendar.FullCalendarManager;
import org.xwiki.fullcalendar.model.CalendarEvent;
import org.xwiki.fullcalendar.model.RecurrentEventModification;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.WeekDayList;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.RecurrenceId;

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

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DEFAULT_TIME = "T000000";

    private static final String GROUP_ID_FORMAT = "%s_group";

    private static final Collection<WeekDay> WEEK_DAYS = new HashSet<>();

    private static final Map<String, Long> TIME_PERIODS = new HashMap<>();

    private static final long YEARLY_DURATION = TimeUnit.DAYS.toMillis(365);

    static {
        WEEK_DAYS.add(WeekDay.MO);
        WEEK_DAYS.add(WeekDay.TU);
        WEEK_DAYS.add(WeekDay.WE);
        WEEK_DAYS.add(WeekDay.TH);
        WEEK_DAYS.add(WeekDay.FR);

        TIME_PERIODS.put("DAILY", TimeUnit.DAYS.toMillis(1));
        TIME_PERIODS.put("WEEKLY", TimeUnit.DAYS.toMillis(7));
        TIME_PERIODS.put("MONTHLY", TimeUnit.DAYS.toMillis(30));
        TIME_PERIODS.put("YEARLY", YEARLY_DURATION);
    }

    @Override
    public String iCalToJSON(String iCalStringURL) throws Exception
    {
        return getICalEvents(new URL(iCalStringURL), null, null);
    }

    @Override
    public String getICalEvents(URL iCalURL, java.util.Date intervalStart, java.util.Date intervalEnd, boolean collapse)
        throws Exception
    {
        Date icalIntervalStart = intervalStart == null ? null : new Date(intervalStart);
        Date icalIntervalEnd = intervalEnd == null ? null : new Date(intervalEnd);
        ArrayList<CalendarEvent> jsonArrayList = new ArrayList<>();

        CalendarReader calendarReader = new CalendarReader(iCalURL);
        List<CalendarComponent> sortedEvents = getSortedEvents(calendarReader.getEvents());

        addEvents(sortedEvents, calendarReader.getTimeZone(), icalIntervalStart, icalIntervalEnd, jsonArrayList,
            collapse);
        return MAPPER.writeValueAsString(jsonArrayList);
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
        Date icalIntervalStart = intervalStart == null ? null : new Date(intervalStart);
        Date icalIntervalEnd = intervalEnd == null ? null : new Date(intervalEnd);
        ArrayList<CalendarEvent> calendarEventsJSON = new ArrayList<>();

        CalendarReader calendarReader = new CalendarReader(iCalFile);
        List<CalendarComponent> sortedEvents = getSortedEvents(calendarReader.getEvents());

        addEvents(sortedEvents, calendarReader.getTimeZone(), icalIntervalStart, icalIntervalEnd, calendarEventsJSON,
            collapse);
        return calendarEventsJSON;
    }

    private void addEvents(List<CalendarComponent> events, TimeZone timeZone, Date icalIntervalStart,
        Date icalIntervalEnd, ArrayList<CalendarEvent> jsonArrayList, boolean collapse) throws Exception
    {
        for (CalendarComponent eventComponent : events) {
            VEvent event = (VEvent) eventComponent;
            CalendarEvent jsonMap = new CalendarEvent();

            addBasicEventProperties(jsonMap, event);
            addEventPeriod(event, jsonMap, timeZone);

            // If the interval dates are null and the collapse flag is false, we don't check for recurring events. Done
            // in order to maintain backwards compatibility.
            if (!collapse && (icalIntervalStart == null || icalIntervalEnd == null)) {
                jsonArrayList.add(jsonMap);
                continue;
            }

            // Check if there is a RRule.
            RRule rRule = event.getProperty(Property.RRULE);
            RecurrenceId recurrenceId = event.getProperty(Property.RECURRENCE_ID);
            if (rRule != null && rRule.getRecur() != null) {
                handleRecurrentEvent(icalIntervalStart, icalIntervalEnd, jsonArrayList, collapse, rRule, event,
                    jsonMap);
            } else if (collapse && recurrenceId != null) {
                addRecurrentModifiedInstance(jsonMap, timeZone, jsonArrayList, recurrenceId);
            } else if (maybeAddEvent(icalIntervalStart, icalIntervalEnd, jsonMap)) {
                jsonArrayList.add(jsonMap);
            }
        }
    }

    private boolean maybeAddEvent(Date icalIntervalStart, Date icalIntervalEnd, CalendarEvent jsonMap)
    {
        return icalIntervalStart == null || icalIntervalEnd == null || areIntervalsIntersected(
            new Date(jsonMap.getStart()), new Date(jsonMap.getEnd()), icalIntervalStart, icalIntervalEnd);
    }

    private void handleRecurrentEvent(Date icalIntervalStart, Date icalIntervalEnd,
        ArrayList<CalendarEvent> jsonArrayList, boolean collapse, RRule rRule, VEvent event,
        CalendarEvent jsonMap)
    {
        if (collapse) {
            boolean areIntervalDatesEmpty = icalIntervalStart == null || icalIntervalEnd == null;
            Recur recur = rRule.getRecur();
            if (!areIntervalDatesEmpty && recur.getDates(event.getStartDate().getDate(), icalIntervalStart,
                icalIntervalEnd, Value.DATE_TIME).isEmpty())
            {
                return;
            }
            addRecurringEventsCollapsed(jsonMap, jsonArrayList, recur);
        } else {
            long differenceInMillis = jsonMap.getDatesDifference();
            DateList recurringEventStartDates = rRule.getRecur()
                .getDates(event.getStartDate().getDate(), icalIntervalStart, icalIntervalEnd, Value.DATE_TIME);
            addRecurringEventsExpanded(jsonMap, differenceInMillis, jsonArrayList, recurringEventStartDates);
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

    private void addEventPeriod(VEvent event, CalendarEvent jsonMap, TimeZone timeZone) throws Exception
    {
        String startDateValue = event.getStartDate() == null ? "" : event.getStartDate().getValue();
        String endDateValue = event.getEndDate() == null ? "" : event.getEndDate().getValue();

        // If either the start or end value has a "T" as part of the ISO8601 date string, allDay will become
        // false. Otherwise, it will be true.
        boolean allDay = !(startDateValue.contains(T_VALUE) || endDateValue.contains(T_VALUE));
        jsonMap.setAllDay(allDay);
        jsonMap.setRecurrent(0);
        DateTime startDateTime;
        DateTime endDateTime;
        if (!allDay) {
            startDateTime = new DateTime(startDateValue, timeZone);
            endDateTime = new DateTime(endDateValue, timeZone);
        } else {
            startDateTime = new DateTime(startDateValue + DEFAULT_TIME);
            endDateTime = new DateTime(endDateValue + DEFAULT_TIME);
        }

        jsonMap.setStart(startDateTime);
        jsonMap.setEnd(endDateTime);
    }

    private void addRecurringEventsExpanded(CalendarEvent jsonMap, long differenceInMillis,
        ArrayList<CalendarEvent> jsonArrayList, DateList recurringEventStartDates)
    {
        String groupId = String.format(GROUP_ID_FORMAT, jsonMap.getId());
        for (int i = 0; i < recurringEventStartDates.size(); i++) {
            CalendarEvent recurringEvent = new CalendarEvent(jsonMap);

            recurringEvent.setStart(recurringEventStartDates.get(i));
            recurringEvent.setEnd(new DateTime(recurringEventStartDates.get(i).getTime() + differenceInMillis));
            recurringEvent.setId(String.format("%s_%d", jsonMap.getId(), i));
            recurringEvent.setGroupId(groupId);
            jsonArrayList.add(recurringEvent);
        }
    }

    private void addRecurringEventsCollapsed(CalendarEvent jsonMap, ArrayList<CalendarEvent> jsonArrayList,
        Recur recur)
    {
        String groupId = String.format(GROUP_ID_FORMAT, jsonMap.getId());

        setRecurrenceEndDate(jsonMap, recur);
        jsonMap.setRecurrent(1);
        String frequency = recur.getFrequency().name();
        String eventFrequency = getEventFrequency(recur, frequency);
        jsonMap.setRecurrenceFreq(eventFrequency);
        jsonMap.setGroupId(groupId);
        jsonArrayList.add(jsonMap);
    }

    private static void setRecurrenceEndDate(CalendarEvent jsonMap, Recur recur)
    {
        int recurCount = recur.getCount();
        if (recur.getUntil() != null) {
            jsonMap.setRecEndDate(recur.getUntil());
        } else if (recurCount != -1) {
            jsonMap.setRecEndDate(new DateTime(
                jsonMap.getStart().getTime() + recurCount * TIME_PERIODS.getOrDefault(recur.getFrequency().name(),
                    YEARLY_DURATION)));
        } else {
            // Set end date of recurrence to five years from now.
            jsonMap.setRecEndDate(new DateTime(jsonMap.getStart().getTime() + 5 * YEARLY_DURATION));
        }
    }

    private String getEventFrequency(Recur recur, String frequency)
    {
        if (frequency.equalsIgnoreCase("weekly")) {
            if (isEveryWorkday(recur.getDayList())) {
                return "WORKDAYS";
            }
            return recur.getInterval() == 2 ? "BIWEEKLY" : frequency;
        } else if (frequency.equalsIgnoreCase("monthly")) {
            return recur.getInterval() == 3 ? "QUARTERLY" : frequency;
        }
        return frequency;
    }

    private boolean isEveryWorkday(WeekDayList byDayValue)
    {
        if (byDayValue.isEmpty()) {
            return false;
        }
        return byDayValue.containsAll(WEEK_DAYS) && byDayValue.size() == WEEK_DAYS.size();
    }

    private void addRecurrentModifiedInstance(CalendarEvent jsonMap, TimeZone timeZone,
        ArrayList<CalendarEvent> jsonArrayList, RecurrenceId recurrenceId) throws Exception
    {
        RecurrentEventModification eventModification = getRecurrentEventModification(jsonMap, timeZone, recurrenceId);

        Optional<CalendarEvent> optionalModifiedEvent =
            jsonArrayList.stream().filter(e -> e.getId().equals(jsonMap.getId())).findFirst();

        optionalModifiedEvent.ifPresent(calendarEvent -> calendarEvent.addModifiedEvent(eventModification));
    }

    private RecurrentEventModification getRecurrentEventModification(CalendarEvent jsonMap, TimeZone timeZone,
        RecurrenceId recurrenceId) throws Exception
    {
        RecurrentEventModification eventModification = new RecurrentEventModification();
        DateTime originalDateTime;
        if (jsonMap.isAllDay()) {
            originalDateTime = new DateTime(recurrenceId.getValue() + DEFAULT_TIME);
        } else {
            originalDateTime = new DateTime(recurrenceId.getValue(), timeZone);
        }
        eventModification.setOriginalDate(originalDateTime);
        eventModification.setModifiedTitle(jsonMap.getTitle());
        eventModification.setModifiedDescription(jsonMap.getDescription());
        eventModification.setModifiedStartDate(jsonMap.getStart());
        eventModification.setModifiedEndDate(jsonMap.getEnd());
        return eventModification;
    }

    private void addBasicEventProperties(CalendarEvent jsonMap, VEvent event)
    {
        jsonMap.setId(event.getUid() == null ? "" : event.getUid().getValue());
        jsonMap.setTitle(event.getSummary() == null ? "" : event.getSummary().getValue());

        // Non-standard fields in each Event Object. FullCalendar will not modify or delete these fields.
        jsonMap.setDescription(event.getDescription() == null ? "" : event.getDescription().getValue());
        jsonMap.setLocation(event.getLocation() == null ? "" : event.getLocation().getValue());
        jsonMap.setStatus(event.getStatus() == null ? "" : event.getStatus().getValue());
    }

    private boolean areIntervalsIntersected(Date intervalStart1, Date intervalEnd1, Date intervalStart2,
        Date intervalEnd2)
    {
        return (intervalEnd1.after(intervalStart2) && intervalEnd1.before(intervalEnd2)) || (
            intervalStart1.after(intervalStart2) && intervalStart1.before(intervalEnd2));
    }
}
