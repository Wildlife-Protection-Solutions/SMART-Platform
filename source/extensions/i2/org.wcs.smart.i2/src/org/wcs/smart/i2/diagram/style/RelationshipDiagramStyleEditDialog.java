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
package org.wcs.smart.i2.diagram.style;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.RelationshipDiagramManager;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.model.RelationshipDiagramEdgeStyleOptions;
import org.wcs.smart.i2.model.RelationshipDiagramEntityTypeStyle;
import org.wcs.smart.i2.model.RelationshipDiagramNodeStyleOptions;
import org.wcs.smart.i2.model.RelationshipDiagramRelationshipTypeStyle;
import org.wcs.smart.i2.model.RelationshipDiagramStyle;
import org.wcs.smart.i2.model.RelationshipDiagramStyleOptions;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * The relationship diagram style dialog for managing 
 * relationship diagram style options.
 * 
 * @author elitvin
 * @since 6.0.0
 */
public class RelationshipDiagramStyleEditDialog extends AbstractPropertyJHeaderDialog {

	private static final int DIALOG_WIDTH = 700;
	private static final int DIALOG_HEIGHT = 700;
	
	private RelationshipDiagramStyle rdStyle;
	
	private Text txtStyleName;
	private ControlDecoration styleNameDecoration;
	
	private TreeViewer treeViewer;

	private Composite infoInnerPanel;
	private Composite emptyComposite;
	private RelationshipDiagramDefaultStyleComposite defaultComposite;
	private RelationshipDiagramNodeStyleComposite nodeRootComposite;
	private RelationshipDiagramNodeStyleComposite entityTypeComposite;
	private RelationshipDiagramEdgeStyleComposite relationshipTypeComposite;
	
	public RelationshipDiagramStyleEditDialog(Shell shell, final RelationshipDiagramStyle style) {
		super(shell, "Relationship Diagram Style");
		if (style.getUuid() == null) {
			//this is a newly created style
			rdStyle = style;
		} else {
			//reloading current style with full data
			rdStyle = getStyle(getShell(), style.getUuid());
		}
	}

