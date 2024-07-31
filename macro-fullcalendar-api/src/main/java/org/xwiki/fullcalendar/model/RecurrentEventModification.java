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

import java.util.Date;

import org.xwiki.stability.Unstable;

/**
 * Represents the data of a mocca calendar recurrent event instance modification.
 *
 * @version $Id$
 * @since 2.4.0
 */
@Unstable
public class RecurrentEventModification
{
    private Date originalDate;

    private Date modifiedStartDate;

    private Date modifiedEndDate;

    private String modifiedTitle;

    private String modifiedDescription;

    /**
     * Default constructor.
     */
    public RecurrentEventModification()
    {
    }

    /**
     * Get the original date of the modified instance.
     *
     * @return the original date of the modified instance.
     */
    public Date getOriginalDate()
    {
        return originalDate;
    }

    /**
     * Set the original date of the modified instance.
     *
     * @param originalDate the original date of the modified instance.
     */
    public void setOriginalDate(Date originalDate)
    {
        this.originalDate = originalDate;
    }

    /**
     * Get the new end date of the modified instance.
     *
     * @return the new end date of the modified instance.
     */
    public Date getModifiedEndDate()
    {
        return modifiedEndDate;
    }

    /**
     * Set the new end date of the modified instance.
     *
     * @param modifiedEndDate the new end date of the modified instance.
     */
    public void setModifiedEndDate(Date modifiedEndDate)
    {
        this.modifiedEndDate = modifiedEndDate;
    }

    /**
     * Get the new start date of the modified instance.
     *
     * @return the new start date of the modified instance.
     */
    public Date getModifiedStartDate()
    {
        return modifiedStartDate;
    }

    /**
     * Set the new start date of the modified instance.
     *
     * @param modifiedStartDate the new start date of the modified instance.
     */
    public void setModifiedStartDate(Date modifiedStartDate)
    {
        this.modifiedStartDate = modifiedStartDate;
    }

    /**
     * Get the new description of the modified instance.
     *
     * @return the new description of the modified instance.
     */
    public String getModifiedDescription()
    {
        return modifiedDescription;
    }

    /**
     * Set the new description of the modified instance.
     *
     * @param modifiedDescription the new description of the modified instance.
     */
    public void setModifiedDescription(String modifiedDescription)
    {
        this.modifiedDescription = modifiedDescription;
    }

    /**
     * Get the new title of the modified instance.
     *
     * @return the new title of the modified instance.
     */
    public String getModifiedTitle()
    {
        return modifiedTitle;
    }

    /**
     * Set the new title of the modified instance.
     *
     * @param modifiedTitle the new title of the modified instance.
     */
    public void setModifiedTitle(String modifiedTitle)
    {
        this.modifiedTitle = modifiedTitle;
    }
}
