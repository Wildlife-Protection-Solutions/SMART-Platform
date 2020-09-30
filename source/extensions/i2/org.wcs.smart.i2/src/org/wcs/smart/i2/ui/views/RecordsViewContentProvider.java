/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.views;

import java.text.Collator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecord.Status;
import org.wcs.smart.i2.model.IntelRecordSource;

/**
 * Content provider fro records list view.
 * @author Emily
 *
 */
public class RecordsViewContentProvider implements ITreeContentProvider {

	/**
	 * Sort by options
	 * @author Emily
	 *
	 */
	public static enum SortBy{
		SOURCE (Messages.RecordsViewContentProvider_SourceSortByOption), 
		NAME(Messages.RecordsViewContentProvider_NameSortByOption), 
		DATE(Messages.RecordsViewContentProvider_DateSortByOption),
		PROFILE(Messages.RecordsViewContentProvider_ProfileSortByOp);
		
		private String guiName;
		
		SortBy(String name) {
			this.guiName= name;
		}
		
		public String getGuiName() {
			return this.guiName;
		}
	};
	
	/**
	 * Group by options
	 * @author Emily
	 *
	 */
	public static enum GroupBy{
		SOURCE (Messages.RecordsViewContentProvider_SourceGroupByOption), 
		STATUS (Messages.RecordsViewContentProvider_StatusGroupByOption),		
		MONTH(Messages.RecordsViewContentProvider_MonthGroupByOption),
		PROFILE(Messages.RecordsViewContentProvider_ProfileGroupByOp),
		NONE(Messages.RecordsViewContentProvider_NoneGroupByOption);
		
		private String guiName;
		
		GroupBy(String name) {
			this.guiName = name;
		}
		
		public String getGuiName() {
			return this.guiName;
		}
	}
	
	/**
	 * NONE Source key
	 */
	public static final String NONE_SOURCE = Messages.RecordsViewContentProvider_NoneSourceLabel;
	
	
	private SortBy sortBy = SortBy.DATE;
	private GroupBy groupBy = null;
	
	private List<IntelRecordProxy> records;
	private HashMap<Object, List<IntelRecordProxy>> groups = null;
	private Object input;	
	
	/**
	 * Sets the sort by option
	 * @param sortBy
	 */
	public void setSortBy(SortBy sortBy) {
		this.sortBy = sortBy;
		setupData();
	}
	
	/**
	 * Sets the group by option
	 * @param groupBy
	 */
	public void setGroupBy(GroupBy groupBy) {
		if (groupBy == GroupBy.NONE) {
			this.groupBy = null;
		}else {
			this.groupBy = groupBy;
		}
		setupData();
	}
	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		input = newInput;
		this.records = null;
		this.groups = null;
		