	private RelationshipDiagramStyle getStyle(Shell shell, UUID uuid) {
		final RelationshipDiagramStyle[] style = new RelationshipDiagramStyle[1];
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Loading relationship diagram style", 1);
					try(Session s = HibernateManager.openSession()){
						s.beginTransaction();
						try {
							style[0] = RelationshipDiagramManager.INSTANCE.getStyle(s, uuid);
						} catch (Exception ex) {
							SmartPlugIn.displayLog("Error occurs while loading relationship diagram style.", ex);
						} finally {
							s.getTransaction().rollback();
						}
					}
				}
			});
		} catch (Exception e) {
			SmartPlugIn.displayLog("Error occurs while loading relationship diagram style.", e);
		}
		return style[0];
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(DIALOG_WIDTH, DIALOG_HEIGHT);
	}

	@Override
	protected Composite createContent(Composite parent) {
		throw new IllegalStateException("Method shuold never be called."); //$NON-NLS-1$
	}
	
	@Override
	public Control createDialogArea(Composite parent) {
		setChangesMade(rdStyle.getUuid() == null);
		
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite topCmp = new Composite(main, SWT.NONE);
		GridLayout topLayout = new GridLayout(3, false);
		topLayout.horizontalSpacing = 7; //need this to properly fit error decorator
		topCmp.setLayout(topLayout);
		topCmp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Label lblStyleName = new Label(topCmp, SWT.NONE);
		lblStyleName.setText("Style Name:");
		lblStyleName.setToolTipText("The name for current relationship diagram style.");

		txtStyleName = new Text(topCmp, SWT.BORDER);
		txtStyleName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtStyleName.setToolTipText("The name for current relationship diagram style.");
		txtStyleName.setText(rdStyle.getName() != null ? rdStyle.getName() : ""); //$NON-NLS-1$
		txtStyleName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isStyleNameValid()) {
					//update cached name
					rdStyle.setName(txtStyleName.getText());
					//update name for current language
					rdStyle.updateName(SmartDB.getCurrentLanguage(), txtStyleName.getText());
					styleNameDecoration.hide();
				} else {
					styleNameDecoration.show();
				}
				setChangesMade(true);
			}
		});

		styleNameDecoration = new ControlDecoration(txtStyleName, SWT.LEFT);
		styleNameDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		styleNameDecoration.setShowHover(true);
		styleNameDecoration.setDescriptionText(MessageFormat.format("Style Name cannot exceed {0} characters.", org.wcs.smart.ca.Label.MAX_LENGTH));
		styleNameDecoration.hide();
		
		Button btnTranslate = new Button(topCmp, SWT.PUSH);
		btnTranslate.setText("Translate...");
		btnTranslate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (rdStyle != null){
					TranslateSimpleListItemDialog translateDialog = new TranslateSimpleListItemDialog(getShell(), rdStyle);
					if (translateDialog.open() == Window.OK){
						updateText(rdStyle);
						setChangesMade(true);
					}
				}
			}
		});
		
		createMainControls(main);
		
		setTitle("Relationship Diagram Style");
		setMessage("Style configuration which can be applied to relationship diagram.");
		
		return main;
	}

	private void createMainControls(Composite main) {
		SashForm container = new SashForm(main, SWT.HORIZONTAL);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite innerLeft = new Composite(container, SWT.NONE);
		innerLeft.setLayout(new GridLayout());
		
		treeViewer = new TreeViewer(innerLeft, SWT.V_SCROLL | SWT.H_SCROLL| SWT.BORDER);
		treeViewer.setLabelProvider(new RelationshipDiagramStyledObjectsLabelProvider());
		treeViewer.setContentProvider(new  RelationshipDiagramStyledObjectsTreeContentProvider());
		treeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		treeViewer.setInput(rdStyle);
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateRightPanelState();
			}
		});
		treeViewer.expandToLevel(2);

		Composite rightPanel = new Composite(container, SWT.NONE);
		rightPanel.setLayout(new GridLayout(1, false));

		Group area = new Group(rightPanel, SWT.NONE);
		area.setLayout(new GridLayout());
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)area.getLayout()).marginWidth = 0;
		((GridLayout)area.getLayout()).marginHeight = 0;
		ScrolledComposite scrolled = new ScrolledComposite(area, SWT.V_SCROLL | SWT.H_SCROLL );
		scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// always show the focus control
		scrolled.setShowFocusedControl(true);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		
		infoInnerPanel = new Composite(scrolled, SWT.NONE);

		StackLayout layout = new StackLayout();
		layout.marginHeight = 2;
		infoInnerPanel.setLayout(layout);
		infoInnerPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		emptyComposite = new Composite(infoInnerPanel, SWT.NONE);
		emptyComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

		defaultComposite = new RelationshipDiagramDefaultStyleComposite(infoInnerPanel);
		defaultComposite.addOptionsChangeListener(new IStyleOptionsChangeListener() {
			@Override
			public void optionsChanged(RelationshipDiagramStyleOptions options) {
				rdStyle.setStyleOptions(options);
				setChangesMade(true);
			}
		});
		
		nodeRootComposite = new RelationshipDiagramNodeStyleComposite(infoInnerPanel);
		nodeRootComposite.addOptionsChangeListener(new INodeStyleOptionsChangeListener() {
			@Override
			public void optionsChanged(RelationshipDiagramNodeStyleOptions options) {
				RelationshipDiagramStyleOptions so = rdStyle.getStyleOptions();
				so.setRootNodeStyle(options);
				rdStyle.setStyleOptions(so);
				setChangesMade(true);
			}
		});
		

		entityTypeComposite = new RelationshipDiagramNodeStyleComposite(infoInnerPanel);
		entityTypeComposite.addOptionsChangeListener(new INodeStyleOptionsChangeListener() {
			@Override
			public void optionsChanged(RelationshipDiagramNodeStyleOptions options) {
				IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
				Object obj = selection.getFirstElement();
				if (obj instanceof IntelEntityType) {
					IntelEntityType entityType = (IntelEntityType) obj;
					if (options == null) {
						rdStyle.getEntityTypeStyles().remove(entityType);
					} else {
						RelationshipDiagramEntityTypeStyle etStyle = rdStyle.getEntityTypeStyles().get(entityType);
						if (etStyle == null) {
							etStyle = new RelationshipDiagramEntityTypeStyle();
							etStyle.setStyle(rdStyle);
							etStyle.setEntityType(entityType);
						}
						etStyle.setStyleOptions(options);
						rdStyle.getEntityTypeStyles().put(entityType, etStyle);
					}
					setChangesMade(true);
				}
			}
		});

		relationshipTypeComposite = new RelationshipDiagramEdgeStyleComposite(infoInnerPanel);
		relationshipTypeComposite.addOptionsChangeListener(new IEdgeStyleOptionsChangeListener() {
			@Override
			public void optionsChanged(RelationshipDiagramEdgeStyleOptions options) {
				IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
				Object obj = selection.getFirstElement();
				if (obj instanceof IntelRelationshipType) {
					IntelRelationshipType relationshipType = (IntelRelationshipType) obj;
					if (options == null) {
						rdStyle.getRelationshipTypeStyles().remove(relationshipType);
					} else {
						RelationshipDiagramRelationshipTypeStyle rtStyle = rdStyle.getRelationshipTypeStyles().get(relationshipType);
						if (rtStyle == null) {
							rtStyle = new RelationshipDiagramRelationshipTypeStyle();
							rtStyle.setStyle(rdStyle);
							rtStyle.setRelationshipType(relationshipType);
						}
						rtStyle.setStyleOptions(options);
						rdStyle.getRelationshipTypeStyles().put(relationshipType, rtStyle);
					}
					setChangesMade(true);
				}
			}
		});
		
		container.setWeights(new int[]{50,50});
		

		scrolled.setContent(infoInnerPanel);
		scrolled.setMinSize(infoInnerPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		updateRightPanelState();
	}

	private void updateRightPanelState() {
		IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
		Object obj = selection.getFirstElement();
		
		if (obj instanceof RelationshipDiagramTreeRootStyleObjects) {
			RelationshipDiagramTreeRootStyleObjects styleObj = (RelationshipDiagramTreeRootStyleObjects) obj;
			switch (styleObj) {
			case DEFAULT: {
				defaultComposite.setSourceOptions(rdStyle.getStyleOptions());
				((StackLayout)infoInnerPanel.getLayout()).topControl = defaultComposite;
				break;
			}
			case ROOT: {
				nodeRootComposite.setSourceOptions(rdStyle.getStyleOptions().getRootNodeStyle());
				((StackLayout)infoInnerPanel.getLayout()).topControl = nodeRootComposite;
				break;
			}
			case ENTITY_TYPE:
			case RELATIONSIP_TYPE:
				((StackLayout)infoInnerPanel.getLayout()).topControl = emptyComposite;
				break;
			}
		} else if (obj instanceof IntelEntityType) {
			RelationshipDiagramEntityTypeStyle etStyle = rdStyle.getEntityTypeStyles().get(obj);
			entityTypeComposite.setSourceOptions(etStyle != null ? etStyle.getStyleOptions() : null);
			((StackLayout)infoInnerPanel.getLayout()).topControl = entityTypeComposite;
		} else if (obj instanceof IntelRelationshipType) {
			RelationshipDiagramRelationshipTypeStyle rtStyle = rdStyle.getRelationshipTypeStyles().get(obj);
			relationshipTypeComposite.setSourceOptions(rtStyle != null ? rtStyle.getStyleOptions() : null);
			((StackLayout)infoInnerPanel.getLayout()).topControl = relationshipTypeComposite;
		} else {
			((StackLayout)infoInnerPanel.getLayout()).topControl = emptyComposite;
		}
		infoInnerPanel.layout();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(this.changesMade); //this will enable "Save" button when new model is just created
	}

	private void updateText(NamedItem item){
		String name = item.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
		txtStyleName.setText(name);
	}
	
	protected boolean isStyleNameValid() {
		return txtStyleName != null && txtStyleName.getText() != null 
				&& txtStyleName.getText().length() <= org.wcs.smart.ca.Label.MAX_LENGTH;
	}
	
	@Override
	protected boolean performSave() {
		if (!isStyleNameValid()) {
			MessageDialog.openError(getShell(), "Error", "Some data is not valid. Correct the data before saving changes.");
			return false;
		}
		boolean isOk = RelationshipDiagramManager.INSTANCE.saveStyle(getShell(), rdStyle);
		if (isOk) {
			setChangesMade(false);
		}
		return isOk;
	}


}
