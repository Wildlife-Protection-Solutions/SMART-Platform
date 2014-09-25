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
package org.wcs.smart.er.ui.mision.editor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.ui.ISurveyListener;
import org.wcs.smart.er.ui.mision.importwp.MissionImportGpsDataWizard;
import org.wcs.smart.observation.common.importwp.GPSDataImport;
import org.wcs.smart.observation.common.importwp.ImportGpsDataWizard;
import org.wcs.smart.ui.map.location.MapComposite;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite to display list of tracks.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class TracksComposite extends Composite {

	private List<ISurveyListener> changeListeners = new ArrayList<ISurveyListener>();
	
	private static final int MAP_MIN_WIDTH = 280;
	
	private TableViewer trackViewer;
	private MissionTrackEditDialog dialog;

	public TracksComposite(Composite parent, MissionTrackEditDialog dialog) {
		super(parent, SWT.NONE);
		this.dialog = dialog;
		createControls();
		updateInput();
	}
	
	private List<MissionTrack> buildTrackInput(Mission m) {
		List<MissionTrack> tblInput = new ArrayList<MissionTrack>();
		Date date = dialog.getDate();
		for (MissionTrack t : m.getTracks()) {
			if (SmartUtils.isSameDate(t.getDate(), date)) {
				tblInput.add(t);
			}
		}
		return tblInput;
	}
	
	public void updateInput() {
		trackViewer.setInput(buildTrackInput(dialog.getMission()).toArray());
	}

	private void createControls() {
		setLayout(new GridLayout(3, false));
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		trackViewer = new TableViewer(this, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		trackViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		trackViewer.setContentProvider(ArrayContentProvider.getInstance());
		((GridData)trackViewer.getTable().getLayoutData()).heightHint = 150;
		trackViewer.getTable().setHeaderVisible(true);
		trackViewer.getTable().setLinesVisible(true);

		final TableViewerColumn columnId = new TableViewerColumn(trackViewer, SWT.NONE);
		columnId.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof MissionTrack) {
					MissionTrack track = (MissionTrack)element;
					return track.getId();
				}
				return super.getText(element);
			}
		});
		columnId.getColumn().setText(Messages.TracksComposite_TrackId);
		columnId.getColumn().setResizable(true);
		columnId.getColumn().setMoveable(false);
		columnId.getColumn().setWidth(120);
		columnId.setEditingSupport(new IdTableEditor(trackViewer));
		
		final TableViewerColumn columnAssoc = new TableViewerColumn(trackViewer, SWT.NONE);
		columnAssoc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof MissionTrack) {
					MissionTrack track = (MissionTrack)element;
					return track.getType().toString();
				}
				return super.getText(element);
			}
		});
		columnAssoc.getColumn().setText(Messages.TracksComposite_Association);
		columnAssoc.getColumn().setResizable(true);
		columnAssoc.getColumn().setMoveable(false);
		columnAssoc.getColumn().setWidth(120);
		
		//========map part========
		ScrolledComposite mapScrollCmp = new ScrolledComposite(this, SWT.H_SCROLL);
		MapComposite mapComposite = new MapComposite(mapScrollCmp, SWT.NONE);
		mapComposite.setDataProvider(null);