		setupData();
	}
	
	@Override
	public Object[] getElements(Object inputElement) {
		if (records == null) return (Object[]) input;
		if (records.size() == 0) return new Object[] {};
		if (groupBy == null) {
			return records.toArray();
		}else {
			List<Object> x = new ArrayList<>(groups.keySet());
			if (groupBy == GroupBy.MONTH) {
				x.sort((a,b)-> -1 * ((LocalDate)a).compareTo((LocalDate)b));
			}else if (groupBy == GroupBy.STATUS) {
				x.sort((a,b)-> {
					IntelRecord.Status s1 = (IntelRecord.Status)a;
					IntelRecord.Status s2 = (IntelRecord.Status)b;
					if (s1 == s2) return 0;
					if (s1 == Status.NEW) return -1;
					if (s1 == Status.COMPLETE) return 1;
					if (s2 == Status.NEW) return 1;
					if (s2 == Status.COMPLETE) return -1;
					return 0;
				});
			}else if (groupBy == GroupBy.SOURCE) {
				x.sort((a,b)-> {
					String s1 = ""; //$NON-NLS-1$
					String s2 = ""; //$NON-NLS-1$
					if (a == b) return 0;
					if (a == NONE_SOURCE) return 1;
					if (b == NONE_SOURCE) return -1;
					if (a instanceof String) {
						s1 = (String)a;
					}else if (a instanceof IntelRecordSource) {
						s1 = ((IntelRecordSource)a).getName();
					}
					if (b instanceof String) {
						s2 = (String)a;
					}else if (b instanceof IntelRecordSource) {
						s2 = ((IntelRecordSource)b).getName();
					}
					return Collator.getInstance().compare(s1, s2);
				});
			}else if (groupBy == GroupBy.PROFILE) {
				x.sort((a,b)->{
					return Collator.getInstance().compare(((IntelProfile)a).getName(), ((IntelProfile)b).getName());
				});

			}
			return x.toArray();
		}
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (groups.keySet().contains(parentElement)) {
			return groups.get(parentElement).toArray();
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (groups == null) return null;
		for (Entry<Object, List<IntelRecordProxy>> ee : groups.entrySet()) {
			if (ee.getValue().contains(element)) return ee.getKey();
		}
		return null;
	}
	

	@Override
	public boolean hasChildren(Object element) {
		if (groups == null) return false;
		if (groups.containsKey(element)) return true;
		return false;
	}

	/**
	 * 
	 * Converts an intelligence record to a date;
	 * @param record 
	 * @return
	 */
	private LocalDateTime getDate(IntelRecordProxy record) {
		return record.getDate();
	}
	
	/**
	 * Sort a list of records based on the sortBy option
	 * @param tosort
	 */
	private void sort(List<IntelRecordProxy> tosort) {
		if (sortBy == null) return;
		
		if (sortBy == SortBy.NAME) {
			Collections.sort(tosort, (a,b) -> Collator.getInstance().compare(a.getTitle(), b.getTitle()));
		}else if (sortBy == SortBy.DATE) {
			Collections.sort(tosort, (a,b) -> -1* getDate(a).compareTo(getDate(b)));
		}else if (sortBy == SortBy.SOURCE){
			Collections.sort(tosort, (a,b)->{
				if ((a.getRecordSource() == null && b.getRecordSource() == null) || (a.getRecordSource() != null && a.getRecordSource().equals(b.getRecordSource()))) {
					return a.getTitle().compareTo(b.getTitle());
				}else {
					if (a.getRecordSource() == null) return 1;
					if (b.getRecordSource() == null) return -1;
					return a.getRecordSource().getName().compareTo(b.getRecordSource().getName());
				}
			});
		}else if (sortBy == SortBy.PROFILE) {
			Collections.sort(tosort, (a,b)->{
				return Collator.getInstance().compare(a.getProfile().getName(), b.getProfile().getName());
			});
		}
	}
	
	/**
	 * groups and sorts the data as configured
	 */
	private void setupData() {
		if (input instanceof List) {
			records = new ArrayList<>();
			for (Object x : (List<?>)input) {
				if (x instanceof IntelRecordProxy) {
					records.add((IntelRecordProxy)x);
				}
			}
			if (records.isEmpty()) return;
			
			if (groupBy != null) {
				groups = new HashMap<Object, List<IntelRecordProxy>>();
				if (groupBy == GroupBy.SOURCE) {
					for (IntelRecordProxy r : records) {
						Object x = r.getRecordSource();
						if (x == null) x = NONE_SOURCE;
						List<IntelRecordProxy> items = groups.get(x);
						if (items == null) {
							items = new ArrayList<>();
							groups.put(x, items);
						}
						items.add(r);
					}	
	
				}else if (groupBy == GroupBy.STATUS) {
					for (IntelRecordProxy r : records) {
						List<IntelRecordProxy> items = groups.get(r.getStatus());
						if (items == null) {
							items = new ArrayList<>();
							groups.put(r.getStatus(), items);
						}
						items.add(r);
					}	
				}else if (groupBy == GroupBy.PROFILE) {
					for (IntelRecordProxy r : records) {
						List<IntelRecordProxy> items = groups.get(r.getProfile());
						if (items == null) {
							items = new ArrayList<>();
							groups.put(r.getProfile(), items);
						}
						items.add(r);
					}	
				}else if (groupBy == GroupBy.MONTH) {
					for (IntelRecordProxy r : records) {
						LocalDateTime d = getDate(r);
						LocalDate mon = LocalDate.of(d.getYear(), d.getMonth(), 1);
						
						List<IntelRecordProxy> items = groups.get(mon);
						if (items == null) {
							items = new ArrayList<>();
							groups.put(mon, items);
						}
						items.add(r);
					}	
				}
				
				for (List<IntelRecordProxy> toSort  : groups.values()) {
					sort(toSort);
				}
			}else if (sortBy != null) {
				//sort the input
				sort(records);
			}
		}	
	}
}
