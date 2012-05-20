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
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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
import com.pagesociety.util.Base64;
import com.pagesociety.util.CALLBACK;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.json.JsonDecoder;
import com.pagesociety.web.module.util.Util;

public abstract class OAuth1BaseHandler implements AuthorizationBaseHandler
{

	public OAuth1BaseHandler()
	{
	}

	class api implements ConnectionApi
	{
		String api_endpoint;
		OBJECT api_call_style;
		OBJECT api_endpoint_json_config;
		OBJECT api_endpoint_headers;
		OBJECT api_endpoint_params;
		CALLBACK api_endpoint_error_check_f;
		String consumer_key;
		String consumer_secret;
		String access_token;
		String access_token_secret;
		OBJECT profile;

		public api(String api_endpoint,OBJECT api_call_style,OBJECT api_endpoint_json_config,OBJECT api_endpoint_headers,OBJECT api_endpoint_params,CALLBACK api_endpoint_error_check_f,String consumer_key,String consumer_secret,String access_token,String access_token_secret,OBJECT profile)
		{
			this.api_endpoint                = api_endpoint;
			this.api_call_style              = api_call_style;
			this.api_endpoint_json_config    = api_endpoint_json_config;
			this.api_endpoint_headers        = api_endpoint_headers;
			this.api_endpoint_params         = api_endpoint_params;
			this.api_endpoint_error_check_f  = api_endpoint_error_check_f;
			this.consumer_key                = consumer_key;
			this.consumer_secret             = consumer_secret;
			this.access_token                = access_token;
			this.access_token_secret         = access_token_secret;
			this.profile					 = profile;
		}

		public OBJECT profile()
		{
			return this.profile;
		}

		public OBJECT call(String path_or_method) throws WebApplicationException
		{
			return call(path_or_method,new OBJECT());
		}

		public OBJECT call(String path_or_method,OBJECT params) throws WebApplicationException
		{
			return call(path_or_method,params,OAuthorizationModule.METHOD_GET,null,true);
		}

		public OBJECT call(String path_or_method,OBJECT params,String method,OBJECT headers,boolean use_endpoint_as_base) throws WebApplicationException
		{
			if(OAuthorizationModule.fail_every_x_calls != 0  && ++OAuthorizationModule.current_failure_count % OAuthorizationModule.fail_every_x_calls == 0)
				throw new WebApplicationException("SIMULATED AUTH FAILURE");

			String response = null;
			try{
				 response = this.do_call(path_or_method,params,method,headers,use_endpoint_as_base);

			}catch(HttpResponseException re)
			{
				int status_code = re.getStatusCode();
				System.err.println("!!!API path= "+this.api_endpoint+path_or_method);
				System.err.println("!!!API response_code was "+status_code);
				throw new WebApplicationException("Call to "+this.api_endpoint +" method: "+ path_or_method+" with params "+params.toString()+" failed. Status code was "+status_code,status_code);
			}
			catch(WebApplicationException wae)
			{
				throw wae;
			}
			//convert to json//
			try{
				OBJECT response_dict = JsonDecoder.decodeAsOBJECT(response);
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
			}catch(Exception jse)
			{
				throw new WebApplicationException("JSON Decode Failed: "+this.api_endpoint +" method: "+ path_or_method+" with params "+params.toString()+"\nresponse was "+response);
			}

		}

		
		public String calls(String path_or_method,OBJECT params,String method,OBJECT headers) throws WebApplicationException
		{
			throw new WebApplicationException("OAuth1 connections only support JSON");
		}
		
