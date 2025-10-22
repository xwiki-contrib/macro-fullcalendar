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
package org.xwiki.fullcalendar.internal.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.annotation.Component;
import org.xwiki.fullcalendar.model.CalendarEvent;
import org.xwiki.fullcalendar.model.RecurrentEventModification;
import org.xwiki.stability.Unstable;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.RecurrenceId;

/**
 * Helper class for processing the recurrence of a {@link VEvent}.
 *
 * @version $Id$
 * @since 2.5.0
 */
@Unstable
@Component(roles = RecurrenceProcessor.class)
@Singleton
public class RecurrenceProcessor
{
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

    @Inject
    private DateProcessor dateProcessor;

    private ZoneId zoneId;

    /**
     * Check the recurrence rule of a {@link VEvent}.
     *
     * @param zoneId {@link ZoneId}
     * @param icalIntervalStart start date of the calendar
     * @param icalIntervalEnd end date of the calendar
     * @param jsonArrayList the events list
     * @param collapse expands the recurring events if {@code false}, or collapses them into the parent otherwise.
     * @param event the event source
     * @param jsonMap the event target
     */
    public void checkRRule(ZoneId zoneId, LocalDateTime icalIntervalStart, LocalDateTime icalIntervalEnd,
        List<CalendarEvent> jsonArrayList, boolean collapse, VEvent event, CalendarEvent jsonMap)
    {
        // Check if there is a RRule.
        this.zoneId = zoneId;
        Optional<Property> rRuleOptional = event.getProperty(Property.RRULE);
        Optional<Property> recurrenceIdOptional = event.getProperty(Property.RECURRENCE_ID);
        if (rRuleOptional.isPresent()) {
            RRule<Temporal> rRule = (RRule) rRuleOptional.get();
            if (rRule.getRecur() != null) {
                handleRecurrentEvent(icalIntervalStart, icalIntervalEnd, jsonArrayList, collapse, rRule, event,
                    jsonMap);
            }
        } else if (collapse && recurrenceIdOptional.isPresent()) {
            RecurrenceId<Temporal> recurrenceId = (RecurrenceId) recurrenceIdOptional.get();
            addRecurrentModifiedInstance(jsonMap, jsonArrayList, recurrenceId);
        } else if (maybeAddEvent(icalIntervalStart, icalIntervalEnd, jsonMap, zoneId)) {
            jsonArrayList.add(jsonMap);
        }
    }

    private boolean maybeAddEvent(LocalDateTime icalIntervalStart, LocalDateTime icalIntervalEnd, CalendarEvent jsonMap,
        ZoneId zoneId)
    {
        LocalDateTime eventStartTime = jsonMap.getStart().toInstant().atZone(zoneId).toLocalDateTime();
        LocalDateTime eventEndTime = jsonMap.getEnd().toInstant().atZone(zoneId).toLocalDateTime();
        return icalIntervalStart == null || icalIntervalEnd == null || dateProcessor.areIntervalsIntersected(
            Pair.of(eventStartTime, eventEndTime), Pair.of(icalIntervalStart, icalIntervalEnd));
    }

    private void handleRecurrentEvent(LocalDateTime icalIntervalStart, LocalDateTime icalIntervalEnd,
        List<CalendarEvent> jsonArrayList, boolean collapse, RRule<Temporal> rRule, VEvent event, CalendarEvent jsonMap)
    {
        if (collapse) {
            boolean areIntervalDatesEmpty = icalIntervalStart == null || icalIntervalEnd == null;
            Recur<Temporal> recur = rRule.getRecur();
            DtStart<?> dtStart = event.getDateTimeStart();
            Object startObj = dtStart.getDate();
            if (!areIntervalDatesEmpty && recur.getDates(dateProcessor.toLocalDateTime(startObj, zoneId),
                icalIntervalStart, icalIntervalEnd).isEmpty())
            {
                return;
            }
            addRecurringEventsCollapsed(jsonMap, jsonArrayList, recur);
        } else {
            long differenceInMillis = jsonMap.getDatesDifference();
            Recur<Temporal> recur = rRule.getRecur();
            DtStart<LocalDateTime> dtStart = event.getDateTimeStart();
            List<Temporal> recurringEventStartDates =
                recur.getDates(dtStart.getDate(), icalIntervalStart, icalIntervalEnd);
            addRecurringEventsExpanded(jsonMap, differenceInMillis, jsonArrayList, recurringEventStartDates);
        }
    }

