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

public class GoogleAnalyticsAuthorizationHandler extends OAuth1BaseHandler
{
	
	private static String gdata_consumer_key;
	private static String gdata_consumer_secret_key;

	public static void init(String ck, String sk)
	{
		gdata_consumer_key = ck;
		gdata_consumer_secret_key = sk;
	}
	
	//""" http://code.google.com/apis/gdata/articles/oauth.html """
	public String get_type()
	{
		return ConnectionApi.CONNECTION_TYPE_GOOGLE_ANALYTICS;
	}
	public String get_consumer_key()
	{
		return gdata_consumer_key;
	}
	public String get_consumer_secret()
	{
		return gdata_consumer_secret_key;
	}
	public String get_request_url()
	{
		return "https://www.google.com/accounts/OAuthGetRequestToken";
	}
	public String get_authorize_url()
	{
		return "https://www.google.com/accounts/OAuthAuthorizeToken";
	}
	public String get_access_url()
	{
		return "https://www.google.com/accounts/OAuthGetAccessToken";
	}

	public String get_preferred_auth_flow_method()
	{
		return OAuthorizationModule.METHOD_GET;
	}

	public String get_scope(HttpServletRequest request)
	{
		return "https://www.google.com/analytics/feeds/";
	}

	//#api metadata
	public String get_api_call_endpoint()
	{
		return "https://www.google.com/analytics/feeds";
	}

	public OBJECT get_api_call_json_config()
	{
		return new OBJECT("type","param","param_name","alt","param_value","json");
	}

	public OBJECT get_api_call_params()
	{
		return new OBJECT("v","2");
	}

	public CALLBACK get_api_call_translate_non_200_response_f()
	{
		/*
		#def f(code,response_dict):
		#201 CREATED 	Creation of a resource was successful.
		#304 NOT MODIFIED 	The resource hasn't changed since the time specified in the request's If-Modified-Since header.
		#400 BAD REQUEST 	Invalid request URI or header, or unsupported nonstandard parameter.
		#??rate limit as well??
		#401 UNAUTHORIZED 	Authorization required.
		#403 FORBIDDEN 	Unsupported standard parameter, or authentication or authorization failed.
		#404 NOT FOUND 	Resource (such as a feed or entry) not found.
		#409 CONFLICT 	Specified version number doesn't match resource's latest version number.
		#420 Rate limit???
		#410 GONE 	Requested change history is no longer available on the server. Refer to service-specific documentation for more details.
		#500 INTERNAL SERVER ERROR 	Internal error. This is the default code that is used for all unrecognized server errors.
		#return f
		*/
		return null;
	}

	public OBJECT get_user_profile_info(String access_token,String access_token_secret) throws WebApplicationException
	{
		ConnectionApi api = create_api(access_token,access_token_secret,null);
		OBJECT profile 	  = null;

		try{
			profile 	  = api.call("/accounts/default");
		}catch(WebApplicationException e)
		{
			if(e.getCode() == 403)
			{
				throw new WebApplicationException("No analytics account for this user. Please connect with a user that has an analytics account.");
			}
			else
				throw e;
		}
		
		String uname = profile.S("feed.title.$t");
		if(uname.startsWith("Profile list for "))
			uname = uname.substring(17);
		profile.put("username",uname);
		profile.put("uid",profile.S("feed.id.$t"));
		profile.put("pic","http://www.google.com/images/logos/analytics_logo.gif");
		return profile;
	}
}