		private String do_call(String path_or_method,OBJECT params,String method,OBJECT headers,boolean use_endpoint_as_base) throws WebApplicationException,HttpResponseException
		{
			String call_url = this.api_endpoint;
			if (use_endpoint_as_base)
				if (this.api_call_style.get("type").equals("path"))
					call_url = call_url + path_or_method;


			//if method name is passed as a param instead as part of path
			if(this.api_call_style.get("type").equals("param"))
			{
				if(params == null)
					params = new OBJECT();
				params.put((String)this.api_call_style.get("param_name"), path_or_method);
			}

			//config request for json
			if (this.api_endpoint_json_config.get("type").equals("param"))
				params.put((String)this.api_endpoint_json_config.get("param_name"),this.api_endpoint_json_config.get("param_value"));
			else if (this.api_endpoint_json_config.get("type").equals("extension"))
				call_url = call_url +"."+ this.api_endpoint_json_config.get("extension");

			//add additional headers
			if (this.api_endpoint_headers != null)
			{
				if(headers == null)
					headers = new OBJECT();
				headers.putAll(this.api_endpoint_headers);
			}
			//add additional params
			if (this.api_endpoint_params != null)
			{
				if(params == null)
					params = new OBJECT();
				params.putAll(this.api_endpoint_params);
			}

			return make_oauth1_request(call_url, this.consumer_key, this.consumer_secret, this.access_token_secret, new OBJECT("oauth_token",this.access_token), params, method, headers);
		}
	}

	public abstract String get_type();
	public abstract String get_consumer_key();
	public abstract String get_consumer_secret();
	public abstract String get_request_url();
	public abstract String get_authorize_url();

	public OBJECT get_xtra_authorize_params()
	{
		return null;//not abstract
	}
	public abstract String get_access_url();
	public abstract OBJECT get_user_profile_info(String access_token,String access_token_secret) throws WebApplicationException;


	public String get_preferred_auth_flow_method()
	{
		return OAuthorizationModule.METHOD_POST;
	}

	public String get_scope(HttpServletRequest request)
	{
		return request.getParameter("scopes");
	}

	//api metadata
	public abstract String get_api_call_endpoint();

	public OBJECT get_api_call_style()
	{
		return new OBJECT("type","path");
	}

	public OBJECT get_api_call_json_config()
	{
		return new OBJECT("type","param",
						  "param_name","format",
						  "param_value","json");
	}

	public OBJECT get_api_call_headers()
	{
		return null;
	}

	public OBJECT get_api_call_params()
	{
		return null;
	}

	public CALLBACK get_api_call_check_error_on_200_f()
	{
		//see Vimeo for an example
		return null;
	}

	public CALLBACK get_api_call_translate_non_200_response_f()
	{
		//see Twitter for an example
		return null;
	}

	public ConnectionApi create_api(String access_token,String access_token_secret,OBJECT profile)
	{
		ConnectionApi api =  new api(this.get_api_call_endpoint(),this.get_api_call_style(),this.get_api_call_json_config(),this.get_api_call_headers(),this.get_api_call_params(),this.get_api_call_check_error_on_200_f(),this.get_consumer_key(),this.get_consumer_secret(),access_token,access_token_secret,profile);
		return api;
	}

