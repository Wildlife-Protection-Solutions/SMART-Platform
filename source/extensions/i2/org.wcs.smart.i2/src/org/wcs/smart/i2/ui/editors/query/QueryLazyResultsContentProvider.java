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
package org.wcs.smart.i2.ui.editors.query;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.ui.properties.DialogConstants;


/**
 * QueryResultsContentProvider allows to lazy load required data from database.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class QueryLazyResultsContentProvider implements ILazyContentProvider { //, IQueryColumnSorter {
	
	private static final ISchedulingRule CELL_JOB_MUTEX = new LoadCellDataMutex();
	
	private TableViewer viewer;
	private IPagedQueryResultSet input;
	private int pageSize = IPagedQueryResultSet.TABLE_DEFAULT_PAGE_SIZE;
	
	private QueryTableViewerColumn sortColumn = null;
	private int direction = SWT.UP;
	
	 
	
	/**
	 * As viewer.replace(...) is executed in separate thread noticed that we might get
	 * extra request for item that was about to be masked. To avoid this we will maintain
	 * our own set of items that are about to be loaded.
	 */
	private Set<Integer> loadingIndexes = new HashSet<Integer>();
	
	public QueryLazyResultsContentProvider(TableViewer viewer) {
		this.viewer = viewer;
	}

	@Override
	public void dispose() {
		if (input != null) {
//			CleanUpQueryJob.schedule(input);
			input = null;
		}
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null || newInput instanceof IPagedQueryResultSet) {
			if (input != null && !input.equals(newInput)) {
				loadingIndexes.clear();
//				CleanUpQueryJob.schedule(input);
			}
			input = (IPagedQueryResultSet) newInput;
		}
	}

	@Override
	public void updateElement(final int index) {
		//due to weird implementation of AbstractTableViewer.virtualSetSelectionToWidget() line 974
		//and due to this method is called on selection change in AbstractTableViewer.getVirtualSelection() line 510
		//we do NOT want to reload data every time as we are displaying static content
		if (viewer.getElementAt(index) != null || loadingIndexes.contains(index))
			return;
		if (index < 0 || index >= viewer.getTable().getItemCount())
			return;

		//We are going to fetch not only item at index but several nearby elements
		//therefore we need to guess in which direction nearby elements should go
		//as user can scroll down or up.
		//If next element is unknown that most likely user is scrolling down
		int from = index+1;
		boolean isDown = from < viewer.getTable().getItemCount() && viewer.getElementAt(from) == null;
		if (!isDown) {
			from -= pageSize;
			if (from < 0) {
				from = 0;
			}
		} else {
			from--;
		}
		if (index > 0)
			loadingIndexes.contains(index);
		maskItems(from);
		Job job = new LoadCellDataJob(from);
		job.setRule(CELL_JOB_MUTEX);
		job.schedule();
		
	}

	private void maskItems(int from) {
		int to = from + pageSize - 1;
		if (to >= viewer.getTable().getItemCount()) {
			to = viewer.getTable().getItemCount() - 1;
		}
		for (int i = from; i <= to; i++) {
			loadingIndexes.add(i);
			viewer.replace(DialogConstants.LOADING_TEXT, i);
		}
		
	}
	
	/**
	 * Sets the sort column
	 * @param sort the column to sort on
	 */
	public void setSortColumn(QueryTableViewerColumn sort) {			
		if (sortColumn != null && sortColumn == sort) {
			if (direction == SWT.DOWN) {
				direction = SWT.UP;
			} else {
				direction = SWT.DOWN;
			}
		}
		this.sortColumn = sort;
		viewer.getTable().setSortDirection(direction);
		if (sort == null){
			viewer.getTable().setSortColumn(null);
		}else{
			viewer.getTable().setSortColumn(sort.getTableColumn().getColumn());
			//clearing all the data that was previously loaded
			//there are other ways to clear data but this is the fastest (0.3s on 250k items comparing to 10+ seconds with other approaches)
		}
		
		viewer.getTable().removeAll();
		if (input == null){
			viewer.getTable().setItemCount(0);
		}else{
			viewer.getTable().setItemCount(input.getItemCount());
		}
		
		if (sort != null){
			IQueryColumn sColumn = sortColumn.getColumn();
			input.setSorting(sColumn, direction);
		}
		
		loadingIndexes.clear();
		viewer.refresh(true);
		
	}

	/**
	 * Job which is responsible for updating particular set of items in current table.
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	private class LoadCellDataJob extends Job {

		private int from;
		
		public LoadCellDataJob(int from) {
			super(MessageFormat.format("Loading results {0} to {1}", from, from + pageSize));
			this.from = from;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (input == null){ return Status.OK_STATUS; }
			final List<? extends IResultItem> data = input.getData(from, pageSize);
			if (viewer.getControl().isDisposed()) return Status.CANCEL_STATUS;
			viewer.getControl().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (input == null){return;}
					if (viewer != null && viewer.getTable() != null && !viewer.getTable().isDisposed()) {
						for (int i = 0; i < data.size(); i++) {
							viewer.replace(data.get(i), i+from); //replacing current item and several nearby
							loadingIndexes.remove(i+from);
						}
					}
				}
			});
			return Status.OK_STATUS;
		}
	}

	/**
	 * Mutex to ensure that jobs will not be conflicting as simultaneous jobs execution
	 * might result in SQLException. This will ensure that jobs are running one by one.
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	private static class LoadCellDataMutex implements ISchedulingRule {

        public boolean contains(ISchedulingRule rule) {
            return (rule == this);
        }

        public boolean isConflicting(ISchedulingRule rule) {
            return (rule == this);
        }

	}
}
