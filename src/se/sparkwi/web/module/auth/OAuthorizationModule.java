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



import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import se.sparkwi.web.module.auth.type.BingApp;
import se.sparkwi.web.module.auth.type.FacebookAuthorizationHandler;
import se.sparkwi.web.module.auth.type.FlickrAuthorizationHandler;
import se.sparkwi.web.module.auth.type.GoogleAnalyticsAuthorizationHandler;
import se.sparkwi.web.module.auth.type.GoogleSpreadsheetsAuthorizationHandler;
import se.sparkwi.web.module.auth.type.PicasaAuthorizationHandler;
import se.sparkwi.web.module.auth.type.TwitterAuthorizationHandler;
import se.sparkwi.web.module.auth.type.VimeoAuthorizationHandler;
import se.sparkwi.web.module.auth.type.YouTubeAuthorizationHandler;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.gateway.RawCommunique;
import com.pagesociety.web.json.JsonDecoder;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.permissions.PermissionEvaluator;
import com.pagesociety.web.module.user.UserModule;

public class OAuthorizationModule extends WebStoreModule
{

	public static String PARAM_API_FAIL_ON_CALLS				= "api-fail-on-calls";
	public static String PARAM_API_FAIL_EVERY_X_CALLS		    = "api-fail-every-x-calls";

	public static String PARAM_TWITTER_CONSUMER_KEY 		    = "twitter-consumer-key";
	public static String PARAM_TWITTER_CONSUMER_SECRET_KEY		= "twitter-consumer-secret-key";

	public static String PARAM_VIMEO_CONSUMER_KEY 		    	= "vimeo-consumer-key";
	public static String PARAM_VIMEO_CONSUMER_SECRET_KEY		= "vimeo-consumer-secret-key";

	public static String PARAM_GDATA_CONSUMER_KEY 		        = "gdata-consumer-key";
	public static String PARAM_GDATA_CONSUMER_SECRET_KEY		= "gdata-consumer-secret-key";
	public static String PARAM_YOUTUBE_DEVELOPER_KEY			= "youtube-developer-key";

	public static String PARAM_FACEBOOK_APP_ID 		    		= "facebook-app-id";
	public static String PARAM_FACEBOOK_SECRET_KEY				= "facebook-secret-key";

	public static String PARAM_FLICKR_CONSUMER_KEY 		        = "flickr-consumer-key";
	public static String PARAM_FLICKR_CONSUMER_SECRET_KEY		= "flickr-secret-key";

	public static String PARAM_BING_APP_ID	 		    		= "bing-app-id";

