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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
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
import org.wcs.smart.cybertracker.util.SightsUtil;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.util.SharedUtils;

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

	private DateFormat formatter = createCyberTrackerDateFormatter();

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
		
		//filtering <A>
		List<String> iList = new ArrayList<>(aList.size()); //the list of unique "I" values in <A> that have "V"
		Map<String, List<String>> i2a = new HashMap<>();
		for (A a : aList) {
			if (a.getV() == null)
				continue;
			
			String i = a.getI();
			List<String> avList = i2a.get(i);
			if (avList == null) {
				iList.add(i);
				avList = new ArrayList<>();
				i2a.put(i, avList);
			}
			avList.add(a.getV());
		}
		
		for (String i : iList) {
			E e = eMap.get(i);
			List<String> avList = i2a.get(i); //must have at least one record (see filtering code above)

			if (ElementsUtil.DEFAULT_VALUES_ELEMENT_TAG.equals(e.getTag1())) {
				//handling default values
				if (avList.size() > 1) {
					addWarning(MessageFormat.format(Messages.AbstractSmartImporter_MultipleDefaultAttributesRecords, avList.size()));
				}
				String defaultData = avList.get(avList.size()-1);
				
				//import either old version style data (4.0.0 or lower) or new 4.0.1 or higher
				List<E> eLst = ElementsUtil.isCtIdsList(defaultData) ? getBefore401DefaultEValues(defaultData, eMap) : ElementsUtil.extractJsonDefaulValues(defaultData, eMap);
				for (E de : eLst) {
					if (de != null) {
						String tag2 = de.getTag2();
						WaypointObservationAttribute wpoa = createWaypointObservationAttribute(de, Arrays.asList(tag2), eMap, session);
						if (wpoa == null)
							continue;
						wpoa.setObservation(obs);
						result.add(wpoa);
					} else {
						addWarning(Messages.AbstractSmartImporter_MissingDefaultAttributeRecord);
					}
				}
				continue;
			}
				
			if (!ElementsUtil.ATTRIBUTE_ELEMENT_TAG.equals(e.getTag1()))
				continue;

			WaypointObservationAttribute wpoa = createWaypointObservationAttribute(e, avList, eMap, session);
			if (wpoa == null)
				continue;
			wpoa.setObservation(obs);
			result.add(wpoa);
		}	
		return result;
	}

	private List<E> getBefore401DefaultEValues(String defaultData, Map<String, E> eMap) {
		List<String> ctidLst = Arrays.asList(defaultData.split(ICyberTrackerConstants.ATTRIBUTE_DEFAULT_VALUES_SEPATATOR));
		return ctidLst.stream().map(ctid -> eMap.get(ctid)).collect(Collectors.toList());
	}

	private WaypointObservationAttribute createWaypointObservationAttribute(E e, List<String> avList, Map<String, E> eMap, Session session) {
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
		
		if (avList.size() > 1 && !AttributeType.TREE.equals(attr.getType())) {
			//only tree attributes might have multiple recorded values - one per level in hierarchy (by design)
			addWarning(MessageFormat.format(Messages.AbstractSmartImporter_MultipleValuesForAttribute, e.getN()));
		}
		//NOTE: avList must have at least one record by design!!!
		String av = avList.get(avList.size()-1); //used everywhere except trees

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
			//tree may have multiple values - one for each hierarchy level, ideally the last level should match, otherwise try to use next to last
			AttributeTreeNode item = null;
			E eTr = null;
			StringBuilder warnSb = new StringBuilder();
			int i = avList.size()-1;
			for (; i >= 0; i--) {
				eTr = eMap.get(avList.get(i));
				item = CyberTrackerHibernateManager.fetchByUuid(AttributeTreeNode.class, eTr.getTag0(), session);
				if (item == null) {
					if (warnSb.length() > 0) {
						warnSb.append(", "); //$NON-NLS-1$
					}
					warnSb.append(MessageFormat.format(Messages.AbstractSmartImporter_MissingTreeItemDetails, eTr.getN(), eTr.getTag0(), i+1));
				} else {
					break; //we found good item that exists in database
				}
			}
			
			if (item == null) {
				//no items found
				addWarning(MessageFormat.format(Messages.AbstractSmartImporter_NoTreeAttrItemInDatamodel_Skip, e.getN(), warnSb));
				return null;
				
			}
			
			if (warnSb.length() > 0) { //same as i != avList.size()-1 (leaf node was not found)
				addWarning(MessageFormat.format(Messages.AbstractSmartImporter_NoTreeAttrItemInDatamodel_Replace, e.getN(), warnSb, eTr.getN(), i+1));
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
		int fileCnt = 0;
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
				String fileName = namePrefix + "_Waypoint_" + wp.getId() + (fileCnt == 0 ? "" : "_" + fileCnt) + ext; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				fileCnt++;
				
				WaypointAttachment attachment = new WaypointAttachment();
				attachment.setWaypoint(wp);
				attachment.setFilename(fileName);
				attachment.setCopyFromLocation(file);
				wp.getAttachments().add(attachment);
			}
		}
	}

	/**
	 * NOTE: This method must never return original list as this list can be changed in future.
	 */
	protected List<S> extractAndPreProcessSights(AbstractCyberTrackerData ctData) {
		List<S> sData = validateData(ctData);
		return SightsMultiObsUtil.convertMultiObs(sData, ctData.getElementsMap());
	}	
	
	private List<S> validateData(AbstractCyberTrackerData ctData) {
		List<S> result = new ArrayList<S>(ctData.getSData().size());
		for (S s : ctData.getSData()) {
			if (!validateMandatoryData(s)) {
				continue;
			}
			result.add(validateNoExtraData(s, ctData.getElementsMap()));
		}
		return result;
	}

	private boolean validateMandatoryData(S s) {
		Set<String> mandatoryNames = new TreeSet<>(Arrays.asList("Date", "Time")); //$NON-NLS-1$ //$NON-NLS-2$
		
		//currently this ensures that every <S> has date and time
		for (A a : s.getA()) {
			String i = a.getI();
			if (ICyberTrackerConstants.DATE.equals(i)) {
				try {
					formatter.parse(a.getV());
					mandatoryNames.remove("Date"); //$NON-NLS-1$
				} catch (ParseException e) {
					addWarning(MessageFormat.format(Messages.AbstractSmartImporter_ParseDateError, a.getV()));
					CyberTrackerPlugIn.log(e.getMessage(), e);
					return false;
				}
			} else if (ICyberTrackerConstants.TIME.equals(i)) {
				mandatoryNames.remove("Time"); //$NON-NLS-1$
			}
			if (mandatoryNames.isEmpty()) {
				return true;
			}
		}
		addWarning(MessageFormat.format(Messages.AbstractSmartImporter_MissingMandatoryField, mandatoryNames.toString()));
		return false;
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

	/**
	 * Returns a mapping containing rest time in minutes for each day.
	 * @param sList
	 * @param eMap
	 * @return
	 */
	protected RestTimeMap extractRestTime(List<S> sList, Map<String, E> eMap) {
		Comparator<Date> dateComparator = new Comparator<Date>() {
			@Override
			public int compare(Date d1, Date d2) {
				if (d1 == null) return d2 == null ? 0 : -1;
				if (d2 == null) return 1; //d1 is bigger
				if (d1.equals(d2) || SharedUtils.isSameDate(d1, d2)) return 0;
				return d1.compareTo(d2);
			}
		};
		Map<Date, Long> restMap = new TreeMap<>(dateComparator);
		Set<S> pauseSet = new HashSet<>();
		Date pauseDate = null;
		Time pauseTime = null;
		
		for (S s : sList) {
			Date date = null;
			Time time = null;
			boolean paused = false;
			for (A a : s.getA()) {
				String i = a.getI();
				String n = a.getN();
				String v = a.getV();
				if (ICyberTrackerConstants.DATE.equals(i)) {
					date = toDate(v);
				} else if (ICyberTrackerConstants.TIME.equals(i)) {
					time = Time.valueOf(v);
				} else if (ScreensUtil.RESULT_PAUSED.equals(n)) {
					paused = ICyberTrackerConstants.STR_TRUE.equals(v);
					if (!pauseSet.contains(s) && !SightsUtil.hasWaypointData(s, eMap)) {
						pauseSet.add(s);
					}
				}
			}
			
			if (paused) {
				//<S> contains info that patrol/mission was paused
				if (pauseDate == null && pauseTime == null) {
					pauseDate = date;
					pauseTime = time;
				} else {
					addWarning(MessageFormat.format(Messages.AbstractSmartImporter_PauseRecordedSeveralTimes, combine(pauseDate, pauseTime), combine(date, time)));
				}
			} else if (pauseDate != null || pauseTime != null) {
				//record rest time
				if (SharedUtils.isSameDate(pauseDate, date)) {
					//patrol was paused during the day
					Long rest = restMap.get(date);
					if (rest == null) rest = 0L;
					if (time.after(pauseTime)) {
						rest += time.getTime() - pauseTime.getTime();
					} else {
						addWarning(MessageFormat.format(Messages.AbstractSmartImporter_InvalidTimeFrame, combine(pauseDate, pauseTime), combine(date, time)));
					}
					restMap.put(date, rest);
				} else {
					//patrol was paused overnight day
					if (date.after(pauseDate)) {
						//record for day it was paused
						Long rest = restMap.get(pauseDate);
						if (rest == null) rest = 0L;
						rest += SharedUtils.getDatePart(pauseDate, true).getTime() - combine(pauseDate, pauseTime).getTime(); //time at the end of the day minus current time in milliseconds
						restMap.put(pauseDate, rest);
						//record for intermediate days
						Calendar c = Calendar.getInstance(); 
						c.setTime(pauseDate); 
						c.add(Calendar.DATE, 1);
						Date intermediateDate = c.getTime();
						while (!SharedUtils.isSameDate(date, intermediateDate)) {
							restMap.put(intermediateDate, 1000*60*60*24L); //rest all day
							c.add(Calendar.DATE, 1);
							intermediateDate = c.getTime();
						}
						//record for day it was resumed
						restMap.put(date, time.getTime() - SharedUtils.getDatePart(time, false).getTime()); //it must be a first rest time during this day; need to subtract as time zone is taken into account
					} else {
						addWarning(MessageFormat.format(Messages.AbstractSmartImporter_InvalidTimeFrame, combine(pauseDate, pauseTime), combine(date, time)));
					}
				}
				//reset pause date/time
				pauseDate = null;
				pauseTime = null;
			}
		}
		return new RestTimeMap(restMap, pauseSet);
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

	/**
	 * Represents a mapping containing rest time in minutes for each day.
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	public static class RestTimeMap {
		
		private Map<Date, Long> restMap;
		private Set<S> pauseSet;

		private RestTimeMap(Map<Date, Long> restMap, Set<S> pauseSet) {
			this.restMap = restMap;
			this.pauseSet = pauseSet;
		}
		
		public Integer getRestMinutes(Date date) {
			Long rest = restMap.get(date);
			return (int) (rest != null ? rest/(60*1000) : 0);
		}
		
		public List<S> excludePauseS(List<S> sList) {
			List<S> result = new ArrayList<>(sList);
			result.removeAll(pauseSet);
			return result;
		}
	}
}
