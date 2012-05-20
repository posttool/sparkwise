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
package se.sparkwi.web.module.auth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.util.CALLBACK;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.json.JsonDecoder;

public abstract class OAuth2BaseHandler implements AuthorizationBaseHandler
{

	public OAuth2BaseHandler()
	{
	}

	class api implements ConnectionApi
	{
		public String api_endpoint;
		public OBJECT api_call_style;
		public OBJECT api_endpoint_headers;
		public OBJECT api_endpoint_params;
		public CALLBACK api_endpoint_error_check_f;
		public String access_token;
		public OBJECT profile;

		public api(String api_endpoint,OBJECT api_call_style,OBJECT api_endpoint_headers,OBJECT api_endpoint_params,CALLBACK api_endpoint_error_check_f,String access_token,OBJECT profile)
		{
			this.api_endpoint               = api_endpoint;
			this.api_call_style 			= api_call_style;
			this.api_endpoint_headers       = api_endpoint_headers;
			this.api_endpoint_params        = api_endpoint_params;
			this.api_endpoint_error_check_f = api_endpoint_error_check_f;
			this.access_token               = access_token;
			this.profile					= profile;
		}

		public OBJECT profile()
		{
			return this.profile;
		}

		public OBJECT call(String path_or_method) throws WebApplicationException
		{
			return call(path_or_method,null);
		}

		public OBJECT call(String path_or_method,OBJECT params) throws WebApplicationException
		{
			return call(path_or_method,params,OAuthorizationModule.METHOD_GET,null,true);
		}

		public OBJECT call(String api_call,OBJECT params,String method,OBJECT headers,boolean use_endpoint_as_base) throws WebApplicationException
		{

			if(OAuthorizationModule.fail_every_x_calls != 0  && ++OAuthorizationModule.current_failure_count % OAuthorizationModule.fail_every_x_calls == 0)
				throw new WebApplicationException("SIMULATED AUTH FAILURE");

			try
			{
				String response = do_call(api_call,params,method,headers,use_endpoint_as_base);
				OBJECT response_dict =  JsonDecoder.decodeAsOBJECT(response);
				if (this.api_endpoint_error_check_f != null)
				{
					try{
						this.api_endpoint_error_check_f.exec(response_dict);
					}catch(Exception e)
					{
						throw new WebApplicationException(e.getMessage());
					}
				}

				return response_dict;

			}catch(HttpResponseException re)
			{
				throw new WebApplicationException("Call to "+this.api_endpoint +" method: "+ api_call+" with params "+params+" failed with status code: "+re.getStatusCode(),re.getStatusCode());
			}catch(Exception e)
			{
				System.err.println(e);
				throw new WebApplicationException("API CALL FAILED "+e.getMessage());
			}

		}

		public String calls(String path_or_method,OBJECT params,String method,OBJECT headers) throws WebApplicationException
		{
			throw new WebApplicationException("OAuth2 connections only support JSON");
		}
		
		public String do_call(String api_call,OBJECT params,String method,OBJECT headers,boolean use_endpoint_as_base) throws HttpResponseException, WebApplicationException
		{
			String call_url = this.api_endpoint;
			if (use_endpoint_as_base)
				if (this.api_call_style.get("type").equals("path"))
					call_url = call_url + api_call;
			else
				call_url = api_call;

			if(params == null)
				params = new OBJECT();
			params.put("access_token",this.access_token);

			//#add additional headers
			if (this.api_endpoint_headers != null)
			{
				if(headers == null)
					headers = new OBJECT();
				headers.putAll(this.api_endpoint_headers);
			}
			//#add additional params
			if (this.api_endpoint_params != null)
			{
				params.putAll(this.api_endpoint_params);
			}
//				System.out.println("!!! ABOUT TO MAKE FB API CALL "+call_url+" HTTP METHOD IS "+method+" PARAMS IS "+params+" HEADERS IS "+headers);
			return make_request(call_url, params, method, headers);

		}
	}

	public ConnectionApi create_api(String access_token,String access_token_secret,OBJECT profile)
	{
		return new api(this.get_api_endpoint(),this.get_api_call_style(),this.get_api_call_headers(),this.get_api_call_params(),this.get_api_call_check_error_on_200_f(),access_token,profile);
	}