	public void process(UserApplicationContext uctx, OAuthorizationModule oauth_module, HttpServletRequest request, HttpServletResponse response) throws PersistenceException,IOException,WebApplicationException
	{
		if (oauth_module==null)
			throw new WebApplicationException("OAuthHandler requires oauth_module");

		if (request.getParameter("oauth_verifier") != null)
		{
			String oauth_verifier      = request.getParameter("oauth_verifier");
			String oauth_token         = request.getParameter("oauth_token");
			String dbtid               = request.getParameter("dbtid");
			Entity temp_token          = oauth_module.GET(OAuthorizationModule.TEMPUSERAUTHTOKEN_ENTITY,Long.parseLong(dbtid));//TempUserAuthToken.get_by_key_name(dbtid)
			String response_body = null;
			try
			{
				response_body            = make_oauth1_request(this.get_access_url(),this.get_consumer_key(),this.get_consumer_secret(),(String)temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_SECRET),new OBJECT("oauth_token",oauth_token,"oauth_verifier",oauth_verifier),null,this.get_preferred_auth_flow_method(),null);
			}
			catch(HttpResponseException re)
			{
				//response.getOutputStream().println(get_auth_failed_js("Problem getting access token - response code was "+re.getStatusCode()+" - "+re.getMessage()));
				response.sendRedirect(temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_REDIRECT)+"?error="+"Problem getting access token - response code was "+re.getStatusCode()+" - "+re.getMessage()+((temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_USER_OBJ_JSON)!=null)?"&user_obj="+temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_USER_OBJ_JSON):""));
				oauth_module.DELETE(temp_token);
				return;
			}
			catch( Exception e)
			{
				response.sendRedirect(temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_REDIRECT)+"?error="+"Problem getting access token "+e.getMessage()+((temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_USER_OBJ_JSON)!=null)?"&user_obj="+temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_USER_OBJ_JSON):""));
				oauth_module.DELETE(temp_token);
				return;
				//response.getOutputStream().println(get_auth_failed_js("Problem getting access token "+e.getMessage()));
				//return;
			}

			OBJECT response_dict        = OAuthorizationModule.decode_form_encoded_data(response_body);
			String	access_token        = response_dict.S("oauth_token");
			String	access_token_secret = response_dict.S("oauth_token_secret");
			OBJECT profile = null;
			try
			{
				profile             =    this.get_user_profile_info(access_token, access_token_secret);
			}catch(Exception e)
			{
				e.printStackTrace();
				response.sendRedirect(temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_REDIRECT)+"?error="+"Lookup of user info failed:\n\n+"+e.getMessage()+((temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_USER_OBJ_JSON)!=null)?"&user_obj="+temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_USER_OBJ_JSON):""));
				oauth_module.DELETE(temp_token);
				return;
				//response.getOutputStream().println(get_auth_failed_js("LOOKUP OF USERINFO FAILED\n\n+"+e.getMessage()+""));
				//return;
			}
				//might want to find a better uid
			String username = profile.S("username");
			String uid      = profile.S("uid");
			String pic      = profile.S("pic");
			//save auth with user
			Entity user 	= (Entity)temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_USER);

			Entity parent		 = null;
			if (temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_PARENT) != null)
				parent	 = (Entity)temp_token.getAttribute(OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_PARENT);
			oauth_module.INFO("CALLING USER OBJECT IS "+user);
			oauth_module.INFO("USERNAME IS "+username);
			oauth_module.INFO("UID IS "+uid);
			oauth_module.INFO("PIC IS "+pic);

			//#look for existing integration point
			Entity existing_ip_for_account = null;
			if(parent != null)
				existing_ip_for_account = oauth_module.get_connection_by_parent(parent, this.get_type(), uid);
			else
				existing_ip_for_account = oauth_module.get_connection(user, this.get_type(), uid);
			if (existing_ip_for_account != null)
			{
				String existing_username = (String)existing_ip_for_account.getAttribute(OAuthorizationModule.CONNECTION_FIELD_USERNAME);
				if (!username.equals(existing_username))
					existing_ip_for_account = null;
			}
			Entity ip = null;
			if(existing_ip_for_account == null)
			{
				oauth_module.INFO("NO EXISTING INTEGRATION POINT OF TYPE "+this.get_type()+ "CREATING ONE WITH "+access_token+" secret:"+access_token_secret+" FOR USER "+user);
				ip = oauth_module.create_connection(user,parent, this.get_type(), access_token, access_token_secret, null, username, uid, pic);
			}
			else
			{
				oauth_module.INFO("FOUND EXISTING INTEGRATION POINT OF TYPE "+this.get_type()+ "UPDATING WITH "+access_token+" secret:"+access_token_secret+" FOR USER "+user);
				ip              =   existing_ip_for_account;
				ip = oauth_module.UPDATE(ip,
						OAuthorizationModule.CONNECTION_FIELD_TOKEN,access_token,
						OAuthorizationModule.CONNECTION_FIELD_SECRET,access_token_secret,
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
			URL parts = new URL(request.getRequestURL().toString());
			String redirect_uri = parts.getProtocol()+"://"+parts.getHost()+parts.getPath();

			String return_to    = request.getParameter("r");
			String parent_type 	= request.getParameter("p_type");
			String parent_id   	= request.getParameter("p_id");
			String user_obj   	= request.getParameter("user_obj");
			Entity parent = null;
			if(parent_type != null && parent_id != null)
				parent = oauth_module.GET(parent_type,Long.parseLong(parent_id));
			Entity user = (Entity)uctx.getUser();
			Entity temp_token = oauth_module.create_temp_user_auth_token(user, parent, user, null, return_to,user_obj);
			redirect_uri = redirect_uri + "?dbtid="+temp_token.getId()+"&type="+this.get_type();

			String scope = this.get_scope(request);

			OBJECT additional_params = null;
			if(scope != null)
				additional_params = new OBJECT("scope",scope);

			try{
				String response_body 	  = make_oauth1_request(this.get_request_url(),this.get_consumer_key(),this.get_consumer_secret(), "", new OBJECT("oauth_callback",redirect_uri),additional_params,this.get_preferred_auth_flow_method(),null);
				OBJECT response_dict 	  = OAuthorizationModule.decode_form_encoded_data(response_body);
				String oauth_token  	  = response_dict.S("oauth_token");
				String oauth_token_secret = response_dict.S("oauth_token_secret");

				oauth_module.UPDATE(temp_token,OAuthorizationModule.TEMPUSERAUTHTOKEN_FIELD_SECRET,oauth_token_secret);

				String authorize_qs = "?oauth_token="+oauth_token;
				if(this.get_xtra_authorize_params() != null)
				{
					for(Map.Entry<String,Object> entry: this.get_xtra_authorize_params().entrySet())
						authorize_qs = authorize_qs +"&"+entry.getKey()+"="+(String)entry.getValue();
				}
				response.sendRedirect(this.get_authorize_url()+authorize_qs);
			}
			catch(HttpResponseException re)
			{
				//response.getOutputStream().println(get_auth_failed_js("Problem getting access token: code was "+re.getStatusCode()+" - "+re.getMessage()+"</pre>"));

				response.sendRedirect(return_to+"?error="+re.getMessage()+((user_obj!=null)?"&user_obj="+user_obj:""));
			}
			catch(Exception e)
			{
				e.printStackTrace();

			}

		}
	}



	protected String make_oauth1_request(String url,String consumer_key,String consumer_secret,String secret,OBJECT oauth_params, OBJECT additional_params, String method,OBJECT headers) throws WebApplicationException,HttpResponseException
	{
		if (headers == null)
			headers = new OBJECT();

		oauth_params.put("oauth_consumer_key",consumer_key);
		oauth_params.put("oauth_signature_method","HMAC-SHA1");
		oauth_params.put("oauth_version","1.0");

		if(!oauth_params.containsKey("oauth_timestamp"))
			oauth_params.put("oauth_timestamp",String.valueOf(new Date().getTime()/1000));//very important. seconds since the epoch not ms!!!!
		if (!oauth_params.containsKey("oauth_nonce"))
			oauth_params.put("oauth_nonce",generate_nonce());

		/* we use a tree map so that the paramter keys are sorted in natural string order ascending */
		/* this is the order that oauth wants them in */
		Map<String,Object> all_params = new TreeMap<String,Object>(oauth_params);
		if(additional_params != null)
			all_params.putAll(additional_params);

		Iterator<String> keys = all_params.keySet().iterator();
		StringBuilder params_buf = new StringBuilder();
		while(keys.hasNext())
		{
			String key = keys.next();
			String val = (String)all_params.get(key);
			params_buf.append(OAuthorizationModule.oauth_quote(key));
			params_buf.append('=');
			params_buf.append(OAuthorizationModule.oauth_quote(val));
			params_buf.append('&');
		}
		//remove trailng ampersand
		params_buf.setLength(params_buf.length()-1);
		String params_str = params_buf.toString();


		//# Join the entire message together per the OAuth specification.
		String signature_base_string = method+"&"+OAuthorizationModule.oauth_quote(url)+"&"+OAuthorizationModule.oauth_quote(params_str);

//		("!!!BASE STRING IS "+signature_base_string);

		//# Create a HMAC-SHA1 signature of the message.
		String key = OAuthorizationModule.oauth_quote(consumer_secret)+"&"+OAuthorizationModule.oauth_quote(secret);

		byte[] signature = hmac_sha1(signature_base_string, key);
		String digest_base64 = to_base_64_string(signature);
		System.out.println("!!! signature is "+digest_base64);


		oauth_params.put("oauth_signature",digest_base64);
		StringBuilder auth_header_buf = new StringBuilder("OAuth ");
		for(Map.Entry<String,Object> entry: oauth_params.entrySet())
		{
			auth_header_buf.append(OAuthorizationModule.oauth_quote(entry.getKey()));
			auth_header_buf.append('=');
			auth_header_buf.append('"');
			auth_header_buf.append(OAuthorizationModule.oauth_quote((String)entry.getValue()));
			auth_header_buf.append('"');
			auth_header_buf.append(',');
			auth_header_buf.append(' ');
		}
		auth_header_buf.setLength(auth_header_buf.length()-2); //remove last ', '
		String auth_header = auth_header_buf.toString();

		headers.put("Authorization",auth_header);
//		("!!!AUTH HEADER IS "+auth_header);
//		("!!!URL IS "+url);

		String additional_params_str = null;
		if(additional_params != null && ! additional_params.isEmpty())
		{
			StringBuilder additional_params_buf = new StringBuilder();
			try{
			for(Map.Entry<String,Object> entry: additional_params.entrySet())
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


		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, 15000);
		HttpConnectionParams.setSoTimeout(params, 15000);


		HttpClient httpclient = new DefaultHttpClient(params);

		if(method.equals(OAuthorizationModule.METHOD_GET))
		{
			if(additional_params_str != null)
				url = url+"?"+additional_params_str;


	        try {
	        	HttpGet httpget = new HttpGet(url);
				for(Map.Entry<String,Object> entry: headers.entrySet())
					httpget.addHeader(entry.getKey(),(String) entry.getValue());

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
	            catch(HttpResponseException hre)
	            {
	            	throw hre;
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
				for(Map.Entry<String,Object> entry: headers.entrySet())
				{
					httppost.addHeader(entry.getKey(),(String) entry.getValue());
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
	            catch(HttpResponseException hre)
	            {
	            	throw hre;
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






	private  String generate_nonce()
	{
			StringBuilder rr = new StringBuilder();
			for(int i = 0;i < 40;i++)
			{
				rr.append(OAuthorizationModule.RANDOM(0,9));
			}
			return md5(String.valueOf(new Date().getTime())+rr.toString());
	}

	private  String md5(String data)
	{
		return Util.stringToHexEncodedMD5(data);
		//return DigestUtils.md5Hex(data);
	}

	private  String to_base_64_string(byte[] b)
	{
		return Base64.encodeBytes(b).trim();
	}

	private  byte[] hmac_sha1(String baseString, String keyString) throws WebApplicationException
	{
		try{
		    SecretKey secretKey = null;
		    byte[] keyBytes = keyString.getBytes(OAuthorizationModule.ENCODING);
		    secretKey = new SecretKeySpec(keyBytes, "HmacSHA1");
		    Mac mac = Mac.getInstance("HmacSHA1");
		    mac.init(secretKey);
		    byte[] text = baseString.getBytes(OAuthorizationModule.ENCODING);
		    return mac.doFinal(text);
		}catch(Exception e)
		{
			e.printStackTrace();
			throw new WebApplicationException(e.getMessage());
		}
	}



}
