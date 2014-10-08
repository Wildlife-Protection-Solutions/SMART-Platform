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

import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.screens.Controls;
import org.wcs.smart.cybertracker.model.screens.Controls.Control;
import org.wcs.smart.cybertracker.model.screens.Map;
import org.wcs.smart.cybertracker.model.screens.MovingMaps;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.cybertracker.model.screens.Screens;
import org.wcs.smart.cybertracker.util.PdaUtil;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Factory for creating common screens objects.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ScreensObjectFactory {
	
	private static final int CONTROL_2_INDEX = 0;
	private static final int CONTROL_RADIO_MAIN_INDEX = 2;
	private static final int CONTROL_NUMBER_MAIN_INDEX = 3;
	private static final int CONTROL_NOTE_MAIN_INDEX = 3;

	private static final int LIST_MODE_DEFAULT = 1;
	private static final int LIST_MODE_NUMBERS = 5;

	private CyberTrackerProperties ctProperties;
	
	public ScreensObjectFactory(CyberTrackerProperties properties) {
		this.ctProperties = properties;
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
	public Screens createScreens(List<Node> screenNodes, CyberTrackerProperties properties, String appName) {
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
	
	private MovingMaps createMovingMaps(CyberTrackerProperties properties) {
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

	private String ctBooleanValue(boolean value) {
		return value ? ICyberTrackerConstants.STR_TRUE : ICyberTrackerConstants.STR_FALSE;
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
            <NextId>16</NextId>
            <TemplateId>{A049074A-8769-4A9C-AFC4-EC1B1A213B2C}</TemplateId>
            <Name>???</Name>
            <Controls>
                <Control>...</Control>
                ...
        </Data>
    </Node>
	 */
	public Node createNodeRadio(String id, String name, List<String> values, String trElements, String trLinks, String resultElement) {
		Node node = new Node();
		node.setId(id);
		node.setName(name);
		if (values != null) {
			node.setItems(new Node.Items());
			node.getItems().getValue().addAll(values);
		}
		node.setDataClass("TctScreen"); //$NON-NLS-1$
		
		Node.Data data = new Node.Data();
		data.setNextId(16);
		data.setTemplateId("{A049074A-8769-4A9C-AFC4-EC1B1A213B2C}"); //$NON-NLS-1$
		data.setName(name);
		Controls controls = new Controls();
		controls.getControl().add(createControl2());
		controls.getControl().add(createControl6());
		controls.getControl().add(createRadioControl7(trElements, trLinks, resultElement));
		controls.getControl().add(createControl11());
		data.setControls(controls);
		node.setData(data);
		
		return node;
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
            <NextId>12</NextId>
            <TemplateId>{E4BFBBF6-F16B-4C0E-8729-FFCE68569AF3}</TemplateId>
            <Name>???</Name>
            <Controls>
                <Control>...</Control>
                ...
        </Data>
    </Node>
	 */
	public Node createNodeMultiList(String id, String name, List<String> values, String trElements, String trLinks, Integer minChecks, boolean withNumbers) {
		Node node = new Node();
		node.setId(id);
		node.setName(name);
		if (values != null) {
			node.setItems(new Node.Items());
			node.getItems().getValue().addAll(values);
		}
		node.setDataClass("TctScreen"); //$NON-NLS-1$
		
		Node.Data data = new Node.Data();
		data.setNextId(12);
		data.setTemplateId("{E4BFBBF6-F16B-4C0E-8729-FFCE68569AF3}"); //$NON-NLS-1$
		data.setName(name);
		Controls controls = new Controls();
		controls.getControl().add(createControl2());
		controls.getControl().add(createControl6());
		controls.getControl().add(withNumbers ? createChecklistControl7(trElements, trLinks, minChecks, LIST_MODE_NUMBERS, null) : createChecklistControl7(trElements, trLinks, minChecks, LIST_MODE_DEFAULT, null));
		controls.getControl().add(createControl11());
		data.setControls(controls);
		node.setData(data);
		
		return node;
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
            <NextId>14</NextId>
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
		data.setNextId(14);
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
			<Value>{82D16C8E-776E-4E8B-A459-6EBF62E50076}</Value> <!-- this is PHOTO_ID -->
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
	public Node createPhoto(String id, String name, boolean required) {
		Node node = new Node();
		node.setId(id);
		node.setName(name);
		node.setDataClass("TctScreen"); //$NON-NLS-1$
		
		Node.Items items = new Node.Items();
		items.getValue().add(ICyberTrackerConstants.PHOTO);
		node.setItems(items);

		Node.Data data = new Node.Data();
		data.setNextId(14);
		data.setTemplateId("{5DC23CAB-C51C-47A7-B8B6-C25DE35DDF4B}"); //$NON-NLS-1$
		data.setName(name);
		Controls controls = new Controls();
		controls.getControl().add(createControl2());
		controls.getControl().add(createControl11());
		controls.getControl().add(createPhotoControl12(required));
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
	public Controls.Control createRadioControl7(String trElements, String trLinks, String radioElement) {
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
		control.setAttribute(0);
		control.setAutoRadioNext(ctProperties.isAutoNext() ? "True" : "False"); //$NON-NLS-1$ //$NON-NLS-2$
		control.setTranslateElements(trElements);
		control.setTranslateLinks(trLinks);
		control.setTranslateRadioElement(radioElement);
		return control;
	}

	/**
    <Control>
        <Type>{289461C2-B3EE-4075-9538-451580BD4B38}</Type>
        <Version>1</Version>
        <LockProperties>Elements</LockProperties>
        <Id>7</Id>
        <Align>5</Align>
        <Left>0</Left>
        <Top>34</Top>
        <Width>240</Width>
        <Height>262</Height>
        <Translate__Font>MS Sans Serif,10,B</Translate__Font>
        <ItemHeight>25</ItemHeight>
        <Version>1</Version>
        <Attribute>0</Attribute>
        <ListMode>???</ListMode>
        <NumberChecks>???</NumberChecks>
        <MinChecks>???</MinChecks>
        <Translate__Elements>???</Translate__Elements>
        <Translate__Links>???</Translate__Links>
    </Control>
	 */
	public Controls.Control createChecklistControl7(String trElements, String trLinks, Integer minChecks, Integer listMode, String numberChecks) {
		Controls.Control control = new Controls.Control();
		control.setType("{289461C2-B3EE-4075-9538-451580BD4B38}"); //$NON-NLS-1$
		control.setVersion(1);
		control.setLockProperties("Elements"); //$NON-NLS-1$
		control.setId(7);
		control.setAlign(5);
		control.setLeft(0);
		control.setTop(34);
		control.setWidth(240);
		control.setHeight(262);
		control.setTranslateFont("MS Sans Serif,10,B"); //$NON-NLS-1$
		control.setItemHeight(25);
		control.setAttribute(0);
		control.setListMode(listMode);
		control.setNumberChecks(numberChecks);
		control.setMinChecks(minChecks);
		control.setTranslateElements(trElements);
		control.setTranslateLinks(trLinks);
		return control;
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
		control.setPassword(password);
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
	    <Translate__Element>{82D16C8E-776E-4E8B-A459-6EBF62E50076}</Translate__Element>   <!-- this is PHOTO_ID -->
	 </Control>
	 */
	private Control createPhotoControl12(boolean required) {
		Controls.Control control = new Controls.Control();
		control.setType("{02F601F3-28BD-4D1F-A9DD-CAA71ABC25FC}"); //$NON-NLS-1$
		control.setId(12);
		control.setAlign(5);
		control.setLeft(0);
		control.setTop(32);
		control.setWidth(240);
		control.setHeight(264);
		control.setTranslateFont("Arial,12,B"); //$NON-NLS-1$
		control.setTranslateElement(ICyberTrackerConstants.PHOTO);
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
	public Controls.Control createConfigureGPSControl13(CyberTrackerProperties props) {
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
		node.getData().getControls().getControl().add(control);
	}	
}
