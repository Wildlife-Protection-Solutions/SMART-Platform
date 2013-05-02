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
                	<Name>Application 1</Name>
                	<DataClass>TctSequence</DataClass>
                	<Node> ... </Node>
                	<Node> ... </Node>
                	...
            	</Node>
        	</Node>
    	</Root>
	</Screens>
	 */
	public static Screens createScreens(List<Node> screenNodes) {
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
		a1Node.setName("Application 1"); //$NON-NLS-1$
		a1Node.setDataClass("TctSequence"); //$NON-NLS-1$
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
            <NextId>12</NextId>
            <TemplateId>{A049074A-8769-4A9C-AFC4-EC1B1A213B2C}</TemplateId>
            <Name>???</Name>
            <Controls>
                <Control>...</Control>
                ...
        </Data>
    </Node>
	 */
	public static Node createNodeRadio(String id, String name, List<String> values, String trElements, String trLinks) {
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
		data.setTemplateId("{A049074A-8769-4A9C-AFC4-EC1B1A213B2C}"); //$NON-NLS-1$
		data.setName(name);
		Controls controls = new Controls();
		controls.getControl().add(createControl2());
		controls.getControl().add(createControl6());
		controls.getControl().add(createRadioControl7(trElements, trLinks));
		controls.getControl().add(createControl11());
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
            <TemplateId>{1C1DAD73-F942-41C0-806C-B5C9AFCA6E3B}</TemplateId>
            <Name>???</Name>
            <Controls>
                <Control>...</Control>
                ...
        </Data>
    </Node>
	 */
	public static Node createNodeNumber(String id, String name) {
		Node node = new Node();
		node.setId(id);
		node.setName(name);
		node.setDataClass("TctScreen"); //$NON-NLS-1$

		Node.Data data = new Node.Data();
		data.setNextId(13);
		data.setTemplateId("{1C1DAD73-F942-41C0-806C-B5C9AFCA6E3B}"); //$NON-NLS-1$
		data.setName(name);
		Controls controls = new Controls();
		controls.getControl().add(createControl2());
		controls.getControl().add(createControl6());
		controls.getControl().add(createControl11());
		controls.getControl().add(createNumberControl12());
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
	public static Node createNodeNote(String id, String name) {
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
		controls.getControl().add(createNoteControl12());
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
        <BorderWidth>0</BorderWidth> <!-- present in radio, but missing in number & note -->
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
//		control.setBorderWidth(0);
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
        <Translate__Elements>???</Translate__Elements>
        <Translate__Links>???</Translate__Links>
    </Control>
	 */
	public static Controls.Control createRadioControl7(String trElements, String trLinks) {
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
    </Control>
	 */
	public static Controls.Control createNumberControl12() {
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
    </Control>
	 */
	public static Controls.Control createNoteControl12() {
		Controls.Control control = new Controls.Control();
		control.setType("{49728018-D9F6-49B8-86E4-3EA49A29F4BC}"); //$NON-NLS-1$
		control.setId(12);
		control.setBorderWidth(1);
		control.setAlign(5);
		control.setLeft(0);
		control.setTop(34);
		control.setWidth(240);
		control.setHeight(262);
		return control;
	}
	
}
