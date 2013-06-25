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
import org.wcs.smart.cybertracker.model.screens.Controls;
import org.wcs.smart.cybertracker.model.screens.Node;
import org.wcs.smart.cybertracker.model.screens.Screens;

/**
 * Factory for creating common screens objects.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ScreensObjectFactory {
	
	private static final int CONTROL_2_INDEX = 0;
	private static final int CONTROL_7_INDEX = 2;

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
	public static Screens createScreens(List<Node> screenNodes, CyberTrackerProperties properties) {
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
			a1Node.setName(properties.getApplicationName());
			Node.Data a1Data = new Node.Data();
			a1Node.setData(a1Data);
			a1Data.setName(properties.getApplicationName());
			a1Data.setTestTime("True"); //$NON-NLS-1$
			a1Data.setBigTitle("True"); //$NON-NLS-1$
			a1Data.setKioskMode(Boolean.TRUE.equals(properties.getKioskMode()) ? "True" : "False");  //$NON-NLS-1$//$NON-NLS-2$
			a1Data.setWaypointTimer(properties.getWaypointTimer());
			a1Data.setGpsTimeZone(properties.getGpsTimeZone());
		}
		if (screenNodes != null) {
			a1Node.getNode().addAll(screenNodes);
		}
		appsNode.getNode().add(a1Node);
		
		return screens;
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
            <NextId>14</NextId>
            <TemplateId>{A049074A-8769-4A9C-AFC4-EC1B1A213B2C}</TemplateId>
            <Name>???</Name>
            <Controls>
                <Control>...</Control>
                ...
        </Data>
    </Node>
	 */
	public static Node createNodeRadio(String id, String name, List<String> values, String trElements, String trLinks, String resultElement) {
		Node node = new Node();
		node.setId(id);
		node.setName(name);
		if (values != null) {
			node.setItems(new Node.Items());
			node.getItems().getValue().addAll(values);
		}
		node.setDataClass("TctScreen"); //$NON-NLS-1$
		
		Node.Data data = new Node.Data();
		data.setNextId(14);
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
            <TemplateId>{AB59A6BF-A6CB-4446-8350-DA326FABA878}</TemplateId>
            <Name>???</Name>
            <Controls>
                <Control>...</Control>
                ...
        </Data>
    </Node>
	 */
	public static Node createNodeChecklist(String id, String name, List<String> values, String trElements, String trLinks) {
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
		data.setTemplateId("{AB59A6BF-A6CB-4446-8350-DA326FABA878}"); //$NON-NLS-1$
		data.setName(name);
		Controls controls = new Controls();
		controls.getControl().add(createControl2());
		controls.getControl().add(createControl6());
		controls.getControl().add(createChecklistControl7(trElements, trLinks));
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
	public static Node createNodeNumber(String id, String name, String resultElementId) {
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
            <NextId>13</NextId>
            <TemplateId>{2F0173A3-6FF9-4B78-8ADF-80C16B1587B9}</TemplateId>
            <Name>???</Name>
            <Controls>
                <Control>...</Control>
                ...
        </Data>
    </Node>
	 */
	public static Node createNodeNote(String id, String name, String resultElementId) {
		Node node = new Node();
		node.setId(id);
		node.setName(name);
		node.setDataClass("TctScreen"); //$NON-NLS-1$

		Node.Data data = new Node.Data();
		data.setNextId(13);
		data.setTemplateId("{2F0173A3-6FF9-4B78-8ADF-80C16B1587B9}"); //$NON-NLS-1$
		data.setName(name);
		Controls controls = new Controls();
		controls.getControl().add(createControl2());
		controls.getControl().add(createControl6());
		controls.getControl().add(createControl11());
		controls.getControl().add(createNoteControl12(resultElementId));
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
	public static Node createNodeMsgText(String id, String name, String text) {
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
	public static Controls.Control createControl2() {
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
	public static Controls.Control createControl6() {
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
        <AutoRadioNext>True</AutoRadioNext>
        <Translate__Elements>???</Translate__Elements>
        <Translate__Links>???</Translate__Links>
        <Translate__RadioElement>???</Translate__RadioElement>
    </Control>
	 */
	public static Controls.Control createRadioControl7(String trElements, String trLinks, String radioElement) {
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
		control.setAutoRadioNext("True"); //$NON-NLS-1$
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
        <ListMode>1</ListMode>
        <Translate__Elements>???</Translate__Elements>
        <Translate__Links>???</Translate__Links>
    </Control>
	 */
	public static Controls.Control createChecklistControl7(String trElements, String trLinks) {
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
		control.setListMode(1);
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
	public static Controls.Control createControl11() {
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
	public static Controls.Control createNumberControl12(String element) {
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
        <Type>{49728018-D9F6-49B8-86E4-3EA49A29F4BC}</Type>
        <Id>12</Id>
        <BorderWidth>1</BorderWidth>
        <Align>5</Align>
        <Left>0</Left>
        <Top>34</Top>
        <Width>240</Width>
        <Height>262</Height>
        <Translate__Element>???</Translate__Element>
    </Control>
	 */
	public static Controls.Control createNoteControl12(String element) {
		Controls.Control control = new Controls.Control();
		control.setType("{49728018-D9F6-49B8-86E4-3EA49A29F4BC}"); //$NON-NLS-1$
		control.setId(12);
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
	public static Controls.Control createMsgTextControl12(String text) {
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
		control.setStretch("False"); //$NON-NLS-1$
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
	        <Caption>Enter text here...</Caption>
	    </Control>
	</Controls>
	 */
	public static Controls.Control createMsgTextControl13(String text) {
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
	public static Controls.Control createFormulaControl12(String formula) {
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
	public static Controls.Control createNavFormulaControl12(String formula, String id0, String id1) {
		Controls.Control control = createFormulaControl12(formula);
		control.setLockProperties("Elements A;Elements B;Elements C;Font;Formula;Result Element;Link 0;Link 1"); //$NON-NLS-1$
		control.setTranslateTargetScreenId0(id0);
		control.setTranslateTargetScreenId1(id1);
		return control;
	}

	/**
	<Control>
		...
 		<Translate__ResultElementId>...</Translate__ResultElementId>
		<ResultGlobalValue>...</ResultGlobalValue>
	 </Control>
	 */
	public static Controls.Control createCounterFormulaControl12(String counterName, String resultElementId) {
		String formula = counterName+"="+counterName+"+1"; //x=x+1  //$NON-NLS-1$ //$NON-NLS-2$
		Controls.Control control = createFormulaControl12(formula);
		control.setResultGlobalValue(counterName);
		control.setTranslateResultElementId(resultElementId);
		return control;
	}

	/**
	<Control>
    	<Type>{440B2C86-D385-4EFC-8ACE-5D3C0A4A016B}</Type>
    	<Id>13</Id>
    	<SightingAccuracy>49</SightingAccuracy> <!-- will be added as default by CyberTracker -->
    	<SightingFixCount>1</SightingFixCount>  <!-- will be added as default by CyberTracker -->
    	<WaypointAccuracy>49</WaypointAccuracy> <!-- will be added as default by CyberTracker -->
    	<WaypointTimer>...</WaypointTimer>
    	<UseRangeFinderForAltitude>False</UseRangeFinderForAltitude> <!-- will be added as default by CyberTracker -->
	</Control>
	 */
	public static Controls.Control createConfigureGPSControl13(Integer timer) {
		Controls.Control control = new Controls.Control();
		control.setType("{440B2C86-D385-4EFC-8ACE-5D3C0A4A016B}"); //$NON-NLS-1$
		control.setId(13);
		control.setWaypointTimer(timer);
		return control;
	}
	
	//Util methods
	public static Controls.Control getNavigationControl(Node node) {
		return node.getData().getControls().getControl().get(CONTROL_2_INDEX);
	}

	public static Controls.Control getRadioMainControl(Node node) {
		return node.getData().getControls().getControl().get(CONTROL_7_INDEX);
	}

}
