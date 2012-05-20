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

import javax.servlet.http.HttpServletRequest;

import se.sparkwi.web.module.auth.ConnectionApi;
import se.sparkwi.web.module.auth.OAuth1BaseHandler;
import se.sparkwi.web.module.auth.OAuthorizationModule;

import com.pagesociety.util.CALLBACK;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public class FlickrAuthorizationHandler extends OAuth1BaseHandler
{
	
	private static String flicker_consumer_key;
	private static String flicker_secret_key;

	public static void init(String ck, String sk)
	{
		flicker_consumer_key = ck;
		flicker_secret_key = sk;
	}
	
	public String get_type()
	{
		return ConnectionApi.CONNECTION_TYPE_FLICKR;
	}
	public String get_consumer_key()
	{
		return flicker_consumer_key;
	}
	public String get_consumer_secret()
	{
		return flicker_secret_key;
	}
	public String get_request_url()
	{
		return "http://www.flickr.com/services/oauth/request_token";
	}
	public String get_authorize_url()
	{
		return "http://www.flickr.com/services/oauth/authorize";
	}
	public String get_access_url()
	{
		return "http://www.flickr.com/services/oauth/access_token";
	}

	public String get_preferred_auth_flow_method()
	{
		return OAuthorizationModule.METHOD_GET;
	}

	public String get_scope(HttpServletRequest request)
	{
		return "http://api.flickr.com/services/rest/";
	}

	//#api metadata
	public String get_api_call_endpoint()
	{
		return "http://api.flickr.com/services/rest/";
	}

	public OBJECT get_api_call_json_config()
	{
		return new OBJECT("type","param","param_name","format","param_value","json");
	}

	public OBJECT get_api_call_params()
	{
		return new OBJECT("v","2", "nojsoncallback", "1");
	}

	public CALLBACK get_api_call_translate_non_200_response_f()
	{
		return null;
	}

	public OBJECT get_user_profile_info(String access_token,String access_token_secret) throws WebApplicationException
	{
		ConnectionApi api = create_api(access_token,access_token_secret,null);
		OBJECT profile 	  = new OBJECT();

		try{
			profile 	  = api.call("/", new OBJECT("method","flickr.test.login"));
		}catch(WebApplicationException e)
		{
				throw e;
		}
		
		String uname = profile.S("user.username._content");
		profile.put("username",uname);
		return profile;
	}
}
