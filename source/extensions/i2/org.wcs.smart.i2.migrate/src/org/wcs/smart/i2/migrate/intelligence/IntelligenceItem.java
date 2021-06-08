/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.i2.migrate.intelligence;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;

/**
 * Represents the details of a SMART6 intelligence record
 * 
 * @author Emily
 *
 */
public class IntelligenceItem {
	private UUID uuid;
	private String name;
	private LocalDate recievedDate;
	private UUID creator;
	private UUID source;
	private UUID patroluuid;
	
	private String description;
	private LocalDate fromDate;
	private LocalDate toDate;
	private List<Coordinate> points;
	private List<Path> attachments;

	public IntelligenceItem() {
		points = new ArrayList<>();
		attachments = new ArrayList<>();
	}
	
	public void addPoint(Coordinate c) {
		points.add(c);
	}
	
	public void addAttachment(Path attachment) {
		attachments.add(attachment);
	}
	
	public List<Path> getAttachments(){
		return this.attachments;
	}
	
	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public LocalDate getRecievedDate() {
		return recievedDate;
	}
	public void setRecievedDate(LocalDate recievedDate) {
		this.recievedDate = recievedDate;
	}
	public UUID getCreator() {
		return creator;
	}
	public void setCreator(UUID creator) {
		this.creator = creator;
	}
	public UUID getSource() {
		return source;
	}
	public void setSource(UUID source) {
		this.source = source;
	}
	public UUID getPatroluuid() {
		return patroluuid;
	}
	public void setPatroluuid(UUID patroluuid) {
		this.patroluuid = patroluuid;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public LocalDate getFromDate() {
		return fromDate;
	}
	public void setFromDate(LocalDate fromDate) {
		this.fromDate = fromDate;
	}
	public LocalDate getToDate() {
		return toDate;
	}
	public void setToDate(LocalDate toDate) {
		this.toDate = toDate;
	}
	public List<Coordinate> getPoints() {
		return points;
	}
	public void setPoints(List<Coordinate> points) {
		this.points = points;
	}
	
	
}
