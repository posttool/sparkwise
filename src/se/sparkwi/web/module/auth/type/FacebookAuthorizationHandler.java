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
package se.sparkwi.web.module.auth.type;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import se.sparkwi.web.module.auth.ConnectionApi;
import se.sparkwi.web.module.auth.OAuth2BaseHandler;

import com.pagesociety.util.CALLBACK;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public class FacebookAuthorizationHandler extends OAuth2BaseHandler
{

	public static String facebook_app_id;
	private static String facebook_secret_key;
	public static void init(String ck, String sk)
	{
		facebook_app_id = ck;
		facebook_secret_key = sk;
	}

	public String get_type()
 	{
		return ConnectionApi.CONNECTION_TYPE_FACEBOOK;
 	}
	public String get_app_id()
	{
		return facebook_app_id;
	}

	public String get_app_secret()
	{
		return facebook_secret_key;
	}

	public String get_access_url()
	{
		return "https://graph.facebook.com/oauth/access_token";
	}

	public String get_authorize_url()
	{
		return "https://graph.facebook.com/oauth/authorize";
	}

	public String get_scope(HttpServletRequest request)
	{
		//return request.getParameter("scopes");
		return "offline_access,read_stream,manage_pages,read_insights,user_photos,user_videos,friends_videos";
	}

	//#api meta data
	public String get_api_endpoint()
	{
		return "https://graph.facebook.com";
	}

	public CALLBACK get_api_call_check_error_on_200_f()
	{
		 //TODO allow classes w/o module instance to construct callbacks
		return new CALLBACK()
		{
			public void exec(OBJECT response_dict) throws Exception
			{
				if (response_dict.containsKey("error"))
				{
					if (((Map)response_dict.get("error")).containsKey("message"))
						throw new WebApplicationException((String)response_dict.find("error.message"));
					else
						throw new WebApplicationException("Unknown Facebook Error");
				}
			}
		};
	}
		

	public OBJECT get_user_profile_info(String access_token) throws WebApplicationException
	{
		ConnectionApi api     = this.create_api(access_token,null,null);
		OBJECT profile = api.call("/me");
		profile.put("username",profile.get("name"));
		profile.put("uid",profile.get("id"));
		profile.put("pic","http://graph.facebook.com/"+profile.get("uid")+"/picture");
		return profile;
	}

}
