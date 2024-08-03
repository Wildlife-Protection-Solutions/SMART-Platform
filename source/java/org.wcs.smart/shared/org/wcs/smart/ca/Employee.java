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
package org.wcs.smart.ca;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;


/**
 * Employee object
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="employee", schema="smart")
/*
 * If this class extends UuidItem then a hibernate error occurs when trying
 * to load cross conservation area (conservation area) reports:
 * org.hibernate.PropertyAccessException: IllegalArgumentException occurred while calling setter of org.wcs.smart.ca.Employee.conservationArea
 * So this class is not extending UuidItem for now.
 * 
 * If two classes have the same id they can't have the same abstract class.
 * https://app.assembla.com/spaces/smart-cs/tickets/3621
 */
public class Employee implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * UUID for the 'shared' employee.  This employee uuid
	 * is associated with the CrossCA Conservation Area.
	 */
	public static final UUID SHARED_UUID = UuidUtils.stringToUuid(UuidUtils.ZERO_UUID_STR);
	
	
	public static final String USER_LEVEL_SEP = ","; //$NON-NLS-1$
	
	//non internationalizable
	public static final char DB_FEMALE = 'F';
	public static final char DB_MALE = 'M';
	
	/**
	 * Maximum length of family and given names
	 */
	public static final int MAX_NAME_LENGTH = 64;
	
	/**
	 * Maximum smart id length
	 */
	public static final int MAX_SMART_ID_LENGTH = 16;
	
	/**
	 * Minimum smart id length
	 */
	public static final int MIN_SMART_ID_LENGTH = 4;
	/**
	 * Maximum smart password length
	 */
	public static final int MAX_SMART_PASSWORD_LENGTH = 16;
	/**
	 * Minimum smart password length
	 */
	public static final int MIN_SMART_PASSWORD_LENGTH = 4;
	
	/**
	 * Minimum number of years in the past employee birthdate must be.
	 */
	public static final int MIN_EMPLOYEE_AGE = 10;
	
	/**
	 * Max number of chars for the staff id
	 */
	public static final int MAX_ID_LENGTH = 32;
	
	private String id;
	private String givenName;
	private String familyName;
	
	private LocalDate startEmploymentDate;
	private LocalDate endEmploymentDate;
	private LocalDate birthDate;
	private LocalDate dateCreated;
	
	private char gender;
	private String smartUserId;
	private String smartPassword;
	private String userLevelKey;
	
	private Set<String> userLevels;
	private List<EmployeeTeamMember> teams;
	
	private ConservationArea ca;
	private Agency agency;
	private Rank rank;
	private UUID uuid;
	
	public Employee(){
		this.dateCreated = LocalDate.now();
	}
	
	/**
	 * 
	 * @return the uuid for the list element
	 */
	@Id
	@UuidGenerator(style = UuidGenerator.Style.RANDOM)
	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	
	@Column(name="id")
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	@Column(name = "datecreated")
	public LocalDate getDateCreated() {
		return dateCreated;
	}
	public void setDateCreated(LocalDate dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	/**
	 * If you are creating a name label with given and family 
	 * names use the {@link #getShortLabel()} or {@link #getFullLabel()} instead.
	 * @return
	 */
	@Column(name="givenname")
	public String getGivenName() {
		return givenName;
	}
	
	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}
	
	/**
	 * If you are creating a name label with given and family 
	 * names use the {@link #getShortLabel()} or {@link #getFullLabel()} instead.
	 * @return
	 */
	@Column(name="familyname")
	public String getFamilyName() {
		return familyName;
	}
	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}
	
	@Column(name="startemploymentdate")
	public LocalDate getStartEmploymentDate() {
		return startEmploymentDate;
	}
	public void setStartEmploymentDate(LocalDate startEmploymentDate) {
		this.startEmploymentDate = startEmploymentDate;
	}
	
	@Column(name="endemploymentdate")
	public LocalDate getEndEmploymentDate() {
		return endEmploymentDate;
	}
	public void setEndEmploymentDate(LocalDate endEmploymentDate) {
		this.endEmploymentDate = endEmploymentDate;
	}
	
	@Column(name="birthdate")
	public LocalDate getBirthDate() {
		return birthDate;
	}
	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}
	
	public char getGender() {
		return gender;
	}
	public void setGender(char gender) {
		this.gender = gender;
	}
	
	@Column(name="smartuserid")
	public String getSmartUserId() {
		return smartUserId;
	}
	public void setSmartUserId(String smartUserId) {
		this.smartUserId = smartUserId;
	}
	
	@Column(name="smartpassword")
	public String getSmartPassword() {
		return smartPassword;
	}
	public void setSmartPassword(String smartPassword) {
		this.smartPassword = smartPassword;
	}

	/**
	 * 
	 * @return a comma delimited list of supported user levels
	 */
	@Column(name="smartuserlevel")
	public String getSmartUserLevelKeys() {
		return userLevelKey;
	}
	
	public void setSmartUserLevelKeys(String userLevelKey) {
		this.userLevelKey = userLevelKey;
		userLevels = new HashSet<>();
		if (userLevelKey != null && !userLevelKey.isEmpty()){
			String[] parts = userLevelKey.split(USER_LEVEL_SEP);
			for (String p : parts) userLevels.add(p);
		}
	}
	
	@Transient
	public void setSmartUserLevel(Collection<SmartUserLevel> levels){
		StringBuilder sb = new StringBuilder();
		levels.forEach(l -> sb.append(l.getKey() + USER_LEVEL_SEP));
		if (sb.length() > 0){
			sb.deleteCharAt(sb.length() -1);
			setSmartUserLevelKeys(sb.toString());
		}else{
			setSmartUserLevelKeys(null);
		}
		
	}
	
	/**
	 * 
	 * @return set of user levels associated with this employee
	 */
	@Transient
	public Collection<String> getSmartUserLevels(){
		return this.userLevels;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="agency_uuid", referencedColumnName="uuid")
	public Agency getAgency() {
		return this.agency;
	}
	
	public void setAgency(Agency agency){
		this.agency = agency;
	}
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="rank_uuid", referencedColumnName="uuid")
	public Rank getRank() {
		return this.rank;
	}
	
	public void setRank(Rank rank){
		this.rank = rank;
	}
	
	/**
	 * Get employee teams this employee is associated with
	 * @return
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.employee", orphanRemoval=true, cascade={CascadeType.ALL})
	public List<EmployeeTeamMember> getEmployeeTeams(){
		return this.teams;
	}
	/**
	 * Sets the employee teams this employee is associated with
	 * @param teams
	 */
	public void setEmployeeTeams(List<EmployeeTeamMember> teams) {
		this.teams = teams;
	}
	
	@Transient
	public boolean isActive(){
		return this.endEmploymentDate == null;
	}
	

	
	@Override
	public boolean equals(Object other){
		if (this == other) return true;
		if (other == null) return false;
		if (getUuid() == null) return false;
		//this is required for proxy classes
		//https://stackoverflow.com/questions/11013138/hibernate-equals-and-proxy
		if (!getClass().isInstance(other) && !other.getClass().isInstance(this) ) return false;		
		UuidItem s = (UuidItem)other;
		//must use getUuid for hibernate proxies 
		return (Objects.equals(getUuid(), s.getUuid()));
	}

	public int hashCode(){
		return Objects.hashCode(uuid);
	}

}

