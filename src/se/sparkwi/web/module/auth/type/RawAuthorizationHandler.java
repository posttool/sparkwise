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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import se.sparkwi.web.module.auth.AuthorizationBaseHandler;
import se.sparkwi.web.module.auth.ConnectionApi;
import se.sparkwi.web.module.auth.OAuthorizationModule;

import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.json.JsonDecoder;


public class RawAuthorizationHandler implements AuthorizationBaseHandler
{

	@Override
	public void process(UserApplicationContext uctx, OAuthorizationModule module, HttpServletRequest request,
			HttpServletResponse response) throws PersistenceException, IOException, WebApplicationException
	{

	}



	@Override
	public ConnectionApi create_api(String access_token, String access_token_secret, OBJECT profile)
	{
		return createApi(profile);
	}
	
	public static ConnectionApi createApi(final OBJECT profile)
	{
		return new ConnectionApi()
		{
			@Override
			public OBJECT profile()
			{
				return profile;
			}

			@Override
			public OBJECT call(String path, OBJECT params, String method, OBJECT headers, boolean use_endpoint_as_base)
					throws WebApplicationException
			{
				if(OAuthorizationModule.fail_every_x_calls != 0 && ++OAuthorizationModule.current_failure_count % OAuthorizationModule.fail_every_x_calls == 0)
					throw new WebApplicationException("SIMULATED AUTH FAILURE");
				return get_json(path,params,method,headers);
			}

			@Override
			public OBJECT call(String path, OBJECT params) throws WebApplicationException
			{
				return call(path,params,"GET",null,false);
			}

			@Override
			public OBJECT call(String path) throws WebApplicationException
			{
				return call(path,null);
			}
			
//			@Override
//			public String calls(String path, OBJECT params, String method, OBJECT headers)
//					throws WebApplicationException
//			{
//				if(OAuthorizationModule.fail_every_x_calls != 0 && ++OAuthorizationModule.current_failure_count % OAuthorizationModule.fail_every_x_calls == 0)
//					throw new WebApplicationException("SIMULATED AUTH FAILURE");
//				return get_str(path,params,method,headers);
//			}
		};
	}

	public static OBJECT get_json(String url,OBJECT params,String method,OBJECT headers) throws WebApplicationException
	{
    	try
		{
			return JsonDecoder.decodeAsOBJECT(get_str(url,params,method,headers));
		} catch (Exception e)
		{
        	throw new WebApplicationException("Request Failed  - "+url+" "+e.getMessage());
		}

	}

	public static String get_str(String url,OBJECT params,String method,OBJECT headers) throws WebApplicationException
	{
		String additional_params_str = null;
		if(params != null && ! params.isEmpty())
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

		if(method.equals("GET"))
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
	            ResponseHandler<String> responseHandler = new BasicResponseHandler();
	            String responseBody = null;
	            try{
	            	return httpclient.execute(httpget, responseHandler);
	            }catch(Exception e)
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
	            ResponseHandler<String> responseHandler = new BasicResponseHandler();
	            try{
	            	return httpclient.execute(httppost, responseHandler);
	            }catch(Exception e)
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



}
