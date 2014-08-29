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
package org.wcs.smart.er.ui.surveydesign.editor;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.ISurveyEventListener;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.hibernate.SurveyFilter;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.handlers.DeleteSurveyElementHandler;
import org.wcs.smart.er.ui.handlers.NewMissionHandler;
import org.wcs.smart.er.ui.handlers.NewSurveyHandler;
import org.wcs.smart.er.ui.mision.editor.MissionEditor;
import org.wcs.smart.er.ui.mision.editor.MissionEditorInput;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Survey design editor page that displays all the 
 * survey and mission data.
 * 
 * @author Emily
 *
 */
public class SurveyDesignDataPage extends EditorPart {

	private static Comparator<TreeNode> treeNodeComparator = new Comparator<TreeNode>() {
		@Override
		public int compare(TreeNode o1, TreeNode o2) {
			if (o1.getStartDate().equals(o2.getStartDate())){
				return Collator.getInstance().compare(o1.getId(), o2.getId());
			}
			return -o1.getStartDate().compareTo(o2.getStartDate());
		}
	};
	
	private Form form;
	private FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	
	private SurveyDesignEditor parentEditor;
	
	private TreeViewer dataViewer;
	private HashMap<Integer, String> columnToAttributeKey;
	
	private EventType[] events = new EventType[] { EventType.MISSION_ADDED,
			EventType.MISSION_DELETED, EventType.MISSION_MODIFIED,
			EventType.SURVEY_ADDED, EventType.SURVEY_DELETED,
			EventType.SURVEY_MODIFIED };
	private ISurveyEventListener refreshListener = new ISurveyEventListener() {
		
		@Override
		public void event(Object o) {
			loadSurveysJob.schedule();
		}
	};
	
	public SurveyDesignDataPage(SurveyDesignEditor parent) {
		this.parentEditor = parent;
		
		for (EventType event : events){
			SurveyEventHandler.getInstance().addListener(event, refreshListener);
		}
	}
	