//		if (layerStyle != null){
//			mapComposite.setStyleSld(this.layerStyle);
//		}
		
		mapScrollCmp.setContent(mapComposite);
		mapScrollCmp.setExpandVertical(true);
		mapScrollCmp.setExpandHorizontal(true);
		mapScrollCmp.setMinWidth(MAP_MIN_WIDTH);
		
		//========links========
		Composite linksCmp = new Composite(this, SWT.NONE);
		linksCmp.setLayout(new GridLayout(1, false));
		linksCmp.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		Hyperlink lnk = new Hyperlink(linksCmp, SWT.NONE);
		lnk.setUnderlined(true);
		lnk.setText(Messages.TracksComposite_Import);
		lnk.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		lnk.addHyperlinkListener(new IHyperlinkListener() {
			@Override
			public void linkEntered(HyperlinkEvent e) {
				//nothing
			}

			@Override
			public void linkExited(HyperlinkEvent e) {
				//nothing
			}

			@Override
			public void linkActivated(HyperlinkEvent e) {
				importTracks();
			}
		});
		
		lnk = new Hyperlink(linksCmp, SWT.NONE);
		lnk.setUnderlined(true);
		lnk.setText(Messages.TracksComposite_Split);
		lnk.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		lnk.addHyperlinkListener(new IHyperlinkListener() {
			@Override
			public void linkEntered(HyperlinkEvent e) {
				//nothing
			}

			@Override
			public void linkExited(HyperlinkEvent e) {
				//nothing
			}

			@Override
			public void linkActivated(HyperlinkEvent e) {
				splitTrack();
			}
		});

		lnk = new Hyperlink(linksCmp, SWT.NONE);
		lnk.setUnderlined(true);
		lnk.setText(Messages.TracksComposite_Edit);
		lnk.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
		lnk.addHyperlinkListener(new IHyperlinkListener() {
			@Override
			public void linkEntered(HyperlinkEvent e) {
				//nothing
			}

			@Override
			public void linkExited(HyperlinkEvent e) {
				//nothing
			}

			@Override
			public void linkActivated(HyperlinkEvent e) {
				editTrack();
			}
		});
	}

	public void addChangeListener(ISurveyListener listener){
		changeListeners.add(listener);
	}
	
	protected void fireChangeListeners() {
		for (ISurveyListener listener: changeListeners) {
			listener.compositeModified();
		}
	}
	
	protected void importTracks() {
		final ImportGpsDataWizard wizard = new MissionImportGpsDataWizard(dialog.getMission(), GPSDataImport.ImportType.TRACK);
		wizard.setDateOption(dialog.getDate());
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(false, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.setTaskName(Messages.MissionDayComposite_LoadingWizard);
					WizardDialog dialog = new WizardDialog(getShell(), wizard);

					if (dialog != null) {
						monitor.setTaskName(Messages.MissionDayComposite_DisplayingWizard);
						dialog.open();
					}
				}
			});
		} catch (Exception ex) {
			EcologicalRecordsPlugIn.displayLog(Messages.MissionDayComposite_ImportWizardError + ex.getLocalizedMessage(), ex);
		}
	}
	
	protected void splitTrack() {
		// TODO Auto-generated method stub
		
	}

	protected void editTrack() {
		IStructuredSelection sel = (IStructuredSelection) trackViewer.getSelection();
		if (sel != null && !sel.isEmpty()) {
			MissionTrack track = (MissionTrack) sel.getFirstElement();
			MissionTrackPointDialog tpd = new MissionTrackPointDialog(Display.getCurrent().getActiveShell(), track);
			tpd.open();
//			ApplicationGIS.getToolManager().setCurrentEditor(dialog.getMissionEditor());
		}
	}

	/**
	 * Cell editor for track id
	 */
	private class IdTableEditor extends EditingSupport {
		private TableViewer viewer;

		private CellEditor editor;
		
		IdTableEditor(TableViewer viewer) {
			super(viewer);
			this.viewer = viewer;
			this.editor = new TextCellEditor(viewer.getTable());	
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (element instanceof MissionTrack) {
				MissionTrack t = (MissionTrack) element;
				t.setId((String)value);
				viewer.refresh();
				fireChangeListeners();
			}
		}

		@Override
		protected Object getValue(Object element) {
			if (element instanceof MissionTrack) {
				MissionTrack t = (MissionTrack) element;
				return t.getId() != null ? t.getId() : ""; //$NON-NLS-1$
			}
			return ""; //$NON-NLS-1$
		}

		@Override
		protected CellEditor getCellEditor(final Object element) {
			return editor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
	}
	
}
