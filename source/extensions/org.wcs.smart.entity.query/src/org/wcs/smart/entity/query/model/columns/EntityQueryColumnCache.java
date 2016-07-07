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
package org.wcs.smart.entity.query.model.columns;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.IDataModelListener;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.events.IWaypointEventListener;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.events.WaypointEventManager.EventType;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Entity query column cache
 * @author Emily
 *
 */
public class EntityQueryColumnCache {
	
	private static volatile EntityQueryColumnCache instance = null;

	public static EntityQueryColumnCache getInstance() {
		if (instance == null) {
			synchronized (EntityQueryColumnCache.class) {
				if (instance == null) {
					instance = new EntityQueryColumnCache();
				}
			}
		}
		return instance;
	}

	private volatile QueryColumn[] queryColumns = null;
	private volatile QueryColumn[] waypointQueryColumns = null;
	private volatile QueryColumn[] gridQueryColumns = null;

	private final Object GRIDLOCK = new Object();
	private final Object OBSERVATIONLOCK = new Object();
	private final Object WAYPOINTLOCK = new Object();

	private EntityQueryColumnCache() {
		DataModelManager.INSTANCE.addChangeListener(
				new IDataModelListener() {
					@Override
					public void modified() {
						queryColumns = null;
						gridQueryColumns = null;

					}
				});
		WaypointEventManager.getInstance().addListener(EventType.WAYPOINT_OPTIONS_MODIFIED, new IWaypointEventListener() {
			@Override
			public void handleEvent(Waypoint wp) {
				queryColumns = null;
			}
		});
	}

	/**
	 * 
	 * @return query columns available to a waypoint query based on the patrol
	 *         options and the data model of the conservation area. This
	 *         function will access the database the first time it is called,
	 *         subsequent calls return cached values.
	 */
	public QueryColumn[] getObservationQueryColumns() {

		if (queryColumns != null) {
			return cloneColumns(queryColumns);
		}
		synchronized (OBSERVATIONLOCK) {
			if (queryColumns != null) {
				return cloneColumns(queryColumns);
			}
			//outside job to prevent deadlocking
			final DataModel dataModel = QueryDataModelManager.getInstance().getDataModel();
			
			Job j = new Job(Messages.EntityQueryColumnCache_jobname) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					// load from the database
					ObservationOptions obsOptions = null;
					Session session = HibernateManager.openSession();
					try {
						obsOptions = ObservationHibernateManager
								.getPatrolOptions(
										SmartDB.getCurrentConservationArea(),
										session);
					} finally {
						session.close();
					}

					ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();
					for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
						FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns
								.values()[i];
						boolean add = true;
						if (item == FixedQueryColumn.FixedColumns.CA_ID
								|| item == FixedQueryColumn.FixedColumns.CA_NAME) {
							add = SmartDB.isMultipleAnalysis();
						} else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION
								|| item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE) {
							add = obsOptions.getTrackDistanceDirection();
						} else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER){
							add = obsOptions.getTrackObserver();
						}
						if (add) {
							cols.add(new FixedQueryColumn(item, Locale.getDefault()));
						}
					}

					// add data model category columns
					int numCategory = QueryDataModelManager.getInstance()
							.getActiveDepth();
					for (int i = 0; i < numCategory; i++) {
						cols.add(new EtCategoryQueryColumn(MessageFormat.format(Messages.QueryColumn_ObservationCategoryTableHeader, i), i));
					}

					// sort attributes alphabetically
					List<Attribute> atts = new ArrayList<Attribute>();
					atts.addAll(dataModel.getAttributes());
					Collections.sort(atts, new Comparator<Attribute>() {
						@Override
						public int compare(Attribute o1, Attribute o2) {
							return Collator.getInstance().compare(o1.getName(),
									o2.getName());
						}
					});

					for (Attribute att : atts) {
						String name = att.getName();
						cols.add(new EtAttributeQueryColumn(name, att.getKeyId(), att.getType()));
					}
					queryColumns = cols.toArray(new QueryColumn[cols.size()]);

					return Status.OK_STATUS;
				}
			};
			j.schedule();
			try {
				j.join();
			} catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

		return cloneColumns(queryColumns);
	}

	/**
	 * 
	 * @return query columns available to a waypoint query based on the patrol
	 *         options and the data model of the conservation area. This
	 *         function will access the database the first time it is called,
	 *         subsequent calls return cached values.
	 */
	public QueryColumn[] getWaypointQueryColumns() {

		if (waypointQueryColumns != null) {
			return cloneColumns(waypointQueryColumns);
		}
		synchronized (WAYPOINTLOCK) {
			if (waypointQueryColumns != null) {
				return cloneColumns(waypointQueryColumns);
			}

			Job j = new Job(
					Messages.EntityQueryColumnCache_jobname2) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					// load from the database
					ObservationOptions obsOptions = null;
					Session session = HibernateManager.openSession();
					try {
						obsOptions = ObservationHibernateManager
								.getPatrolOptions(
										SmartDB.getCurrentConservationArea(),
										session);
					} finally {
						session.close();
					}
					ArrayList<QueryColumn> cols = new ArrayList<QueryColumn>();

					for (int i = 0; i < FixedQueryColumn.FixedColumns.values().length; i++) {
						FixedQueryColumn.FixedColumns item = FixedQueryColumn.FixedColumns
								.values()[i];
						boolean add = true;
						if (item == FixedQueryColumn.FixedColumns.CA_ID
								|| item == FixedQueryColumn.FixedColumns.CA_NAME) {
							add = SmartDB.isMultipleAnalysis();
						} else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_DIRECTION
								|| item == FixedQueryColumn.FixedColumns.WAYPOINT_DISTANCE) {
							add = obsOptions.getTrackDistanceDirection();
						} else if (item == FixedQueryColumn.FixedColumns.WAYPOINT_OBSERVER){
							add = false;
						}
						if (add) {
							cols.add(new FixedQueryColumn(item, Locale.getDefault()));
						}
					}
					waypointQueryColumns = cols.toArray(new QueryColumn[cols
							.size()]);
					return Status.OK_STATUS;
				}
			};
			j.schedule();
			try {
				j.join();
			} catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

		return cloneColumns(waypointQueryColumns);
	}

	public QueryColumn[] getGridColumns() {
		if (gridQueryColumns != null) {
			return cloneColumns(gridQueryColumns);
		}
		synchronized (GRIDLOCK) {
			if (gridQueryColumns != null) {
				return cloneColumns(gridQueryColumns);
			}
			QueryColumn[] tmp = new QueryColumn[GridQueryColumn.GridColumns
					.values().length];
			for (int i = 0; i < GridQueryColumn.GridColumns.values().length; i++) {
				GridQueryColumn.GridColumns item = GridQueryColumn.GridColumns
						.values()[i];
				tmp[i] = new GridQueryColumn(item, Locale.getDefault());
			}
			gridQueryColumns = tmp;
		}
		return cloneColumns(gridQueryColumns);
	}

	private QueryColumn[] cloneColumns(QueryColumn[] cols) {
		QueryColumn[] copies = new QueryColumn[cols.length];
		for (int i = 0; i < copies.length; i++) {
			copies[i] = cols[i].clone();
		}
		return copies;
	}
}
