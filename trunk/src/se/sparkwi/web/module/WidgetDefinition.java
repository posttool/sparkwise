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


import static com.pagesociety.web.module.WebStoreModule.EMPTY_STRING_ARRAY;
import static se.sparkwi.web.module.DashboardModule.WIDGETDATAPROXY_FIELD_CONNECTION;
import static se.sparkwi.web.module.DashboardModule.WIDGETDATAPROXY_FIELD_PROPS;
import static se.sparkwi.web.module.DashboardModule.WIDGETDATAPROXY_FIELD_SELECTOR_VALUES;
import static se.sparkwi.web.module.DashboardModule.WIDGETINSTANCE_FIELD_PROPS;
import se.sparkwi.web.module.auth.ConnectionApi;
import se.sparkwi.web.module.auth.OAuthorizationModule;

import com.pagesociety.persistence.Entity;
import com.pagesociety.util.ARRAY;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.exception.WebApplicationException;

public abstract class WidgetDefinition
{
	
	public static final int SELECTOR_TYPE_SELECT                = 0x00;
	public static final int SELECTOR_TYPE_FREE_TEXT         	= 0x10;
	
	public static final String SERVICE_SELECTOR_NAME               = "name";
	public static final String SERVICE_SELECTOR_LABEL              = "label";
	public static final String SERVICE_SELECTOR_TYPE               = "type"; //select or free text
	public static final String SERVICE_SELECTOR_INPUT_BEHAVIOR     = "input_behavior";//force prepend... match regexp...
	public static final String SERVICE_SELECTOR_INPUT_BEHAVIOR_VAL = "input_behavior_val";
	public static final String SERVICE_SELECTOR_OPTIONAL	       = "optional";
	//TODO extend selector framework 
	//1. to specify dependencies... ie selecting a picture is dependent on the album
	//2. to specify default values... (for example, if there is only 1 value, choose it)
	//3. to specify that a set of selector props all have the same possible vals (like a spreadsheet column name)
	
	public static final int RETURN_TYPE_TEXT                    = 0x00;
	public static final int RETURN_TYPE_NUMBER                  = 0x10;
	public static final int RETURN_TYPE_ANNOTATED_NUMBER        = 0x11;
	public static final int RETURN_TYPE_GEODATA                 = 0x20;
	public static final int RETURN_TYPE_DICT                    = 0x30;
	public static final int RETURN_TYPE_SLIDES                  = 0x40;
	public static final int[] RETURN_TYPE_VOID           		= new int[]{ };
	public static final int[] RETURN_TYPES_TEXT           		= new int[]{ RETURN_TYPE_TEXT };
	public static final int[] RETURN_TYPES_NUMBER         		= new int[]{ RETURN_TYPE_NUMBER };
	public static final int[] RETURN_TYPES_DICT         		= new int[]{ RETURN_TYPE_DICT };
	public static final int[] RETURN_TYPES_ANNOTATED_NUMBER     = new int[]{ RETURN_TYPE_NUMBER, RETURN_TYPE_ANNOTATED_NUMBER };
	public static final int[] RETURN_TYPES_SLIDES         		= new int[]{ RETURN_TYPE_SLIDES };
	

	public static WidgetDefinition create(Entity proxy, Entity def_meta_info) throws WebApplicationException
	{
		WidgetDefinition def = forEntity(def_meta_info);
		def.setMetaInfo(proxy);
		return def;
	}

	public static WidgetDefinition forEntity(Entity def_meta_info) throws WebApplicationException
	{
		return forName((String) def_meta_info.getAttribute(DashboardModule.WIDGET_FIELD_CLASS_NAME));
	}

	public static WidgetDefinition forName(String className) throws WebApplicationException
	{
		if (className==null)
			return null;
		try
		{
			WidgetDefinition wd = (WidgetDefinition)Class.forName(className).newInstance();
			return (WidgetDefinition) wd;
		} catch (Exception e)
		{
			e.printStackTrace();
			throw new WebApplicationException("Cannot create widget def: "+className,e);
		}
	}


	/* protected methods should be overloaded */

	protected OBJECT get_data(ConnectionApi api,OBJECT selector_values, OBJECT props) throws WebApplicationException
	{
		throw new WebApplicationException("Error- superclass must override get_data or return void");
	}

	protected String get_name()
	{
		return getClass().getName();
	}
	
	protected String get_required_connection()
	{
		return ConnectionApi.CONNECTION_TYPE_NONE;
	}

	
	protected String[] get_scopes()
	{
		return new String[]{};
	}

	protected int[] get_return_types()
	{
		return RETURN_TYPES_NUMBER;
	}


	protected OBJECT get_default_props()
	{
		if(returnsAnnotation())
			return new OBJECT("display-style", "feed");
		if(returnsNumber())
			return new OBJECT("display-style", "total");
		else if(returnsDict())
			return new OBJECT("display-style", "pie");
		else if(returnsSlides())
			return new OBJECT("display-style", "slides");
		else
			return new OBJECT();
	}
	
	protected String[] get_required_props()
	{
		return EMPTY_STRING_ARRAY;
	}

	protected OBJECT[] get_service_selector_info()
	{
		return new OBJECT[]{};
	}

