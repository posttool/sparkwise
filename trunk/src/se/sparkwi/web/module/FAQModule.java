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

import java.util.HashMap;
import java.util.Map;

import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.email.IEmailModule;

public class FAQModule extends WebStoreModule
{
	
	private static final String SLOT_EMAIL_MODULE = "email-module"; 
	private IEmailModule 		email_module;
	
	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
		email_module = (IEmailModule)getSlot(SLOT_EMAIL_MODULE);
	}
	
	protected void defineSlots() 
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_EMAIL_MODULE,IEmailModule.class,true);
	}
	
	@Export
	public void AskQuestion(UserApplicationContext uctx, com.pagesociety.util.OBJECT info) throws WebApplicationException,PersistenceException
	{
		Map<String,Object> data = new HashMap<String,Object>();
		data.put("info",info);
		email_module.sendEmail(info.S("txt_email"), new String[]{"support@sparkwi.se"}, "FAQ Question", "faq.fm", data);
		
	}
	
	
	
}
