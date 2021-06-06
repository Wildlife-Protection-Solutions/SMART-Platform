package org.wcs.smart.i2.migrate.intelligence;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;

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
