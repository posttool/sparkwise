/*******************************************************************************
 * Copyright 2012 Tomorrow Partners LLC
 * 
 * This file is part of SparkwiseServer.
 * 
 * SparkwiseServer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 * 
 * Sparkwise is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with SparkwiseServer.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package se.sparkwi.web.module;

import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.TransactionProtect;
import com.pagesociety.web.module.email.IEmailModule;
import com.pagesociety.web.module.registration.RegistrationModule;
public class SparkwiseRegistration extends RegistrationModule
{

	private static final String SLOT_MAILING_LIST_MODULE = "mailing-list-module"; 
	private static final String SLOT_EMAIL_MODULE		  = "email-module";

	private MailingListModule 		list_module;
	private IEmailModule 		email_module;
	
	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
		list_module = (MailingListModule)getSlot(SLOT_MAILING_LIST_MODULE);
		email_module = (IEmailModule)getSlot(SLOT_EMAIL_MODULE);
	}
	
	public void defineSlots() 
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_MAILING_LIST_MODULE,MailingListModule.class,true);
	}

	
	@Export
	@TransactionProtect
	public Entity Register(UserApplicationContext uctx,String email,String username,String password,OBJECT extra_props) throws WebApplicationException,PersistenceException
	{
		Entity user = super.Register(uctx, email, username, password);
		UpdateUserInfo(uctx,extra_props);
		extra_props.remove("txt_password");
		extra_props.remove("txt_confirm_password");
//		Map<String,Object> data = new HashMap<String,Object>();
//		data.put("info",extra_props);
//		email_module.sendEmail("support@sparkwi.se", REG_NOTIFICATION_LIST, "Sparkwise Registration #"+user.getId(), "faq.fm", data);
		return user;
	}
	
	@Export
	public Entity UpdateUserInfo(UserApplicationContext uctx,OBJECT extra_props) throws WebApplicationException, PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity user_info = GetUserInfo(uctx);
		if (extra_props == null)
			extra_props = new OBJECT();

		String updates = extra_props.S("chk_signmeup_for_updates");
		if (updates!=null && updates.equals("yes"))
		{
			list_module.signUp(extra_props.S("txt_email"), extra_props);
		}
		
		if(user_info==null)
		{
			user_info = NEW(USERINFO_ENTITY, user, USERINFO_PROPS, OBJECT.encode(extra_props));
		}
		else
		{
			OBJECT props = getProps(user_info);
			for (String k : extra_props.keySet())
				props.put(k, extra_props.get(k));
			
			user_info = UPDATE(user_info, USERINFO_PROPS, OBJECT.encode(props));
		}
		return user_info;
	}
	
	
	public com.pagesociety.util.OBJECT getProps(Entity user_info) throws WebApplicationException
	{
		return OBJECT.decode((String)user_info.getAttribute(USERINFO_PROPS));
	}

	@Export
	public Entity GetUserInfo(UserApplicationContext uctx) throws WebApplicationException, PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		return getUserInfo(user);
	}
	
	public Entity getUserInfo(Entity user) throws PersistenceException
	{
		Query q = new Query(USERINFO_ENTITY);
		q.idx(IDX_BY_USER);
		q.eq(user);
		QueryResult r = QUERY(q);
		if (r.size()==0)
			return null;
		return r.getEntities().get(0);
	}
	
	
	public static final String USERINFO_ENTITY				= "UserInfo";
	public static final String USERINFO_PROPS 				= "props";
	
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY(USERINFO_ENTITY,
				USERINFO_PROPS, Types.TYPE_STRING,""
		);
	}
	public static final String IDX_BY_USER 				= "IDX_BY_USER";

	protected void defineIndexes(Map<String,Object> config) throws PersistenceException, InitializationException
	{
		DEFINE_ENTITY_INDEX(USERINFO_ENTITY,IDX_BY_USER,EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX,FIELD_CREATOR);
	}
}
