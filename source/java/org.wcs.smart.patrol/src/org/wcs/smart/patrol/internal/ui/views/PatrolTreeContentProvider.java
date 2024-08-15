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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.Station;
import org.wcs.smart.common.folder.FolderTreeContentProvider;
import org.wcs.smart.common.folder.IGroupContentBuilder;
import org.wcs.smart.common.folder.ITreeElement;
import org.wcs.smart.common.folder.NoGroupingContentBuilder;
import org.wcs.smart.common.folder.TreeElement;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.ui.PatrolEditorInput;

/**
 * Content provider for the patrol list viewer that allows the grouping
 * of patrols by various patrol attributes.
 * 
 * @author Emily
 * @author elitvin
 * @since 6.0.0
 */
public class PatrolTreeContentProvider extends FolderTreeContentProvider {

//	private final static String sortingJobName = Messages.PatrolTreeContentProvider_JobName;

	public static enum GroupByType{
		FOLDER (Messages.PatrolTreeContentProvider_FolderOption), 
		STATION (Messages.PatrolTreeContentProvider_StationOption),
		TEAM (Messages.PatrolTreeContentProvider_TeamOption),
		PATROLTYPE (Messages.PatrolTreeContentProvider_TypeOption1),
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
	
	private GroupByType groupBy = GroupByType.NONE;
    private Map<GroupByType, IGroupContentBuilder> groupingMap;
    private IGroupContentBuilder defaultGroupingContentBuilder = new NoGroupingContentBuilder();
	
	/**
	 * Creates a new content provider with the given group by default
	 * @param defaultValue
	 */
	public PatrolTreeContentProvider(String defaultValue) {
		groupingMap = new HashMap<>();
		groupingMap.put(GroupByType.YEAR,          new YearGroupingContentBuilder());
		groupingMap.put(GroupByType.MONTH,         new MonthGroupingContentBuilder());
		groupingMap.put(GroupByType.PATROLTYPE,    new PatrolTypeGroupingContentBuilder());
		groupingMap.put(GroupByType.MANDATE,       new MandateGroupingContentBuilder());
		groupingMap.put(GroupByType.TRANSPORTTYPE, new TransportTypeGroupingContentBuilder());
		groupingMap.put(GroupByType.TEAM,          new TeamGroupingContentBuilder());
		groupingMap.put(GroupByType.STATION,       new StationGroupingContentBuilder());
		groupingMap.put(GroupByType.FOLDER,        new PatrolFolderGroupingContentBuilder());
		groupingMap.put(GroupByType.NONE,          defaultGroupingContentBuilder);

		setGroupBy(defaultValue);
	}
	
