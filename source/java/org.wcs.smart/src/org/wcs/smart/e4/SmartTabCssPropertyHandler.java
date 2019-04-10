package org.wcs.smart.e4;


public class SmartTabCssPropertyHandler{
	
}
//public class SmartTabCssPropertyHandler extends AbstractCSSPropertySWTHandler {
//		private static final String UNSELECTED_TABS_COLOR_PROP = "swt-unselected-tabs-color";
//
//		private static final String DEPRECATED_UNSELECTED_TABS_COLOR_PROP = "unselected-tabs-color";
//
//		private static final String RESIZE_LISTENER = "CSSPropertyUnselectedTabsSWTHandler.resizeListener";
//
//		@Override
//		protected void applyCSSProperty(Control control, String property,
//				CSSValue value, String pseudo, CSSEngine engine) throws Exception {
//			if (!(control instanceof CTabFolder)
//					|| !isUnselectedTabsColorProp(property)) {
//				return;
//			}
//			CTabFolder folder = ((CTabFolder) control);
//			CTabFolderRenderer renderer = folder.getRenderer();
//			if (!(renderer instanceof ICTabRendering)) {
//				return;
//			}
//
//			if (value.getCssValueType() == CSSValue.CSS_PRIMITIVE_VALUE) {
//				Color color = (Color) engine.convert(value, Color.class,
//						control.getDisplay());
//				((ICTabRendering) renderer).setUnselectedTabsColor(color);
//				folder.setBackground(color);
//				removeResizeEventListener(folder);
//				return;
//			}
//			if (value.getCssValueType() == CSSValue.CSS_VALUE_LIST) {
//				Gradient grad = (Gradient) engine.convert(value, Gradient.class, control.getDisplay());
//				if (grad == null) {
//					return;
//				}
//				Color[] colors = null;
//				int[] percents = null;
//				if (!grad.getValues().isEmpty()) {
//					colors = CSSSWTColorHelper.getSWTColors(grad,
//							control.getDisplay(), engine);
//					percents = CSSSWTColorHelper.getPercents(grad);
//				}
//				((ICTabRendering) renderer)
//				.setUnselectedTabsColor(colors, percents);
//				folder.setBackground(colors, percents, true);
//				appendResizeEventListener(folder);
//			}
//		}
//
//
//		@Override
//		protected String retrieveCSSProperty(Control control, String property,
//				String pseudo, CSSEngine engine) throws Exception {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		private boolean isUnselectedTabsColorProp(String property) {
//			return UNSELECTED_TABS_COLOR_PROP.equals(property)
//					|| DEPRECATED_UNSELECTED_TABS_COLOR_PROP.equals(property);
//		}
//
//		// TODO: It needs to be refactored when the Bug 33276 gets fixed
//		private void appendResizeEventListener(CTabFolder folder) {
//			if (hasResizeEventListener(folder)) {
//				return;
//			}
//
//			final Listener resizeListener = new Listener() {
//				@Override
//				public void handleEvent(Event event) {
//					CTabFolder folder = (CTabFolder) event.widget;
//					for (Control child : folder.getChildren()) {
//						if (isReskinRequired(child)) {
//							child.reskin(SWT.NONE);
//						}
//					}
//				}
//			};
//
//			folder.addListener(SWT.Resize, resizeListener);
//			folder.setData(RESIZE_LISTENER, resizeListener);
//			folder.addDisposeListener(new DisposeListener() {
//				@Override
//				public void widgetDisposed(DisposeEvent e) {
//					e.widget.removeListener(SWT.Resize, resizeListener);
//				}
//			});
//		}
//
//		private void removeResizeEventListener(CTabFolder folder) {
//			Object obj = folder.getData(RESIZE_LISTENER);
//			if (obj instanceof Listener) {
//				folder.removeListener(SWT.Resize, (Listener) obj);
//				folder.setData(RESIZE_LISTENER, null);
//			}
//		}
//
//		private boolean hasResizeEventListener(CTabFolder folder) {
//			return folder.getData(RESIZE_LISTENER) instanceof Listener;
//		}
//
//		private boolean isReskinRequired(Control control) {
//			if (control instanceof Composite) {
//				Composite composite = (Composite) control;
//				return composite.isVisible() && composite.getChildren().length > 0;
//			}
//			return false;
//		}
//	}