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
package org.xwiki.fullcalendar.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Represents the data of a mocca calendar event.
 *
 * @version $Id$
 * @since 2.4.0
 */
@Unstable
public class CalendarEvent
{
    private String id;

    private String title;

    @JsonSerialize(using = CustomDateSerializer.class)
    private Date start;

    @JsonSerialize(using = CustomDateSerializer.class)
    private Date end;

    private boolean allDay;

    private String description;

    private String location;

    private String status;

    private int recurrent;

    private Date recEndDate;

    private String recurrenceFreq;

    private List<RecurrentEventModification> modificationList;

    private String groupId;

    /**
     * Default constructor.
     */
    public CalendarEvent()
    {
        modificationList = new ArrayList<>();
    }

    /**
     * Copy constructor.
     *
     * @param calendarEvent {@link CalendarEvent} to be copied.
     */
    public CalendarEvent(CalendarEvent calendarEvent)
    {
        this.setId(calendarEvent.getId());
        this.setTitle(calendarEvent.getTitle());
        this.setStart(calendarEvent.getStart());
        this.setEnd(calendarEvent.getEnd());
        this.setAllDay(calendarEvent.isAllDay());
        this.setDescription(calendarEvent.getDescription());
        this.setLocation(calendarEvent.getLocation());
        this.setStatus(calendarEvent.getStatus());
        this.setRecurrent(calendarEvent.isRecurrent());
        this.setRecEndDate(calendarEvent.getRecEndDate());
        this.setRecurrenceFreq(calendarEvent.getRecurrenceFreq());
        this.setGroupId(calendarEvent.getGroupId());
        modificationList = new ArrayList<>(calendarEvent.getModificationList());
    }

    /**
     * Common id for all events generated from the same recurrence rule.
     *
     * @return the group id.
     */
    public String getGroupId()
    {
        return groupId;
    }

    /**
     * See {@link #getGroupId()}.
     *
     * @param groupId the group id.
     */
    public void setGroupId(String groupId)
    {
        this.groupId = groupId;
    }

    /**
     * Get the modified instances to the recurrent event.
     *
     * @return the modified instances from the recurrent event.
     */
    public List<RecurrentEventModification> getModificationList()
    {
        return modificationList;
    }

    /**
     * Add a modified recurrence instance.
     *
     * @param modificationResult modified recurrent instance.
     */
    public void addModifiedEvent(RecurrentEventModification modificationResult)
    {
        this.modificationList.add(modificationResult);
    }

    /**
     * See {@link #setLocation}.
     *
     * @return the event location.
     */
    public String getLocation()
    {
        return location;
    }

    /**
     * Set the event location.
     *
     * @param location the event location.
     */
    public void setLocation(String location)
    {
        this.location = location;
    }

    /**
     * See {@link #setStatus}.
     *
     * @return the event status.
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * Set the event status.
     *
     * @param status the event status.
     */
    public void setStatus(String status)
    {
        this.status = status;
    }

    /**
     * see {@link #setId}.
     *
     * @return the event ID.
     */
    public String getId()
    {
        return id;
    }

    /**
     * Set the event ID.
     *
     * @param id the event ID.
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * See {@link #setRecEndDate}.
     *
     * @return the {@link Date} until the recurrence takes place.
     */
    public Date getRecEndDate()
    {
        return recEndDate;
    }

    /**
     * Set the end date of the recurrence.
     *
     * @param recEndDate the {@link Date} until the recurrence takes place.
     */
    public void setRecEndDate(Date recEndDate)
    {
        this.recEndDate = recEndDate;
    }

    /**
     * See {@link #setEnd}.
     *
     * @return the end {@link Date} of the event.
     */
    public Date getEnd()
    {
        return end;
    }

    /**
     * Set the end date of the event.
     *
     * @param end the {@link Date} when the event ends.
     */
    public void setEnd(Date end)
    {
        this.end = end;
    }

    /**
     * See {@link #setStart}.
     *
     * @return the start {@link Date} of the event.
     */
    public Date getStart()
    {
        return start;
    }

    /**
     * Set the start date of the event.
     *
     * @param start the {@link Date} when the event starts.
     */
    public void setStart(Date start)
    {
        this.start = start;
    }

    /**
     * See {@link #setAllDay}.
     *
     * @return {@code 1} if the event takes all day, or {@code 0} otherwise.
     */
    public boolean isAllDay()
    {
        return allDay;
    }

    /**
     * Set the all day flag.
     *
     * @param allDay the all day flag. {@code 1} if the event takes all day, or {@code 0} otherwise.
     */
    public void setAllDay(boolean allDay)
    {
        this.allDay = allDay;
    }

    /**
     * See {@link #setRecurrent}.
     *
     * @return {@code 1} if the event is recurrent, or {@code 0} otherwise.
     */
    public int isRecurrent()
    {
        return recurrent;
    }

    /**
     * Set the recurrence flag.
     *
     * @param recurrent the recurrence flag. {@code 1} if the event is recurrent, or {@code 0} otherwise.
     */
    public void setRecurrent(int recurrent)
    {
        this.recurrent = recurrent;
    }

    /**
     * See {@link #setDescription}.
     *
     * @return the event description.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Set the event description.
     *
     * @param description the event description.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * See {@link #setRecurrenceFreq}.
     *
     * @return the recurrence frequency of the event, if there is any.
     */
    public String getRecurrenceFreq()
    {
        return recurrenceFreq;
    }

    /**
     * Set the recurrence frequency.
     *
     * @param recurrenceFreq the event recurrence frequency.
     */
    public void setRecurrenceFreq(String recurrenceFreq)
    {
        this.recurrenceFreq = recurrenceFreq;
    }

    /**
     * See {@link #setTitle}.
     *
     * @return the title of the event.
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Set the event title.
     *
     * @param title the event title.
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * Get the time difference between the event end date and the event start date.
     *
     * @return the time difference between the event end date and the event start date.
     */
    public long getDatesDifference()
    {
        return end.getTime() - start.getTime();
    }
}
