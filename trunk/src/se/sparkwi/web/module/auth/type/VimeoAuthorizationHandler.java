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
import se.sparkwi.web.module.auth.OAuth1BaseHandler;
import se.sparkwi.web.module.auth.OAuthorizationModule;

import com.pagesociety.util.CALLBACK;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

/*********************************************************/



public class VimeoAuthorizationHandler extends OAuth1BaseHandler
{

	private static String vimeo_consumer_key;
	private static String vimeo_consumer_secret_key;
	
	public static void init(String ck, String sk)
	{
		vimeo_consumer_key = ck;
		vimeo_consumer_secret_key = sk;
	}
	
	public String get_type()
	{
		return ConnectionApi.CONNECTION_TYPE_VIMEO;
	}

	public String get_consumer_key()
	{
		return vimeo_consumer_key;
	}

	public String get_consumer_secret()
	{
		return vimeo_consumer_secret_key;
	}

	public String get_request_url()
	{
		return "http://vimeo.com/oauth/request_token";
	}

	public String get_authorize_url()
	{
		return "http://vimeo.com/oauth/authorize";
	}

	public String get_access_url()
	{
		return "http://vimeo.com/oauth/access_token";
	}

	public OBJECT get_xtra_authorize_params()
	{
		return new OBJECT("permission","read");
	}

	public String get_preferred_auth_flow_method()
	{
		return OAuthorizationModule.METHOD_GET;
	}

	public String get_scope(HttpServletRequest request)
	{
		return null;
	}

	//#api metadata
	public String get_api_call_endpoint()
	{
		return "http://vimeo.com/api/rest/v2";
	}

	public OBJECT get_api_call_style()
	{
		return new OBJECT("type","param","param_name","method");
	}

	public OBJECT get_api_call_json_config()
	{
		return new OBJECT("type","param","param_name","format","param_value","json");
	}

	public CALLBACK get_api_call_check_error_on_200_f()
	{
		return new CALLBACK()
		{
			public void exec(OBJECT response_dict) throws Exception
			{
				if (response_dict.containsKey("err"))
				{
					if (((Map)response_dict.get("error")).containsKey("expl"))
						throw new WebApplicationException(response_dict.S("error.expl"));
					else
						throw new WebApplicationException("Unknown Vimeo Exception");
				}
			}
		};
	}

	public CALLBACK get_api_call_translate_non_200_response_f()
	{
		/*
			#def f(code,response_dict):
					#* 404 Not Found: The URI requested is invalid or the resource requested, such as a user, does not exists.
					#* 500 Internal Server Error: Something is broken.  Please post to the group so the Twitter team can investigate.

			#API_EXCEPTION_CODE_AUTH_ERROR
			#API_EXCEPTION_CODE_BAD_SELECTOR
			#API_EXCEPTION_CODE_BAD_REQUEST
			#API_EXCEPTION_CODE_RATE_EXCEEDED
			#API_EXCEPTION_CODE_SERVICE_ERROR
			#API_EXCEPTION_CODE_UNKNOWN_ERROR
			#return f
			return None
		*/
		return null;
	}
	public OBJECT get_user_profile_info(String access_token,String access_token_secret) throws WebApplicationException
	{
		//api = self.create_api(access_token,access_token_secret)
		//profile = api.call('vimeo.people.getInfo')
		//profile['username'] = profile['person']['display_name']
		//profile['uid']      = profile['person']['id']
		//profile['pic']      = profile['person']['portraits']['portrait'][0]["_content"]
		//return profile

		ConnectionApi api = create_api(access_token,access_token_secret,null);
		OBJECT profile = api.call("vimeo.people.getInfo");
		profile.put("username",profile.S("person.display_name"));
		profile.put("uid",profile.S("person.id"));
		profile.put("pic",profile.S("person.portraits.portrait[0]._content"));
		return profile;
	}
}