	public GroupByType getGroupBy() {
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
		
	public void setGroupBy(GroupByType groupBy) {
		this.groupBy = groupBy;
		IGroupContentBuilder gcb = groupingMap.get(groupBy);
		if (gcb == null) {
			gcb = defaultGroupingContentBuilder;
		}
		updateGroupContentBuilder(gcb);
	}

	private static class ObjectNameComparator implements Comparator<ITreeElement> {

		public Object getTopObject() {
			return null;
		}
		
		@Override
		public int compare(ITreeElement a, ITreeElement b) {
			if (a.equals(b)) return 0;
			Object o1 = a.getObject();
			Object o2 = b.getObject();
			if (o1 == getTopObject()) return -1;
			if (o2 == getTopObject()) return 1;
			if (o1 instanceof NamedItem && o2 instanceof NamedItem) {
				return Collator.getInstance().compare(((NamedItem)o1).getName(), ((NamedItem)o2).getName());
			}
			return 0;
		}
	}
	
	private static class YearGroupingContentBuilder implements IGroupContentBuilder {
		@Override
		public List<ITreeElement> applyGrouping(Object input) {
			if (input instanceof PatrolEditorInput[]){
				PatrolEditorInput[] patrols = (PatrolEditorInput[]) input;
				List<ITreeElement> result = new ArrayList<>();
				Map<DateGroupBy, TreeElement> obj2Element = new HashMap<>();
				for (PatrolEditorInput p : patrols) {
					DateGroupBy year = new DateGroupBy(p.getStartDate(), DateGroupBy.Type.YEAR);
					TreeElement yearEl = obj2Element.get(year);
					if(yearEl == null) {
						yearEl = new TreeElement(year, null);
						obj2Element.put(year, yearEl);
						result.add(yearEl);
					}
					yearEl.getChildren().add(new TreeElement(p, year));
				}
				result.sort((a,b)->{
					Integer a1 = Integer.valueOf(((DateGroupBy)a.getObject()).getYear());
					Integer b1 = Integer.valueOf(((DateGroupBy)b.getObject()).getYear());
					return -1 * a1.compareTo(b1);
				});
				return result;
				
			}
			return null;
		}
	}

	private static class MonthGroupingContentBuilder implements IGroupContentBuilder {
		@Override
		public List<ITreeElement> applyGrouping(Object input) {
			if (input instanceof PatrolEditorInput[]){
				PatrolEditorInput[] patrols = (PatrolEditorInput[]) input;
				List<ITreeElement> result = new ArrayList<>();
				Map<DateGroupBy, TreeElement> obj2Element = new HashMap<>();
				for (PatrolEditorInput p : patrols) {
					DateGroupBy month = new DateGroupBy(p.getStartDate(), DateGroupBy.Type.MONTH); 
					TreeElement monthEl = obj2Element.get(month);
					if(monthEl == null) {
						monthEl = new TreeElement(month, null);
						obj2Element.put(month, monthEl);
						result.add(monthEl);
					}
					monthEl.getChildren().add(new TreeElement(p, month));
				}
				result.sort((a,b)->{					
					Integer y1 = Integer.valueOf(((DateGroupBy)a.getObject()).getYear());
					Integer y2 = Integer.valueOf(((DateGroupBy)b.getObject()).getYear());
					if (y1.intValue() != y2.intValue()) return -1 * y1.compareTo(y2);
					
					Integer m1 = Integer.valueOf(((DateGroupBy)a.getObject()).getMonth());
					Integer m2 = Integer.valueOf(((DateGroupBy)b.getObject()).getMonth());
					return -1 * m1.compareTo(m2);
				});
				return result;
				
			}
			return null;
		}
	}

	private static class PatrolTypeGroupingContentBuilder implements IGroupContentBuilder {
		@Override
		public List<ITreeElement> applyGrouping(Object input) {
			if (input instanceof PatrolEditorInput[]){
				PatrolEditorInput[] patrols = (PatrolEditorInput[]) input;
				List<ITreeElement> result = new ArrayList<>();
				Map<PatrolType, TreeElement> obj2Element = new HashMap<>();
				
				for (PatrolEditorInput p : patrols) {
					PatrolType type = p.getType();
					TreeElement typeEl = obj2Element.get(type);
					if(typeEl == null) {
						typeEl = new TreeElement(type, null);
						obj2Element.put(type, typeEl);
						result.add(typeEl);
					}
					typeEl.getChildren().add(new TreeElement(p, type));
				}
				result.sort((a,b)->{
					String s1 = ((PatrolType)a.getObject()).getName();
					String s2 = ((PatrolType)a.getObject()).getName();
					return Collator.getInstance().compare(s1, s2);
				});
				return result;
				
			}
			return null;
		}
	}

	private static class MandateGroupingContentBuilder implements IGroupContentBuilder {
		
		private static final PatrolMandate NONE_MANDATE = new PatrolMandate();
		
		static {
			NONE_MANDATE.setName(Messages.PatrolTreeContentProvider_NoStationLabel);
		}
		
		@Override
		public List<ITreeElement> applyGrouping(Object input) {
			if (input instanceof PatrolEditorInput[]) {
				PatrolEditorInput[] patrols = (PatrolEditorInput[]) input;
				List<ITreeElement> result = new ArrayList<>();
				
				Job j = new Job(Messages.PatrolTreeContentProvider_JobName) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						Map<PatrolMandate, TreeElement> obj2Element = new HashMap<>();
						Map<PatrolMandate, Set<PatrolEditorInput>> mandate2Input = new HashMap<>();
						try(Session s = HibernateManager.openSession()) {
							for (PatrolEditorInput p : patrols) {
								Patrol pp = (Patrol) s.get(Patrol.class, p.getUuid());
								for (PatrolLeg leg : pp.getLegs()) {
									PatrolMandate m = leg.getMandate();
									if (m == null) {
										m = NONE_MANDATE;
									}
									TreeElement el = obj2Element.get(m);
									if(el == null) {
										el = new TreeElement(m, null);
										obj2Element.put(m, el);
										result.add(el);
									}
									
									Set<PatrolEditorInput> inputSet = mandate2Input.get(m);
									if (inputSet == null) {
										inputSet = new HashSet<>();
										mandate2Input.put(m, inputSet);
									}

									if (!inputSet.contains(p)) {
										inputSet.add(p);
										el.getChildren().add(new TreeElement(p, m));
									}
									
								}
							}
						}
						return Status.OK_STATUS;
					}
				};
				j.schedule();
				try {
					j.join();
				} catch (InterruptedException e) {
					SmartPlugIn.displayError(Messages.PatrolTreeContentProvider_SortMandateJob_Error, e);
				}
				result.sort(new ObjectNameComparator() {
					@Override
					public Object getTopObject() {
						return NONE_MANDATE;
					}
				});
				return result;
			}
			return null;
		}
	}

