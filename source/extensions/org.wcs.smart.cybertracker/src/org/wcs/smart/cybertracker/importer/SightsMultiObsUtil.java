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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.AbstractCyberTrackerData;
import org.wcs.smart.cybertracker.model.data.Data.Elements.E;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S.A;

/**
 * Util class to support import of categories exported with option "Collect multiple observations"
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class SightsMultiObsUtil {

	/**
	 * Converts SMART output with multi observation data to a regular old fashioned data where each
	 * S tag contains a single observation.
	 * 
	 * @return
	 */
	public static final List<S> convertMultiObs(AbstractCyberTrackerData ctData) {
		return convertMultiObs(ctData.getSData(), ctData.getElementsMap());
	}
	
	private static final List<S> convertMultiObs(List<S> sData, Map<String, E> eMap) {
		//create and register some fake data that may be useful to import multiobservation groups
		FakeData fakeData = new FakeData();
		eMap.remove(fakeData.eAddAsNew.getI());
		eMap.remove(fakeData.eAddToLast.getI());
		eMap.put(fakeData.eAddAsNew.getI(), fakeData.eAddAsNew);
		eMap.put(fakeData.eAddToLast.getI(), fakeData.eAddToLast);
		
		List<S> result = new ArrayList<S>(sData.size());
		List<S> adoptList = new ArrayList<S>(); //list of <S> records that represent multiple observation group
		E groupCategory = null;
		
		for (S s : sData) {
			E catE = findMultiObsCategoryE(s, eMap);
			if (catE != null) {
				if (!adoptList.isEmpty()) {
					//ensure that it is the same multi-obs
					if (groupCategory != catE) {
						//this should never happen
						//TODO: error: unexpected end of multiple observations group in category groupCategory
						SmartPlugIn.displayLog(Messages.SightsMultiObsUtil_NonendedGroup, null);
					}
				}
				groupCategory = catE;
				if (isLastInGroup(s, eMap)) {
					if (ElementsUtil.isCategoryMultiObsSingleGps(groupCategory)) {
						result.addAll(adoptSingleGps(adoptList, s, fakeData));
					} else if (ElementsUtil.isCategoryMultiObsMultiGps(groupCategory)) {
						result.addAll(adoptMultiGps(adoptList, s, fakeData));
					} else {
						//TODO: unexpected group type
						SmartPlugIn.displayLog(Messages.SightsMultiObsUtil_UnknownCategory, null);
						result.addAll(adoptList);
						result.add(s);
					}
					adoptList.clear(); //group was processed and added, so clear the list
				} else {
					adoptList.add(s);
				}
			} else {
				//this is a regular <S> item
				//multiple observations group should be ended already if we are here
				if (!adoptList.isEmpty()) {
					//TODO: error: observations group in category groupCategory was not ended properly
					//need to end group
					SmartPlugIn.displayLog(Messages.SightsMultiObsUtil_GroupNotEndedProperly, null);
					result.addAll(adoptList);
					adoptList.clear();
				}
				result.add(s);
			}
		}

		if (!adoptList.isEmpty()) {
			//TODO: error: observations group in category groupCategory was not ended properly
			//need to end group
			SmartPlugIn.displayLog(Messages.SightsMultiObsUtil_GroupNotEndedProperlyEOF, null);
			result.addAll(adoptList);
			adoptList.clear();
		}
		return result;
	}

	private static List<S> adoptSingleGps(List<S> adoptList, S lastS, FakeData fakeData) {
		List<S> result = new ArrayList<S>(adoptList.size()+1);
		if (!adoptList.isEmpty()) {
			result.add(cloneS(adoptList.get(0), fakeData.aAddAsNew));
			//we are not adding onceAfterA to the first record, because it only contain GPS data 
			List<A> onceAfterAList = getEnterOnceAterList(lastS);
			onceAfterAList.add(fakeData.aAddToLast);
			A[] onceAfterA = onceAfterAList.toArray(new A[onceAfterAList.size()]);
			for (int i = 1; i < adoptList.size(); i++) {
				result.add(cloneS(adoptList.get(i), onceAfterA));
			}
			result.add(cloneS(lastS, fakeData.aAddToLast));
		} else {
			//observation group contain single (lastS) record
			//NOTE: this is invalid case because GPS data need to e recorded separately
			SmartPlugIn.displayLog(Messages.SightsMultiObsUtil_SigleGpsGroup_NoGps, null);
			result.add(cloneS(lastS, fakeData.aAddAsNew));
		}
		return result;
	}

	private static List<S> adoptMultiGps(List<S> adoptList, S lastS, FakeData fakeData) {
		List<S> result = new ArrayList<S>(adoptList.size()+1);
		List<A> onceAfterAList = getEnterOnceAterList(lastS);
		onceAfterAList.add(fakeData.aAddAsNew);
		A[] onceAfterA = onceAfterAList.toArray(new A[onceAfterAList.size()]);
		for (S s : adoptList) {
			result.add(cloneS(s, onceAfterA));
		}
		result.add(cloneS(lastS, fakeData.aAddAsNew));
		return result;
	}
	
	private static S cloneS(S s, A... appendA) {
		S cloned = new S();
		cloned.getA().addAll(s.getA());
		Collections.addAll(cloned.getA(), appendA);
		return cloned;
	}
	
	/**
	 * If this record has "#WaypointGroupEnd" = "End Observation Group"
	 */
	private static boolean isLastInGroup(S s, Map<String, E> eMap) {
		for (int i = s.getA().size()-1; i >= 0; i--) {
			A a = s.getA().get(i);
			if (ScreensUtil.RESULT_ENG_WAYPOINT_GROUP.equals(a.getN())) {
				E eV = eMap.get(a.getV());
				return eV != null && ElementsUtil.BOOL_TRUE.equals(eV.getTag0());
			}
		}
		//NOTE: we should never be here for a group record!!!
		return false;
	}

	private static List<A> getEnterOnceAterList(S s) {
		List<A> list = new ArrayList<A>();
		for (int i = s.getA().size()-1; i >= 0; i--) {
			A a = s.getA().get(i);
			if (ScreensUtil.RESULT_ENG_WAYPOINT_GROUP.equals(a.getN())) {
				for (int j = i+1; j < s.getA().size(); j++) {
					list.add(s.getA().get(j));
				}
				break;
			}
		}
		return list;
	}

	/**
	 * return the category value that matched
	 */
	private static final E findMultiObsCategoryE(S s, Map<String, E> elementsMap) {
		for (A a : s.getA()) {
			E eI = elementsMap.get(a.getI());
			if (ElementsUtil.isCategoryResultElement(eI)) {
				E eV = elementsMap.get(a.getV());
				if (ElementsUtil.isCategoryMultiObs(eV)) {
					return eV;
				}
			}
		}
		return null;
	}

	/**
	 * Fake "Add As New"/"Add To Last" options.
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	private static final class FakeData {
		
		E eAddAsNew;
		A aAddAsNew;
		E eAddToLast;
		A aAddToLast;
		
		public FakeData() {
			//fake "Add As New"/"Add To Last" option
			eAddAsNew = new E();
			eAddAsNew.setI("fake-as-new"); //$NON-NLS-1$
			eAddAsNew.setTag0(ElementsUtil.BOOL_TRUE);
			
			aAddAsNew = new A();
			aAddAsNew.setN(ScreensUtil.RESULT_NEW_WAYPOINT);
			aAddAsNew.setV(eAddAsNew.getI());
			
			eAddToLast = new E();
			eAddToLast.setI("fake-to-last"); //$NON-NLS-1$
			eAddToLast.setTag0(ElementsUtil.BOOL_FALSE);

			aAddToLast = new A();
			aAddToLast.setN(ScreensUtil.RESULT_NEW_WAYPOINT);
			aAddToLast.setV(eAddToLast.getI());
		}
		
	}
}