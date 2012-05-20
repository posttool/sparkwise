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

import com.pagesociety.bdb.BDBQueryResult;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.bean.BeanRegistry;
import com.pagesociety.web.exception.AccountLockedException;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.LoginFailedException;
import com.pagesociety.web.exception.PermissionsException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.PagingQueryResult;

public class SparkWiseApplication extends WebApplication 
{
	public SparkWiseApplication() throws InitializationException
	{
		super();
		BeanRegistry.register(BDBQueryResult.class);
		BeanRegistry.register(PagingQueryResult.class);
		BeanRegistry.register(WebApplicationException.class);
		BeanRegistry.register(PermissionsException.class);
		BeanRegistry.register(LoginFailedException.class);
		BeanRegistry.register(AccountLockedException.class);
		BeanRegistry.register(NullPointerException.class);
		BeanRegistry.register(PersistenceException.class);
	}
}
