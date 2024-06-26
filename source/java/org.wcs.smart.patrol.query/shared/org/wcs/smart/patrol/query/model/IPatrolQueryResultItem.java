/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.wcs.smart.patrol.query.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.query.common.engine.IResultItem;

public interface IPatrolQueryResultItem extends IResultItem {
	
	public String getPatrolId();
	public String getLeader();
	public String getMembers();
	public String getPilot();
	public LocalDate getPatrolStartDate() ;
	public LocalDate getPatrolEndDate() ;
	public String getStation() ;
	public String getTeam() ;
	public String getObjective() ;
	public String getMandate() ;
	public PatrolType.Type getPatrolType() ;
	public UUID getPatrolUuid() ;
	public boolean isArmed() ;
	public String getPatrolLegId() ;
	public UUID getPatrolLegUuid() ;
	public String getTransportType() ;
	public LocalDate getPatrolLegStartDate();
	public LocalDate getPatrolLegEndDate();
	public String getConservationAreaId();
	public String getConservationAreaName();
	public UUID getConservationAreaUuid();	
	
	public default LocalDateTime getPatrolMaxDateTime() { return null; }
	public default LocalDateTime getPatrolMinDateTime() { return null; }
	
	public Object getPatrolAttribute(String keyId);
	
}