    private void addRecurringEventsExpanded(CalendarEvent jsonMap, long differenceInMillis,
        List<CalendarEvent> jsonArrayList, List<Temporal> recurringEventStartDates)
    {
        String groupId = String.format(GROUP_ID_FORMAT, jsonMap.getId());
        for (int i = 0; i < recurringEventStartDates.size(); i++) {
            CalendarEvent recurringEvent = new CalendarEvent(jsonMap);

            recurringEvent.setStart(dateProcessor.toUtilDate(recurringEventStartDates.get(i), zoneId));
            recurringEvent.setEnd(
                dateProcessor.toUtilDate(recurringEventStartDates.get(i).plus(Duration.ofMillis(differenceInMillis)),
                    zoneId));
            recurringEvent.setId(String.format("%s_%d", jsonMap.getId(), i));
            recurringEvent.setGroupId(groupId);
            jsonArrayList.add(recurringEvent);
        }
    }

    private void addRecurringEventsCollapsed(CalendarEvent jsonMap, List<CalendarEvent> jsonArrayList,
        Recur<Temporal> recur)
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

    private void setRecurrenceEndDate(CalendarEvent jsonMap, Recur<Temporal> recur)
    {
        int recurCount = recur.getCount();
        if (recur.getUntil() != null) {
            jsonMap.setRecEndDate(dateProcessor.toUtilDate(recur.getUntil(), zoneId));
        } else if (recurCount != -1) {
            jsonMap.setRecEndDate(new DateTime(
                jsonMap.getStart().getTime() + recurCount * TIME_PERIODS.getOrDefault(recur.getFrequency().name(),
                    YEARLY_DURATION)));
        } else {
            // Set end date of recurrence to five years from now.
            jsonMap.setRecEndDate(new DateTime(jsonMap.getStart().getTime() + 5 * YEARLY_DURATION));
        }
    }

    private String getEventFrequency(Recur<Temporal> recur, String frequency)
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

    private boolean isEveryWorkday(List<WeekDay> byDayValue)
    {
        if (byDayValue.isEmpty()) {
            return false;
        }
        return new HashSet<>(byDayValue).containsAll(WEEK_DAYS) && byDayValue.size() == WEEK_DAYS.size();
    }

    private void addRecurrentModifiedInstance(CalendarEvent jsonMap, List<CalendarEvent> jsonArrayList,
        RecurrenceId<Temporal> recurrenceId)
    {
        RecurrentEventModification eventModification = getRecurrentEventModification(jsonMap, recurrenceId);

        Optional<CalendarEvent> optionalModifiedEvent =
            jsonArrayList.stream().filter(e -> e.getId().equals(jsonMap.getId())).findFirst();

        optionalModifiedEvent.ifPresent(calendarEvent -> calendarEvent.addModifiedEvent(eventModification));
    }

    private RecurrentEventModification getRecurrentEventModification(CalendarEvent jsonMap,
        RecurrenceId<Temporal> recurrenceId)
    {
        RecurrentEventModification eventModification = new RecurrentEventModification();
        eventModification.setOriginalDate(dateProcessor.toUtilDate(recurrenceId.getValue(), zoneId));
        eventModification.setModifiedTitle(jsonMap.getTitle());
        eventModification.setModifiedDescription(jsonMap.getDescription());
        eventModification.setModifiedStartDate(jsonMap.getStart());
        eventModification.setModifiedEndDate(jsonMap.getEnd());
        return eventModification;
    }
}
