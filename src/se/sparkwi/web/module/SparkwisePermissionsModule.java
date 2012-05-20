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
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.module.permissions.DefaultPermissionsModule;
import com.pagesociety.web.module.permissions.PermissionEvaluator;
import com.pagesociety.web.module.resource.ResourceModule;
import com.pagesociety.web.module.user.UserModule;

public class SparkwisePermissionsModule extends DefaultPermissionsModule
{
	owner_pf 		is_owner 	 = new owner_pf();
	is_same_user_pf is_same_user = new is_same_user_pf();

	public void definePermissions()
	{
		super.definePermissions();
		ROLE_PERMISSIONS(ROLE_PUBLIC,
				MODULE_PERMISSIONS("Resource",
						PERMISSION(ResourceModule.CAN_GET_RESOURCE_URL)
				),
				MODULE_PERMISSIONS("Public",GLOB,true)
		);
		ROLE_PERMISSIONS(UserModule.USER_ROLE_SYSTEM_USER,
			MODULE_PERMISSIONS("Dashboard",
					PERMISSION(DashboardModule.CAN_MANAGE_PROJECT,is_owner),
					PERMISSION(DashboardModule.IS_OWNER,is_owner)
			),
			MODULE_PERMISSIONS("Resource",
					PERMISSION(ResourceModule.CAN_GET_RESOURCE_URL)
			),
			MODULE_PERMISSIONS("User",
						PERMISSION(UserModule.CAN_EDIT_USER,is_same_user)
			),
			MODULE_PERMISSIONS("Resource",
					PERMISSION(ResourceModule.CAN_CREATE_RESOURCE),
					PERMISSION(ResourceModule.CAN_UPDATE_RESOURCE,is_owner),
					PERMISSION(ResourceModule.CAN_READ_RESOURCE,is_owner),
					PERMISSION(ResourceModule.CAN_GET_RESOURCE_URL,is_owner)
			)
		);
	}

	
	


	class owner_pf extends PermissionEvaluator
	{

		@Override
		public boolean exec(Entity user,String namespace,String perm_id,Map<String,Object> context) throws PersistenceException
		{
			Entity instance = (Entity)context.get(GUARD_INSTANCE);
			return IS_CREATOR(store, user, instance);
		}

	}

	class is_same_user_pf extends PermissionEvaluator
	{

		@Override
		public boolean exec(Entity user,String namespace,String perm_id,Map<String,Object> context) throws PersistenceException
		{
			Entity target = (Entity)context.get(GUARD_USER);
			return IS_SAME(user,target);
		}

	}

}