	@Override
	public void createPartControl(Composite parent) {
		toolkit.setBorderStyle(SWT.BORDER);
		
		Composite container = toolkit.createComposite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		form = toolkit.createForm(container);
		form.setText(Messages.SamplingUnitEditorPage_FormName);
		form.getBody().setLayout(new GridLayout());
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite main = toolkit.createComposite(form.getBody(), SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		ToolBar tools = new ToolBar(main, SWT.FLAT);
		toolkit.adapt(tools);
		
		ToolItem newSurvey = new ToolItem(tools, SWT.PUSH);
		newSurvey.setToolTipText(Messages.SurveyDesignDataPage_newSurveyTooltip);
		newSurvey.setImage(EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.NEW_SURVEY_ICON));
		newSurvey.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				newSurvey();
			}
		});
		
		ToolItem newMission = new ToolItem(tools, SWT.PUSH);
		newMission.setToolTipText(Messages.SurveyDesignDataPage_newMissionTooltip);
		newMission.setImage(EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.NEW_MISSION_ICON));
		newMission.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				newMission();
			}
		});
		
		ToolItem deleteItem = new ToolItem(tools, SWT.PUSH);
		deleteItem.setToolTipText(Messages.SurveyDesignDataPage_DeleteTooltip);
		deleteItem.setImage(EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.DELETE_ICON));
		deleteItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteSelection();
			}
		});
		
		Tree dataTree = new Tree(main, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		dataTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dataTree.setHeaderVisible(true);
//		dataTree.setLinesVisible(true);
		
		dataViewer = new TreeViewer(dataTree);
		dataViewer.addDoubleClickListener(new IDoubleClickListener() {
		
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editSelection();
			}
		});
		
		dataViewer.setContentProvider(new ITreeContentProvider(){
			private List<TreeNode> elements;
			
			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof TreeNode){
					if (((TreeNode) parentElement).getType() == TreeNode.Type.SURVEY){
						return ((TreeNode) parentElement).getKids().toArray();
					}
				}
				return null;
			}

			public Object getParent(Object element) {
				if (element instanceof TreeNode)
					return ((TreeNode) element).getParent();
				return null;
			}

			public boolean hasChildren(Object element) {
				if (element instanceof TreeNode){
					if (((TreeNode) element).getType() == TreeNode.Type.SURVEY){
						return true;
					}
				}
				return false;
			}

			public Object[] getElements(Object cities) {
				if (elements == null){
					return new String[]{Messages.SurveyDesignDataPage_loadingLabel};
				}
				return elements.toArray();
			}

			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				if (newInput instanceof List){
					elements = (List<TreeNode>) newInput;
				}
			}
		});
		dataViewer.setLabelProvider(new ITableLabelProvider() {
			
			@Override
			public void removeListener(ILabelProviderListener listener) {
			}
			
			@Override
			public boolean isLabelProperty(Object element, String property) {
				return false;
			}
			
			@Override
			public void dispose() {}
			
			@Override
			public void addListener(ILabelProviderListener listener) {}
			
			@Override
			public String getColumnText(Object element, int columnIndex) {
				if (element instanceof String){
					return (String) element;
				}
				if (columnIndex == 0){
					//id
					if (element instanceof TreeNode){
						return ((TreeNode) element).getId();
					}
				}else if (columnIndex == 1){
					//start
					if (element instanceof TreeNode){
						return DateFormat.getDateInstance(DateFormat.MEDIUM).format( ((TreeNode) element).getStartDate());
					}
				}else  if (columnIndex == 2){
					//end
					if (element instanceof TreeNode){
						return DateFormat.getDateInstance(DateFormat.MEDIUM).format( ((TreeNode) element).getEndDate());
					}
				}else{
					
					if (element instanceof TreeNode){
						String key = columnToAttributeKey.get(columnIndex);
						String value = ((TreeNode)element).getAttributeValue(key);
						if (value == null){
							return ""; //$NON-NLS-1$
						}else{
							return value;
						}
					}
					
				}
				return null;
			}
			
			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				if (columnIndex == 0){
					if (element instanceof TreeNode){
						return ((TreeNode) element).getImage();
					}
				}
				return null;
			}
		});
		
		MenuManager mgr = new MenuManager();
		Menu menu = mgr.createContextMenu(dataViewer.getTree());
		dataViewer.getTree().setMenu(menu);
		
		mgr.add(new Action(Messages.SurveyDesignDataPage_EditMenuLabel){
			@Override
			public void run(){
				editSelection();
			}
		});
		mgr.add(new Separator());
		mgr.add(new Action(Messages.SurveyDesignDataPage_NewMissionMenuLabel, EcologicalRecordsPlugIn.getDefault().getImageRegistry().getDescriptor(EcologicalRecordsPlugIn.NEW_MISSION_ICON)) {
			@Override
			public void run(){
				newMission();
			}
		});
		
		mgr.add(new Action(Messages.SurveyDesignDataPage_NewSurveyMenuLabel, EcologicalRecordsPlugIn.getDefault().getImageRegistry().getDescriptor(EcologicalRecordsPlugIn.NEW_SURVEY_ICON)) {
			@Override
			public void run(){
				newSurvey();
			}
		});
		
		mgr.add(new Action(Messages.SurveyDesignDataPage_DeleteMenuLabel, EcologicalRecordsPlugIn.getDefault().getImageRegistry().getDescriptor(EcologicalRecordsPlugIn.DELETE_ICON)) {
			@Override
			public void run(){
				deleteSelection();
			}
		});
		
		initValues();
	}

	
	
	private TreeNode getSelectedItem(){
		return (TreeNode)((IStructuredSelection)dataViewer.getSelection()).getFirstElement();
	}
	
	/*
	 * opens new mission wizard 
	 */
	private void newMission(){
		TreeNode n = getSelectedItem();
		if (n != null){
			if (n.getType() == TreeNode.Type.MISSION){
				n = n.getParent();
			}
		}
		NewMissionHandler.newMission(getSite().getShell(),
				parentEditor.getSurveyDesign().getUuid(), 
				n==null? null : n.getUuid());
	}
	
	/*
	 * opens new survey wizard
	 */
	private void newSurvey(){
		NewSurveyHandler.newSurvey(getSite().getShell(), parentEditor.getSurveyDesign().getUuid());
	}
	
	/*
	 * edits selected elements
	 */
	private void editSelection(){
		TreeNode node = getSelectedItem();
		if (node.getType() == TreeNode.Type.MISSION){
			MissionEditorInput mi = new MissionEditorInput(node.getId(), node.getUuid());
			try {
				getSite().getPage().openEditor(mi, MissionEditor.ID);
			} catch (PartInitException e) {
				EcologicalRecordsPlugIn.displayLog(e.getMessage(), e);
			}
		}
	}
	
	/*
	 * delete the selected elements
	 */
	private void deleteSelection(){
		final TreeNode node = getSelectedItem();
		
		if (!MessageDialog.openConfirm(getSite().getShell(), Messages.SurveyDesignDataPage_DeleteDialogTitle, 
				MessageFormat.format(Messages.SurveyDesignDataPage_DeleteMessage1 + "\n\n" + Messages.SurveyDesignDataPage_DeleteMessage2, new Object[]{node.getType() == TreeNode.Type.MISSION? Messages.SurveyDesignDataPage_missionLabel : Messages.SurveyDesignDataPage_surveyLabel, node.getId()}))){ //$NON-NLS-1$
			return;
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getSite().getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					Session session = HibernateManager.openSession();
					try{
						if (node.getType() == TreeNode.Type.MISSION){
							DeleteSurveyElementHandler.deleteMission(node.getUuid(), session);
						}else if (node.getType() == TreeNode.Type.SURVEY){
							DeleteSurveyElementHandler.deleteSurvey(node.getUuid(), session);
						}
					}finally{
						session.close();
					}
				}
			});
		} catch (Exception ex) {
			EcologicalRecordsPlugIn.displayLog(MessageFormat.format(Messages.SurveyDesignDataPage_DeleteError, new Object[]{node.getId()}), ex);
		}
		
	}
	
	/*
	 * creates the tree/table columns
	 */
	private void createTableColumns(SurveyDesign sd){
		//dispose of existing columns
		for(TreeColumn tc : dataViewer.getTree().getColumns()){
			tc.dispose();
		}
		
		columnToAttributeKey = new HashMap<Integer, String> ();
		
		TreeColumn column1 = new TreeColumn(dataViewer.getTree(), SWT.LEFT);
		column1.setText(Messages.SurveyDesignDataPage_IdColumnName);
		column1.setWidth(150);
		
		TreeColumn column2 = new TreeColumn(dataViewer.getTree(), SWT.LEFT);
		column2.setText(Messages.SurveyDesignDataPage_StartColumnName);
		column2.setWidth(100);
		
		TreeColumn column3 = new TreeColumn(dataViewer.getTree(), SWT.LEFT);
		column3.setText(Messages.SurveyDesignDataPage_EndColumnName);
		column3.setWidth(100);
		
		//TODO: when implementing multi-ca analysis we probably want a ca column
		
		int col = 3;
		for (MissionProperty mp : sd.getMissionProperties()){
			TreeColumn column = new TreeColumn(dataViewer.getTree(), SWT.LEFT);
			column.setText(mp.getAttribute().getName());
			column.setWidth(100);
			column.setImage(mp.getAttribute().getType().getImage());
			columnToAttributeKey.put(col, mp.getAttribute().getKeyId());
			col++;
		}
		
	}
	
	/**
	 * Updates the widgets with the value from the intelligence.
	 */
	public void initValues() {
		createTableColumns(parentEditor.getSurveyDesign());
		form.setText(MessageFormat.format(Messages.SurveyDesignDataPage_FormHeader, new Object[]{parentEditor.getSurveyDesign().getName()}));
		loadSurveysJob.schedule();

	}
	
	@Override
	public void setFocus() {
		dataViewer.getControl().setFocus();
	}

	@Override
	public void dispose(){
		loadSurveysJob.cancel();
		for (EventType event : events){
			SurveyEventHandler.getInstance().removeListener(event, refreshListener);
		}
		super.dispose();
		if (toolkit != null){
			toolkit.dispose();
			toolkit = null;
		}
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing
	}

	@Override
	public void doSaveAs() {
		// not allowed
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	/*
	 * job for loading all data
	 */
	private Job loadSurveysJob = new Job(Messages.SurveyDesignDataPage_LoadingJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<TreeNode> nodes = new ArrayList<TreeNode>();
			Session s = HibernateManager.openSession();
			try{
				SurveyFilter sf = new SurveyFilter();
				sf.setDateFilter(null, null, null);
				sf.setSurveyState(null);
				sf.setSurveyDesignKeyFilters(new String[]{parentEditor.getSurveyDesign().getKeyId()});

				List<SurveyEditorInput> surveys = SurveyHibernateManager.getInstance().getSurveys(s, sf);
				for(SurveyEditorInput in : surveys){
					Survey ss = (Survey) s.load(Survey.class, in.getUuid());
					TreeNode node = new TreeNode(ss.getUuid(), ss.getId(), ss.getStartDate(), ss.getEndDate(), TreeNode.Type.SURVEY);
					
					for (Mission m : ss.getMissions()){
						TreeNode kid = new TreeNode(m.getUuid(), m.getId(), m.getStartDate(), m.getEndDate(), TreeNode.Type.MISSION);
						node.addKid(kid);
						for (MissionPropertyValue mpv : m.getMissionPropertyValues()){
							kid.addAttribute(mpv.getMissionAttribute().getKeyId(), mpv.getValueAsString());
						}
					}
					node.sortKids();
					nodes.add(node);
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				}
				
				Collections.sort(nodes, treeNodeComparator);
				
			}finally{
				s.close();
			}

			if (monitor.isCanceled()) return Status.CANCEL_STATUS;
			Display.getDefault().syncExec(new Runnable(){

				@Override
				public void run() {
					if (dataViewer == null || dataViewer.getTree().isDisposed()) return;
					
					dataViewer.setInput(nodes);
					dataViewer.expandAll();
					dataViewer.refresh();
				}});
			return Status.OK_STATUS;
		}
		
	};
	
	/*
	 * Internal tree node
	 */
	private static class TreeNode{

		public static enum Type {SURVEY, MISSION};
		
		private String id;
		private Date startDate;
		private Date endDate;

		private byte[] uuid;
		private Type type;
		
		private HashMap<String, String> attributeValues;
		private List<TreeNode> kids;
		private TreeNode parent;
		public TreeNode(byte[] uuid, String id, Date startDate, Date endDate, Type type){
			this.uuid = uuid;
			this.id = id;
			this.startDate = startDate;
			this.endDate = endDate;
			this.type = type;
			
			kids = new ArrayList<TreeNode>();
			attributeValues = new HashMap<String, String>();
		}
		
		public List<TreeNode> getKids(){
			return this.kids;
		}
		
		public void addKid(TreeNode kid){
			kid.parent = this;
			this.kids.add(kid);
		}
		
		public TreeNode getParent(){
			return this.parent;
		}
		public void addAttribute(String key, String value){
			attributeValues.put(key, value);
		}
		
		public Type getType(){
			return this.type;
		}
		
		public Image getImage(){
			if (type == Type.SURVEY){
				return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SURVEY_ICON);
			}else {
				return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.MISSION_ICON);
			}
		}
		public String getId(){
			return this.id;
		}
		public byte[] getUuid(){
			return this.uuid;
		}
		public Date getStartDate(){
			return this.startDate;
		}
		public Date getEndDate(){
			return this.endDate;
		}
		public String getAttributeValue(String key){
			return attributeValues.get(key);
		}
		
		public void sortKids(){
			Collections.sort(kids, treeNodeComparator);
		}
	}
}
