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
package org.wcs.smart.cybertracker.importer;

import java.io.File;
import java.sql.Time;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.AbstractCyberTrackerData;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.data.Data.Elements.E;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S.A;
import org.wcs.smart.cybertracker.util.PdaUtil;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Common smart importing logic.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public abstract class AbstractSmartImporter {
	
	public static final int WARN_WP_TIME_FRAME = 10; //in minutes
	
	private DateFormat filenameDateFormat = new SimpleDateFormat("yyyy_MM_dd"); //$NON-NLS-1$

	private List<String> warnings = new ArrayList<String>();

	public static DateFormat createCyberTrackerDateFormatter() {
		DateFormat formatter = new SimpleDateFormat(ICyberTrackerConstants.CT_DATE_FORMAT); //will this always work or CT might provide different format depending on locale settings?
		return formatter;
	}
	
	protected DateFormat getFilenameDateFormat() {
		return filenameDateFormat;
	}

	public static Date combine(Date date, Time time) {
		if (date == null)
			return time;
		if (time == null)
			return date;
		Calendar timeCalendar = Calendar.getInstance();
		timeCalendar.setTime(time);
		Calendar dateCalendar = Calendar.getInstance();
		dateCalendar.setTime(date);
		dateCalendar.add(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
		dateCalendar.add(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
		dateCalendar.add(Calendar.SECOND, timeCalendar.get(Calendar.SECOND));
		return dateCalendar.getTime();
	}

	/**
	 * Returns list of coordinates recorded during specified period of time.
	 * Assumes that source list is sorted by z coordinate (date+time).
	 * @param list
	 * @param from
	 * @param to
	 * @return
	 */
	public static List<Coordinate> listPart(final List<Coordinate> list, Date from, Date to) {
		Coordinate fromC = new Coordinate(0, 0, from.getTime() - 0.5);
		Coordinate toC = new Coordinate(0, 0, to.getTime() + 0.5);
		Comparator<Coordinate> cmp = new CoordinateZComparator();
		int low = binaryCut(list, fromC, cmp) + 1;
		int high = binaryCut(list, toC, cmp) + 1;
		return new ArrayList<Coordinate>(list.subList(low, high));
	}

	/**
	 * Returns max index of an element smaller or equal to the key
	 */
    private static <T> int binaryCut(List<? extends T> l, T key, Comparator<? super T> c) {
        int low = 0;
        int high = l.size()-1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T midVal = l.get(mid);
            int cmp = c.compare(midVal, key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return low-1;  // key not found
    }
	

	protected void addObservations(Waypoint wp, S s, Map<String, E> eMap, Session session) {
		ObservationSplitResult splitResult = splitObservationRecords(s, eMap);
		List<A> catList = splitResult.getCategories();
		if (catList.isEmpty())
			return;
		
		Category category = fetchCategory(catList, eMap, session);
		if (category == null)
			return;
		
		Employee observer = fetchObserver(splitResult.getObserver(), eMap, session);
		
		for (List<A> attrList : splitResult.getAttributes()) {
			WaypointObservation obs = new WaypointObservation();
			obs.setWaypoint(wp);
			obs.setCategory(category);
			obs.setObserver(observer);
			obs.setAttributes(fetchAttributes(obs, attrList, eMap, session));
			
			wp.getObservations().add(obs);
		}
	}

	/**
	 * @param s
	 * @param eMap
	 * @return data structure containing:
	 *            - a list of A tags that define category
	 *            - one or several list of A tags that define attributes in this category
	 *            - observer information if it presents
	 */
	private ObservationSplitResult splitObservationRecords(S s, Map<String, E> eMap) {
		List<A> categories = new ArrayList<A>();
		Map<Integer, List<A>> attributes = new HashMap<Integer, List<A>>();
		List<A> preMsAtts = null;
		boolean processMultiselect = true;
		A defaultValue = null;
		A observer = null;
		for (A a : s.getA()) {
			E e = eMap.get(a.getI());
			if (e == null)
				continue; //skip invalid records
			
			if (ElementsUtil.isCategoryResultElement(e)) {
				categories.add(a);
			} else if (ElementsUtil.ATTRIBUTE_ELEMENT_TAG.equals(e.getTag1())) {
				Integer tag2 = e.getTag2() != null ? Integer.valueOf(e.getTag2()) : 0;
				List<A> aList = attributes.get(tag2);
				if (aList == null) {
					aList = new ArrayList<A>();
					attributes.put(tag2, aList);
				}
				aList.add(a);
			} else if (ScreensUtil.RESULT_OBSERVER.equals(e.getN())) {
				observer = a;
			} else if (ElementsUtil.DEFAULT_VALUES_ELEMENT_TAG.equals(e.getTag1())) {
				defaultValue = a;
			} else if (ElementsUtil.MULISELECT_ELEMENT_TAG.equals(e.getTag1())) {
				if (processMultiselect) {
					processMultiselect = false;
					preMsAtts = attributes.remove(0); //all previously enter values MUST be here
					if (preMsAtts == null) {
						preMsAtts = Collections.emptyList();
					}
					if (!attributes.isEmpty()) {
						//development validation; MUST never enter here
						throw new IllegalStateException("Mapping to entry different from #0 present before multi-select occured."); //$NON-NLS-1$
					}
				}
				Integer tag2 = e.getTag2() != null ? Integer.valueOf(e.getTag2()) : 0;
				List<A> aList = attributes.get(tag2);
				if (aList == null) {
					aList = new ArrayList<A>();
					aList.addAll(preMsAtts); //add all previously entered values as they should be applied to all items selected in multi-select
					attributes.put(tag2, aList);
				}
				//create fake record as if it is result element for listAttribute with listAttributeValue
				A fakeA = new A();
				fakeA.setN("List Attribute for item " + a.getN()); //will never be displayed //$NON-NLS-1$
				fakeA.setI(e.getTag3());
				fakeA.setV(a.getI());
				fakeA.setValue(""); //$NON-NLS-1$
				aList.add(fakeA);
				if (e.getTag4() != null && !e.getTag4().isEmpty()) {
					//also need to add numberAttribute if this is multiselect+number (reference to numAttr in tag4, value in a.getV())
					fakeA = new A();
					fakeA.setN("Number Attribute for item " + a.getN()); //will never be displayed //$NON-NLS-1$
					fakeA.setI(e.getTag4());
					fakeA.setV(a.getV());
					fakeA.setValue(""); //$NON-NLS-1$
					aList.add(fakeA);
				}
			}
		}
		
		List<List<A>> attrList = new ArrayList<List<A>>();
		for (Integer i : attributes.keySet()) {
			List<A> aList = attributes.get(i);
			if (defaultValue != null) {
				aList.add(defaultValue);
			}
			attrList.add(aList);
		}
		if (attrList.isEmpty() && defaultValue != null) { //note that attrList.isEmpty() only if attributes.keySet().isEmpty() 
			//case with no visible attributes, but default values (see ticket #1610)
			List<A> aList = new ArrayList<A>(1);
			aList.add(defaultValue);
			attrList.add(aList);
		}
		return new ObservationSplitResult(categories, attrList, observer);
	}
	
	private List<WaypointObservationAttribute> fetchAttributes(WaypointObservation obs, List<A> aList, Map<String, E> eMap, Session session) {
		List<WaypointObservationAttribute> result = new ArrayList<WaypointObservationAttribute>();
		for (A a : aList) {
			if (a.getV() == null)
				continue;
			
			E e = eMap.get(a.getI());

			if (ElementsUtil.DEFAULT_VALUES_ELEMENT_TAG.equals(e.getTag1())) {
				//handling default values
				String[] ctIdArray = a.getV().split(ICyberTrackerConstants.ATTRIBUTE_DEFAULT_VALUES_SEPATATOR);
				for (String ctid : ctIdArray) {
					E de = eMap.get(ctid);
					WaypointObservationAttribute wpoa = createWaypointObservationAttribute(de, de.getTag2(), eMap, session);
					if (wpoa == null)
						continue;
					wpoa.setObservation(obs);
					result.add(wpoa);
				}
				continue;
			}
				
			if (!ElementsUtil.ATTRIBUTE_ELEMENT_TAG.equals(e.getTag1()))
				continue;

			WaypointObservationAttribute wpoa = createWaypointObservationAttribute(e, a.getV(), eMap, session);
			if (wpoa == null)
				continue;
			wpoa.setObservation(obs);
			result.add(wpoa);
		}	
		return result;
	}

	private WaypointObservationAttribute createWaypointObservationAttribute(E e, String av, Map<String, E> eMap, Session session) {
		String tag0 = e.getTag0();
		if (tag0 == null || tag0.isEmpty()) {
			addWarning(MessageFormat.format(Messages.SmartImporter_Warn_AttributeTag0_Missing, e.getN()));
			return null;
		}

		Attribute attr = CyberTrackerHibernateManager.fetchByUuid(Attribute.class, tag0, session);
		if (attr == null) {
			addWarning(MessageFormat.format(Messages.SmartImporter_Warn_NoAttributeInDatamodel, e.getN(), tag0));
			return null;
		}

		WaypointObservationAttribute wpoa = new WaypointObservationAttribute();
		wpoa.setAttribute(attr);
		switch (attr.getType()) {
		case NUMERIC:
			wpoa.setNumberValue(Double.valueOf(av));
			break;
		case TEXT:
			wpoa.setStringValue(av);
			break;
		case LIST:
		{
			E eLst = eMap.get(av);
			AttributeListItem item = CyberTrackerHibernateManager.fetchByUuid(AttributeListItem.class, eLst.getTag0(), session);
			if (item == null && !CyberTrackerHibernateManager.isEmptyTag0(eLst.getTag0())) {
				addWarning(MessageFormat.format(Messages.SmartImporter_Warn_NoListAttrItemInDatamodel, e.getN(), eLst.getN(), eLst.getTag0()));
				return null;
			}
			wpoa.setAttributeListItem(item);
			break;
		}
		case TREE:
		{
			E eTr = eMap.get(av);
			AttributeTreeNode item = CyberTrackerHibernateManager.fetchByUuid(AttributeTreeNode.class, eTr.getTag0(), session);
			if (item == null && !CyberTrackerHibernateManager.isEmptyTag0(eTr.getTag0())) {
				addWarning(MessageFormat.format(Messages.SmartImporter_Warn_NoTreeAttrItemInDatamodel, e.getN(), eTr.getN(), eTr.getTag0()));
				return null;
			}
			wpoa.setAttributeTreeNode(item);
			break;
		}
		case BOOLEAN:
		{
			/*
			 * av will be CT_id in case of regular data or getBooleanValue().toString() in case of default value
			 * see CyberTrackerConfExporter.recordDefaultValue(CmAttribute cmAttr)
			 */
			Double value = null;
			if (Boolean.TRUE.toString().equals(av)) {
				value = 1.0;
			} else if (Boolean.FALSE.toString().equals(av)) {
				value = 0.0;
			} else {
				//this is a regular data
				E eBool = eMap.get(av);
				if (eBool == null) {
					addWarning(MessageFormat.format(Messages.SmartImporter_Warn_BooleanValueProblem, av, e.getN()));
					return null;
				}
				if (ElementsUtil.BOOL_TRUE.equals(eBool.getTag0())) {
					value = 1.0;
				} else if (ElementsUtil.BOOL_FALSE.equals(eBool.getTag0())) {
					value = 0.0;
				}
			}
			wpoa.setNumberValue(value);
			break;
		}
		case DATE:
		{
			try {
				DateFormat formatter = createCyberTrackerDateFormatter();
				wpoa.setDateValue(formatter.parse(av));
			} catch (ParseException ex) {
				CyberTrackerPlugIn.log(ex.getMessage(), ex);
				addWarning(MessageFormat.format(Messages.SmartImporter_Warn_CannotParseDate, e.getN(), av));
				return null;
			}
			break;
		}
		}
		return wpoa;
	}
	
	private Category fetchCategory(List<A> aList, Map<String, E> eMap, Session session) {
		String v = null;
		int vIndex = -1;
		for (A a : aList) {
			E iE = eMap.get(a.getI());
			if (ElementsUtil.isCategoryResultElement(iE)) {
				int index = Integer.valueOf(iE.getTag0());
				if (index > vIndex) {
					vIndex = index;
					v = a.getV();
				}
			}
		}
		
		if (v == null)
			return null;
		
		E e = eMap.get(v);
		if (e == null) {
			addWarning(MessageFormat.format(Messages.SmartImporter_Warn_ElementNotDefined, v));
			return null;
		}
		String tag0 = e.getTag0();
		if (tag0 == null || tag0.isEmpty()) {
			addWarning(MessageFormat.format(Messages.SmartImporter_Warn_CategoryTag0_Missing, e.getN()));
			return null;
		}
		
		Category category = CyberTrackerHibernateManager.fetchByUuid(Category.class, e.getTag0(), session);
		if (category == null)
			addWarning(MessageFormat.format(Messages.SmartImporter_Warn_NoCategoryInDatamodel, e.getN(), e.getTag0()));

		return category;
	}

	private Employee fetchObserver(A a, Map<String, E> eMap, Session session) {
		if (a != null && a.getV() != null) {
			E e = eMap.get(a.getV());
			String tag0 = e != null ? e.getTag0() : null;
			if (tag0 != null) {
				Employee emp = CyberTrackerHibernateManager.fetchByUuid(Employee.class, tag0, session);
				if (emp == null) {
					addWarning(MessageFormat.format(Messages.AbstractSmartImporter_Observer_NotFound, e.getN(), e.getTag0()));
				}
				return emp;
			} else {
				addWarning(Messages.AbstractSmartImporter_Observer_InvalidData);
				return null;
			}
		}
		return null;
	}
	
	protected Date toDate(String strDate) {
		if (strDate == null)
			return null;
		DateFormat formatter = createCyberTrackerDateFormatter();
		try {
			return formatter.parse(strDate);
		} catch (ParseException e) {
			CyberTrackerPlugIn.log(e.getMessage(), e);
			return null;
		}
	}

	public void addAttachments(Waypoint wp, S s, Map<String, E> eMap, String namePrefix) {
		String mediaFolder = null;
		try {
			mediaFolder = PdaUtil.getCTMediaFolder();
		} catch (Exception ex) {
			CyberTrackerPlugIn.log("Could not determine CyberTracker ExportMedia folder", ex); //$NON-NLS-1$
		}
		for (S.A a : s.getA()) {
			if (isPhotoI(a.getI(), eMap)) {
				if (mediaFolder == null) {
					addWarning(MessageFormat.format(Messages.SmartImporter_Warn_ExportMedia_UnknownFolder, a.getV()));
					continue;
				}
				File file = new File(mediaFolder + a.getV());
				if (!file.exists()) {
					addWarning(MessageFormat.format(Messages.SmartImporter_Warn_ExportMedia_FileNotFound, mediaFolder, a.getV()));
					continue;
				}
				if (wp.getAttachments() == null) {
					wp.setAttachments(new ArrayList<WaypointAttachment>());
				}

				//create user friendly file name
				int index = a.getV().lastIndexOf('.');
				String ext = index >= 0 ? a.getV().substring(index) : ""; //$NON-NLS-1$
				String fileName = namePrefix+"_Waypoint_"+wp.getId()+ext; //$NON-NLS-1$
				
				WaypointAttachment attachment = new WaypointAttachment();
				attachment.setWaypoint(wp);
				attachment.setFilename(fileName);
				attachment.setCopyFromLocation(file);
				wp.getAttachments().add(attachment);
			}
		}
	}

	protected List<S> extractAndPreProcessSights(AbstractCyberTrackerData ctData) {
		List<S> sData = validateData(ctData);
		return SightsMultiObsUtil.convertMultiObs(sData, ctData.getElementsMap());
		
	}	
	
	private List<S> validateData(AbstractCyberTrackerData ctData) {
		List<S> result = new ArrayList<S>(ctData.getSData().size());
		for (S s : ctData.getSData()) {
			result.add(validateNoExtraData(s, ctData.getElementsMap()));
		}
		return result;
	}

	/**
	 * in one of the customers dataset we had some strange output from CT device (ticket #1626).
	 * #DefaultPtrolValues, Resume Patrol, #DefaultAttributeValues were recorded
	 * before "Start Patrol" item is recorded. This validation is indented to detect
	 * and remove this data of unknown nature (probably bug in CT) as this data may causes errors on import.
	 * @param s
	 * @param eMap 
	 */
	private S validateNoExtraData(S s, Map<String, E> eMap) {
		List<A> invalid = new ArrayList<A>();
		boolean started = false;
		for (A a : s.getA()) {
			started =  started || ScreensUtil.RESULT_ID.equals(a.getN()) || ScreensUtil.RESULT_START_DATE.equals(a.getN()) || ScreensUtil.RESULT_START_TIME.equals(a.getN());
			E e = eMap.get(a.getI());
			if (e == null) {
				addWarning(MessageFormat.format(Messages.AbstractSmartImporter_ElementMissing, a.getN(), a.getI()));
				invalid.add(a);
			} else if (!started) {
				//no start patrol record yet, only static data is valid (also ignore if value is empty)
				if (e.getStatic() == null && a.getV() != null) {
					addWarning(MessageFormat.format(Messages.AbstractSmartImporter_NonstaticDataBeforeStart, a.getN(), a.getI(), getDateTime(s)));
					invalid.add(a);
				}
			}
		}
		return invalid.isEmpty() ? s : cloneExcluding(s, invalid);
	}

	protected Date getDateTime(S s) {
		Date date = null;
		Time time = null;
		for (S.A a : s.getA()) {
			String i = a.getI();
			String v = a.getV();
			if (ICyberTrackerConstants.DATE.equals(i)) {
				try {
					DateFormat formatter = createCyberTrackerDateFormatter();
					date = formatter.parse(v);
					if (time != null) {
						return combine(date, time);
					}
				} catch (ParseException e) {
					CyberTrackerPlugIn.log(e.getMessage(), e);
				}
			} else if (ICyberTrackerConstants.TIME.equals(i)) {
				time = Time.valueOf(v);
				if (date != null) {
					return combine(date, time);
				}
			}
		}
		return combine(date, time);
	}

	private S cloneExcluding(S s, List<A> toExclude) {
		S sClone = new S();
		sClone.getA().addAll(s.getA());
		sClone.getA().removeAll(toExclude);
		return sClone;
	}
	
	public List<String> getWarnings() {
		return warnings;
	}
	
	protected void addWarning(String warning) {
		warnings.add(warning);
	}
	
	protected void clearWarning() {
		warnings.clear();
	}
	
	private boolean isPhotoI(String i, Map<String, E> eMap) {
		if (i != null) {
			if (ICyberTrackerConstants.PHOTO.equals(i))
				return true;
			E e = eMap.get(i);
			if (e != null) {
				return e.getN().startsWith(ScreensUtil.RESULT_PHOTO);
			}
		}
		return false;
	}

	public static class CoordinateZComparator implements Comparator<Coordinate> {
		@Override
		public int compare(Coordinate o1, Coordinate o2) {
			return ((Double) o1.z).compareTo((Double) o2.z);
		}
	}

}
