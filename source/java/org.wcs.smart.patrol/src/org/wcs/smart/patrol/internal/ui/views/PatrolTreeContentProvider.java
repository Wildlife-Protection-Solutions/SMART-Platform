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
package org.wcs.smart.patrol.internal.ui.views;

import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Content provider for the patrol list viewer that allows the grouping
 * of patrols by various patrol attributes.
 * 
 * @author Emily
 *
 */
public class PatrolTreeContentProvider implements ITreeContentProvider{

	public static enum GroupByType{
		FOLDER ("Folder"), 
		STATION ("Station"),
		TEAM ("Team"),
		PATROLTYPE ("Patrol Type"),
		MANDATE ("Mandate"),
		TRANSPORTTYPE ("Transport Type"),
		YEAR("Year"),
		MONTH("Month"),
		NONE("None");
		
		String guiName;
		
		GroupByType(String guiName){
			this.guiName = guiName;
		}
	}
	
	private final static SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("MMM, YYYY");
	private final static SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("YYYY");
	private GroupByType groupBy = GroupByType.NONE;

	private Object input;
	private PatrolEditorInput[] patrols;
	
	private HashMap<Object, List<PatrolEditorInput>> sorted;
	private List<Object> sortedKeys;
	
	private static Team NONE_TEAM = new Team();
	private static Station NONE_STATION = new Station();
	static{
		NONE_TEAM.setName("None");
		NONE_STATION.setName("None");
	}
	private Viewer viewer;
	
	public void setGroupBy(String newGroupBy){
		GroupByType type = GroupByType.NONE;
		for (GroupByType t : GroupByType.values()){
			if (t.name().equals(newGroupBy)){
				type = t;
				break;
			}
		}
		setGroupBy(type);
	}
	
