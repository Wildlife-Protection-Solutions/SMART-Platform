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
package org.wcs.smart.ui.internal.ca.properties;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hibernate.Query;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IResolve;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.internal.ui.actions.ResetService;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.Language;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.udig.catalog.smart.SmartGeoResource;
import org.wcs.smart.udig.catalog.smart.SmartGeoResourceInfo;
import org.wcs.smart.udig.catalog.smart.SmartService;
import org.wcs.smart.udig.catalog.smart.SmartServiceExtension;
import org.wcs.smart.udig.catalog.smart.ui.DesktopSessionProvider;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBWriter;

/**
 * Property page for managing conservation area
 * areas including the conservation area, administrative areas etc.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AreaPropertyPage extends AbstractPropertyJHeaderDialog {

	
	private static final String CLEAR_TEXT = Messages.AreaPropertyPage_Clear_Text;
	private static final String UPDATE_TEXT = Messages.AreaPropertyPage_ChangeLabels_Text;
	private static final String LOAD_TEXT = Messages.AreaPropertyPage_Load_Text;
	
	private final static String MSG_NOT_SET = Messages.AreaPropertyPage_Area_Underfined;
	private final static String MSG_ERROR = Messages.AreaPropertyPage_Error_Message;

	// buttons for modifying layers; these are ordered by Area.AreaType.values()
	private Button[] btnLoad;
	private Button[] btnClear;
	private Button[] btnUpdate;
	
	private FileDialog fileDialog;
	/* map of areatype to status label */
	private HashMap<Area.AreaType, Label> lblStatus = new HashMap<Area.AreaType, Label>();
	/* map of areatype to label values */
	private HashMap<Area.AreaType, String> initValues = new HashMap<Area.AreaType, String>();

	private ConservationArea currentCa = null;
	
	public AreaPropertyPage(Shell parent) {
		super(parent, Messages.AreaPropertyPage_Dialog_Title);
		currentCa = SmartDB.getCurrentConservationArea();
	}

	@Override
	public boolean close() {
		boolean canClose = super.close();
		return canClose;
	}

	@Override
	public int open() {
		ProgressMonitorDialog ppd = new ProgressMonitorDialog(getShell());
		try {
			ppd.run(true,  false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					initLayers(false, monitor);
					
				}
			});
		} catch (Exception ex) {
			String err = Messages.AreaPropertyPage_Error_LoadingFeatureCounts;
			SmartPlugIn.log(err, ex);
			setErrorMessage(err);
		}
		return super.open();
	}

	/**
	 * updates the udig service
	 * and determines which of the layers have features (are set) and which are underfined (not set) 
	 * 
	 * @param updated if the udig service needs to be reset; otherwise the existing service will be used
	 */
	private void initLayers(boolean updated, IProgressMonitor monitor) {
		monitor.beginTask(Messages.AreaPropertyPage_Progress_RefreshingCounts, Area.AreaType.values().length);
		
		// find smart service for given conservation area
		HashMap<String, Serializable> params = new HashMap<String, Serializable>();
		params.put(SmartServiceExtension.CA_UUID_KEY, currentCa.getUuid());
		URL serviceurl = SmartServiceExtension.createURL(params);
		
		List<IResolve> services = CatalogPlugin.getDefault().getLocalCatalog().find(serviceurl, monitor);
		SmartService ss = null;
		if (services.size() > 0){
			ss = (SmartService) CatalogPlugin.getDefault().getLocalCatalog().find(serviceurl, monitor).get(0);
		}else{
			ss = new SmartService(params, new DesktopSessionProvider());
			CatalogPlugin.getDefault().getLocalCatalog().add(ss);
		}
		
		if (updated){
			//we need to reset the service
			List<IService> list = new ArrayList<IService>();
			list.add(ss);
			ResetService.reset(list, monitor);
			ss = (SmartService) CatalogPlugin.getDefault().getLocalCatalog().find(serviceurl, monitor).get(0);
		}
		
		//update information about each servce
		try {
			List<IResolve> smartresource = ss.members(monitor);

			int i = 0;
			for (Iterator<?> iterator = smartresource.iterator(); iterator.hasNext();) {
				monitor.worked(i++);
				SmartGeoResource sgeo = (SmartGeoResource) iterator.next();
				String message = null;
				int cnt = ((SmartGeoResourceInfo)sgeo.getInfo(monitor)).getFeatureCount();
				if (cnt == 0) {
					message = MSG_NOT_SET;
				} else if (cnt > 0) {
					message = MessageFormat.format(Messages.AreaPropertyPage_Set_Message, new Object[]{cnt});
				} else if (cnt < 0){
					message = MSG_ERROR;
				}
				initValues.put(sgeo.getType(), message);
			}
		} catch (Exception ex) {
			String error = Messages.AreaPropertyPage_Error_LoadingFeatureCountsSmart;
			SmartPlugIn.log(error, ex);
			setErrorMessage(error);
		}
		monitor.done();
	}

	/**
	 * @see
	 * org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent
	 * (org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createContent(Composite parent) {
		setTitle(Messages.AreaPropertyPage_PageTitle);
		setMessage(Messages.AreaPropertyPage_Dialog_Message);

		Composite comp = new Composite(parent, SWT.BORDER_DASH);
		comp.setLayout(new GridLayout(5, false));

		lblStatus = new HashMap<Area.AreaType, Label>();
		btnLoad = new Button[Area.AreaType.values().length];
		btnClear = new Button[Area.AreaType.values().length];
		btnUpdate = new Button[Area.AreaType.values().length];
		for (int i = 0; i < Area.AreaType.values().length; i++) {
			Label lbl = new Label(comp, SWT.NONE);
			lbl.setText(SmartLabelProvider.getAreaTypeName(Area.AreaType.values()[i]) + ":"); //$NON-NLS-1$
			lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
			
			lbl = new Label(comp, SWT.NONE);
			lbl.setText(initValues.get(Area.AreaType.values()[i]));
			lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			lblStatus.put(Area.AreaType.values()[i], lbl);
			
			btnLoad[i] = new Button(comp, SWT.NONE);
			final Area.AreaType type = Area.AreaType.values()[i];
			final int index = i;
			btnLoad[i].setText(LOAD_TEXT);
			btnLoad[i].addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					openFileDialog(getShell(), type, btnLoad[index].getText().equals(LOAD_TEXT));
				}
			});
			
			btnClear[i] = new Button(comp, SWT.NONE);
			btnClear[i].setText(CLEAR_TEXT);
			btnClear[i].addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					deleteAll(type);
				}
			});
			
			btnUpdate[i] = new Button(comp, SWT.NONE);
			btnUpdate[i].setText(UPDATE_TEXT);
			btnUpdate[i].addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					updateLabels(type);
				}
			});

		}
		updateLabels();
		return comp;
	}
	
	private void updateLabels(Area.AreaType type){
		AreaNameDialogPage dialog = new AreaNameDialogPage(getShell(), type);
		dialog.open();
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create Close button only
		createButton(parent, IDialogConstants.CLOSE_ID,IDialogConstants.CLOSE_LABEL, false);
		getButton(IDialogConstants.CLOSE_ID).setFocus();
		super.setReturnCode(IDialogConstants.CLOSE_ID);
	}
	
	/*
	 * clears areas from database
	 */
	private void deleteAll(Area.AreaType areatype){
		boolean ret = MessageDialog.openConfirm(getShell(), Messages.AreaPropertyPage_Clear_DialogTitle + SmartLabelProvider.getAreaTypeName(areatype), MessageFormat.format(Messages.AreaPropertyPage_Clear_DialogMessage, new Object[]{SmartLabelProvider.getAreaTypeName(areatype)}));
		if (!ret ){
			return;
		}
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			// remove existing areas
			String query = "delete from Area where conservationArea = :ca and type =:type"; //$NON-NLS-1$
			Query q = session.createQuery(query);
			q.setParameter("ca", currentCa); //$NON-NLS-1$
			q.setParameter("type", areatype); //$NON-NLS-1$
			q.executeUpdate();
			session.getTransaction().commit();
		}catch (Exception ex){
			session.getTransaction().rollback();
			SmartPlugIn.displayLog(Messages.AreaPropertyPage_Error_DeletingArea, ex);
		}finally{
			session.close();
		}
		
		// reset feature counts
		ProgressMonitorDialog ppd = new ProgressMonitorDialog(getShell());
		try {
			ppd.run(true,  false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					initLayers(true, monitor);
				}
			});
		} catch (Exception ex) {
			String error = Messages.AreaPropertyPage_Error_ResettingCounts;
			SmartPlugIn.log(error, ex);
			setErrorMessage(error);
		}
		//update labels
		updateLabels();
	}


	/*
	 * Displays file dialog for loading new conservation area boundaries.
	 * 
	 * @param parent 
	 * @param areatype - the area type to load
	 * @verify - if user should be prompted to ensure they want to overwrite existing
	 */
	private boolean openFileDialog(Composite parent,
			final Area.AreaType areatype, boolean verify) {
		
		//check to ensure
		if (verify){
			boolean ret = MessageDialog.openConfirm(getShell(),
					Messages.AreaPropertyPage_Update_DialogTitle + 
					SmartLabelProvider.getAreaTypeName(areatype), 
					MessageFormat.format(Messages.AreaPropertyPage_Update_DialogMessage, 
							new Object[]{SmartLabelProvider.getAreaTypeName(areatype)}));
			if (!ret ){
				return false;
			}
		}
		
		
		fileDialog = new FileDialog(parent.getShell(), SWT.SINGLE | SWT.OPEN);
		fileDialog.setText(Messages.AreaPropertyPage_Load_DialogTitle + SmartLabelProvider.getAreaTypeName(areatype));
		fileDialog.setFilterExtensions(new String[]{"*.shp", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fileDialog.setFilterNames(new String[]{Messages.AreaPropertyPage_Shapefile_FilterName, Messages.AreaPropertyPage_AllFiles_FilterName});
		
		String result = fileDialog.open();
		if (result == null) {
			return false;
		}

		// get file name
		String path = fileDialog.getFilterPath();
		String filenames = fileDialog.getFileName();
		URL url = null;
		try {
			url = new File(path
					+ System.getProperty("file.separator") + filenames).toURI().toURL(); //$NON-NLS-1$
		} catch (Throwable e) {
			SmartPlugIn.displayLog("Cannot determine file selected.", e); //$NON-NLS-1$
		}

		if (url == null) {
			setErrorMessage(Messages.AreaPropertyPage_Error_CouldNotLoadFile);
			return false;
		}
		loadDataSet(areatype, url);
		updateLabels();
		return true;
	}

	private void loadDataSet(final Area.AreaType areatype, final URL url2) {
		final ProgressMonitorDialog ppd = new ProgressMonitorDialog(getShell());
		try {
			ppd.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.AreaPropertyPage_Progress_LoadingFeatures, 0);

					SimpleFeatureCollection collection = null;
					final AreaIdDialog[] idDialog = new AreaIdDialog[1];
					try{
						FileDataStore store = FileDataStoreFinder.getDataStore(url2);
						collection = store.getFeatureSource().getFeatures();
					}catch (final Exception ex){
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								SmartPlugIn.displayLog(Messages.AreaPropertyPage_Error_ReadingFile, ex);
								
							}});
						
						return;
					}
					if (collection.getSchema().getCoordinateReferenceSystem() == null){
						//check projection
						getShell().getDisplay().syncExec(new Runnable(){
							@Override
							public void run() {
								MessageDialog.openError(ppd.getShell(), Messages.AreaPropertyPage_Error_DialogTitle, Messages.AreaPropertyPage_Error_Projection);
							}});
						return;
					}
					final SimpleFeatureCollection collection2 = collection;
					getShell().getDisplay().syncExec(new Runnable(){
						@Override
						public void run() {
							idDialog[0] = new AreaIdDialog(getShell(), collection2.getSchema());
							idDialog[0].open();
						}});
					
					if (idDialog[0].getReturnCode() != IDialogConstants.OK_ID){
						return;
					}
					
					
					boolean modifiedWarnings = false;
					Session s = HibernateManager.openSession();
					s.beginTransaction();
					// add new areas
					try {
						//remove existing areas
						String query = "delete from Area where conservationArea = :ca and type =:type"; //$NON-NLS-1$
						Query q =s.createQuery(query);
						q.setParameter("ca", currentCa); //$NON-NLS-1$
						q.setParameter("type", areatype); //$NON-NLS-1$
						q.executeUpdate();

						MathTransform transform = CRS.findMathTransform(collection.getSchema().getCoordinateReferenceSystem(), Area.AREA_CRS);
						//find feature store
						WKBWriter writer = new WKBWriter();
						
						try(SimpleFeatureIterator it = collection.features()) {
							int cnt = 0;
							List<String> currentKeys = new ArrayList<String>();
							while (it.hasNext()) {
								SimpleFeature sf = it.next();
								if (monitor.isCanceled()) {
									s.getTransaction().rollback();
									break;
								}

								Area area = new Area();
								area.setType(areatype);
								area.setConservationArea(AreaPropertyPage.this.currentCa);
								
								String defaultName = areatype + "_" + cnt++; //$NON-NLS-1$
								HashMap<Language, AttributeDescriptor> data = idDialog[0].getSelectedFields();
								for(Entry<Language, AttributeDescriptor> entry : data.entrySet()){
									String id = sf.getAttribute(entry.getValue().getName()).toString();
									if (id.length() > Area.NAME_MAX_LENGTH){
										id = id.substring(0, Area.NAME_MAX_LENGTH);
									}
									area.updateName(entry.getKey(), id);
									if (entry.getKey().isDefault()){
										defaultName = id;
									}
								}
								
								/// - geometry; ensure they are valid and simple
								Geometry geom = (Geometry) sf.getDefaultGeometry();
								geom = JTS.transform(geom, transform);
								if (!geom.isValid() || !geom.isSimple()){
									//try buffer 0 to clean up and check again
									modifiedWarnings = true;
									geom = geom.buffer(0);
									if (!geom.isValid() || !geom.isSimple()){
										//still not valid and not simple so error out
										throw new Exception(MessageFormat.format(Messages.AreaPropertyPage_InvalidGeometry, defaultName));
									}
								}
								area.setGeom(writer.write(geom));
								
								String key = Area.generateKey(defaultName, Messages.Area_EmptyKey, currentKeys);
								area.setKeyId(key);
								currentKeys.add(key);
								
								//save
								s.save(area);
							}

							s.getTransaction().commit();
						}

					} catch (Exception e) {
						try{
							s.getTransaction().rollback();
						}catch (Exception ex){
							SmartPlugIn.log("", ex); //$NON-NLS-1$
						}
						throw(new InvocationTargetException(e, e.getMessage()));
					}finally{
						s.close();
					}

					if (monitor.isCanceled()) {
						return;
					}
					
					ConservationAreaManager.getInstance().fireAreaChanged(areatype);
					initLayers(true, monitor);
					monitor.done();
					
					if (modifiedWarnings){
						Display.getDefault().syncExec(()->{
							MessageDialog.openWarning(getShell(), Messages.AreaPropertyPage_GeomModWarnTitle, Messages.AreaPropertyPage_GeomModWarn);
						});
					}
				}
			});
		} catch (Exception e) {
			SmartPlugIn.displayLog(Messages.AreaPropertyPage_Error_UpdatingAreas + ": " + e.getMessage(), e); //$NON-NLS-1$
		}
	}
	
	/*
	 * Updates the layer status labels and associated buttons
	 */
	private void updateLabels(){
		for (int i =0; i < Area.AreaType.values().length; i ++){
			final Label lbl = lblStatus.get(Area.AreaType.values()[i]);
			final String fmessage = initValues.get(Area.AreaType.values()[i]);
			final int index = i;
			if (lbl != null && getShell() != null && getShell().getDisplay() != null){
				getShell().getDisplay().asyncExec(new Runnable(){
					@Override
					public void run() {
						lbl.setText(fmessage);
						if (fmessage.equals(MSG_ERROR)){
							btnClear[index].setEnabled(false);
							btnLoad[index].setEnabled(false);
							btnUpdate[index].setEnabled(false);
						}else if (fmessage.equals(MSG_NOT_SET)){
							btnClear[index].setEnabled(false);
							btnLoad[index].setEnabled(true);
							btnUpdate[index].setEnabled(false);
						}else{
							btnClear[index].setEnabled(true);
							btnLoad[index].setEnabled(true);
							btnUpdate[index].setEnabled(true);
						}
					
						
					}});
				
			}
		}

	}

	/*
	 * @see
	 * org.wcs.smart.ui.ca.properties.AbstractPropertyJHeaderDialog#performSave
	 * ()
	 */
	@Override
	protected boolean performSave() {
		return true;
	}
}
