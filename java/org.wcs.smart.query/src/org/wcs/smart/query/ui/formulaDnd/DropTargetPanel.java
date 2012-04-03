package org.wcs.smart.query.ui.formulaDnd;

import java.util.ArrayList;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

public class DropTargetPanel {

	private ScrolledComposite dropTarget = null;
	private ProxyItem proxy = null;

	private ArrayList<Control> items = new ArrayList<Control>();

	final static Transfer[] types = new Transfer[] { LocalSelectionTransfer
			.getTransfer() };
	private Composite dropTargetContent;

	public static Transfer[] getTransferTypes() {
		return types;
	}

	public void dispose() {

	}

	public void addElement(DropItem item) {
		items.add(item);
		orderElements();
	}

	private void orderElements() {
		int currx = 0;
		int curry = 0;
		int maxWidth = dropTarget.getBounds().width;
		int lastHeight = 10;
		for (int i = 0; i < items.size(); i++) {
			Point pnt = items.get(i).computeSize(SWT.DEFAULT, SWT.DEFAULT);
			// Rectangle r = items.get(i).getBounds();

			if (currx + pnt.x > maxWidth) {
				// move to next line

				curry += curry | pnt.y;
				currx = 0;
			}
			items.get(i).setBounds(currx, curry, pnt.x, pnt.y);
			currx += pnt.x;
			lastHeight = pnt.y;
			if (items.get(i) instanceof Composite) {
				((Composite) items.get(i)).layout();
			}

		}

		dropTargetContent.setSize(maxWidth, curry + lastHeight);
		dropTarget.redraw();

	}

	public Composite getComposite() {
		return dropTargetContent;
	}

	public Composite createComposite(Composite parent) {

		dropTarget = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.BORDER);
		dropTarget.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dropTarget.addListener(SWT.Resize, new Listener() {

			@Override
			public void handleEvent(Event event) {
				orderElements();

			}
		});

		dropTargetContent = new Composite(dropTarget, SWT.BORDER);
		dropTarget.setContent(dropTargetContent);

		// dropTarget = new ScrolledComposite(parent, SWT.V_SCROLL |
		// SWT.BORDER);
		// dropTarget.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
		// true));
		// dropTarget.setExpandVertical(true);
		// dropTarget.setLayout(new GridLayout(1, false));
		//
		// dropTargetContent = new Composite(dropTarget, SWT.BORDER);
		// dropTargetContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
		// true, true));
		// Label lblhelp = new Label(dropTargetContent, SWT.NONE);
		// lblhelp.setText("HEKO!!!!");
		// lblhelp.setBounds(0,0,200,100);
		// dropTarget.setContent(dropTargetContent);

		proxy = new ProxyItem(dropTargetContent);
		proxy.setVisible(false);

		DropTarget dtarget = new DropTarget(dropTarget, DND.DROP_MOVE);
		dtarget.setTransfer(types);
		dtarget.addDropListener(new DropTargetAdapter() {

			private DropItem dp;

			public void dragEnter(DropTargetEvent event) {
				if (dp != null) {
					// continuing drag
					return;
				}

				StructuredSelection selection = (StructuredSelection) LocalSelectionTransfer
						.getTransfer().getSelection();
				if (selection == null) {
					return;
				}
				Object obj = selection.getFirstElement();
				dp = (DropItem) obj;
				dp.setVisible(false);

				int i = items.indexOf(dp);
				items.add(i, proxy);
				items.remove(dp);

				dp.setBounds(0, 0, 0, 0);
				proxy.setLabelText(dp.getText());
				proxy.setVisible(true);

				orderElements();
			}

			public void dragLeave(DropTargetEvent event) {

			}

			public void dragOperationChanged(DropTargetEvent event) {

			}

			public void dragOver(DropTargetEvent event) {
				moveElements(event.x, event.y);
				orderElements();
			}

			public void dropAccept(DropTargetEvent event) {
			}

			@Override
			public void drop(DropTargetEvent event) {
				moveElements(event.x, event.y);

				int i = items.indexOf(proxy);
				items.add(i, dp);
				dp.setVisible(true);
				items.remove(proxy);
				proxy.setVisible(false);
				orderElements();
				dp = null;
			}

			private void moveElements(int x, int y) {
				Control target = null;
				boolean before = false;
				for (Control children : items) {
					Point p = children.getParent().toDisplay(
							children.getBounds().x, children.getBounds().y);
					Rectangle r = new Rectangle(p.x, p.y,
							children.getBounds().width,
							children.getBounds().height);
					if (r.contains(x, y)) {
						target = children;
						before = x < p.x + (children.getBounds().width / 2.0);
						break;
					}
				}
				if (target == null) {
					items.remove(proxy);
					items.add(proxy);
				} else if (target == proxy) {
					return;
				} else {
					items.remove(proxy);
					int toIndex = items.indexOf(target);
					if (!before) {
						toIndex++;
					}
					if (toIndex < 0) {
						toIndex = 0;
					}
					items.add(toIndex, proxy);
				}

			}
		});

		dropTarget.setSize(dropTarget.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		dropTargetContent.setSize(dropTarget.computeSize(SWT.DEFAULT,
				SWT.DEFAULT));
		return dropTarget;
	}
}
