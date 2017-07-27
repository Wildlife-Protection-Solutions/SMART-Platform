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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.internal.Messages;
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

	private final static String sortingJobName = Messages.PatrolTreeContentProvider_JobName;
	
	public static enum GroupByType{
		FOLDER (Messages.PatrolTreeContentProvider_FolderOption), 
		STATION (Messages.PatrolTreeContentProvider_StationOption),
		TEAM (Messages.PatrolTreeContentProvider_TeamOption),
		PATROLTYPE (Messages.PatrolTreeContentProvider_TypeOption),
		MANDATE (Messages.PatrolTreeContentProvider_MandateOption),
		TRANSPORTTYPE (Messages.PatrolTreeContentProvider_TransportOption),
		YEAR(Messages.PatrolTreeContentProvider_YearOption),
		MONTH(Messages.PatrolTreeContentProvider_MonthOption),
		NONE(Messages.PatrolTreeContentProvider_NoneOption);
		
		String guiName;
		
		GroupByType(String guiName){
			this.guiName = guiName;
		}
	}
	
	private static Team NONE_TEAM = new Team();
	private static Station NONE_STATION = new Station();
	
	static{
		NONE_TEAM.setName(Messages.PatrolTreeContentProvider_NoTeamLabel);
		NONE_STATION.setName(Messages.PatrolTreeContentProvider_NoStationLabel);
	}
	
	private GroupByType groupBy = GroupByType.NONE;

	//input
	private Object input;
	//all patrols
	private PatrolEditorInput[] patrols;
	//sorted patrols (if applicable)
	private HashMap<Object, List<PatrolEditorInput>> sorted;
	//sorted keys (if applicable)
	private List<Object> sortedKeys;
	
	private Viewer viewer;
	
	public PatrolTreeContentProvider(){
		this(null);
	}
	
	/**
	 * Creates a new content provider with the given group by default
	 * @param defaultValue
	 */
	public PatrolTreeContentProvider(String defaultValue){
		setGroupBy(defaultValue);
	}
	
	public GroupByType getGroupBy(){
		return this.groupBy;
	}
	
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
		synchronized (this) {
			sorted = null;
			sortedKeys = null;
		}
		//group data
		if (groupBy == GroupByType.YEAR){
			groupByYear();
		}else if (groupBy == GroupByType.MONTH){
			groupByMonth();
		}else if (groupBy == GroupByType.PATROLTYPE){
			groupByPatrolType();
		}else if (groupBy == GroupByType.MANDATE){
			groupByMandate();
		}else if (groupBy == GroupByType.TRANSPORTTYPE){
			groupByTransportType();
		}else if (groupBy == GroupByType.TEAM){
			groupByTeam();
		}else if (groupBy == GroupByType.STATION){
			groupByStation();
		}else if (groupBy == GroupByType.NONE || groupBy == GroupByType.FOLDER){
			setGroupByData(patrols, null,  null);
		}
	}
	
	private void groupByStation(){
		PatrolEditorInput[] currentInput = patrols;
		inputChanged(viewer, input, DialogConstants.LOADING_TEXT);
		
		//we need to get the patrol type for each patrol and configure it
		Job j = new Job(sortingJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				HashMap<Station, List<PatrolEditorInput>> types = new HashMap<>();
				try(Session s = HibernateManager.openSession()){
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
				}
				HashMap<Object, List<PatrolEditorInput>> sorted = new HashMap<>();
				List<Object> sortedKeys = new ArrayList<>();
				sorted.putAll(types);
				sortedKeys.addAll(types.keySet());
				sortNamedItems(sortedKeys);
				
				setGroupByData(currentInput, sorted, sortedKeys);
				return Status.OK_STATUS;
			}
		};
		j.schedule();
	}
	
	private void groupByTeam(){
		PatrolEditorInput[] currentInput = patrols;
		inputChanged(viewer, input, DialogConstants.LOADING_TEXT);
		
		Job j = new Job(sortingJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				HashMap<Team, List<PatrolEditorInput>> types = new HashMap<>();
				try(Session s = HibernateManager.openSession()){
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
				}
				HashMap<Object, List<PatrolEditorInput>> sorted = new HashMap<>();
				List<Object> sortedKeys = new ArrayList<>();
				sorted.putAll(types);
				sortedKeys.addAll(types.keySet());
				sortNamedItems(sortedKeys);
				
				setGroupByData(currentInput, sorted, sortedKeys);
				return Status.OK_STATUS;
			}
		};
		j.schedule();
	}
	
	private void groupByTransportType(){
		PatrolEditorInput[] currentInput = patrols;
		inputChanged(viewer, input, DialogConstants.LOADING_TEXT);
		
		Job j = new Job(sortingJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				HashMap<PatrolTransportType, List<PatrolEditorInput>> types = new HashMap<>();
				try(Session s = HibernateManager.openSession()){
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
				}
				HashMap<Object, List<PatrolEditorInput>> sorted = new HashMap<>();
				List<Object> sortedKeys = new ArrayList<>();
				sorted.putAll(types);
				sortedKeys.addAll(types.keySet());
				sortNamedItems(sortedKeys);					
				setGroupByData(currentInput, sorted, sortedKeys);
				return Status.OK_STATUS;
			}
		};
		j.schedule();
	}
	
	private void groupByMandate(){
		PatrolEditorInput[] currentInput = patrols;
		inputChanged(viewer, input, DialogConstants.LOADING_TEXT);

		Job j = new Job(sortingJobName){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				HashMap<PatrolMandate, List<PatrolEditorInput>> types = new HashMap<>();
				try(Session s = HibernateManager.openSession()){
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
				}
				HashMap<Object, List<PatrolEditorInput>> sorted = new HashMap<>();
				List<Object> sortedKeys = new ArrayList<>();
				sorted.putAll(types);
				sortedKeys.addAll(types.keySet());
				sortNamedItems(sortedKeys);				
				setGroupByData(currentInput, sorted, sortedKeys);
				return Status.OK_STATUS;
			}
		};
		j.schedule();
	}
	
	private void groupByPatrolType(){
		HashMap<Object, List<PatrolEditorInput>> sorted = new HashMap<>();
		List<Object> sortedKeys = new ArrayList<>();
		
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
		setGroupByData(patrols, sorted, sortedKeys);
	}
	
	private void groupByYear(){
		HashMap<Object, List<PatrolEditorInput>> sorted = new HashMap<>();
		List<Object> sortedKeys = new ArrayList<>();
		for (PatrolEditorInput p : patrols){
			DateGroupBy year = new DateGroupBy(p.getStartDate(), DateGroupBy.Type.YEAR);
			List<PatrolEditorInput> pp = sorted.get(year);
			if (pp == null){
				pp = new ArrayList<>();
				sorted.put(year, pp);
				sortedKeys.add(year);
			}
			pp.add(p);
		}
		sortedKeys.sort((a,b)->{
			Integer a1 = Integer.valueOf(((DateGroupBy)a).getYear());
			Integer b1 = Integer.valueOf(((DateGroupBy)a).getYear());
			return -1 * a1.compareTo(b1);
		});
		setGroupByData(patrols, sorted, sortedKeys);
	}
	
	private void groupByMonth(){
		HashMap<Object, List<PatrolEditorInput>> sorted = new HashMap<>();
		List<Object> sortedKeys = new ArrayList<>();
		for (PatrolEditorInput p : patrols){
			DateGroupBy month = new DateGroupBy(p.getStartDate(), DateGroupBy.Type.MONTH); 
			
			List<PatrolEditorInput> pp = sorted.get(month);
			if (pp == null){
				pp = new ArrayList<>();
				sorted.put(month, pp);
				sortedKeys.add(month);
			}
			pp.add(p);
		}
		
		sortedKeys.sort((a,b)->{
			Integer y1 = Integer.valueOf(((DateGroupBy)a).getYear());
			Integer y2 = Integer.valueOf(((DateGroupBy)a).getYear());
			if (y1 != y2) return -1 * y1.compareTo(y2);
			
			Integer m1 = Integer.valueOf(((DateGroupBy)a).getMonth());
			Integer m2 = Integer.valueOf(((DateGroupBy)a).getMonth());
			return -1 * m1.compareTo(m2);
		});
		
		setGroupByData(patrols, sorted, sortedKeys);
	}
	
	
	private void setGroupByData(PatrolEditorInput[] patrolData, HashMap<Object, List<PatrolEditorInput>> sortedData, List<Object> sortedKeys){
		synchronized (this) {
			this.sorted = sortedData;
			this.sortedKeys = sortedKeys;
			this.patrols = patrolData;
		}
		
		Display.getDefault().syncExec(()->{
			viewer.refresh();
			((TreeViewer)viewer).expandAll();
		});
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
		if (parentElement instanceof PatrolEditorInput) return null;
		synchronized (this) {
			if (sorted != null && sorted.containsKey(parentElement)){
				return sorted.get(parentElement).toArray();
			}	
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		synchronized (this) {
			if (sorted != null && element instanceof PatrolEditorInput){
				PatrolEditorInput i = (PatrolEditorInput)element;
				
				for (Entry<Object, List<PatrolEditorInput>> entry : sorted.entrySet()){
					for (PatrolEditorInput e : entry.getValue()){
						if (e.equals(i)) return entry.getKey();
					}
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
