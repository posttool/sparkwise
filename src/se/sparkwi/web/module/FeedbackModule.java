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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.gateway.RawCommunique;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.email.IEmailModule;

public class FeedbackModule extends WebStoreModule
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
	public void AddFeedbackEntry(UserApplicationContext uctx, String subject, String os, String browser, String message, String from) throws WebApplicationException,PersistenceException
	{
		//Entity from = (Entity)uctx.getUser();
		Entity feedback = NEW(FEEDBACK_ENTITY,
				null,
				FEEDBACK_SUBJECT,subject,
				FEEDBACK_OS,os,
				FEEDBACK_BROWSER,browser,
				FEEDBACK_MESSAGE,message,
				FEEDBACK_FROM,from
		);
		Map<String,Object> data = new HashMap<String,Object>();
		data.put("feedback",feedback);
		email_module.sendEmail(from, new String[]{"support@sparkwi.se"}, subject, "feedback.fm", data);
	}
	
	@Export
	public void Dump(UserApplicationContext uctx,RawCommunique c) throws PersistenceException,WebApplicationException
	{
		if (!AdminModule.isAdmin(uctx))
			throw new WebApplicationException("NONO");
		
		HttpServletResponse resp = (HttpServletResponse)c.getResponse();
		
		Query q = new Query(FEEDBACK_ENTITY);
		q.idx(Query.PRIMARY_IDX);
		q.eq(Query.VAL_GLOB);
		q.orderBy(FIELD_DATE_CREATED,Query.DESC);
	
		try{
			
			resp.getWriter().println("<table>");	
			List<Entity> ee  = QUERY(q).getEntities();
			for(int i = 0;i < ee.size();i++)
			{
				Entity e = ee.get(i);
				resp.getWriter().println("<tr>");	
				resp.getWriter().print("<td>"+e.getId()+"</td>");
				resp.getWriter().print("<td>"+e.getAttribute(FIELD_DATE_CREATED)+"</td>");
				resp.getWriter().print("<td>"+e.getAttribute(FEEDBACK_SUBJECT)+"</td>");
				resp.getWriter().print("<td>"+e.getAttribute(FEEDBACK_OS)+"</td>");
				resp.getWriter().print("<td>"+e.getAttribute(FEEDBACK_BROWSER)+"</td>");
				resp.getWriter().print("<td>"+e.getAttribute(FEEDBACK_MESSAGE)+"</td>");
				resp.getWriter().println("</tr>");	
			}
			resp.getWriter().println("</table>");	
			
		}catch(IOException ioe)
		{
			throw new WebApplicationException(ioe.getMessage());
		}
	}
	
	public static final String FEEDBACK_ENTITY				= "Feedback";
	public static final String FEEDBACK_SUBJECT 			= "subject";
	public static final String FEEDBACK_OS  				= "os";
	public static final String FEEDBACK_BROWSER 			= "browser";
	public static final String FEEDBACK_MESSAGE 			= "message";
	public static final String FEEDBACK_FROM 				= "from";
	
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY(FEEDBACK_ENTITY,
				FEEDBACK_SUBJECT, Types.TYPE_STRING,"", 
				FEEDBACK_OS, Types.TYPE_STRING,"",
				FEEDBACK_BROWSER, Types.TYPE_STRING,"",
				FEEDBACK_MESSAGE, Types.TYPE_STRING,"",
				FEEDBACK_FROM, Types.TYPE_STRING,""
		);
					  
	}
	
	
	
}