	public void setGroupBy(GroupByType groupBy){
		this.groupBy = groupBy;
		if (patrols == null) return;
		
		//group data
		if (groupBy == GroupByType.YEAR){
			sorted = new HashMap<>();
			sortedKeys = new ArrayList<>();
			for (PatrolEditorInput p : patrols){
				String year = YEAR_FORMAT.format(p.getStartDate());
				List<PatrolEditorInput> pp = sorted.get(year);
				if (pp == null){
					pp = new ArrayList<>();
					sorted.put(year, pp);
					sortedKeys.add(year);
				}
				pp.add(p);
			}
			sortedKeys.sort((a,b)->{
				Integer a1 = Integer.valueOf((String)a);
				Integer b1 = Integer.valueOf((String)b);
				return -1 * a1.compareTo(b1);
			});
		}else if (groupBy == GroupByType.MONTH){
			sorted = new HashMap<>();
			sortedKeys = new ArrayList<>();
			List<Integer[]> dates = new ArrayList<>();
			for (PatrolEditorInput p : patrols){
				String month = MONTH_FORMAT.format(p.getStartDate());
				List<PatrolEditorInput> pp = sorted.get(month);
				if (pp == null){
					pp = new ArrayList<>();
					sorted.put(month, pp);
//					sortedKeys.add(month);
					Calendar c = Calendar.getInstance();
					c.setTime(p.getStartDate());
					dates.add(new Integer[]{c.get(Calendar.MONTH), c.get(Calendar.YEAR)});
				}
				pp.add(p);
			}
			
			dates.sort((a,b)->{
				Integer m1 = Integer.valueOf(a[0]);
				Integer y1 = Integer.valueOf(a[1]);
				Integer m2 = Integer.valueOf(b[0]);
				Integer y2 = Integer.valueOf(b[1]);
				if (y1 == y2) return -1 * m1.compareTo(m2);
				return -1 * y1.compareTo(y2);
			});
			for (Integer[] date : dates){
				Calendar c = Calendar.getInstance();
				c.set(Calendar.MONTH, date[0]);
				c.set(Calendar.YEAR, date[1]);
				sortedKeys.add(MONTH_FORMAT.format(c.getTime()));
			}
			
		}else if (groupBy == GroupByType.PATROLTYPE){
			sorted = new HashMap<>();
			sortedKeys = new ArrayList<>();
			
			for (PatrolEditorInput p : patrols){
				PatrolType.Type type = p.getType();
				List<PatrolEditorInput> pp = sorted.get(type);
				if (pp == null){
					pp = new ArrayList<>();
					sorted.put(type, pp);
					sortedKeys.add(type);
				}
				pp.add(p);
			}
			sortedKeys.sort((a,b)->{
				String s1 = ((PatrolType.Type)a).getGuiName(Locale.getDefault());
				String s2 = ((PatrolType.Type)a).getGuiName(Locale.getDefault());
				return Collator.getInstance().compare(s1, s2);
			});
		}else if (groupBy == GroupByType.MANDATE){
			PatrolEditorInput[] currentInput = patrols;
			inputChanged(viewer, input, DialogConstants.LOADING_TEXT);
			
			//we need to get the patrol type for each patrol and configure it
			Job j = new Job("sorting patrols"){

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					HashMap<PatrolMandate, List<PatrolEditorInput>> types = new HashMap<>();
					Session s = HibernateManager.openSession();
					try{
						for (PatrolEditorInput p : currentInput){
							Patrol pp = (Patrol) s.get(Patrol.class, p.getUuid());
							for (PatrolLeg leg : pp.getLegs()){
								PatrolMandate m = leg.getMandate();
								List<PatrolEditorInput> mpatrols = types.get(m);
								if (mpatrols == null){
									mpatrols = new ArrayList<>();
									types.put(m, mpatrols);
								}
								mpatrols.add(p);
							}
						}
					}finally{
						s.close();
					}
					sorted = new HashMap<>();
					sortedKeys = new ArrayList<>();
					sorted.putAll(types);
					sortedKeys.addAll(types.keySet());
					sortNamedItems(sortedKeys);					
					Display.getDefault().syncExec(()->{
						patrols = currentInput;
						viewer.refresh();
					});
					return Status.OK_STATUS;
				}
			};
			j.schedule();
		}else if (groupBy == GroupByType.TRANSPORTTYPE){
			PatrolEditorInput[] currentInput = patrols;
			inputChanged(viewer, input, DialogConstants.LOADING_TEXT);
			
			//we need to get the patrol type for each patrol and configure it
			Job j = new Job("sorting patrols"){

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					HashMap<PatrolTransportType, List<PatrolEditorInput>> types = new HashMap<>();
					Session s = HibernateManager.openSession();
					try{
						for (PatrolEditorInput p : currentInput){
							Patrol pp = (Patrol) s.get(Patrol.class, p.getUuid());
							for (PatrolLeg leg : pp.getLegs()){
								PatrolTransportType m = leg.getType();
								List<PatrolEditorInput> mpatrols = types.get(m);
								if (mpatrols == null){
									mpatrols = new ArrayList<>();
									types.put(m, mpatrols);
								}
								mpatrols.add(p);
							}
						}
					}finally{
						s.close();
					}
					sorted = new HashMap<>();
					sortedKeys = new ArrayList<>();
					sorted.putAll(types);
					sortedKeys.addAll(types.keySet());
					sortNamedItems(sortedKeys);					
					Display.getDefault().syncExec(()->{
						patrols = currentInput;
						viewer.refresh();
					});
					return Status.OK_STATUS;
				}
			};
			j.schedule();
		}else if (groupBy == GroupByType.TEAM){
			PatrolEditorInput[] currentInput = patrols;
			inputChanged(viewer, input, DialogConstants.LOADING_TEXT);
			
			//we need to get the patrol type for each patrol and configure it
			Job j = new Job("sorting patrols"){

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					HashMap<Team, List<PatrolEditorInput>> types = new HashMap<>();
					Session s = HibernateManager.openSession();
					try{
						for (PatrolEditorInput p : currentInput){
							Patrol pp = (Patrol) s.get(Patrol.class, p.getUuid());
							
							Team team = pp.getTeam();
							if (team == null){
								team = NONE_TEAM;
							}
							List<PatrolEditorInput> mpatrols = types.get(team);
							if (mpatrols == null){
								mpatrols = new ArrayList<>();
								types.put(team, mpatrols);
							}
							mpatrols.add(p);
						}
					}finally{
						s.close();
					}
					sorted = new HashMap<>();
					sortedKeys = new ArrayList<>();
					sorted.putAll(types);
					sortedKeys.addAll(types.keySet());
					sortNamedItems(sortedKeys);
					
					Display.getDefault().syncExec(()->{
						//viewer.setInput(currentInput);
						patrols = currentInput;
						viewer.refresh();
					});
					return Status.OK_STATUS;
				}
			};
			j.schedule();
		}else if (groupBy == GroupByType.STATION){
			PatrolEditorInput[] currentInput = patrols;
			inputChanged(viewer, input, DialogConstants.LOADING_TEXT);
			
			//we need to get the patrol type for each patrol and configure it
			Job j = new Job("sorting patrols"){

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					HashMap<Station, List<PatrolEditorInput>> types = new HashMap<>();
					Session s = HibernateManager.openSession();
					try{
						for (PatrolEditorInput p : currentInput){
							Patrol pp = (Patrol) s.get(Patrol.class, p.getUuid());
							
							Station station = pp.getStation();
							if (station == null) station = NONE_STATION;
							List<PatrolEditorInput> mpatrols = types.get(station);
							if (mpatrols == null){
								mpatrols = new ArrayList<>();
								types.put(station, mpatrols);
							}
							mpatrols.add(p);
						}
					}finally{
						s.close();
					}
					sorted = new HashMap<>();
					sortedKeys = new ArrayList<>();
					sorted.putAll(types);
					sortedKeys.addAll(types.keySet());
					sortNamedItems(sortedKeys);
					
					Display.getDefault().syncExec(()->{
						patrols = currentInput;
						viewer.refresh();
					});
					return Status.OK_STATUS;
				}
			};
			j.schedule();
		}
		
		
		viewer.refresh();
		
		
	}
	
	private void sortNamedItems(List<Object> items){
		items.sort((a,b)->{
			if (a.equals(b)) return 0;
			if (a == NONE_STATION || a == NONE_TEAM) return 1;
			if (b == NONE_STATION || b == NONE_TEAM) return -1;
			return Collator.getInstance().compare(((NamedItem)a).getName(), ((NamedItem)b).getName());
		});
	}
	
	@Override
	public void dispose() {

	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = viewer;
		patrols = null;
		input = null;
		sorted = null;	
		if (newInput instanceof PatrolEditorInput[]){
			patrols = (PatrolEditorInput[])newInput;
		}
		input = newInput;		
		setGroupBy(groupBy);
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (sorted != null){
			return sortedKeys.toArray();
		}else if (patrols != null){
			 return patrols;
		}else if (input != null){
			return new Object[]{input};
		}
		return null;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (sorted != null){
			return sorted.get(parentElement).toArray();
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (sorted != null && element instanceof PatrolEditorInput){
			PatrolEditorInput i = (PatrolEditorInput)element;
			
			for (Entry<Object, List<PatrolEditorInput>> entry : sorted.entrySet()){
				for (PatrolEditorInput e : entry.getValue()){
					if (e.equals(i)) return entry.getKey();
				}
			}
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (sorted != null && sortedKeys.contains(element)){
			return true;
		}
		return false;
	}
}
