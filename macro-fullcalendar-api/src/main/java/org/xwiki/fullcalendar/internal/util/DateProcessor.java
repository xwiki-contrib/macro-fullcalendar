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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.stability.Unstable;

/**
 * Helper class for processing date operations and conversions.
 *
 * @version $Id$
 * @since 2.5.0
 */
@Unstable
@Component(roles = DateProcessor.class)
@Singleton
public class DateProcessor
{
    private static final String LOG_FORMAT = "Date object of type [{}] is not implemented for conversion to type [{}].";

    @Inject
    private Logger logger;

    /**
     * Test if two intervals are intersecting.
     *
     * @param firstInterval the first interval to check.
     * @param secondInterval the second interval to check.
     * @return {@code true} if the intervals are intersected, or {@code false} otherwise.
     */
    public boolean areIntervalsIntersected(Pair<LocalDateTime, LocalDateTime> firstInterval,
        Pair<LocalDateTime, LocalDateTime> secondInterval)
    {
        return (firstInterval.getRight().isAfter(secondInterval.getLeft()) && firstInterval.getRight()
            .isBefore(secondInterval.getRight())) || (firstInterval.getLeft().isAfter(secondInterval.getLeft())
            && firstInterval.getLeft().isBefore(secondInterval.getRight()));
    }

    /**
     * Convert objects of type {@link LocalDate}, {@link LocalDateTime}, {@link OffsetDateTime}, {@link ZonedDateTime}
     * to {@link Date}.
     *
     * @param dateObj the date object of the above-mentioned types
     * @param zoneId {@link ZoneId}
     * @return a {@link Date} object, or null if the given object type is not supported.
     */
    public Date toUtilDate(Object dateObj, ZoneId zoneId)
    {
        Date date = null;
        if (dateObj instanceof Date) {
            return (Date) dateObj;
        } else if (dateObj instanceof LocalDate) {
            LocalDate ld = (LocalDate) dateObj;
            Instant instant = ld.atStartOfDay(zoneId).toInstant();
            date = Date.from(instant);
        } else if (dateObj instanceof LocalDateTime) {
            LocalDateTime ldt = (LocalDateTime) dateObj;
            Instant instant = ldt.atZone(zoneId).toInstant();
            date = Date.from(instant);
        } else if (dateObj instanceof OffsetDateTime) {
            OffsetDateTime odt = (OffsetDateTime) dateObj;
            Instant inst = odt.toInstant();
            date = Date.from(inst);
        } else if (dateObj instanceof ZonedDateTime) {
            ZonedDateTime zdt = (ZonedDateTime) dateObj;
            Instant instant = zdt.toLocalDateTime().atZone(zdt.getZone()).toInstant();
            date = Date.from(instant);
        } else if (dateObj != null) {
            logger.warn(LOG_FORMAT, dateObj.getClass(), Date.class);
        }
        return date;
    }

    /**
     * Convert objects of type {@link LocalDate}, {@link Date}, {@link OffsetDateTime}, {@link ZonedDateTime} to
     * {@link LocalDateTime}.
     *
     * @param dateObj the date object of the above-mentioned types
     * @param zoneId {@link ZoneId}
     * @return a {@link LocalDateTime} object, or null if the given object type is not supported.
     */
    public LocalDateTime toLocalDateTime(Object dateObj, ZoneId zoneId)
    {
        LocalDateTime date = null;
        if (dateObj instanceof LocalDateTime) {
            return (LocalDateTime) dateObj;
        } else if (dateObj instanceof LocalDate) {
            date = ((LocalDate) dateObj).atStartOfDay();
        } else if (dateObj instanceof OffsetDateTime) {
            date = ((OffsetDateTime) dateObj).toLocalDateTime();
        } else if (dateObj instanceof Date) {
            Instant instant = ((Date) dateObj).toInstant();
            date = instant.atZone(zoneId).toLocalDateTime();
        } else if (dateObj instanceof ZonedDateTime) {
            date = ((ZonedDateTime) dateObj).toLocalDateTime();
        } else if (dateObj != null) {
            logger.warn(LOG_FORMAT, dateObj.getClass(), LocalDateTime.class);
        }
        return date;
    }
}
