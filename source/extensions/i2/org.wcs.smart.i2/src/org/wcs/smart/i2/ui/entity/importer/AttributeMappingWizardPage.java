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
package org.wcs.smart.i2.ui.entity.importer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.entity.importer.EntityImportConfig;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttributeGroup;
import org.wcs.smart.i2.model.OtherAttributeGroup;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Wizard page for collecting attribute to column mappings.
 * 
 * @author Emily
 *
 */
public class AttributeMappingWizardPage extends WizardPage{
	

	private static final String Y_ATTRIBUTE_DATA_KEY = "Y_ATTRIBUTE";

	private static final String ATTRIBUTE_DATA_KEY = "ATTRIBUTE";

	public static final String FILE_PAGE = "org.wcs.smart.i2.ui.entity.importer.mapping";
		
	private Composite mappingPanel;
	private ScrolledComposite sc ;
	private List<ComboViewer> mappings = null;
	private Font boldFont = null;
	
	private IntelEntityType lastType = null;
	private Path lastFile = null;
	
	protected AttributeMappingWizardPage() {
		super(FILE_PAGE);
	}

	@Override
	public boolean isPageComplete() {
		return super.isPageComplete();
	}

	@Override
	public void createControl(Composite parent) {
		sc = new ScrolledComposite(parent, SWT.V_SCROLL);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		
		mappingPanel = new Composite(sc, SWT.NONE);
		mappingPanel.setLayout(new GridLayout(2, false));
		
		FontData fd = mappingPanel.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(mappingPanel.getDisplay(), fd);
		mappingPanel.addDisposeListener(e->boldFont.dispose());
		
		sc.setContent(mappingPanel);
		sc.setMinSize(mappingPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		setTitle("Attribute Mapping");
		setMessage("Map entity type attributes to a column");
		setControl(sc);
	}
	
	public void initPage(){
		j.schedule();
	}
	
	public void updateConfiguration(EntityImportConfig configuration){
		for (ComboViewer viewer : mappings){
			if (!viewer.getSelection().isEmpty()){
				IntelAttribute attribute = (IntelAttribute) viewer.getData(ATTRIBUTE_DATA_KEY);
				
				if (attribute.getType() == AttributeType.POSITION){
					ComboViewer yField = (ComboViewer) viewer.getData(Y_ATTRIBUTE_DATA_KEY);
					
					Object[] items = (Object[]) ((IStructuredSelection)viewer.getSelection()).getFirstElement();
					if (items != null){
						int xindex = (int) items[1];
						items = (Object[]) ((IStructuredSelection)yField.getSelection()).getFirstElement();
						if (items != null){
							int yindex = (int) items[1];
							if (xindex > 0 && yindex > 0) configuration.addMapping(attribute, xindex, yindex);
						}
					}
				}else{
					Object[] items = (Object[]) ((IStructuredSelection)viewer.getSelection()).getFirstElement();
					if (items != null){
						int index = (int) items[1];
						if (index >= 0 ) configuration.addMapping(attribute, index);
					}
				}
			}
		}
	}
	
	Job j = new Job("load attribute info"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (lastType != null && lastFile != null){
				if (lastType.equals(((ImportEntityWizard)getWizard()).getImportConfiguration().getType()) &&
						lastFile.equals(((ImportEntityWizard)getWizard()).getImportConfiguration().getFile())){
					//same type & same file; lets not update details 
					return Status.OK_STATUS;
				}
			}
			HashMap<IntelEntityTypeAttributeGroup, List<IntelAttribute>> attributes = new HashMap<>();
			String name;
			Session s = HibernateManager.openSession();
			try{
				IntelEntityType type = (IntelEntityType) s.get(IntelEntityType.class, ((ImportEntityWizard)getWizard()).getImportConfiguration().getType().getUuid());
				lastType = type;
				name = type.getName();
				type.getAttributes().forEach( a -> {
					List<IntelAttribute> as = attributes.get(a.getAttributeGroup());
					if (as == null){
						as = new ArrayList<>();
						attributes.put(a.getAttributeGroup(), as);
					}
					as.add(a.getAttribute());
				}); 
				
			}finally{
				s.close();
			}
			
			//create the column headers from the csv file
			Path file = ((ImportEntityWizard)getWizard()).getImportConfiguration().getFile();
			lastFile = file;
			String[] headers = null;
			try(CSVReader reader = new CSVReader(Files.newBufferedReader(file))){
				headers = reader.readNext();
			}catch (Exception ex){
				Intelligence2PlugIn.displayLog(MessageFormat.format("Unable to read file ''{0}''. {1}", file.toString(), ex.getMessage()), ex);
				return Status.OK_STATUS;
			}
			final Object[] fheaders = new Object[headers.length+1];
			fheaders[0] = new Object[]{"",-1};
			for (int i = 1; i < fheaders.length; i ++){
				fheaders[i] = new Object[]{headers[i-1], i-1};
			}
			Display.getDefault().syncExec(()->{
				for (Control c : mappingPanel.getChildren()){
					c.dispose();
				}
				setMessage(MessageFormat.format("Map entity type ''{0}'' attributes to a column", name));
				mappings = new ArrayList<>();
				ArrayList<IntelEntityTypeAttributeGroup> groups = new ArrayList<>();
				groups.addAll(attributes.keySet());
				groups.sort((a,b)-> {
					if (a == null) return 1;
					if (b == null) return -1;
					return  Integer.compare(a.getOrder(), b.getOrder());
				});
				
				for (IntelEntityTypeAttributeGroup g : groups){
					Composite t = new Composite(mappingPanel, SWT.NONE);
					t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2,1));
					t.setLayout(new GridLayout(2, false));
					((GridLayout)t.getLayout()).marginWidth = 0;
					
					Label l = new Label(t, SWT.NONE);
					l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
					l.setFont(boldFont);
					if (g != null){
						l.setText(g.getName());
					}else{
						l.setText(OtherAttributeGroup.INSTANCE.getName());
					}
					l = new Label(t, SWT.SEPARATOR | SWT.HORIZONTAL);
					l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
					for (IntelAttribute a : attributes.get(g)){
						
						l = new Label(mappingPanel, SWT.NONE);
						l.setText(MessageFormat.format("{0}:", a.getName()));
						
						if (a.getType() == AttributeType.POSITION){
							Composite xy = new Composite(mappingPanel, SWT.NONE);
							xy.setLayout(new GridLayout(4, false));
							xy.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
							((GridLayout)xy.getLayout()).marginWidth = 0;
							((GridLayout)xy.getLayout()).marginHeight = 0;
							
							l = new Label(xy, SWT.NONE);
							l.setText("X:");
							
							ComboViewer xviewer = new ComboViewer(xy, SWT.READ_ONLY | SWT.DROP_DOWN);
							xviewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
							xviewer.setContentProvider(ArrayContentProvider.getInstance());
							xviewer.setLabelProvider(new LabelProvider(){
									@Override
									public String getText(Object element){
										if (element instanceof Object[]){
											return (String)((Object[])element)[0];
										}
										return super.getText(element);
									}
							});
							xviewer.setInput(fheaders);
							xviewer.setData(ATTRIBUTE_DATA_KEY, a);
							
							l = new Label(xy, SWT.NONE);
							l.setText("Y:");
							
							ComboViewer yviewer = new ComboViewer(xy, SWT.READ_ONLY | SWT.DROP_DOWN);
							yviewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
							yviewer.setContentProvider(ArrayContentProvider.getInstance());
							yviewer.setLabelProvider(new LabelProvider(){
									@Override
									public String getText(Object element){
										if (element instanceof Object[]){
											return (String)((Object[])element)[0];
										}
										return super.getText(element);
									}
							});
							yviewer.setInput(fheaders);
							yviewer.setData(ATTRIBUTE_DATA_KEY, a);
							
							mappings.add(xviewer);
							xviewer.setData(Y_ATTRIBUTE_DATA_KEY, yviewer);
						}else{
							ComboViewer viewer = new ComboViewer(mappingPanel, SWT.READ_ONLY | SWT.DROP_DOWN);
							viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
							viewer.setContentProvider(ArrayContentProvider.getInstance());
							viewer.setLabelProvider(new LabelProvider(){
									@Override
									public String getText(Object element){
										if (element instanceof Object[]){
											return (String)((Object[])element)[0];
										}
										return super.getText(element);
									}
							});
							viewer.setInput(fheaders);
							viewer.setData(ATTRIBUTE_DATA_KEY, a);
							mappings.add(viewer);
						}
					}
				}
				mappingPanel.layout(true, true);
				sc.setMinSize(mappingPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT));		
			});
			return Status.OK_STATUS;
		}
		
	};
}