	private static class TransportTypeGroupingContentBuilder implements IGroupContentBuilder {
		@Override
		public List<ITreeElement> applyGrouping(Object input) {
			if (input instanceof PatrolEditorInput[]) {
				PatrolEditorInput[] patrols = (PatrolEditorInput[]) input;
				List<ITreeElement> result = new ArrayList<>();
				
				Job j = new Job(Messages.PatrolTreeContentProvider_JobName) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						Map<PatrolTransportType, TreeElement> obj2Element = new HashMap<>();
						Map<PatrolTransportType, Set<PatrolEditorInput>> tt2Input = new HashMap<>();
						try(Session s = HibernateManager.openSession()) {
							for (PatrolEditorInput p : patrols) {
								Patrol pp = (Patrol) s.get(Patrol.class, p.getUuid());
								for (PatrolLeg leg : pp.getLegs()) {
									PatrolTransportType m = leg.getType();
									
									TreeElement el = obj2Element.get(m);
									if(el == null) {
										el = new TreeElement(m, null);
										obj2Element.put(m, el);
										result.add(el);
									}
									
									Set<PatrolEditorInput> inputSet = tt2Input.get(m);
									if (inputSet == null) {
										inputSet = new HashSet<>();
										tt2Input.put(m, inputSet);
									}

									if (!inputSet.contains(p)) {
										inputSet.add(p);
										el.getChildren().add(new TreeElement(p, m));
									}
									
								}
							}
						}
						return Status.OK_STATUS;
					}
				};
				j.schedule();
				try {
					j.join();
				} catch (InterruptedException e) {
					SmartPlugIn.displayError(Messages.PatrolTreeContentProvider_SortTransportJob_Error, e);
				}
				result.sort(new ObjectNameComparator());
				return result;
			}
			return null;
		}
	}
	
	private static class TeamGroupingContentBuilder implements IGroupContentBuilder {
		
		private static final Team NONE_TEAM = new Team();
		
		static {
			NONE_TEAM.setName(Messages.PatrolTreeContentProvider_NoTeamLabel);
		}

		@Override
		public List<ITreeElement> applyGrouping(Object input) {
			if (input instanceof PatrolEditorInput[]) {
				PatrolEditorInput[] patrols = (PatrolEditorInput[]) input;
				List<ITreeElement> result = new ArrayList<>();
				
				Job j = new Job(Messages.PatrolTreeContentProvider_JobName) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						Map<Team, TreeElement> obj2Element = new HashMap<>();
						try(Session s = HibernateManager.openSession()) {
							for (PatrolEditorInput p : patrols) {
								Patrol pp = (Patrol) s.get(Patrol.class, p.getUuid());
								
								Team team = pp.getTeam();
								if (team == null) {
									team = NONE_TEAM;
								}
								
								TreeElement el = obj2Element.get(team);
								if(el == null) {
									el = new TreeElement(team, null);
									obj2Element.put(team, el);
									result.add(el);
								}
								el.getChildren().add(new TreeElement(p, team));
							}
						}
						return Status.OK_STATUS;
					}
				};
				j.schedule();
				try {
					j.join();
				} catch (InterruptedException e) {
					SmartPlugIn.displayError(Messages.PatrolTreeContentProvider_SortTeamJob_Error, e);
				}
				
				result.sort(new ObjectNameComparator() {
					@Override
					public Object getTopObject() {
						return NONE_TEAM;
					}
				});
				
				return result;
			}
			return null;
		}
	}

	private static class StationGroupingContentBuilder implements IGroupContentBuilder {
		
		private static final Station NONE_STATION = new Station();
		
		static {
			NONE_STATION.setName(Messages.PatrolTreeContentProvider_NoStationLabel);
		}

		@Override
		public List<ITreeElement> applyGrouping(Object input) {
			if (input instanceof PatrolEditorInput[]) {
				PatrolEditorInput[] patrols = (PatrolEditorInput[]) input;
				List<ITreeElement> result = new ArrayList<>();
				
				Job j = new Job(Messages.PatrolTreeContentProvider_JobName) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						Map<Station, TreeElement> obj2Element = new HashMap<>();
						try(Session s = HibernateManager.openSession()) {
							for (PatrolEditorInput p : patrols) {
								Patrol pp = (Patrol) s.get(Patrol.class, p.getUuid());
								
								Station station = pp.getStation();
								if (station == null) {
									station = NONE_STATION;
								}
								
								TreeElement el = obj2Element.get(station);
								if(el == null) {
									el = new TreeElement(station, null);
									obj2Element.put(station, el);
									result.add(el);
								}
								el.getChildren().add(new TreeElement(p, station));
							}
						}
						return Status.OK_STATUS;
					}
				};
				j.schedule();
				try {
					j.join();
				} catch (InterruptedException e) {
					SmartPlugIn.displayError(Messages.PatrolTreeContentProvider_SortStationJob_Error, e);
				}
				
				result.sort(new ObjectNameComparator() {
					@Override
					public Object getTopObject() {
						return NONE_STATION;
					}
				});
				
				return result;
			}
			return null;
		}
	}
	
}
