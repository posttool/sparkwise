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
package se.sparkwi.web;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.module.user.UserModule;
import com.pagesociety.web.UserApplicationContext;

public class SparkWiseUserApplicationContext extends UserApplicationContext
{
	public void setUser(Object user)
	{
		
		if(user != null)
		{
			Entity u = (Entity)user;
			u.setAttribute(UserModule.FIELD_PASSWORD, "");
		}
		super.setUser(user);
	}
}
