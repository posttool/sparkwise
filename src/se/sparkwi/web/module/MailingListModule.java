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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.gateway.RawCommunique;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.WebStoreModule;

public class MailingListModule extends WebStoreModule
{
	

	@Export
	public Entity SignUp(UserApplicationContext uctx,String email, com.pagesociety.util.OBJECT info) throws WebApplicationException,PersistenceException
	{
		return signUp(email,info);
	}
	
	public Entity signUp(String email, com.pagesociety.util.OBJECT  info)throws WebApplicationException,PersistenceException
	{
		return NEW(MAILING_LIST_ENTITY,
				null,
				MAILING_LIST_FIELD_EMAIL,email,
				MAILING_LIST_FIELD_PROPS,com.pagesociety.util.OBJECT.encode(info)
		);
	}
	
	@Export
	public void Dump(UserApplicationContext uctx,RawCommunique c) throws PersistenceException,WebApplicationException
	{
		if (!AdminModule.isAdmin(uctx))
			throw new WebApplicationException("NONO");
		
		HttpServletResponse resp = (HttpServletResponse)c.getResponse();
		
		Query q = new Query(MAILING_LIST_ENTITY);
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
				resp.getWriter().print("<td>"+e.getAttribute(MAILING_LIST_FIELD_EMAIL)+"</td>");
				resp.getWriter().print("<td>"+com.pagesociety.util.OBJECT.decode((String)e.getAttribute(MAILING_LIST_FIELD_PROPS))+"</td>");
				resp.getWriter().println("</tr>");	
			}
			resp.getWriter().println("</table>");	
			
		}catch(IOException ioe)
		{
			throw new WebApplicationException(ioe.getMessage());
		}
	}
	
	public static final String MAILING_LIST_ENTITY			= "MailingList";
	public static final String MAILING_LIST_FIELD_EMAIL		= "email";
	public static final String MAILING_LIST_FIELD_PROPS		= "props";
	
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
	{
		DEFINE_ENTITY(MAILING_LIST_ENTITY,
				MAILING_LIST_FIELD_EMAIL, Types.TYPE_STRING,"", 
				MAILING_LIST_FIELD_PROPS, Types.TYPE_STRING,""
		);
					  
	}
	
	
	
}
