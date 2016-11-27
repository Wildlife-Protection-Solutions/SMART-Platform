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
package org.wcs.smart.cybertracker.export;

import java.util.List;

import org.wcs.smart.cybertracker.export.alert.AlertData;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.screens.Controls;
import org.wcs.smart.cybertracker.model.screens.Controls.Control;
import org.wcs.smart.cybertracker.model.screens.Map;
import org.wcs.smart.cybertracker.model.screens.MovingMaps;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.cybertracker.model.screens.Screens;
import org.wcs.smart.cybertracker.util.PdaUtil;
import org.wcs.smart.dataentry.model.DisplayMode;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Factory for creating common screens objects.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ScreensObjectFactory {


	private static final int DEFAULT_NODE_NEXT_ID = 20;
	
	private static final int CONTROL_2_INDEX = 0;
	private static final int CONTROL_RADIO_MAIN_INDEX = 2;
	private static final int CONTROL_NUMBER_MAIN_INDEX = 3;
	private static final int CONTROL_NOTE_MAIN_INDEX = 3;

	private static final int LIST_MODE_MS = 1; //"Multi select"
	private static final int LIST_MODE_MS_ICONS = 3; //"Multi select icons"
	private static final int LIST_MODE_NUMBERS = 5; //"Number keybad"

	private static final int ATTRIBUTE_IMAGE_1 = 12; //"Image1" for Icon

	private CyberTrackerPropertiesProfile ctProperties;
	
	public ScreensObjectFactory(CyberTrackerPropertiesProfile properties) {
		this.ctProperties = properties;
	}
	
	public CyberTrackerPropertiesProfile getCtProperties() {
		return ctProperties;
	}
	
	/**
	<Screens>
    	<Root>
        	<Id>{00000000-0000-0000-0000-000000000001}</Id>
        	<Name>&lt;ROOT&gt;</Name>
        	<Node>
            	<Id>{FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFC}</Id>
            	<Name>Applications</Name>
            	<Node>
                	<Id>{D9343FB1-A4BF-4F0A-8AB3-6AB1B05D6CF8}</Id>
                	<Name>...</Name>
                	<DataClass>TctSequence</DataClass>
                	<Data>
                    	<Name>...</Name>
                    	<TestTime>True</TestTime>
                    	<BigTitle>True</BigTitle>
                    	...
                	</Data>
                	<Node> ... </Node>
                	<Node> ... </Node>
                	...
            	</Node>
        	</Node>
    	</Root>
	</Screens>
	 */
	public Screens createScreens(List<Node> screenNodes, CyberTrackerPropertiesProfile properties, String appName) {
		Screens screens = new Screens();
		Screens.Root root = new Screens.Root();
		screens.setRoot(root);
		root.setId("{00000000-0000-0000-0000-000000000001}"); //$NON-NLS-1$
		root.setName("<ROOT>"); //$NON-NLS-1$
		
		Node appsNode = new Node();
		appsNode.setId("{FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFC}"); //$NON-NLS-1$
		appsNode.setName("Applications"); //$NON-NLS-1$
		root.getNode().add(appsNode);

		Node a1Node = new Node();
		a1Node.setId("{D9343FB1-A4BF-4F0A-8AB3-6AB1B05D6CF8}"); //$NON-NLS-1$
		a1Node.setName("Application"); //$NON-NLS-1$
		a1Node.setDataClass("TctSequence"); //$NON-NLS-1$
		if (properties != null) {
			a1Node.setName(appName);
			Node.Data a1Data = new Node.Data();
			a1Node.setData(a1Data);
			a1Data.setDownloadTargetKey(PdaUtil.getRegistryKey(SmartDB.getCurrentConservationArea()));
			a1Data.setName(appName);
			a1Data.setForceTitleBar(ctBooleanValue(properties.isUseTitleBar()));
			a1Data.setBigTitle(ctBooleanValue(properties.isUseLargeTitles()));
			a1Data.setBigScroller(ctBooleanValue(properties.isLargeScrollBars()));
			a1Data.setBigTab(ctBooleanValue(properties.isUseLargeTabs()));

			a1Data.setKioskMode(ctBooleanValue(properties.isKioskMode()));
			a1Data.setSimpleCamera(ctBooleanValue(properties.isSimpleCamera()));
			a1Data.setDisableEditing(ctBooleanValue(properties.isDisableEditing()));
			a1Data.setUseSD(ctBooleanValue(properties.isUseSdCard()));
			a1Data.setTestTime(ctBooleanValue(properties.isTestTime()));
			a1Data.setResetStateOnSync(ctBooleanValue(properties.isResetOnSync()));
			a1Data.setClearOnNext(ctBooleanValue(properties.isResetOnNext()));
			
			a1Data.setSightingAccuracy(properties.getSightingAccuracy());
			a1Data.setSightingFixCount(properties.getSightingFixCount());
			a1Data.setWaypointAccuracy(properties.getTrackAccuracy());
			a1Data.setWaypointTimer(properties.getWaypointTimer());
			a1Data.setGpsTimeSync(ctBooleanValue(properties.isUseGpsTime()));
			a1Data.setGpsTimeZone(properties.getGpsTimeZone());
			a1Data.setProjection(properties.getProjection());
			a1Data.setUTMZone(properties.getUtmZone());
			a1Data.setGpsSkipTimeout(properties.getSkipButtonTimeout());
			a1Data.setManualOnSkip(ctBooleanValue(properties.isManualGps()));
			a1Data.setManualMapOnSkip(ctBooleanValue(properties.isUseMapOnSkip()));
			a1Data.setManualAllowSkip(ctBooleanValue(properties.isAllowSkipManualGps()));

			a1Data.setMovingMaps(createMovingMaps(properties));
		}
		if (screenNodes != null) {
			a1Node.getNode().addAll(screenNodes);
		}
		appsNode.getNode().add(a1Node);
		
		return screens;
	}
	
	private MovingMaps createMovingMaps(CyberTrackerPropertiesProfile properties) {
		MovingMaps movingMaps = new MovingMaps();
		
		MovingMaps.Items items = new MovingMaps.Items();
		movingMaps.setItems(items);
		
		MovingMaps.Items.Item item = new MovingMaps.Items.Item();
		items.getItem().add(item);

		Map map = new Map();
		item.setMap(map);
		item.setId((new CyberTrackerUtil.CyberTrackerId()).getItemId());

		map.setFileName(properties.getFieldMapFilename());
		map.setLock100(ctBooleanValue(properties.isLock100()));
		
		return movingMaps;
	}

	private String ctBooleanValue(Boolean value) {
		if (value == null) {
			return null;
		}
		return value ? ICyberTrackerConstants.STR_TRUE : ICyberTrackerConstants.STR_FALSE;
	}

	private String getTemplateForMode(DisplayMode mode, boolean isMultiselect) {
		if (mode == null) mode = DisplayMode.DEFAULT_DISPLAY_MODE;
		switch (mode) {
		case TEXT:       return isMultiselect ? "{E4BFBBF6-F16B-4C0E-8729-FFCE68569AF3}" : "{A049074A-8769-4A9C-AFC4-EC1B1A213B2C}"; //$NON-NLS-1$ //$NON-NLS-2$
		case IMAGE:      return isMultiselect ? "{27BA2599-685A-4351-B8A8-942A1BC29C96}" : "{54448E7E-52D6-44C8-85EA-108C129FB01A}"; //$NON-NLS-1$ //$NON-NLS-2$
		case TEXT_IMAGE: return isMultiselect ? "{36143349-6C02-402C-908B-D6934128F360}" : "{D8322C88-66D1-4148-BA28-8F6A3C5342C7}"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		throw new IllegalStateException("Unknown template for the screen with mode: " + mode); //$NON-NLS-1$
	}

	/**
    <Node>
        <Id>???</Id>
        <Name>???</Name>
        <Items>
            <Value>???</Value>
            <Value>???</Value>
            ...
        </Items>
        <DataClass>TctScreen</DataClass>
        <Data>
            <NextId>DEFAULT_NODE_NEXT_ID</NextId>
            <TemplateId>???</TemplateId>
            <Name>???</Name>
            <Controls>
                <Control>...</Control>
                ...
        </Data>
    </Node>
	 */
	private Node createNode(String id, String name, List<String> values, String templateId, Controls.Control mainControl) {
		Node node = new Node();
		node.setId(id);
		node.setName(name);
		if (values != null) {
			node.setItems(new Node.Items());
			node.getItems().getValue().addAll(values);
		}
		node.setDataClass("TctScreen"); //$NON-NLS-1$
		
		Node.Data data = new Node.Data();
		data.setNextId(DEFAULT_NODE_NEXT_ID);
		data.setTemplateId(templateId);
		data.setName(name);
		Controls controls = new Controls();
		controls.getControl().add(createControl2());
		controls.getControl().add(createControl6());
		controls.getControl().add(mainControl);
		controls.getControl().add(createControl11());
		data.setControls(controls);
		node.setData(data);
		
		return node;
	}

	public Node createNodeRadio(String id, String name, List<String> values, String trElements, String trLinks, String resultElement) {
		return createNodeRadio(id, name, values, trElements, trLinks, resultElement, DisplayMode.TEXT);
	}
	
	public Node createNodeRadio(String id, String name, List<String> values, String trElements, String trLinks, String resultElement, DisplayMode mode) {
		Controls.Control control7 = createRadioControl7(trElements, trLinks, resultElement);
		configureControl7(control7, mode, false, false);
		return createNode(id, name, values, getTemplateForMode(mode, false), control7);
	}

	public Node createNodeMultiList(String id, String name, List<String> values, String trElements, String trLinks, DisplayMode mode, boolean withNumbers) {
		Controls.Control control7 = createControl7(trElements, trLinks);
		configureControl7(control7, mode, true, withNumbers);
		return createNode(id, name, values, getTemplateForMode(mode, true), control7);
	}

	/**
    <Node>
        <Id>???</Id>
        <Name>???</Name>
        <Items>
            <Value>???</Value>
        </Items>
        <DataClass>TctScreen</DataClass>
        <Data>
            <NextId>13</NextId>
            <TemplateId>{1C1DAD73-F942-41C0-806C-B5C9AFCA6E3B}</TemplateId>
            <Name>???</Name>
            <Controls>
                <Control>...</Control>
                ...
        </Data>
    </Node>
	 */
	public Node createNodeNumber(String id, String name, String resultElementId) {
		Node node = new Node();
		node.setId(id);
		node.setName(name);
		if (resultElementId != null) {
			node.setItems(new Node.Items());
			node.getItems().getValue().add(resultElementId);
		}
		node.setDataClass("TctScreen"); //$NON-NLS-1$

		Node.Data data = new Node.Data();
		data.setNextId(13);
		data.setTemplateId("{1C1DAD73-F942-41C0-806C-B5C9AFCA6E3B}"); //$NON-NLS-1$
		data.setName(name);
		Controls controls = new Controls();
		controls.getControl().add(createControl2());
		controls.getControl().add(createControl6());
		controls.getControl().add(createControl11());
		controls.getControl().add(createNumberControl12(resultElementId));
		data.setControls(controls);
		node.setData(data);
		
		return node;
	}

	/**
    <Node>
        <Id>???</Id>
        <Name>???</Name>
        <DataClass>TctScreen</DataClass>
        <Data>
            <NextId>14</NextId>
            <TemplateId>{2F0173A3-6FF9-4B78-8ADF-80C16B1587B9}</TemplateId>
            <Name>???</Name>
            <Controls>
                <Control>...</Control>
                ...
        </Data>
    </Node>
	 */
	public Node createNodeNote(String id, String name, String resultElementId) {
		Node node = new Node();
		node.setId(id);
		node.setName(name);
		node.setDataClass("TctScreen"); //$NON-NLS-1$

		Node.Data data = new Node.Data();
		data.setNextId(14);
		data.setTemplateId("{2F0173A3-6FF9-4B78-8ADF-80C16B1587B9}"); //$NON-NLS-1$
		data.setName(name);
		Controls controls = new Controls();
		controls.getControl().add(createControl2());
		controls.getControl().add(createControl6());
		controls.getControl().add(createControl11());
		controls.getControl().add(createNoteControl13(resultElementId));
		data.setControls(controls);
		node.setData(data);
		
		return node;
	}

	/**
    <Node>
        <Id>???</Id>
        <Name>???</Name>
        <DataClass>TctScreen</DataClass>
        <Data>
            <NextId>15</NextId>
            <TemplateId>{8A40BEBA-3329-41EE-B03E-E41D85EB7DCE}</TemplateId>
            <Name>???</Name>
            <Controls>
                <Control>...</Control>
                ...
        </Data>
    </Node>
	 */
	public Node createNodeMsgText(String id, String name, String text) {
		Node node = new Node();
		node.setId(id);
		node.setName(name);
		node.setDataClass("TctScreen"); //$NON-NLS-1$

		Node.Data data = new Node.Data();
		data.setNextId(15);
		data.setTemplateId("{8A40BEBA-3329-41EE-B03E-E41D85EB7DCE}"); //$NON-NLS-1$
		data.setName(name);
		Controls controls = new Controls();
		controls.getControl().add(createControl2());
		controls.getControl().add(createControl6());
		controls.getControl().add(createControl11());
		controls.getControl().add(createMsgTextControl12(text));
		data.setControls(controls);
		node.setData(data);
		
		return node;
	}

	/**
    <Node>
        <Id>???</Id>
        <Name>???</Name>
        <DataClass>TctScreen</DataClass>
        <Data>
            <NextId>14</NextId>
            <TemplateId>{E4BCC108-0A78-4290-AD57-E6E94190A7A9}</TemplateId>
            <Name>???</Name>
            <Controls>
                <Control>...</Control>
                ...
        </Data>
    </Node>
	 */
	public Node createNodePassword(String id, String name) {
		Node node = new Node();
		node.setId(id);
		node.setName(name);
		node.setDataClass("TctScreen"); //$NON-NLS-1$

		Node.Data data = new Node.Data();
		data.setNextId(14);
		data.setTemplateId("{E4BCC108-0A78-4290-AD57-E6E94190A7A9}"); //$NON-NLS-1$
		data.setName(name);
		Controls controls = new Controls();
		controls.getControl().add(createControl2());
		controls.getControl().add(createControl6());
		controls.getControl().add(createControl11());
		controls.getControl().add(createNumpadPasswordExitControl12(ctProperties.getExitPin()));
		data.setControls(controls);
		node.setData(data);
		
		return node;
	}

	/**
    <Node>
        <Id>???</Id>
        <Name>???</Name>
		<Items>
			<Value>???</Value> <!-- this is resultId -->
		</Items>        
        <DataClass>TctScreen</DataClass>
        <Data>
            <NextId>14</NextId>
            <TemplateId>{5DC23CAB-C51C-47A7-B8B6-C25DE35DDF4B}</TemplateId>
            <Name>???</Name>
            <Controls>
                <Control>...</Control>
                ...
        </Data>
    </Node>
	 */
	public Node createPhoto(String id, String name, String resultId, boolean required) {
		Node node = new Node();
		node.setId(id);
		node.setName(name);
		node.setDataClass("TctScreen"); //$NON-NLS-1$
		
		Node.Items items = new Node.Items();
		items.getValue().add(resultId);
		node.setItems(items);

		Node.Data data = new Node.Data();
		data.setNextId(14);
		data.setTemplateId("{5DC23CAB-C51C-47A7-B8B6-C25DE35DDF4B}"); //$NON-NLS-1$
		data.setName(name);
		Controls controls = new Controls();
		controls.getControl().add(createControl2());
		controls.getControl().add(createControl11());
		controls.getControl().add(createPhotoControl12(resultId, required));
		data.setControls(controls);
		node.setData(data);
		
		return node;
	}

	/**
    <Node>
        <Id>???</Id>
        <Name>???</Name>
		<Items>
			<Value>???</Value> <!-- this is resultId -->
		</Items>        
        <DataClass>TctScreen</DataClass>
        <Data>
            <NextId>15</NextId>
            <TemplateId>{BBDDFC72-79FC-4664-B398-CB57E614D71B}</TemplateId>
            <Name>???</Name>
            <Controls>
                <Control>...</Control>
                ...
        </Data>
    </Node>
	 */
	public Node createDate(String id, String name, String resultId, boolean required) {
		Node node = new Node();
		node.setId(id);
		node.setName(name);
		node.setDataClass("TctScreen"); //$NON-NLS-1$
		
		Node.Items items = new Node.Items();
		items.getValue().add(resultId);
		node.setItems(items);

		Node.Data data = new Node.Data();
		data.setNextId(15);
		data.setTemplateId("{BBDDFC72-79FC-4664-B398-CB57E614D71B}"); //$NON-NLS-1$
		data.setName(name);
		Controls controls = new Controls();
		controls.getControl().add(createControl2());
		controls.getControl().add(createControl11());
		controls.getControl().add(createDateControl13(resultId, required));
		data.setControls(controls);
		node.setData(data);
		
		return node;
	}
	
	/**
	<Control>
        <Type>{DD2F292A-4C6B-44D7-92C5-9C3922ED0350}</Type>
        <LockProperties>Show Back;Show save 1;Show save 2;Show Options;Show GPS;Show Next;Take GPS Reading;Save 1 target;Save 2 target;Skip screen;Next Screen</LockProperties>
        <Id>2</Id>
        <Align>2</Align>
        <Left>0</Left>
        <Top>296</Top>
        <Width>240</Width>
        <Height>24</Height>
        <ButtonWidth>25</ButtonWidth>
        <ButtonBorderWidth>0</ButtonBorderWidth>
    </Control>
	 */
	public Controls.Control createControl2() {
		Controls.Control control = new Controls.Control();
		control.setType("{DD2F292A-4C6B-44D7-92C5-9C3922ED0350}"); //$NON-NLS-1$
		control.setLockProperties("Show Back;Show save 1;Show save 2;Show Options;Show GPS;Show Next;Take GPS Reading;Save 1 target;Save 2 target;Skip screen;Next Screen"); //$NON-NLS-1$
		control.setId(2);
		control.setAlign(2);
		control.setLeft(0);
		control.setTop(296);
		control.setWidth(240);
		control.setHeight(24);
		control.setButtonWidth(25);
		control.setButtonBorderWidth(0);
		control.setShowEdit("False"); //$NON-NLS-1$
		control.setShowGPS("False"); //$NON-NLS-1$
		return control;
	}

	/**
    <Control>
        <Type>{1BAF7223-9DF3-44E2-8B0E-969D951492AC}</Type>
        <Id>6</Id>
        <BorderStyle>0</BorderStyle>
        <Align>1</Align>
        <Left>0</Left>
        <Top>32</Top>
        <Width>240</Width>
        <Height>2</Height>
        <Translate__Font>Arial,9,</Translate__Font>
    </Control>
	 */
	public Controls.Control createControl6() {
		Controls.Control control = new Controls.Control();
		control.setType("{1BAF7223-9DF3-44E2-8B0E-969D951492AC}"); //$NON-NLS-1$
		control.setId(6);
		control.setBorderStyle(0);
		control.setAlign(1);
		control.setLeft(0);
		control.setTop(32);
		control.setWidth(240);
		control.setHeight(2);
		control.setTranslateFont("Arial,9,"); //$NON-NLS-1$
		return control;
	}

	/**
    <Control>
        <Type>{289461C2-B3EE-4075-9538-451580BD4B38}</Type>
        <Version>1</Version>
        <LockProperties>Elements;Result Element</LockProperties>
        <Id>7</Id>
        <Align>5</Align>
        <Left>0</Left>
        <Top>34</Top>
        <Width>240</Width>
        <Height>262</Height>
        <Translate__Font>MS Sans Serif,10,B</Translate__Font>
        <ItemHeight>25</ItemHeight>
        <Attribute>0</Attribute>
        <AutoRadioNext>...</AutoRadioNext>
        <Translate__Elements>???</Translate__Elements>
        <Translate__Links>???</Translate__Links>
        <Translate__RadioElement>???</Translate__RadioElement>
    </Control>
	 */
	private Controls.Control createControl7(String trElements, String trLinks) {
		Controls.Control control = new Controls.Control();
		control.setType("{289461C2-B3EE-4075-9538-451580BD4B38}"); //$NON-NLS-1$
		control.setVersion(1);
		control.setLockProperties("Elements;Result Element"); //$NON-NLS-1$
		control.setId(7);
		control.setAlign(5);
		control.setLeft(0);
		control.setTop(34);
		control.setWidth(240);
		control.setHeight(262);
		control.setTranslateFont("MS Sans Serif,10,B"); //$NON-NLS-1$
		control.setItemHeight(25);
		control.setTranslateElements(trElements);
		control.setTranslateLinks(trLinks);
		return control;
	}
	
	/**
    <Control>
    	...
        <AutoRadioNext>...</AutoRadioNext>
        <Translate__RadioElement>???</Translate__RadioElement>
    </Control>
	 */
	public Controls.Control createRadioControl7(String trElements, String trLinks, String radioElement) {
		Controls.Control control = createControl7(trElements, trLinks);
		control.setAutoRadioNext(ctBooleanValue(ctProperties.isAutoNext()));
		control.setTranslateRadioElement(radioElement);
		return control;
	}

	/**
    <Control>
    	...
        <ListMode>???</ListMode>
        <MinChecks>???</MinChecks>
    </Control>
	 */
	public void configureControl7(Controls.Control control7, DisplayMode mode, boolean isMultiSelect, boolean isNumeric) {
		if (mode == null) mode = DisplayMode.DEFAULT_DISPLAY_MODE;
		switch (mode) {
		case TEXT: 
			control7.setAttribute(0);
			if (isMultiSelect) {
				int listMode = isNumeric ? LIST_MODE_NUMBERS : LIST_MODE_MS;
				control7.setListMode(listMode);
				control7.setMinChecks(1);
			}
			break;
		case IMAGE:
			control7.setAttribute(ATTRIBUTE_IMAGE_1); 
			control7.setAttributeDetails(ATTRIBUTE_IMAGE_1);
			control7.setColumnCount(4);
			control7.setItemHeight(34);
			control7.setShowCaption(ctBooleanValue(false));
			if (isMultiSelect) {
				int listMode = isNumeric ? LIST_MODE_NUMBERS : LIST_MODE_MS_ICONS;
				control7.setListMode(listMode);
				control7.setMinChecks(1);
			}
			break;
		case TEXT_IMAGE:
			control7.setAttribute(ATTRIBUTE_IMAGE_1); 
			control7.setAttributeDetails(ATTRIBUTE_IMAGE_1); 
			if (isMultiSelect) {
				int listMode = isNumeric ? LIST_MODE_NUMBERS : LIST_MODE_MS;
				control7.setListMode(listMode);
				control7.setMinChecks(1);
			}
			break;
		}
	}
	
	/**
    <Control>
        <Type>{1BAF7223-9DF3-44E2-8B0E-969D951492AC}</Type>
        <Id>11</Id>
        <Color>00000000</Color>
        <Align>1</Align>
        <Left>0</Left>
        <Top>0</Top>
        <Width>240</Width>
        <Height>32</Height>
        <Translate__Font>MS Sans Serif,12,B</Translate__Font>
        <TextColor>FFFFFF00</TextColor>
        <MinHeight>16</MinHeight>
        <UseScreenName>True</UseScreenName>
    </Control>
	 */
	public Controls.Control createControl11() {
		Controls.Control control = new Controls.Control();
		control.setType("{1BAF7223-9DF3-44E2-8B0E-969D951492AC}"); //$NON-NLS-1$
		control.setId(11);
		control.setColor("00000000"); //$NON-NLS-1$
		control.setAlign(1);
		control.setLeft(0);
		control.setTop(0);
		control.setWidth(240);
		control.setHeight(32);
		control.setTranslateFont("MS Sans Serif,12,B"); //$NON-NLS-1$
		control.setTextColor("FFFFFF00"); //$NON-NLS-1$
		control.setMinHeight(16);
		control.setUseScreenName("True"); //$NON-NLS-1$
		return control;
	}

	/**
    <Control>
        <Type>{5D9A98BA-0F3A-4B6C-9439-7D72D6B06F9E}</Type>
        <LockProperties>Decimals;Digits;Result Element</LockProperties>
        <Id>12</Id>
        <Align>5</Align>
        <Left>0</Left>
        <Top>34</Top>
        <Width>240</Width>
        <Height>262</Height>
        <Translate__Font>MS Sans Serif,18,B</Translate__Font>
        <Caption>0.</Caption>
        <ButtonWidth>50</ButtonWidth>
        <ButtonHeight>40</ButtonHeight>
        <ButtonBorderStyle>3</ButtonBorderStyle>
        <Translate__ButtonFont>MS Sans Serif,18,B</Translate__ButtonFont>
        <DisplayHeight>50</DisplayHeight>
        <MinValue>0</MinValue>
        <MaxValue>99999999</MaxValue>
        <Decimals>8</Decimals>
        <Translate__Element>???</Translate__Element>
    </Control>
	 */
	public Controls.Control createNumberControl12(String element) {
		Controls.Control control = new Controls.Control();
		control.setType("{5D9A98BA-0F3A-4B6C-9439-7D72D6B06F9E}"); //$NON-NLS-1$
		control.setLockProperties("Decimals;Digits;Result Element"); //$NON-NLS-1$
		control.setId(12);
		control.setAlign(5);
		control.setLeft(0);
		control.setTop(34);
		control.setWidth(240);
		control.setHeight(262);
		control.setTranslateFont("MS Sans Serif,18,B"); //$NON-NLS-1$
		control.setCaption("0."); //$NON-NLS-1$
		control.setButtonWidth(50);
		control.setButtonHeight(40);
		control.setButtonBorderStyle(3);
		control.setTranslateButtonFont("MS Sans Serif,18,B"); //$NON-NLS-1$
		control.setDisplayHeight(50);
		control.setMinValue(0);
		control.setMaxValue(99999999);
		control.setDecimals(8);
		control.setTranslateElement(element);
		return control;
	}

	/**
    <Control>
	    <Type>{5D9A98BA-0F3A-4B6C-9439-7D72D6B06F9E}</Type>
	    <LockProperties>Decimals;Digits;Font;Formula mode;Minimum value;Maximum value;Require non-zero;Require set value</LockProperties>
	    <Id>12</Id>
	    <Align>5</Align>
	    <Left>0</Left>
	    <Top>34</Top>
	    <Width>240</Width>
	    <Height>262</Height>
	    <Translate__Font>Arial,18,B</Translate__Font>
	    <Caption>Enter pin</Caption>
	    <ButtonSpacingX>0</ButtonSpacingX>
	    <ButtonSpacingY>0</ButtonSpacingY>
	    <Translate__ButtonFont>MS Sans Serif,18,B</Translate__ButtonFont>
	    <DisplayHeight>50</DisplayHeight>
	    <Decimals>2</Decimals>
	    <MinValue>0</MinValue>
	    <Password>???</Password>
	    <PasswordAutoNext>2</PasswordAutoNext>
    </Control>
	 */

	public Controls.Control createNumpadPasswordExitControl12(Integer password) {
		Controls.Control control = new Controls.Control();
		control.setType("{5D9A98BA-0F3A-4B6C-9439-7D72D6B06F9E}"); //$NON-NLS-1$
		control.setLockProperties("Decimals;Digits;Font;Formula mode;Minimum value;Maximum value;Require non-zero;Require set value"); //$NON-NLS-1$
		control.setId(12);
		control.setAlign(5);
		control.setLeft(0);
		control.setTop(34);
		control.setWidth(240);
		control.setHeight(262);
		control.setTranslateFont("MS Sans Serif,18,B"); //$NON-NLS-1$
		control.setCaption("Enter pin"); //$NON-NLS-1$
		control.setButtonSpacingX(0);
		control.setButtonSpacingY(0);
//		control.setButtonWidth(50);
//		control.setButtonHeight(40);
//		control.setButtonBorderStyle(3);
		control.setTranslateButtonFont("MS Sans Serif,18,B"); //$NON-NLS-1$
		control.setDisplayHeight(50);
		control.setDecimals(2);
		control.setMinValue(0);
		control.setPassword(password != null ? String.valueOf(password) : null);
		control.setPasswordAutoNext(2);
		return control;
	}
	
	/**
    <Control>
        <Type>{49728018-D9F6-49B8-86E4-3EA49A29F4BC}</Type>
        <Id>13</Id>
        <BorderWidth>1</BorderWidth>
        <Align>5</Align>
        <Left>0</Left>
        <Top>34</Top>
        <Width>240</Width>
        <Height>262</Height>
        <Translate__Element>???</Translate__Element>
    </Control>
	 */
	public Controls.Control createNoteControl13(String element) {
		Controls.Control control = new Controls.Control();
		control.setType("{49728018-D9F6-49B8-86E4-3EA49A29F4BC}"); //$NON-NLS-1$
		control.setId(13);
		control.setBorderWidth(1);
		control.setAlign(5);
		control.setLeft(0);
		control.setTop(34);
		control.setWidth(240);
		control.setHeight(262);
		control.setTranslateElement(element);
		return control;
	}
	
	/**
	<Control>
	    <Type>{A25E984A-294B-4ADF-B7F7-900C4D991DE5}</Type>
	    <LockProperties>Image</LockProperties>
	    <Id>12</Id>
	    <BorderWidth>2</BorderWidth>
	    <Align>5</Align>
	    <Left>0</Left>
	    <Top>34</Top>
	    <Width>240</Width>
	    <Height>262</Height>
	    <Controls>
	        <Control>
	            <Id>13</Id>
	            ...
	        </Control>
	    </Controls>
	    <Stretch>True</Stretch>
	    <Proportional>False</Proportional>
	</Control>
	 */
	public Controls.Control createMsgTextControl12(String text) {
		Controls.Control control = new Controls.Control();
		control.setType("{A25E984A-294B-4ADF-B7F7-900C4D991DE5}"); //$NON-NLS-1$
		control.setLockProperties("Image"); //$NON-NLS-1$
		control.setId(12);
		control.setBorderWidth(2);
		control.setAlign(5);
		control.setLeft(0);
		control.setTop(34);
		control.setWidth(240);
		control.setHeight(262);
		Controls innerControls = new Controls();
		innerControls.getControl().add(createMsgTextControl13(text));
		control.setControls(innerControls);
		control.setStretch("True"); //$NON-NLS-1$
		control.setProportional("False"); //$NON-NLS-1$
		return control;
	}

	/**
	<Controls>
	    <Control>
	        <Type>{F4D19E36-BC93-4D89-B82B-1A8900710077}</Type>
	        <Id>13</Id>
	        <BorderStyle>0</BorderStyle>
	        <Transparent>True</Transparent>
	        <Align>5</Align>
	        <Left>2</Left>
	        <Top>2</Top>
	        <Width>236</Width>
	        <Height>258</Height>
	        <Translate__Font>MS Sans Serif,10,B</Translate__Font>
	        <Caption>???</Caption>
	    </Control>
	</Controls>
	 */
	public Controls.Control createMsgTextControl13(String text) {
		Controls.Control control = new Controls.Control();
		control.setType("{F4D19E36-BC93-4D89-B82B-1A8900710077}"); //$NON-NLS-1$
		control.setId(13);
		control.setBorderStyle(0);
		control.setTransparent("True"); //$NON-NLS-1$
		control.setAlign(5);
		control.setLeft(2);
		control.setTop(2);
		control.setWidth(236);
		control.setHeight(258);
		control.setTranslateFont("MS Sans Serif,10,B"); //$NON-NLS-1$
		control.setCaption(text);
		return control;
	}

	/**
	<Controls>
	    <Control>
            <Type>{F4D19E36-BC93-4D89-B82B-1A8900710077}</Type>
            <Id>17</Id>
            <Align>2</Align>
            <Left>0</Left>
            <Top>165</Top>
            <Width>240</Width>
            <Height>40</Height>
	        <Transparent>True</Transparent>
	        <Caption>???</Caption>
	    </Control>
	</Controls>
	 */
	public Controls.Control createBottomMemoControl17(String text) {
		Controls.Control control = new Controls.Control();
		control.setType("{F4D19E36-BC93-4D89-B82B-1A8900710077}"); //$NON-NLS-1$
		control.setId(17);
		control.setAlign(2);
		control.setLeft(0);
		control.setTop(165);
		control.setWidth(240);
		control.setHeight(40);
		control.setTransparent("True"); //$NON-NLS-1$
		control.setCaption(text);
		return control;
	}
	
	/**
	<Control>
	    <Type>{F4D19E36-BC93-4D89-B82B-1A8900710077}</Type>
	    <Id>13</Id>
	    <Align>2</Align>
	    <Top>210</Top>
	    <Width>240</Width>
	    <Height>100</Height>
        <Translate__Font>MS Sans Serif,10,B</Translate__Font>
	    <Caption>???</Caption>
	</Control>
	 */
	public Controls.Control createBottomMemoControl13(String text) {
		Controls.Control control = new Controls.Control();
		control.setType("{F4D19E36-BC93-4D89-B82B-1A8900710077}"); //$NON-NLS-1$
		control.setId(13);
		control.setAlign(2);
		control.setTop(210);
		control.setWidth(240);
		control.setHeight(100);
		control.setTranslateFont("MS Sans Serif,10,B"); //$NON-NLS-1$
		control.setCaption(text);
		return control;
	}

	/**
	<Control>
	    <Type>{02F601F3-28BD-4D1F-A9DD-CAA71ABC25FC}</Type>
	    <Id>12</Id>
	    <Align>5</Align>
	    <Left>0</Left>
	    <Top>32</Top>
	    <Width>240</Width>
	    <Height>264</Height>
	    <Translate__Font>Arial,12,B</Translate__Font>
	    <Translate__Element>???</Translate__Element>   <!-- this is resultId -->
	 </Control>
	 */
	private Control createPhotoControl12(String resultId, boolean required) {
		Controls.Control control = new Controls.Control();
		control.setType("{02F601F3-28BD-4D1F-A9DD-CAA71ABC25FC}"); //$NON-NLS-1$
		control.setId(12);
		control.setAlign(5);
		control.setLeft(0);
		control.setTop(32);
		control.setWidth(240);
		control.setHeight(264);
		control.setTranslateFont("Arial,12,B"); //$NON-NLS-1$
		control.setTranslateElement(resultId);
		control.setRequired(required ? "True" : "False"); //$NON-NLS-1$ //$NON-NLS-2$
		return control;
	}

	/**
    <Control>
        <Type>{5DFF7D37-8BA4-4B0A-A792-16BE539CA6BA}</Type>
	    <Id>13</Id>
	    <Align>5</Align>
	    <Left>0</Left>
	    <Top>32</Top>
	    <Width>240</Width>
	    <Height>264</Height>
	    <Translate__Element>???</Translate__Element>   <!-- this is resultId -->
	<Control>
	*/
	private Control createDateControl13(String resultId, boolean required) {
		Controls.Control control = new Controls.Control();
		control.setType("{5DFF7D37-8BA4-4B0A-A792-16BE539CA6BA}"); //$NON-NLS-1$
		control.setId(13);
		control.setAlign(5);
		control.setLeft(0);
		control.setTop(32);
		control.setWidth(240);
		control.setHeight(264);
		control.setTranslateElement(resultId);
		//NOTE: required is not supported in 3.375
		control.setRequired(required ? "True" : "False"); //$NON-NLS-1$ //$NON-NLS-2$
		return control;
	}
	
	/**
	<Control>
	     <Type>{C26ACA43-8C7C-497C-9586-382E5BD27115}</Type>
	     <Id>12</Id>
	     <Align>0</Align>
	     <Left>4</Left>
	     <Top>4</Top>
	     <Width>40</Width>
	     <Height>20</Height>
	     <Formula>...</Formula>
	     <Hidden>True</Hidden>
	 </Control>
	 */
	public Controls.Control createFormulaControl12(String formula) {
		Controls.Control control = new Controls.Control();
		control.setType("{C26ACA43-8C7C-497C-9586-382E5BD27115}"); //$NON-NLS-1$
		control.setId(12);
		control.setAlign(0);
		control.setLeft(4);
		control.setTop(4);
		control.setWidth(40);
		control.setHeight(20);
		control.setFormula(formula);
		control.setHidden("True"); //$NON-NLS-1$
		return control;
	}

	/**
	<Control>
		...
	     <LockProperties>Elements A;Elements B;Elements C;Font;Formula;Result Element;Link 0;Link 1</LockProperties>
	     <Translate__TargetScreenId0>...</Translate__TargetScreenId0>
	     <Translate__TargetScreenId1>...</Translate__TargetScreenId1>
	 </Control>
	 */
	public Controls.Control createNavFormulaControl12(String formula, String id0, String id1) {
		Controls.Control control = createFormulaControl12(formula);
		control.setLockProperties("Elements A;Elements B;Elements C;Font;Formula;Result Element;Link 0;Link 1"); //$NON-NLS-1$
		control.setTranslateTargetScreenId0(id0);
		control.setTranslateTargetScreenId1(id1);
		return control;
	}

	/**
	<Control>
    	<Type>{440B2C86-D385-4EFC-8ACE-5D3C0A4A016B}</Type>
    	<Id>13</Id>
    	<SightingAccuracy>...</SightingAccuracy> <!-- will be populated from properties -->
    	<SightingFixCount>...</SightingFixCount>  <!-- will be populated from properties -->
    	<WaypointAccuracy>...</WaypointAccuracy> <!-- will be populated from properties -->
    	<WaypointTimer>...</WaypointTimer> <!-- will be populated from properties -->
    	<UseRangeFinderForAltitude>False</UseRangeFinderForAltitude> <!-- will be added as default by CyberTracker -->
	</Control>
	 */
	public Controls.Control createConfigureGPSControl13(CyberTrackerPropertiesProfile props) {
		Controls.Control control = new Controls.Control();
		control.setType("{440B2C86-D385-4EFC-8ACE-5D3C0A4A016B}"); //$NON-NLS-1$
		control.setId(13);
		control.setSightingAccuracy(props.getSightingAccuracy());
		control.setSightingFixCount(props.getSightingFixCount());
		control.setWaypointAccuracy(props.getTrackAccuracy());
		control.setWaypointTimer(props.getWaypointTimer());
		return control;
	}

	/**
	 * see "Actions" -> "Add Attribute" in CyberTracker
    <Control>
    	<Type>{B76D0BF3-F4AC-44FF-A1E6-02707127C949}</Type>
    	<Id>14</Id>
    	<Translate__ElementId>...</Translate__ElementId>
    	<Unique>...</Unique>
    	<Value>...</Value>
    </Control>
	 */
	public Controls.Control createAttrubuteControl14(String resultElement, boolean unique, String value) {
		Controls.Control control = new Controls.Control();
		control.setType("{B76D0BF3-F4AC-44FF-A1E6-02707127C949}"); //$NON-NLS-1$
		control.setId(14);
		control.setTranslateElementId(resultElement);
		control.setUnique(unique ? "True" : "False"); //$NON-NLS-1$ //$NON-NLS-2$
		control.setValue(value);
		return control;
	}

	/**
    <Control>
    	<Type>{FD691F38-4527-4A7F-9DA3-E58418F9FE24}</Type>
    	<Id>15</Id>
    	<Translate__DateId>{0D099EF8-F8E7-4892-807E-7C162F6A9273}</Translate__DateId>
    	<Translate__TimeId>{7BDEC9BB-D5CE-424A-9C7C-37677CD36A48}</Translate__TimeId>
	</Control>
	 */
	public Controls.Control createSnapDateTimeControl15(String dateResultElement, String timeResultElement) {
		Controls.Control control = new Controls.Control();
		control.setType("{FD691F38-4527-4A7F-9DA3-E58418F9FE24}"); //$NON-NLS-1$
		control.setId(15);
		control.setTranslateDateId(dateResultElement);
		control.setTranslateTimeId(timeResultElement);
		return control;
	}

	/**
	 * Creates a snap to last gps position control
	 * <Control>
	 * <Type>{582E62E2-C95B-4B87-94D3-EE8603C8056A}</Type>
	 * <Id>20</Id>
	 * <Required>True</Required>
	 * </Control>
	 * @return
	 */
	public Controls.Control createSnapLastGpsPosition(){
		Controls.Control control = new Controls.Control();
		control.setType("{582E62E2-C95B-4B87-94D3-EE8603C8056A}"); //$NON-NLS-1$
		control.setId(20);
		control.setRequired("True"); //$NON-NLS-1$
		return control;
	}

	/**
	 * Creates a snap to last gps position control
	 * <Control>
	 *     <Type>{096D1ED0-442D-41A3-BB4E-19113EBD7EF9}</Type>
	 *     <Id>20</Id>
	 *     <Required>True</Required>
	 *     <AutoConnect>True</AutoConnect>
	 * </Control>
	 * @return
	 */
	public Controls.Control createSnapGpsPosition(){
		Controls.Control control = new Controls.Control();
		control.setType("{096D1ED0-442D-41A3-BB4E-19113EBD7EF9}"); //$NON-NLS-1$
		control.setId(20);
		control.setRequired("True"); //$NON-NLS-1$
		control.setAutoConnect("True"); //$NON-NLS-1$
		return control;
	}
	
	/**
    <Control>
	    <Type>{DBDA4351-F047-4AA9-8B87-684CAF7EEFA9}</Type>
	    <Id>16</Id>
	    <Align>2</Align>
	    <Height>90</Height>
	    <AutoConnect>True</AutoConnect>
	</Control>
	 */
	public Controls.Control createGPSControl16() {
		Controls.Control control = new Controls.Control();
		control.setType("{DBDA4351-F047-4AA9-8B87-684CAF7EEFA9}"); //$NON-NLS-1$
		control.setId(16);
		control.setAlign(2);
		control.setHeight(90);
		control.setAutoConnect("True"); //$NON-NLS-1$
		return control;
	}
	
	/**
	<Control>
    	<Type>{06F58F58-A9BC-4C23-A10A-4A35CC465D06}</Type>
        <Id>20</Id>
        <Enabled>True</Enabled>
	</Control>
	 */
	public Controls.Control createFlushOnSaveControl() {
		Controls.Control control = new Controls.Control();
		control.setType("{06F58F58-A9BC-4C23-A10A-4A35CC465D06}"); //$NON-NLS-1$
		control.setId(20);
		control.setEnabled("True"); //$NON-NLS-1$
		return control;
	}
	
	/**
    <Control>
	    <Type>{0BC657FA-C6C4-4348-9905-20202341662C}</Type>
	    <Id>19</Id>
	    <BorderStyle>0</BorderStyle> <!-- no border -->
	    <Align>1</Align>
	    <Left>0</Left>
	    <Top>34</Top>
	    <Width>240</Width>
	    <Height>25</Height>
	    <Alignment>1</Alignment> <!-- alignment right -->
	    <Style>0</Style> <!-- means "battery" -->
	</Control>
	 */
	public Controls.Control createSystemStateControl19() {
		Controls.Control control = new Controls.Control();
		control.setType("{0BC657FA-C6C4-4348-9905-20202341662C}"); //$NON-NLS-1$
		control.setId(19);
		control.setBorderStyle(0);
		control.setAlign(1);
		control.setLeft(0);
		control.setTop(34);
		control.setWidth(240);
		control.setHeight(25);
		control.setAlignment(1);
		control.setStyle(0);
		return control;
	}

	/**
    <Control>
    	<Type>{935A0794-78BF-4689-B6F5-F3EC3070BD20}</Type>
        <Id>null</Id> <!-- will be assigned by addControlToNode -->
        <Active>???</Active>
        <Url>???</Url>
        <UserName>???</UserName>
        <Password>???</Password>
        <Type>???</Type>
        <Level>???</Level>
        <Translate__Elements>AEDF0166046BED44858F00236E1A9114</Translate__Elements>
    </Control>
	 */
	public Controls.Control createAlertControl(AlertData data, String patrolId, String trElements) {
		Controls.Control control = new Controls.Control();
		control.setType("{935A0794-78BF-4689-B6F5-F3EC3070BD20}"); //$NON-NLS-1$
		control.setId(null);
		if (data.getActive() != null) {
			control.setActive(ctBooleanValue(data.getActive()));
		}
		control.setUrl(data.getUrl());
		control.setUserName(data.getUsername());
		control.setPassword(data.getPassword());
		control.setCaId(AlertData.toCtString(data.getCaId()));
		control.setAlertType(AlertData.toCtString(data.getType()));
		control.setLevel(data.getLevel());
		control.setPingFrequency(data.getPingFrequency());
		control.setTranslatePatrolElementId(patrolId);
		control.setPingOnly(ctBooleanValue(data.getPingOnly()));
		control.setTranslateElements(trElements);
		control.setProtocol(CyberTrackerProperties.Protocol.GEOJSON.ctValue);
		return control;
	}	
	
	//Util methods
	public static Controls.Control getNavigationControl(Node node) {
		return node.getData().getControls().getControl().get(CONTROL_2_INDEX);
	}

	public static Controls.Control getHeaderControl(Node node) {
		for (Controls.Control control : node.getData().getControls().getControl()) {
			if (control.getId() == 11)
				return control;
		}
		return null;
	}
	
	public static Controls.Control getRadioMainControl(Node node) {
		return node.getData().getControls().getControl().get(CONTROL_RADIO_MAIN_INDEX);
	}

	public static Controls.Control getNumberMainControl(Node node) {
		return node.getData().getControls().getControl().get(CONTROL_NUMBER_MAIN_INDEX);
	}

	public static Controls.Control getNoteMainControl(Node node) {
		return node.getData().getControls().getControl().get(CONTROL_NOTE_MAIN_INDEX);
	}
	
	public static void addControlToNode(Node node, Controls.Control control) {
		//do some indexes validation and assign new indexes if they are invalid
		Integer nodeIndex = node.getData().getNextId();
		if (nodeIndex == null || nodeIndex < DEFAULT_NODE_NEXT_ID) {
			node.getData().setNextId(DEFAULT_NODE_NEXT_ID);
			nodeIndex = DEFAULT_NODE_NEXT_ID;
		}
		Integer controlId = 0;
		for (Controls.Control c : node.getData().getControls().getControl()) {
			if (c.getId() != null) {
				controlId = Math.max(controlId, c.getId());
			}
		}
		controlId++; //id for new control
		
		if (control.getId() == null || control.getId() < controlId) {
			control.setId(controlId);
		} else {
			controlId = control.getId();
		}
		
		if (nodeIndex <= controlId) {
			node.getData().setNextId(controlId + 1);
		}
		
		node.getData().getControls().getControl().add(control);
	}

}