	private String make_request(String url,OBJECT params,String method,OBJECT headers) throws WebApplicationException,HttpResponseException
	{
		String additional_params_str = null;
		if(params != null)
		{
			StringBuilder additional_params_buf = new StringBuilder();
			try{
			for(Map.Entry<String,Object> entry: params.entrySet())
			{
				additional_params_buf.append(URLEncoder.encode(entry.getKey(),OAuthorizationModule.ENCODING));
				additional_params_buf.append('=');
				additional_params_buf.append(URLEncoder.encode((String)entry.getValue(),OAuthorizationModule.ENCODING));
				additional_params_buf.append('&');
			}
			}catch(UnsupportedEncodingException use)
			{
				use.printStackTrace();
				throw new WebApplicationException(use.getMessage());
			}
			additional_params_buf.setLength(additional_params_buf.length()-1);
			additional_params_str = additional_params_buf.toString();
		}

		HttpParams httpparams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpparams, 25000);
		HttpConnectionParams.setSoTimeout(httpparams, 25000);


		HttpClient httpclient = new DefaultHttpClient(httpparams);

		if(method.equals(OAuthorizationModule.METHOD_GET))
		{
			if(additional_params_str != null)
				url = url+"?"+additional_params_str;

	        try {
	        	HttpGet httpget = new HttpGet(url);
				if(headers != null)
				{
					for(Map.Entry<String,Object> entry: headers.entrySet())
						httpget.addHeader(entry.getKey(),(String) entry.getValue());
				}

				System.out.println("executing request " + httpget.getURI());

	            // Create a response handler
	            String responseBody = null;
	            try{
	            	 HttpResponse resp = httpclient.execute(httpget);
	            	 int statusCode = resp.getStatusLine().getStatusCode();
	                 HttpEntity entity = resp.getEntity();
	                 responseBody = EntityUtils.toString(entity);
	                 if(statusCode != 200)
	                 {
	                	 System.out.println("ERROR RESPONSE BODY WAS "+responseBody);
	                	 throw new HttpResponseException(statusCode, responseBody);
	                 }
	            	 return responseBody;
	            }
	            catch(HttpResponseException re)
	            {
	            	throw re;
	            }
	            catch(Exception e)
	            {
	            	throw new WebApplicationException("Request Failed  - "+url+" "+e.getMessage());
	            }

	        } finally {
	            // When HttpClient instance is no longer needed,
	            // shut down the connection manager to ensure
	            // immediate deallocation of all system resources
	            httpclient.getConnectionManager().shutdown();
	        }
		}
		else
		{
	        try {
	        	HttpPost httppost = new HttpPost(url);
				if(headers != null)
				{
					for(Map.Entry<String,Object> entry: headers.entrySet())
					{
						httppost.addHeader(entry.getKey(),(String) entry.getValue());
					}
				}
				if(additional_params_str != null)
				{
					try{

						httppost.setEntity(new StringEntity(additional_params_str, "application/x-www-form-urlencoded type‎", OAuthorizationModule.ENCODING));
					}catch(UnsupportedEncodingException use)
					{
						use.printStackTrace();
						throw new WebApplicationException(use.getMessage());
					}
				}
				System.out.println("executing request " + httppost.getURI());

	            // Create a response handler

	            String responseBody = null;
	            try{
	            	 HttpResponse resp = httpclient.execute(httppost);
	            	 int statusCode = resp.getStatusLine().getStatusCode();
	                 HttpEntity entity = resp.getEntity();
	                 responseBody = EntityUtils.toString(entity);
	                 if(statusCode != 200)
	                 {
	                	 System.out.println("ERROR RESPONSE BODY WAS "+responseBody);
	                	 throw new HttpResponseException(statusCode, responseBody);
	                 }
	            	 return responseBody;
	            }
	            catch(HttpResponseException re)
	            {
	            	throw re;
	            }
	            catch(Exception e)
	            {
	            	e.printStackTrace();
	            	throw new WebApplicationException("Request Failed  - "+url+" "+e.getMessage());
	            }


	        } finally {
	            // When HttpClient instance is no longer needed,
	            // shut down the connection manager to ensure
	            // immediate deallocation of all system resources
	            httpclient.getConnectionManager().shutdown();
	        }
		}

	}

	//#END BEGINNING OF OAUTH 2 TYPE STUFF
	public abstract String get_type();
	public abstract String get_app_id();
	public abstract String get_app_secret();
	public abstract String get_access_url();
	public abstract String get_authorize_url();
	public String get_scope(HttpServletRequest request)
	{
		return request.getParameter("scopes");
	}

	//#api meta data
	public abstract String get_api_endpoint();
	public OBJECT get_api_call_headers()
	{
		return null;
	}

	public OBJECT get_api_call_params()
	{
		return null;
	}

	public OBJECT get_api_call_style()
	{
		return new OBJECT("type","path");
	}

	public CALLBACK get_api_call_check_error_on_200_f()
	{
		return null;
	}
			/*#
		#{
		#  "error": {
		#    "type": "SomeBizarreTypeOfException",
		#    "message": "Everything went wrong."
		#  }
		#}
		#Note that FacebookGraphException is a catchall Graph API exception. For your convenience, RestFB will throw more-specific subclasses FacebookOAuthException and FacebookQueryParseException if it detects either of these Graph API error types. These are described below.
		# FacebookOAuthException
		#Thrown when the Graph API returns an OAuth-related error (type OAuthException or OAuthAccessTokenException), as shown in the example JSON snippet below.
		#{
		#  "error": {
		#    "type": "OAuthException",
		#    "message": "Invalid access token signature."
		#  }
		#}
		# FacebookQueryParseException
		#Thrown when the Graph API returns an FQL query parsing error (type QueryParseException), as shown in the example JSON snippet below.
		#
		#{
		#  "error": {
		#    "type": "QueryParseException",
		#    "message": "Unknown path components: /fizzle"
		#  }
		#}
		*/

	public abstract OBJECT get_user_profile_info(String access_token) throws WebApplicationException;

	public void process(UserApplicationContext uctx, OAuthorizationModule oauth_module, HttpServletRequest request, HttpServletResponse response) throws PersistenceException,IOException,WebApplicationException
	{
		if (oauth_module==null)
			throw new WebApplicationException("OAuthHandler requires oauth_module");

		URL parts = new URL(request.getRequestURL().toString());
		String base_uri = parts.getProtocol()+"://"+parts.getHost()+parts.getPath();
		OBJECT params = new OBJECT();
		if(request.getParameter("error") != null)//facebook sends this on cancel
		{
			response.getOutputStream().println("<script>this.close()</script>");
			return;	
		}
		
		if(request.getParameter("code") != null)
		{
			String dbtid            = request.getParameter("dbtid");
			params.put("client_id",this.get_app_id());
			params.put("client_secret", this.get_app_secret());
			params.put("code",request.getParameter("code"));
			params.put("redirect_uri",base_uri+"?dbtid="+dbtid+"&type="+this.get_type());
			Entity temp_token  = oauth_module.GET(OAuthorizationModule.TEMPUSERAUTHTOKEN_ENTITY,Long.parseLong(dbtid));//TempUserAuthToken.get_by_key_name(dbtid)
			String response_content = null;
			try{
				response_content = make_request(this.get_access_url(), params,OAuthorizationModule.METHOD_GET,null);
			}catch(HttpResponseException re)
			{
				//response.getOutputStream().println(get_auth_failed_js("Problem getting access token - code was: "+re.getStatusCode()+" "+re.getMessage()));
				response.sendRedirect(temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_REDIRECT)+"?error="+"Problem getting access token - code was: "+re.getStatusCode()+" "+re.getMessage()+((temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_USER_OBJ_JSON)!=null)?"&user_obj="+temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_USER_OBJ_JSON):""));
				oauth_module.DELETE(temp_token);
				return;
			}
			OBJECT response_dict        = OAuthorizationModule.decode_form_encoded_data(response_content);
			String access_token         = (String)response_dict.get("access_token");

			OBJECT profile = null;
			try
			{
				profile             =    this.get_user_profile_info(access_token);
			}catch(Exception e)
			{
				e.printStackTrace();
				response.sendRedirect(temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_REDIRECT)+"?error="+"LOOKUP OF USERINFO FAILED\n\n+"+e.getMessage()+((temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_USER_OBJ_JSON)!=null)?"&user_obj="+temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_USER_OBJ_JSON):""));
				oauth_module.DELETE(temp_token);
				return;
				//response.getOutputStream().println(get_auth_failed_js("LOOKUP OF USERINFO FAILED\n\n+"+e.getMessage()+""));
				//return;
			}
			//might want to find a better uid
			String username = profile.S("username");
			String uid      = profile.S("uid");
			String pic      = profile.S("pic");

			//#save auth with user

			Entity user        		   = (Entity)temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_USER);
			Entity parent		 	   = null;
			if(temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_PARENT) != null)
				parent	 = (Entity)temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_PARENT);

			//#look for existing integration point
			Entity existing_ip_for_account =  null;
			if(parent != null)
				existing_ip_for_account = oauth_module.get_connection_by_parent(parent, this.get_type(), uid);
			else
				existing_ip_for_account = oauth_module.get_connection(user, this.get_type(), uid);
			
			Entity ip = null;
			if(existing_ip_for_account == null)
			{
				oauth_module.INFO("NO EXISTING INTEGRATION POINT OF TYPE "+this.get_type()+ "CREATING ONE WITH "+access_token+" FOR USER "+user);
				ip = oauth_module.create_connection(user,parent, this.get_type(), access_token, null, null, username, uid, pic);
			}
			else
			{
				oauth_module.INFO("FOUND EXISTING INTEGRATION POINT OF TYPE "+this.get_type()+ "UPDATING WITH "+access_token+" FOR USER "+user);
				ip = existing_ip_for_account;
				ip = oauth_module.UPDATE(ip,
						OAuthorizationModule.CONNECTION_FIELD_TOKEN,access_token,
						OAuthorizationModule.CONNECTION_FIELD_USERNAME,username,
						OAuthorizationModule.CONNECTION_FIELD_PIC,pic);
			}


			oauth_module.INFO("!!REDIRECTING TO "+temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_REDIRECT));
			if(temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_USER_OBJ_JSON) != null)
				response.sendRedirect(temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_REDIRECT)+"?ip="+ip.getId()+"&user_obj="+temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_USER_OBJ_JSON));
			else
				response.sendRedirect(temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_REDIRECT)+"?ip="+ip.getId());

			oauth_module.DELETE(temp_token);
			return;

		}
		else
		{
			String return_to    = request.getParameter("r");
			oauth_module.INFO("!!!RETURN TO IS "+return_to);
			String parent_type = request.getParameter("p_type");
			String parent_id   = request.getParameter("p_id");
			String user_obj	   = request.getParameter("user_obj");
			Entity parent = null;
			if(parent_type != null && parent_id != null)
				parent = oauth_module.GET(parent_type,Long.parseLong(parent_id));
			Entity user = (Entity)uctx.getUser();
			Entity temp_token = oauth_module.create_temp_user_auth_token(user, parent, user, null, return_to,user_obj);

			params.put("client_id" ,this.get_app_id());
			if (this.get_scope(request) != null)
				params.put("scope",this.get_scope(request));

			//could do this part with get_xtra_authorize params .. see oauth 1handler. later.
			params.put("display","popup");

			params.put("redirect_uri",base_uri+"?dbtid="+temp_token.getId()+"&type="+this.get_type());

			String params_str = null;
			StringBuilder params_buf = new StringBuilder();
			try{
			for(Map.Entry<String,Object> entry: params.entrySet())
			{
				params_buf.append(URLEncoder.encode(entry.getKey(),OAuthorizationModule.ENCODING));
				params_buf.append('=');
				params_buf.append(URLEncoder.encode((String)entry.getValue(),OAuthorizationModule.ENCODING));
				params_buf.append('&');
			}
			}catch(UnsupportedEncodingException use)
			{
				use.printStackTrace();
				throw new WebApplicationException(use.getMessage());
			}
			params_buf.setLength(params_buf.length()-1);
			params_str = params_buf.toString();
			response.sendRedirect(this.get_authorize_url()+"?" +params_str);

		}
	}
}
