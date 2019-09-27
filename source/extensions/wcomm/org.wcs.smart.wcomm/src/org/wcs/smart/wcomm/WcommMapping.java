/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.wcomm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.wcs.smart.hibernate.SmartDB;

/**
 * WCoMM data to SMART data model mapping.
 * 
 * @author Emily
 *
 */
public class WcommMapping {

	public enum Field{
		WO_CATEGORY,
		WO_SPECIES,
		WO_NUMINDIV,
		WO_SPOORSCT,
		
		EM_CATEGORY,
		EM_SEPCIES,
		EM_CARCASSAGE,
		EM_SEX,
		EM_AGE,
		EM_CAUSEDEATH,
		EM_MEANSDEATH,
		EM_LEFTTUSK,
		EM_RIGHTTUSK,
		
		OC_CATEGORY,
		OC_SEPCIES,
		OC_NUMBER,
		OC_CARCAASSAGE,
		OC_SEX,
		OC_AGE,
		OC_CAUSEDEATH,
		OC_MEANSDEATH,

		HWC_CATEGORY,
		HWC_SPECIES,
		HWC_LIVESTOCK,
		HWC_TIME
	}
	
	public HashMap<Field, String> values;
	
	private List<AttributeMapping> attributemappings;
	private List<IncidentMapping> incidentmappings;
	
	private WcommMapping() {
		values = new HashMap<>();
		attributemappings = new ArrayList<>();
		incidentmappings = new ArrayList<>();
	}
	
	public void setField(Field field, String value) {
		this.values.put(field,  value);
		try {
			save();
		} catch (IOException e) {
			WCommPlugIn.displayLog(e.getMessage(), e);
		}
	}
	
	public String getValue(Field field) {
		return values.get(field);
	}
	
	public List<IncidentMapping> getIncidentMapping() {
		return incidentmappings;
	}
	
	public IncidentMapping addIncidentMapping() {
		IncidentMapping a = new IncidentMapping();
		incidentmappings.add(a);
		return a;
	}
	public void removeIncidentMapping(IncidentMapping m) {
		incidentmappings.remove(m);
	}
	
	public List<AttributeMapping> getAttributeMapping() {
		return attributemappings;
	}
	
	public AttributeMapping addAttributeMapping() {
		AttributeMapping a = new AttributeMapping();
		attributemappings.add(a);
		return a;
	}
	public void removeAttributeMapping(AttributeMapping m) {
		attributemappings.remove(m);
	}
	
	public static Path getPath() {
		return Paths.get(SmartDB.getCurrentConservationArea().getFileDataStoreLocation())
				.resolve("WCOMMAPPING.csv"); //$NON-NLS-1$
	}
	
	public void save() throws IOException {
		Path p = getPath();
		
		StringBuilder sb = new StringBuilder();
		for (Field f : Field.values()) {
			sb.append("field=" + f.name() + "=" + (values.get(f) == null ? "" : values.get(f))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			sb.append("\n"); //$NON-NLS-1$
		}
		for (AttributeMapping key : attributemappings) {
			sb.append("attribute=" + key.attribute +"=" + key.item + "=" + key.value); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			sb.append("\n"); //$NON-NLS-1$
		}
		for (IncidentMapping key : incidentmappings) {
			sb.append("incident=" + key.category + Messages.WcommMapping_9 + (key.attribute == null ? "" : key.attribute) + "=" + (key.item == null ? "" : key.item) + "=" + key.value); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ 
			sb.append("\n"); //$NON-NLS-1$
		}
		Files.write(p, sb.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}
	
	public static WcommMapping empty() {
		WcommMapping m = new WcommMapping();
		return m;
	}
	
	public static WcommMapping load() throws IOException {
		Path p = getPath();
		WcommMapping m = new WcommMapping();
		if (!Files.exists(p)) return m;
		
		List<String> items = Files.readAllLines(p);
		
		for (String i : items) {
			String[] parts = i.split("="); //$NON-NLS-1$
			String key = parts[0];
			if (key.equals("field")) { //$NON-NLS-1$
				try {
					Field f = Field.valueOf(parts[1]);
					String v = parts.length > 2 ? parts[2] : null;
					m.values.put(f, v);
				}catch (Exception ex) {
					ex.printStackTrace();
				}
			}else if (key.equals("attribute")){ //$NON-NLS-1$
				AttributeMapping aa = new AttributeMapping();
				aa.attribute = parts[1];
				aa.item = parts[2];
				aa.value = parts[3];
				m.attributemappings.add(aa);
			}else if (key.equals("incident")){ //$NON-NLS-1$
				IncidentMapping aa = new IncidentMapping();
				aa.category= parts[1];
				aa.attribute = parts[2].trim().isEmpty() ? null : parts[2].trim();
				aa.item = parts[3].trim().isEmpty() ? null : parts[3].trim();
				aa.value = parts[4];
				m.incidentmappings.add(aa);
			}
		}
		
		return m;
	}
	
	public static class AttributeMapping{
		public String attribute;
		public String item;
		public String value;
	}
	
	public static class IncidentMapping{
		public String category;
		public String attribute;
		public String item;
		public String value;
	}
}
