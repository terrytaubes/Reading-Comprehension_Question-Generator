package generation_models;

//import com.avaje.ebean.Ebean;
//import com.avaje.ebean.Expr;
//import com.avaje.ebean.Model;
//import com.avaje.ebean.SqlRow;
//import com.avaje.ebean.annotation.CreatedTimestamp;
//import com.avaje.ebean.annotation.UpdatedTimestamp;
//import com.google.common.base.CharMatcher;
//import forms.common.UserForm;
//import org.apache.commons.lang3.text.WordUtils;
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;
//import org.json.simple.parser.ParseException;
//import play.data.validation.Constraints.Email;
//import play.data.validation.Constraints.Required;
//import utilities.common.PasswordHash;

import java.sql.Timestamp;


public class UserSimple {
	/********************************
	 ENUMERATOR: For each User role
	 ********************************/
	public static enum Role {
		SUPERADMIN,
		ADMIN,
		INSTRUCTOR,
		STUDENT;
		
		public static Role getRole(String roleString) {
			Role role = null;
			
			switch(roleString) {
				case "Super Administrator":
					role = SUPERADMIN;
					break;
				case "Administrator":
					role = ADMIN;
					break;
				case "Instructor":
					role = INSTRUCTOR;
					break;
				case "Student":
					role = STUDENT;
					break;
				}
			
			return role;
		}
	}
	
	
	/********************************
	 FIELDS
	 ********************************/
	/* Universal */
	/*===========*/
//	@Id
	public Long id;
	
//	@Required
	public boolean retired = false;
	
//	@CreatedTimestamp
	public Timestamp createdTime;
	
//	@UpdatedTimestamp
	public Timestamp updatedTime;
	
	
	/* Specific */
	/*===========*/

//	@Required
	public String firstName;

//	@Required
	public String lastName;
	
//	@Required
//	@Column(unique = true)
	public String username;

//	@Required
	public String password;	// hashed using utilities.PasswordHash
	
//	@Email
	public String email;
		
//	@Required
	public Role role;
	
	public Long creatorId;
	
	
	public UserSimple(String phone, String language, String alias, String password) {
		this.username = phone;
		this.firstName = alias;
		this.lastName = language;
		this.password = password;
		this.role = Role.STUDENT;
		this.creatorId = 0L;
	}


	public static UserSimple create(UserSimple user) {
		return user;
	}


	public String getFullName() {
		return this.firstName + " " + this.lastName;
	}

	
}
