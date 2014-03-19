package org.wcs.smart.entity.query.model.columns;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

public class EntityQueryColumnCache {
	private static EntityQueryColumnCache instance = null;
	private static Object INSTANCE_LOCK = new Object();

	public static EntityQueryColumnCache getInstance() {
		if (instance == null) {
			synchronized (INSTANCE_LOCK) {
				if (instance == null) {
					instance = new EntityQueryColumnCache();
				}
			}
		}
		return instance;
	}

	private QueryColumn[] queryColumns = null;
	private QueryColumn[] waypointQueryColumns = null;
	private QueryColumn[] gridQueryColumns = null;

	private final Object GRIDLOCK = new Object();
	private final Object OBSERVATIONLOCK = new Object();
	private final Object WAYPOINTLOCK = new Object();

	private EntityQueryColumnCache() {
		DataModelManager.getInstance().addChangeListener(
				new IDataModelListener() {
					@Override
					public void modified() {
						queryColumns = null;
						gridQueryColumns = null;

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
						}
						if (add) {
							cols.add(new FixedQueryColumn(item));
						}
					}

					DataModel dataModel = QueryDataModelManager.getInstance()
							.getDataModel();

					// add data model category columns
					int numCategory = QueryDataModelManager.getInstance()
							.getActiveDepth();
					for (int i = 0; i < numCategory; i++) {
						cols.add(new EtCategoryQueryColumn(i));
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
						}
						if (add) {
							cols.add(new FixedQueryColumn(item));
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
				tmp[i] = new GridQueryColumn(item);
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
