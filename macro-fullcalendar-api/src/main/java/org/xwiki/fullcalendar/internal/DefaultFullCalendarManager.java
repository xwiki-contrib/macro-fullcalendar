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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.fullcalendar.FullCalendarManager;
import org.xwiki.fullcalendar.model.MoccaCalendarEvent;
import org.xwiki.fullcalendar.model.RecurrentEventModification;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
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

    @Inject
    private Provider<CalendarProcessor> calendarProcessorProvider;

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

        ArrayList<MoccaCalendarEvent> jsonArrayList = new ArrayList<>();
        CalendarProcessor calendarProcessor = calendarProcessorProvider.get();

        calendarProcessor.processCalendarFromURL(iCalURL);
        addEvents(calendarProcessor.getEvents(), calendarProcessor.getTimeZone(), icalIntervalStart, icalIntervalEnd,
            jsonArrayList, collapse);
        return MAPPER.writeValueAsString(jsonArrayList);
    }

    @Override
    public String getICalEvents(URL iCalURL, java.util.Date intervalStart, java.util.Date intervalEnd) throws Exception
    {
        return getICalEvents(iCalURL, intervalStart, intervalEnd, false);
    }

    @Override
    public List<MoccaCalendarEvent> getICalEventsFromFile(byte[] iCalFile, boolean collapse) throws Exception
    {
        ArrayList<MoccaCalendarEvent> calendarEventsJSON = new ArrayList<>();
        CalendarProcessor calendarProcessor = calendarProcessorProvider.get();

        calendarProcessor.processCalendarFromFile(iCalFile);
        addEvents(calendarProcessor.getEvents(), calendarProcessor.getTimeZone(), null, null, calendarEventsJSON,
            collapse);

        return calendarEventsJSON;
    }

    private void addEvents(List<CalendarComponent> events, TimeZone timeZone, Date icalIntervalStart,
        Date icalIntervalEnd, ArrayList<MoccaCalendarEvent> jsonArrayList, boolean collapse) throws Exception
    {
        for (CalendarComponent eventComponent : events) {
            VEvent event = (VEvent) eventComponent;
            MoccaCalendarEvent jsonMap = new MoccaCalendarEvent();

            addBasicEventProperties(jsonMap, event);
            addEventPeriod(event, jsonMap, timeZone, icalIntervalStart, icalIntervalEnd, collapse);

            long differenceInMillis = jsonMap.getDatesDifference();
            // If the interval dates are null, we don't check for recurring events. Done in order to maintain backwards
            // compatibility.
            if (!collapse && (icalIntervalStart == null || icalIntervalEnd == null)) {
                jsonArrayList.add(jsonMap);
                continue;
            }

            // Check if there is a RRule.
            RRule rRule = event.getProperty("rrule");
            RecurrenceId recurrenceId = event.getProperty("recurrence-id");
            if (rRule != null && rRule.getRecur() != null) {
                if (collapse) {
                    Recur recur = rRule.getRecur();
                    addRecurringEventsCollapsed(jsonMap, timeZone, jsonArrayList, recur);
                } else {
                    DateList recurringEventStartDates = rRule.getRecur()
                        .getDates(event.getStartDate().getDate(), icalIntervalStart, icalIntervalEnd, Value.DATE_TIME);
                    addRecurringEventsExpanded(event, jsonMap, differenceInMillis, jsonArrayList,
                        recurringEventStartDates);
                }
            } else if (collapse && recurrenceId != null) {
                addRecurrentModifiedInstance(jsonMap, timeZone, jsonArrayList, recurrenceId);
            } else {
                jsonArrayList.add(jsonMap);
            }
        }
    }

    private void addEventPeriod(VEvent event, MoccaCalendarEvent jsonMap, TimeZone timeZone, Date icalIntervalStart,
        Date icalIntervalEnd, boolean collapse) throws Exception
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
        // Do not add the event if it's not in the interval.

        // If the interval dates are null, maintain backwards compatibility.
        if (icalIntervalStart == null || icalIntervalEnd == null || areIntervalsIntersected(startDateTime, endDateTime,
            icalIntervalStart, icalIntervalEnd) || collapse)
        {
            jsonMap.setStart(startDateTime);
            jsonMap.setEnd(endDateTime);
        }
    }

    private void addRecurringEventsExpanded(VEvent event, MoccaCalendarEvent jsonMap, long differenceInMillis,
        ArrayList<MoccaCalendarEvent> jsonArrayList, DateList recurringEventStartDates)
    {
        String groupId = String.format(GROUP_ID_FORMAT, jsonMap.getId());
        for (int i = 0; i < recurringEventStartDates.size(); i++) {
            if (recurringEventStartDates.get(i).equals(event.getStartDate().getDate())) {
                continue;
            }
            MoccaCalendarEvent recurringEvent = new MoccaCalendarEvent(jsonMap);

            recurringEvent.setStart(recurringEventStartDates.get(i));
            recurringEvent.setEnd(new DateTime(recurringEventStartDates.get(i).getTime() + differenceInMillis));
            recurringEvent.setId(String.format("%s_%d", jsonMap.getId(), i));
            recurringEvent.setGroupId(groupId);
            jsonArrayList.add(recurringEvent);
        }
    }

    private void addRecurringEventsCollapsed(MoccaCalendarEvent jsonMap, TimeZone timeZone,
        ArrayList<MoccaCalendarEvent> jsonArrayList, Recur recur)
    {
        String groupId = String.format(GROUP_ID_FORMAT, jsonMap.getId());

        if (recur.getUntil() != null) {
            jsonMap.setRecEndDate(recur.getUntil());
        } else {
            LocalDate localDateStartDate = jsonMap.getStart().toInstant().atZone(timeZone.toZoneId()).toLocalDate();
            LocalDate futureLocalDate = localDateStartDate.plusYears(5);
            jsonMap.setRecEndDate(java.util.Date.from(futureLocalDate.atStartOfDay(timeZone.toZoneId()).toInstant()));
        }
        jsonMap.setRecurrent(1);
        String frequency = recur.getFrequency().name();
        String eventFrequency = getEventFrequency(recur, frequency);
        jsonMap.setRecurrenceFreq(eventFrequency);
        jsonMap.setGroupId(groupId);
        jsonArrayList.add(jsonMap);
    }

    private String getEventFrequency(Recur recur, String frequency)
    {
        if (frequency.equalsIgnoreCase("weekly")) {
            if (isEveryWorkday(recur.getDayList())) {
                return "WORKDAYS";
            }
            return recur.getInterval() == 2 ? "BIWEEKLY" : frequency;
        }
        return frequency;
    }

    private boolean isEveryWorkday(WeekDayList byDayValue)
    {
        if (byDayValue.isEmpty()) {
            return false;
        }
        Collection<WeekDay> requiredDays = new HashSet<>();
        requiredDays.add(WeekDay.MO);
        requiredDays.add(WeekDay.TU);
        requiredDays.add(WeekDay.WE);
        requiredDays.add(WeekDay.TH);
        requiredDays.add(WeekDay.FR);
        return byDayValue.containsAll(requiredDays) && byDayValue.size() == requiredDays.size();
    }

    private void addRecurrentModifiedInstance(MoccaCalendarEvent jsonMap, TimeZone timeZone,
        ArrayList<MoccaCalendarEvent> jsonArrayList, RecurrenceId recurrenceId) throws Exception
    {
        RecurrentEventModification eventModification = getRecurrentEventModification(jsonMap, timeZone, recurrenceId);

        Optional<MoccaCalendarEvent> optionalModifiedEvent =
            jsonArrayList.stream().filter(e -> e.getId().equals(jsonMap.getId())).findFirst();

        if (optionalModifiedEvent.isPresent()) {
            MoccaCalendarEvent modifiedInstance = optionalModifiedEvent.get();
            jsonArrayList.remove(modifiedInstance);
            modifiedInstance.addModifiedEvent(eventModification);
            jsonArrayList.add(modifiedInstance);
        }
    }

    private RecurrentEventModification getRecurrentEventModification(MoccaCalendarEvent jsonMap, TimeZone timeZone,
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

    private void addBasicEventProperties(MoccaCalendarEvent jsonMap, VEvent event)
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
        return (intervalEnd1.after(intervalStart2) && intervalEnd1.before(intervalEnd2))
            || (intervalStart1.after(intervalStart2) && intervalStart1.before(intervalEnd2));
    }
}
