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
package org.wcs.smart.cybertracker.patrol.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption;
import org.wcs.smart.patrol.model.PatrolTransportType;

/**
 * Class for managing custom track time settings
 * per patrol type.
 * 
 * @author Emily
 *
 */
public class TransportTypeTrackTimerSetting {

	private static final String ITEM_SEP = "|"; //$NON-NLS-1$

	private static final String FIELD_SEP = ":"; //$NON-NLS-1$

	public static final String METADATA_KEY = "TRANSPORT_TRACK_SETTINGS"; //$NON-NLS-1$
	
	private PatrolTransportType type;
	private CyberTrackerPropertiesProfileOption.TrackTimerOp op;
	private int value = 0;
	
	public TransportTypeTrackTimerSetting(PatrolTransportType type) {
		this(type, null, 0);
	}
	
	public TransportTypeTrackTimerSetting(PatrolTransportType type, CyberTrackerPropertiesProfileOption.TrackTimerOp op, int value) {
		this.type = type;
		this.op = op;
		this.value = value;
		
	}
	
	public PatrolTransportType getTransportType() {
		return this.type;
	}
	
	public int getValue() {
		return this.value;
	}
	
	public CyberTrackerPropertiesProfileOption.TrackTimerOp getTrackTimerOption(){
		return this.op;
	}
	
	public String toString() {
		return type.getKeyId() + FIELD_SEP + op.name() + FIELD_SEP + String.valueOf(value);
	}
	
	/**
	 * Well return null if transport type with key can't be found or there
	 * are any errors parsing the other fields.
	 * @param value
	 * @param session
	 * @return
	 */
	private static TransportTypeTrackTimerSetting fromStringSingle(String value, ConservationArea ca, Session session) {
		String[] bits = value.split(FIELD_SEP);
		if (bits.length != 3) return null;
		
		PatrolTransportType type = session.createQuery("FROM PatrolTransportType WHERE conservationArea = :ca and keyId = :key", PatrolTransportType.class) //$NON-NLS-1$
				.setParameter("ca", ca) //$NON-NLS-1$
				.setParameter("key", bits[0]) //$NON-NLS-1$
				.uniqueResult();
		if (type == null) return null;
		
		CyberTrackerPropertiesProfileOption.TrackTimerOp op;
		try {
			op = CyberTrackerPropertiesProfileOption.TrackTimerOp.valueOf(bits[1]);
		}catch (Exception ex) {
			return null;
		}
		int tvalue = 0;
		try {
			tvalue = Integer.valueOf(bits[2]);
		}catch (Exception ex){
			return null;
		}
		
		return new TransportTypeTrackTimerSetting(type, op, tvalue);
	}
	
	/**
	 * Converts a set of track timer settings into a string for storing
	 * in database
	 * 
	 * @param items
	 * @return
	 */
	public static String toString(List<TransportTypeTrackTimerSetting> items) {
		if (items.isEmpty()) return ""; //$NON-NLS-1$
		StringBuilder sb = new StringBuilder();
		for (TransportTypeTrackTimerSetting item : items) {
			sb.append(item.toString());
			sb.append(ITEM_SEP);
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}
	
	/**
	 * Parses a string into a set of track timer settings.  If parsing any
	 * individual timer settings causes an error that item will be skipped.
	 * 
	 * @param value
	 * @param session
	 * @return
	 */
	public static List<TransportTypeTrackTimerSetting> fromString(String value, ConservationArea ca, Session session){
		List<TransportTypeTrackTimerSetting> items = new ArrayList<>();
		if (value == null || value.isEmpty()) return items;
		String[] parts = value.split("\\|"); //$NON-NLS-1$
		for (String part : parts) {
			TransportTypeTrackTimerSetting item = fromStringSingle(part, ca, session);
			if (item != null) {
				items.add(item);
			}
		}
		return items;
	}
}
