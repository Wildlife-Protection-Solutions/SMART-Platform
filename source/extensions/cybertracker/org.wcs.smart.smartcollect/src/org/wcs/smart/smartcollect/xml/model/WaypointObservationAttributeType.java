package org.wcs.smart.smartcollect.xml.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for WaypointObservationAttributeType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="WaypointObservationAttributeType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="sValue" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="dValue" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *         &lt;element name="itemKey" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="bValue" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="attributeKey" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WaypointObservationAttributeType", propOrder = {
    "sValue",
    "dValue",
    "itemKey",
    "bValue"
})
public class WaypointObservationAttributeType {

    protected String sValue;
    protected Double dValue;
    protected List<String> itemKey;
    protected Boolean bValue;
    @XmlAttribute(name = "attributeKey")
    protected String attributeKey;

    /**
     * Gets the value of the sValue property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSValue() {
        return sValue;
    }

    /**
     * Sets the value of the sValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSValue(String value) {
        this.sValue = value;
    }

    /**
     * Gets the value of the dValue property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getDValue() {
        return dValue;
    }

    /**
     * Sets the value of the dValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setDValue(Double value) {
        this.dValue = value;
    }

    /**
     * Gets the value of the itemKey property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the itemKey property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getItemKey().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getItemKey() {
        if (itemKey == null) {
            itemKey = new ArrayList<String>();
        }
        return this.itemKey;
    }

    /**
     * Gets the value of the bValue property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isBValue() {
        return bValue;
    }

    /**
     * Sets the value of the bValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setBValue(Boolean value) {
        this.bValue = value;
    }

    /**
     * Gets the value of the attributeKey property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAttributeKey() {
        return attributeKey;
    }

    /**
     * Sets the value of the attributeKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAttributeKey(String value) {
        this.attributeKey = value;
    }

}