	/* this is for the api failing subsystem. api calls fail across all connection types.*/
	public static int     fail_every_x_calls				    = 0;
	public static int 	  current_failure_count					= 0;

	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
		TwitterAuthorizationHandler.init(GET_OPTIONAL_CONFIG_PARAM(PARAM_TWITTER_CONSUMER_KEY, config), GET_OPTIONAL_CONFIG_PARAM(PARAM_TWITTER_CONSUMER_SECRET_KEY, config));
		FacebookAuthorizationHandler.init(GET_OPTIONAL_CONFIG_PARAM(PARAM_FACEBOOK_APP_ID, config),GET_OPTIONAL_CONFIG_PARAM(PARAM_FACEBOOK_SECRET_KEY, config));
		GoogleAnalyticsAuthorizationHandler.init(GET_OPTIONAL_CONFIG_PARAM(PARAM_GDATA_CONSUMER_KEY, config), GET_OPTIONAL_CONFIG_PARAM(PARAM_GDATA_CONSUMER_SECRET_KEY, config));
		GoogleSpreadsheetsAuthorizationHandler.init(GET_OPTIONAL_CONFIG_PARAM(PARAM_GDATA_CONSUMER_KEY, config), GET_OPTIONAL_CONFIG_PARAM(PARAM_GDATA_CONSUMER_SECRET_KEY, config));
		PicasaAuthorizationHandler.init(GET_OPTIONAL_CONFIG_PARAM(PARAM_GDATA_CONSUMER_KEY, config), GET_OPTIONAL_CONFIG_PARAM(PARAM_GDATA_CONSUMER_SECRET_KEY, config));
		YouTubeAuthorizationHandler.init(GET_OPTIONAL_CONFIG_PARAM(PARAM_GDATA_CONSUMER_KEY, config), GET_OPTIONAL_CONFIG_PARAM(PARAM_GDATA_CONSUMER_SECRET_KEY, config), GET_OPTIONAL_CONFIG_PARAM(PARAM_YOUTUBE_DEVELOPER_KEY, config));
		VimeoAuthorizationHandler.init(GET_OPTIONAL_CONFIG_PARAM(PARAM_VIMEO_CONSUMER_KEY, config), GET_OPTIONAL_CONFIG_PARAM(PARAM_VIMEO_CONSUMER_SECRET_KEY, config));
		FlickrAuthorizationHandler.init(GET_OPTIONAL_CONFIG_PARAM(PARAM_FLICKR_CONSUMER_KEY, config), GET_OPTIONAL_CONFIG_PARAM(PARAM_FLICKR_CONSUMER_SECRET_KEY, config));
		BingApp.ID = GET_OPTIONAL_CONFIG_PARAM(PARAM_BING_APP_ID, config);
		fail_every_x_calls = GET_OPTIONAL_INT_CONFIG_PARAM(PARAM_API_FAIL_EVERY_X_CALLS, 0, config);
	}

	// create api only from existing connection data
	public static ConnectionApi apiForEntity(Entity connection) throws WebApplicationException
	{
		try{
			AuthorizationBaseHandler ah = auth_for_type((String)connection.getAttribute(CONNECTION_FIELD_TYPE));
			return ah.create_api((String)connection.getAttribute(CONNECTION_FIELD_TOKEN),
					(String)connection.getAttribute(CONNECTION_FIELD_SECRET),
						new OBJECT("username",connection.getAttribute(CONNECTION_FIELD_USERNAME),
								   "uid",connection.getAttribute(CONNECTION_FIELD_UID),
								   "pic",connection.getAttribute(CONNECTION_FIELD_PIC)));
		}catch(Exception e)
		{
			e.printStackTrace();
			throw new WebApplicationException("Problem creating api - "+e.getMessage());
		}
	}

	private static AuthorizationBaseHandler auth_for_type(String type) throws WebApplicationException
	{
		String classname = OAuthorizationModule.class.getPackage().getName()+".type."+type+"AuthorizationHandler";
		try{
			return ( AuthorizationBaseHandler) Class.forName(classname).newInstance();
		}catch(Exception e)
		{
			e.printStackTrace();
			throw new WebApplicationException("Problem creating api - "+e.getMessage());
		}
	}


	@Export
	public void DoAuthorization(UserApplicationContext uctx,RawCommunique c) throws WebApplicationException
	{
		HttpServletRequest  request  = (HttpServletRequest)c.getRequest();
		HttpServletResponse response = (HttpServletResponse)c.getResponse();
		String type = request.getParameter("type");
		if(!PermissionEvaluator.IS_LOGGED_IN((Entity)uctx.getUser()))
			throw new WebApplicationException("NO PERMISSION");
		try{
			AuthorizationBaseHandler ah = auth_for_type(type);
			ah.process(uctx, this, (HttpServletRequest)c.getRequest(), (HttpServletResponse)c.getResponse());
		}
		catch(Exception e)
		{
				ERROR(e);
				try{response.getOutputStream().println("There was a problem starting the auth process. See logs. - "+e.getMessage());}catch(Exception ee){ERROR(ee);}
		}

	}

	public Entity createRawConnection(Entity project) throws WebApplicationException, PersistenceException
	{
		return create_connection((Entity)project.getAttribute(FIELD_CREATOR), project, ConnectionApi.CONNECTION_TYPE_RAW,
				null, null, null, null, null, null);
	}

	protected Entity create_connection(Entity creator,Entity parent,String type,String token,String secret,String scopes,String username,String uid,String pic) throws WebApplicationException,PersistenceException
	{
		Entity connection =	NEW(CONNECTION_ENTITY,
	    						creator,
	    						CONNECTION_FIELD_PARENT,parent,
		 						CONNECTION_FIELD_TYPE,type,
		 						CONNECTION_FIELD_TOKEN,token,
		 						CONNECTION_FIELD_SECRET,secret,
		 						CONNECTION_FIELD_SCOPES,scopes,
		 						CONNECTION_FIELD_USERNAME,username,
		 						CONNECTION_FIELD_UID,uid,
		 						CONNECTION_FIELD_PIC,pic
		);
		return connection;
	}

	protected Entity create_temp_user_auth_token(Entity creator,Entity parent,Entity user,String secret,String redirect,String user_obj) throws WebApplicationException,PersistenceException
	{
		Entity tempuserauthtoken =	NEW(TEMPUSERAUTHTOKEN_ENTITY,
	    						creator,
	    						TEMPUSERAUTHTOKEN_FIELD_PARENT,parent,
		 						TEMPUSERAUTHTOKEN_FIELD_USER,user,
		 						TEMPUSERAUTHTOKEN_FIELD_SECRET,secret,
		 						TEMPUSERAUTHTOKEN_FIELD_REDIRECT,redirect,
		 						TEMPUSERAUTHTOKEN_FIELD_USER_OBJ_JSON,user_obj
		);
		return tempuserauthtoken;
	}


	public Entity get_connection(Entity user,Object type,Object uid) throws PersistenceException
	{
		Query q = new Query(CONNECTION_ENTITY);
		q.idx(IDX_CONNECTION_BY_USER_BY_TYPE_BY_UID);
		q.eq(Query.l(user,type,uid));
		QueryResult qr = QUERY(q);
		if(qr.size() == 0)
			return null;
		else
			return qr.getEntities().get(0);
	}
	
	public Entity get_connection_by_parent(Entity user,Object type,Object uid) throws PersistenceException
	{
		Query q = new Query(CONNECTION_ENTITY);
		q.idx(IDX_CONNECTION_BY_PARENT_BY_TYPE_BY_UID);
		q.eq(Query.l(user,type,uid));
		QueryResult qr = QUERY(q);
		if(qr.size() == 0)
			return null;
		else
			return qr.getEntities().get(0);
	}

	public List<Entity> get_connections(Entity user) throws PersistenceException
	{
		Query q = new Query(CONNECTION_ENTITY);
		q.idx(IDX_CONNECTION_BY_USER_BY_TYPE_BY_UID);
		q.eq(Query.l(user,Query.VAL_GLOB,Query.VAL_GLOB));
		QueryResult qr = QUERY(q);
		return qr.getEntities();
	}

	public List<Entity> get_connections(Entity user, String type) throws PersistenceException
	{
		Query q = new Query(CONNECTION_ENTITY);
		q.idx(IDX_CONNECTION_BY_USER_BY_TYPE_BY_UID);
		q.eq(Query.l(user,type,Query.VAL_GLOB));
		QueryResult qr = QUERY(q);
		return qr.getEntities();
	}
	
	public List<Entity> get_connections_by_parent(Entity parent) throws PersistenceException
	{
		Query q = new Query(CONNECTION_ENTITY);
		q.idx(IDX_CONNECTION_BY_PARENT_BY_TYPE_BY_UID);
		q.eq(Query.l(parent,Query.VAL_GLOB,Query.VAL_GLOB));
		QueryResult qr = QUERY(q);
		return qr.getEntities();
	}

	public List<Entity> get_connections_by_parent(Entity parent, String type) throws PersistenceException
	{
		Query q = new Query(CONNECTION_ENTITY);
		q.idx(IDX_CONNECTION_BY_PARENT_BY_TYPE_BY_UID);
		q.eq(Query.l(parent,type,Query.VAL_GLOB));
		QueryResult qr = QUERY(q);
		return qr.getEntities();
	}



	public static String CONNECTION_ENTITY 			= "OAuthConnection";
	public static String CONNECTION_FIELD_PARENT    = "parent";
	public static String CONNECTION_FIELD_TYPE 		= "type";
	public static String CONNECTION_FIELD_TOKEN 	= "token";
	public static String CONNECTION_FIELD_SECRET 	= "secret";
	public static String CONNECTION_FIELD_SCOPES 	= "scopes";
	public static String CONNECTION_FIELD_USERNAME 	= "username";
	public static String CONNECTION_FIELD_UID 		= "uid";
	public static String CONNECTION_FIELD_PIC 		= "pic";

	public static String TEMPUSERAUTHTOKEN_ENTITY 			 = "TempUserAuthToken";
	public static String TEMPUSERAUTHTOKEN_FIELD_PARENT 	 = "parent";
	public static String TEMPUSERAUTHTOKEN_FIELD_USER 		 = "user";
	public static String TEMPUSERAUTHTOKEN_FIELD_SECRET 	 = "secret";
	public static String TEMPUSERAUTHTOKEN_FIELD_REDIRECT 	 = "redirect";
	public static String TEMPUSERAUTHTOKEN_FIELD_USER_OBJ_JSON = "user_obj";

	protected void defineEntities(Map<String,Object> config) throws PersistenceException, InitializationException
	{
		DEFINE_ENTITY(CONNECTION_ENTITY,
				CONNECTION_FIELD_PARENT,Types.TYPE_REFERENCE,FieldDefinition.REF_TYPE_UNTYPED_ENTITY,null,
				CONNECTION_FIELD_TYPE,Types.TYPE_STRING,null,
				CONNECTION_FIELD_TOKEN,Types.TYPE_STRING,null,
				CONNECTION_FIELD_SECRET,Types.TYPE_STRING,null,
				CONNECTION_FIELD_SCOPES,Types.TYPE_ARRAY|Types.TYPE_STRING,null,
				CONNECTION_FIELD_USERNAME,Types.TYPE_STRING,null,
				CONNECTION_FIELD_UID,Types.TYPE_STRING,null,
				CONNECTION_FIELD_PIC,Types.TYPE_STRING,null);

		DEFINE_ENTITY(TEMPUSERAUTHTOKEN_ENTITY,
				TEMPUSERAUTHTOKEN_FIELD_PARENT,Types.TYPE_REFERENCE,FieldDefinition.REF_TYPE_UNTYPED_ENTITY,null,
				TEMPUSERAUTHTOKEN_FIELD_USER,Types.TYPE_REFERENCE,UserModule.USER_ENTITY,null,
				TEMPUSERAUTHTOKEN_FIELD_SECRET,Types.TYPE_STRING,null,
				TEMPUSERAUTHTOKEN_FIELD_REDIRECT,Types.TYPE_STRING,null,
				TEMPUSERAUTHTOKEN_FIELD_USER_OBJ_JSON,Types.TYPE_STRING,null
				);
	}


	public static final String IDX_CONNECTION_BY_USER_BY_TYPE_BY_UID     = "byUserByTypeByUid";
	public static final String IDX_CONNECTION_BY_PARENT_BY_TYPE_BY_UID 	 = "byParentByTypeByUid";
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException, InitializationException
	{
		DEFINE_ENTITY_INDICES(CONNECTION_ENTITY,
				ENTITY_INDEX(IDX_CONNECTION_BY_USER_BY_TYPE_BY_UID, EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_CREATOR,CONNECTION_FIELD_TYPE,CONNECTION_FIELD_UID),
				ENTITY_INDEX(IDX_CONNECTION_BY_PARENT_BY_TYPE_BY_UID, EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, CONNECTION_FIELD_PARENT,CONNECTION_FIELD_TYPE,CONNECTION_FIELD_UID)
				);
	}

	/* OAuth1BaseHandler */
	public static final String ENCODING = "UTF-8";
	public static final String METHOD_GET = "GET";
	public static final String METHOD_POST = "POST";
	public String getRelativeAuthorizationURL(String connection_type,String[] scopes,String parent_type,Long parent_id,String return_to_url,OBJECT user_obj)
	{

		String scopes_str = null;
		if(scopes != null && scopes.length > 0)
		{
			StringBuilder buf = new StringBuilder();
			for(int i = 0;i < scopes.length;i++)
			{
				buf.append(scopes[i]);
				buf.append(',');
			}
			if (buf.length()!=0)
				buf.setLength(buf.length()-1);
			scopes_str = buf.toString();
		}

		String user_obj_json = null;
		if(user_obj != null)
			try{user_obj_json = URLEncoder.encode(user_obj.toJSON(),ENCODING);}catch(UnsupportedEncodingException uee){ERROR(uee);}

		String redirect_url = "/"+getName()+"/DoAuthorization/.raw?type="+connection_type+"&r="+return_to_url;
		if(user_obj_json != null)
			redirect_url = redirect_url+"&user_obj="+user_obj_json;
		if(parent_type != null)
			redirect_url = redirect_url+"&p_type="+parent_type+"&p_id="+parent_id;

		if(scopes_str != null)
			redirect_url = redirect_url+"&scopes="+scopes_str;
		return redirect_url;
	}

	public OBJECT decodeAuthorizationResponse(HttpServletRequest request) throws PersistenceException,WebApplicationException
	{
		try{
			OBJECT ret = new OBJECT();
			if(request.getParameter("user_obj") != null)
			{
				OBJECT user_object = JsonDecoder.decodeAsOBJECT(URLDecoder.decode(request.getParameter("user_obj"),ENCODING));
				ret.put("user_object",user_object);
			}
			if(request.getParameter("error") != null)
				ret.put("error",request.getParameter("error"));
			else
				ret.put("connection", GET(CONNECTION_ENTITY,Long.parseLong(request.getParameter("ip"))));
			return ret;
		}catch(Exception uee)
		{
			ERROR(uee);
			return null;
		}
	}
	//!!
	public static String get_auth_complete_js(Entity connection)
	{
		//ip.secret = None
		//ip.token = None
		//return "<script>window.opener.authorization_complete("+platform.util.entity_to_json(ip)+");this.close()</script>"
		return "auth_complete "+connection.getAttribute(CONNECTION_FIELD_SECRET)+" "+connection.getAttribute(CONNECTION_FIELD_TOKEN);
	}

	//!!
	public static String get_auth_failed_js(String err)
	{
		//return "<script>window.opener.authorization_failed('"+urllib.quote(err)+"');this.close()</script>"
		return err;
	}


	public  static  String oauth_quote(String s) {
        if (s == null) {
            return "";
        }
        try {
            return URLEncoder.encode(s, ENCODING)
                    // OAuth encodes some characters differently:
                    .replace("+", "%20").replace("*", "%2A")
                    .replace("%7E", "~");
            // This could be done faster with more hand-crafted code.
        } catch (UnsupportedEncodingException wow) {
            throw new RuntimeException(wow.getMessage(), wow);
        }
    }

    public  static String oauth_unquote(String s) {
        try {
            return URLDecoder.decode(s, ENCODING);
            // This implements http://oauth.pbwiki.com/FlexibleDecoding
        } catch (java.io.UnsupportedEncodingException wow) {
            throw new RuntimeException(wow.getMessage(), wow);
        }
    }


    public  static OBJECT decode_form_encoded_data(String data)
    {
        OBJECT ret = new OBJECT();
        if (data!= null) {
            for (String nvp : data.split("\\&")) {
                int equals = nvp.indexOf('=');
                String name;
                String value;
                if (equals < 0) {
                    name = oauth_unquote(nvp);
                    value = null;
                } else {
                    name = oauth_unquote(nvp.substring(0, equals));
                    value = oauth_unquote(nvp.substring(equals + 1));
                }
               ret.put(name,value);
            }
        }
        return ret;
    }


}

