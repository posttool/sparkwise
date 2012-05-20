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

/*********************************************************/



public class TwitterAuthorizationHandler extends OAuth1BaseHandler
{

	private static String twitter_consumer_key;
	private static String twitter_consumer_secret_key;
	
	public static void init(String ck, String sk)
	{
		twitter_consumer_key = ck;
		twitter_consumer_secret_key = sk;
	}

	public String get_type()
	{
		return ConnectionApi.CONNECTION_TYPE_TWITTER;
	}

	public String get_consumer_key()
	{
		return twitter_consumer_key;
	}

	public String get_consumer_secret()
	{
		return twitter_consumer_secret_key;
	}

	public String get_request_url()
	{
		return "https://api.twitter.com/oauth/request_token";
	}

	public String get_authorize_url()
	{
		return "https://api.twitter.com/oauth/authenticate";
	}

	public OBJECT get_xtra_authorize_params()
	{
		return new OBJECT("force_login","true");
	}
	
	public String get_access_url()
	{
		return "https://api.twitter.com/oauth/access_token";
	}

	public String get_preferred_auth_flow_method()
	{
		return OAuthorizationModule.METHOD_POST;
	}

	public String get_scope(HttpServletRequest request)
	{
		return null;
	}

	//#api metadata
	public String get_api_call_endpoint()
	{
		return "http://api.twitter.com/1";
	}

	public OBJECT get_api_call_json_config()
	{
		return new OBJECT("type","extension","extension","json");
	}

	public CALLBACK get_api_call_translate_non_200_response_f()
	{
		/*
		#def f(code,response_dict):
				#* 304 Not Modified: There was no new data to return.
				#* 400 Bad Request: The request was invalid.  An accompanying error message will explain why. This is the status code will be returned during rate limiting.
				#* 401 Unauthorized: Authentication credentials were missing or incorrect.
				#* 403 Forbidden: The request is understood, but it has been refused.  An accompanying error message will explain why. This code is used when requests are being denied due to update limits.
				#* 404 Not Found: The URI requested is invalid or the resource requested, such as a user, does not exists.
				#* 406 Not Acceptable: Returned by the Search API when an invalid format is specified in the request.
				#* 420 Enhance Your Calm: Returned by the Search and Trends API  when you are being rate limited.
				#* 500 Internal Server Error: Something is broken.  Please post to the group so the Twitter team can investigate.
				#* 502 Bad Gateway: Twitter is down or being upgraded.
				#* 503 Service Unavailable: The Twitter servers are up, but overloaded with requests. Try again later.
		#API_EXCEPTION_CODE_AUTH_ERROR
		#API_EXCEPTION_CODE_BAD_SELECTOR
		#API_EXCEPTION_CODE_BAD_REQUEST
		#API_EXCEPTION_CODE_RATE_EXCEEDED
		#API_EXCEPTION_CODE_SERVICE_ERROR
		#API_EXCEPTION_CODE_UNKNOWN_ERROR
		#return f
		*/
		return null;
	}
	public OBJECT get_user_profile_info(String access_token,String access_token_secret) throws WebApplicationException
	{
		//test for static method
		//Entity c = new Entity(CONNECTION_ENTITY);
		//c.setAttribute(CONNECTION_FIELD_TYPE, "Twitter");
		//c.setAttribute(CONNECTION_FIELD_TOKEN, access_token);
		//c.setAttribute(CONNECTION_FIELD_SECRET, access_token_secret);
		//ConnectionApi api = apiFromEntity(c);
		ConnectionApi api = create_api(access_token,access_token_secret,null);
		OBJECT profile = api.call("/account/verify_credentials");
		profile.put("username",profile.S("name"));
		profile.put("uid",profile.S("id_str"));
		profile.put("pic",profile.S("profile_image_url"));
		return profile;
	}
}