	protected String[] get_service_selector_names()
	{
		OBJECT[] sel_info = get_service_selector_info();
		String[] ret = new String[sel_info.length];
		for(int i = 0;i < sel_info.length;i++)
		{
			ret[i] = (String)sel_info[i].get(SERVICE_SELECTOR_NAME);
		}
		return ret;
	}

	protected int[] get_service_selector_types()
	{
		OBJECT[] sel_info = get_service_selector_info();
		int[] ret = new int[sel_info.length];
		for(int i = 0;i < sel_info.length;i++)
		{
			ret[i] = (Integer)sel_info[i].get(SERVICE_SELECTOR_TYPE);
		}
		return ret;	
	}

	protected OBJECT get_selector_data(ConnectionApi api, OBJECT selector_vals, String name) throws WebApplicationException
	{
		return new OBJECT("selector_name", name, "display_values", new ARRAY(), "values", new ARRAY());
	}
	
	protected OBJECT selector_waiting(String name)
	{
		return new OBJECT("selector_name", name, "display_values", new ARRAY(""), "values", new ARRAY("_waiting"));
	}
	
	protected OBJECT selector_empty(String name)
	{
		return new OBJECT("selector_name", name, "display_values", new ARRAY(), "values", new ARRAY());
	}

	private boolean in_return_type(int t)
	{
		int[] rt = get_return_types();
		for (int i=0; i<rt.length; i++)
			if (rt[i]==t)
				return true;
		return false;
	}

	/* public class methods */

	private Entity proxy_info;

	public void setMetaInfo(Entity proxy)
	{
		proxy_info = proxy;
	}

	public ConnectionApi getApi() throws WebApplicationException
	{
		if (proxy_info==null)
			throw new WebApplicationException("WidgetDef.get_data requires that meta info is set.");
		return _get_api(proxy_info);
	}

	private ConnectionApi _get_api(Entity proxy) throws WebApplicationException
	{
		Entity connection = get_connection_for_proxy(proxy);
		if (connection == null)
			return null;
		return OAuthorizationModule.apiForEntity(connection);
	}

	public OBJECT getData() throws WebApplicationException
	{
		if (proxy_info==null)
			throw new WebApplicationException("WidgetDef.get_data requires that meta info is set.");
		return _get_data(proxy_info);
	}

	private OBJECT _get_data(Entity proxy) throws WebApplicationException
	{
		ConnectionApi api = _get_api(proxy);
		OBJECT sel_vals = get_selector_vals_for_proxy(proxy);
		OBJECT props = get_props_for_proxy(proxy);
		return get_data(api,sel_vals, props);
	}

	public boolean returnsAnnotation()
	{
		return in_return_type(RETURN_TYPE_ANNOTATED_NUMBER);
	}
	
	public boolean returnsNumber()
	{
		return in_return_type(RETURN_TYPE_NUMBER);
	}

	public boolean returnsGeodata()
	{
		return in_return_type(RETURN_TYPE_GEODATA);
	}

	public boolean returnsDict()
	{
		return in_return_type(RETURN_TYPE_DICT);
	}

	public boolean returnsText()
	{
		return in_return_type(RETURN_TYPE_TEXT);
	}
	
	public boolean returnsSlides()
	{
		return in_return_type(RETURN_TYPE_SLIDES);
	}
	
	public boolean returnsVoid()
	{
		return  get_return_types() == RETURN_TYPE_VOID;
	}
	
	public boolean requiresConnection()
	{
		return !get_required_connection().equals(ConnectionApi.CONNECTION_TYPE_NONE);
	}

	public boolean requiresRawConnection()
	{
		return get_required_connection().equals(ConnectionApi.CONNECTION_TYPE_RAW);
	}
	
	public boolean isOwned()
	{
		return requiresRawConnection() || !requiresConnection();
	}
	
	public boolean isLiveDataWidget()
	{
		return false;
	}

	public OBJECT getSelectorData(String name,OBJECT selector_vals) throws WebApplicationException
	{
		return get_selector_data( getApi(), selector_vals, name);
	}

	public String getRequiredConnectionType()
	{
		return get_required_connection();
	}

	public int[] getReturnTypes()
	{
		return get_return_types();
	}

	// utils
	
	protected Entity get_connection_for_proxy(Entity proxy) throws WebApplicationException
	{
		return (Entity)proxy.getAttribute(WIDGETDATAPROXY_FIELD_CONNECTION);
	}

	protected OBJECT get_selector_vals_for_proxy(Entity proxy) throws WebApplicationException
	{
		return OBJECT.decode((String)proxy.getAttribute(WIDGETDATAPROXY_FIELD_SELECTOR_VALUES));
	}

	protected OBJECT get_props_for_wi(Entity wi) throws WebApplicationException
	{
		return OBJECT.decode((String)wi.getAttribute(WIDGETINSTANCE_FIELD_PROPS));
	}
	
	protected OBJECT get_props_for_proxy(Entity proxy) throws WebApplicationException
	{
		return OBJECT.decode((String)proxy.getAttribute(WIDGETDATAPROXY_FIELD_PROPS));
	}

	
	

}
