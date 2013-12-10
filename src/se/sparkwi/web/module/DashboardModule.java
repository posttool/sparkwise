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

import static se.sparkwi.web.util.CommonUtil.empty;
import static se.sparkwi.web.util.CommonUtil.get_dict_attr;
import static se.sparkwi.web.util.CommonUtil.is_correlation;
import static se.sparkwi.web.util.CommonUtil.set_dict_attr;
import static se.sparkwi.web.util.CommonUtil.to_list;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import se.sparkwi.web.module.auth.ConnectionApi;
import se.sparkwi.web.module.auth.OAuthorizationModule;
import se.sparkwi.web.module.auth.type.FacebookAuthorizationHandler;
import se.sparkwi.web.util.DateUtil;
import se.sparkwi.web.util.UTCDateInfo;

import com.eaio.uuid.UUID;
import com.jtheory.jdring.AlarmEntry;
import com.jtheory.jdring.AlarmListener;
import com.jtheory.jdring.AlarmManager;
import com.jtheory.jdring.PastDateException;
import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.util.ARRAY;
import com.pagesociety.util.CALLBACK;
import com.pagesociety.util.OBJECT;
import com.pagesociety.util.Text;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.gateway.RawCommunique;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.IEventListener;
import com.pagesociety.web.module.Module;
import com.pagesociety.web.module.ModuleEvent;
import com.pagesociety.web.module.TransactionProtect;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.email.IEmailModule;
import com.pagesociety.web.module.permissions.PermissionEvaluator;
import com.pagesociety.web.module.registration.ForgotPasswordModule;
import com.pagesociety.web.module.registration.RegistrationModule;
import com.pagesociety.web.module.user.UserModule;

public class DashboardModule extends WebStoreModule implements IEventListener
{
	public static final String PARAM_COLLECT_DATA_HOUR   		 = "collect-data-hour";
	public static final String PARAM_COLLECT_DATA_MINUTE 		 = "collect-data-minute";
	public static final String PARAM_DELETE_USER_DATA_ON_LOGIN 	 = "delete-user-data-on-login";
	public static final String PARAM_DELETE_WIDGETS_ON_BOOTSTRAP = "delete-widgets-on-bootstrap";
	public static final String PARAM_DATE_QUANTIZE_UNIT			 = "date-quantize-unit";
	public static final String PARAM_DATE_QUANTIZE_RESOLUTION	 = "date-quantize-resolution";

	private static final String PARAM_CONFIG_FILE = "widget-config-file";
	private static final String SLOT_AUTHORIZATION_MODULE = "authorization-module";

	//session key(s)
	public static final String PROJECT_CTX_KEY       = "project_ctx";

	//permission type(s)
	public static final String CAN_MANAGE_PROJECT  = "CAN_MANAGE_PROJECT";
	public static final String IS_OWNER  = "IS_OWNER";

	//common field mask
	public static final String[] SPARKWISE_MASK_FIELDS = new String[]{FIELD_CREATOR, "parent", "secret", "token"};

	//text properties for default entities
	public static final String DEFAULT_PROJECT_NAME  = "Default Project";
	public static final String MASTER_DASHBOARD_NAME = "Board 1";
	public static final String DEFAULT_WIDGET_COLLECTION = "Default";

	// error code
	public static final int DASHBOARD_ERROR_CODE_NOT_FOUND = 404;


	/*****
	 * (non static) FIELDS
	 */

	// OAuthorizationModule provides all functionality related to making oauth 1 & 2 Connections
	private OAuthorizationModule authorization_module;

	// manages the state property of widget instances and proxies
	private WidgetStateMachine state_machine;

	// transferred from app.xml props
	private int date_quantize_unit;
	private int date_quantize_resolution;
	
	// currently pretty static
	private OBJECT widget_collection;




	/************
	 *
	 * INITIALIZATION
	 * see WebContent/WEB-INF/config/application.xml
	 *
	 */
	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
		state_machine = new WidgetStateMachine();//(WidgetStateMachine) getSlot(SLOT_WIDGET_STATE_MACHINE);
		authorization_module = (OAuthorizationModule)getSlot(SLOT_AUTHORIZATION_MODULE);
	}

	public void loadbang(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.loadbang(app, config);
		String config_filename = GET_REQUIRED_CONFIG_PARAM(PARAM_CONFIG_FILE, config);
		Map<String,Object> widget_config = JSON_CONFIG_TO_MAP(new File(config_filename));
		init_widgets(widget_config);
		init_date_floor(app, config);
		init_data_collect(config);
		app.getModule("User").addEventListener(this);
		app.getModule("Registration").addEventListener(this);
		app.getModule("ForgotPassword").addEventListener(this);
		start_data_rety_thread();
		make_uuids();//TODO should be event based
	}

	private void init_date_floor(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		String date_quantize_unit_s = GET_OPTIONAL_CONFIG_PARAM(PARAM_DATE_QUANTIZE_UNIT,"day", config);
		if(date_quantize_unit_s.equalsIgnoreCase("day"))
			date_quantize_unit = DateUtil.DATE_QUANTIZE_DAY;
		else if(date_quantize_unit_s.equalsIgnoreCase("hour"))
			date_quantize_unit = DateUtil.DATE_QUANTIZE_HOUR;
		else if(date_quantize_unit_s.equalsIgnoreCase("minute"))
			date_quantize_unit = DateUtil.DATE_QUANTIZE_MINUTE;
		else
			throw new InitializationException("Unknown date quantize unit - "+date_quantize_unit_s);

		date_quantize_resolution = GET_OPTIONAL_INT_CONFIG_PARAM(PARAM_DATE_QUANTIZE_RESOLUTION, 1, config);
	}

	protected void exportPermissions()
	{
		EXPORT_PERMISSION(CAN_MANAGE_PROJECT);
	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_AUTHORIZATION_MODULE, OAuthorizationModule.class, true);
	}

	/**
	 * Moves JSON config data into the database. It does not recreate definitions if they
	 * exist, but will otherwise wipe out collections/categories and re-categorize existing
	 * and new definitions.
	 * 
	 * @param widget_config
	 * @throws InitializationException
	 */
	@SuppressWarnings("unchecked")
	private void init_widgets(Map<String, Object> widget_config) throws InitializationException
	{
		Entity default_collection = null;
		try
		{
			default_collection = get_widget_collection_by_user_by_name(null, DEFAULT_WIDGET_COLLECTION);
			if (default_collection == null)
				default_collection = NEW(WIDGET_COLLECTION_ENTITY, null, WIDGET_COLLECTION_FIELD_NAME, DEFAULT_WIDGET_COLLECTION);

			String package_name = (String) widget_config.get("package");
			List<Map<String, Object>> config_cats = (List<Map<String, Object>>) widget_config.get("categories");
			List<Entity> db_cats = (List<Entity>) default_collection.getAttribute(WIDGET_COLLECTION_FIELD_CATEGORIES);
			boolean are_categories_equiv = compare_cats(config_cats, db_cats);
			if (!are_categories_equiv)
			{
				INFO("***RECREATING WIDGET COLLECTION CATEGORIES***");
				PAGE_APPLY(WIDGET_COLLECTION_CATEGORY_ENTITY, new CALLBACK()
				{
					public Object exec(Object... args) throws Exception
					{
						INFO("DELETEING " + args[0]);
						DELETE((Entity) args[0]);
						return null;
					}
				});
				db_cats = new ArrayList<Entity>();
				for (Map<String, Object> config_cat : config_cats)
				{
					String configcatname = (String) config_cat.get("name");
					Entity cat = NEW(WIDGET_COLLECTION_CATEGORY_ENTITY, null, WIDGET_COLLECTION_CATEGORY_FIELD_NAME, configcatname);
					db_cats.add(cat);
				}
				UPDATE(default_collection, WIDGET_COLLECTION_FIELD_CATEGORIES, db_cats);
			}
			Map<String, Entity> db_cats_by_name = new HashMap<String, Entity>();
			for (Entity cat : db_cats)
			{
				String name = (String) cat.getAttribute(WIDGET_COLLECTION_CATEGORY_FIELD_NAME);
				db_cats_by_name.put(name, cat);
			}
			Query q = new Query(WIDGET_ENTITY);
			q.idx(Query.PRIMARY_IDX);
			q.gt(Query.VAL_GLOB);
			QueryResult allwidgets = QUERY(q);
			Map<String, Entity> widgets_by_fqcn = new HashMap<String, Entity>();
			for (Entity w : allwidgets.getEntities())
			{
				String name = (String) w.getAttribute(WIDGET_FIELD_CLASS_NAME);
				widgets_by_fqcn.put(name, w);
			}
			for (Map<String, Object> config_cat : config_cats)
			{
				String cat_name = (String) config_cat.get("name");
				List<Map<String, Object>> config_widgs = (List<Map<String, Object>>) config_cat.get("widgets");
				Entity db_cat = db_cats_by_name.get(cat_name);
				List<Entity> db_widgs = (List<Entity>) db_cat.getAttribute(WIDGET_COLLECTION_CATEGORY_FIELD_WIDGETS);
				boolean are_widgets_equiv = compare_widgets(config_widgs, db_widgs);
				if (!are_widgets_equiv)
				{
					INFO("***RECREATING WIDGETS IN CATEGORY***");
					db_widgs = new ArrayList<Entity>();
					for (int i = 0; i < config_widgs.size(); i++)
					{
						String configwname = (String) config_widgs.get(i).get("name");
						String configwclassname = (String) config_widgs.get(i).get("class");
						String fqcn = package_name + "." + configwclassname;
						Entity widg = widgets_by_fqcn.get(fqcn);
						if (widg == null)
						{
							widg = NEW(WIDGET_ENTITY, null, WIDGET_FIELD_NAME, configwname, WIDGET_FIELD_CLASS_NAME, fqcn);
						}
						else
						{
							widg = UPDATE(widg, WIDGET_FIELD_NAME, configwname);
						}
						db_widgs.add(widg);
					}
					UPDATE(db_cat, WIDGET_COLLECTION_CATEGORY_FIELD_WIDGETS, db_widgs);
				}
			}

		}
		catch (Exception e)
		{
			ERROR(e);
			throw new InitializationException("Problem initing default widget collection - " + e.getMessage());
		}
		try
		{
			widget_collection = ENTITY_TO_OBJECT(get_widget_collection_by_user_by_name(null, DEFAULT_WIDGET_COLLECTION));
		}
		catch (Exception e)
		{
			ERROR(e);
			throw new InitializationException("Problem retrieving default widget collection - " + e.getMessage());
		}
	}
	
	/* utils for json to db routine */

	private boolean compare_cats(List<Map<String, Object>> config_cats, List<Entity> db_cats)
	{
		if (config_cats.size() != db_cats.size())
			return false;
		for (int i = 0; i < db_cats.size(); i++)
		{
			String ccname = (String) db_cats.get(i).getAttribute(WIDGET_COLLECTION_CATEGORY_FIELD_NAME);
			String configcatname = (String) config_cats.get(i).get("name");
			if (!ccname.equals(configcatname))
				return false;
		}
		return true;
	}
	

	private boolean compare_widgets(List<Map<String, Object>> config_widgs, List<Entity> db_widgs)
	{
		if (config_widgs.size()!=db_widgs.size())
			return false;
		for (int i = 0; i < db_widgs.size(); i++)
		{
			String dbwname = (String) db_widgs.get(i).getAttribute(WIDGET_FIELD_NAME);
			String dbwclassname = (String) db_widgs.get(i).getAttribute(WIDGET_FIELD_CLASS_NAME);
			String configwname = (String) config_widgs.get(i).get("name");
			String configwclassname = (String) config_widgs.get(i).get("class");
			if (!dbwname.equals(configwname) || !dbwclassname.endsWith(configwclassname))
				return false;
		}
		return true;
	}



	/*********************
	 *
	 * EXPORTED METHODS
	 *
	 */

	/**
	 * This service returns the dashboard data for a public or embedded dashboard.
	 * 
	 * @param dash_id The dashboard id.
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT GetDashboardEmbed(UserApplicationContext uctx, long dash_id) throws PersistenceException,WebApplicationException
	{
		OBJECT ret = new OBJECT();
		Entity dash = get_dashboard(dash_id);
		Boolean is_public = (Boolean)dash.getAttribute(DASHBOARD_FIELD_PUBLIC);
		if (!is_public)
			throw new WebApplicationException("NO SUCH DASHBOARD");
		ret.put("dashboard",ENTITY_TO_OBJECT(dash));
		ret.put("widget_instances",ENTITIES_TO_OBJECTS(get_widgets_for_dashboard(dash, true)));
		return ret;
	}
	
	/**
	 * GetWidgetEmbed returns the widget data for a widget with a uuid.
	 *
	 * @param uuid The uuid of the desired widget.
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT GetWidgetEmbed(UserApplicationContext uctx, String uuid) throws PersistenceException,WebApplicationException
	{
		
		Entity widget = get_widget_instance(uuid);
		Entity dash   = get_dashboard_for_widget_instance(widget);
		Entity project = get_project_for_dashboard(dash);
		OBJECT ret = new OBJECT();
		widget = FILL_DEEP_AND_MASK(widget,FILL_ALL_FIELDS,SPARKWISE_MASK_FIELDS);
		Boolean is_public = (Boolean)dash.getAttribute(WIDGETINSTANCE_FIELD_PUBLIC);
		if (!is_public)
			throw new WebApplicationException("NO SUCH WIDGET");
		Entity proxy = (Entity)widget.getAttribute(WIDGETINSTANCE_FIELD_PROXY);
		Entity def = (Entity)proxy.getAttribute(WIDGETDATAPROXY_FIELD_WIDGET);
		ret.put("widget", ENTITY_TO_OBJECT(widget));
		ret.put("collection", get_widget_collection_by_widget(def));
		ret.put("events",ENTITIES_TO_OBJECTS(get_events(project)));
		return ret;
	}


	private OBJECT get_widget_collection_by_widget(Entity def)
	{
		@SuppressWarnings("unchecked")
		List<OBJECT> cats = (List<OBJECT>)widget_collection.get(WIDGET_COLLECTION_FIELD_CATEGORIES);
		for (OBJECT cat : cats)
		{
			@SuppressWarnings("unchecked")
			List<OBJECT> wdefs = (List<OBJECT>)cat.get(WIDGET_COLLECTION_CATEGORY_FIELD_WIDGETS);
			for (OBJECT w : wdefs)
				if (w.get("id").equals(def.getId()))
					return cat;
		}
		return null;
	}

	/**
	 * Returns a requested widget collection. Currently, the only collection is "default".
	 * 
	 * @see init_widget
	 * @param name The name of the collection.
	 * @return
	 * @throws WebApplicationException
	 * @throws PersistenceException
	 */
	@Export
	public OBJECT GetWidgetCollectionByName(UserApplicationContext uctx,String name) throws WebApplicationException,PersistenceException
	{
		return widget_collection;
	}


	/**
	 * Returns a dashboard for a particular user.
	 * 
	 * @param uctx The user
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public List<OBJECT> ListDashboards(UserApplicationContext uctx) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity project = get_ctx_project(uctx);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		return ENTITIES_TO_OBJECTS(get_dashboards_for_project(project));
	}

	/**
	 * Adds a new dashboard to the user's collection.
	 * 
	 * @param uctx The user
	 * @param name The name of the new dashboard
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT AddDashboard(UserApplicationContext uctx, String name) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		if (empty(name))
			throw new WebApplicationException("Please provide a name.");
		Entity project = get_ctx_project(uctx);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project); //this is the perm that was expressed earlier, but it seems funny cause we just got the project out of the session...
		return ENTITY_TO_OBJECT(create_dashboard(user, project, name, "", new ArrayList<String>()));
	}

	/**
	 * Rename dashboard. Requires a dashboard id and its new name.
	 * 
	 * @param uctx
	 * @param dash_id
	 * @param new_name
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT RenameDashboard(UserApplicationContext uctx, long dash_id, String new_name) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		if (empty(new_name))
			throw new WebApplicationException("Name is required.");
		Entity dash = get_dashboard(dash_id);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		return ENTITY_TO_OBJECT(UPDATE(dash, DASHBOARD_FIELD_NAME, new_name));
	}

	/**
	 * Deletes a dashboard. 
	 * 
	 * @param uctx
	 * @param dash_id
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	@TransactionProtect
	public OBJECT DeleteDashboard(UserApplicationContext uctx, long dash_id) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity dash = get_dashboard(dash_id);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		List<Entity> wi = get_widgets_for_dashboard(dash,false);
		for(int i = 0;i < wi.size();i++ )
		{
			DELETE(wi.get(i));
		}

		return ENTITY_TO_OBJECT(DELETE(dash));
	}
	
	/**
	 * Publish dashboard adds a uuid string and published flag to the dashboard data object so that it can be accessed
	 * by a url pattern.
	 * 
	 * @param uctx
	 * @param dash_id
	 * @return
	 * @throws WebApplicationException
	 * @throws PersistenceException
	 */
	@Export 
	public OBJECT PublishDashboard(UserApplicationContext uctx, long dash_id,String public_name,String public_org) throws WebApplicationException,PersistenceException
	{
		Entity user = get_user(uctx);
		Entity dash = get_dashboard(dash_id);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		return ENTITY_TO_OBJECT(UPDATE(dash,DASHBOARD_FIELD_PUBLIC,true,DASHBOARD_FIELD_PUBLIC_NAME,public_name,DASHBOARD_FIELD_PUBLIC_ORG,public_org));
	}
	
	/**
	 * UnpublishDashboard removes the publication flag.
	 * @param uctx
	 * @param dash_id
	 * @return
	 * @throws WebApplicationException
	 * @throws PersistenceException
	 */
	@Export 
	public OBJECT UnpublishDashboard(UserApplicationContext uctx, long dash_id) throws WebApplicationException,PersistenceException
	{
		
		Entity user = get_user(uctx);
		Entity dash = get_dashboard(dash_id);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		return ENTITY_TO_OBJECT(UPDATE(dash,DASHBOARD_FIELD_PUBLIC,false));
	}
	
	/**
	 * GetPublishedDashboard aggregates everything a page needs to know about a published dashboard.
	 * @param uctx
	 * @param uuid
	 * @return
	 * @throws WebApplicationException
	 * @throws PersistenceException
	 */
	@Export 
	public OBJECT GetPublishedDashboard(UserApplicationContext uctx, String uuid) throws WebApplicationException,PersistenceException
	{		
		Entity dash = get_dashboard(uuid);
		if(dash == null || dash.getAttribute(DASHBOARD_FIELD_PUBLIC).equals(false))
			throw new WebApplicationException("Unable to find dashboard "+uuid,DASHBOARD_ERROR_CODE_NOT_FOUND);
		Entity project = get_project_for_dashboard(dash);
		String username = "";
		Entity creator = EXPAND((Entity)dash.getAttribute(FIELD_CREATOR));
		if((username = (String)creator.getAttribute(UserModule.FIELD_USERNAME)) != null)
			;
		else
			username = "";
		
		OBJECT ret = new OBJECT();
		ret.put("username",username);
		ret.put("collection",GetWidgetCollectionByName(uctx,"Default"));
		ret.put("dashboard",ENTITY_TO_OBJECT(FILL_DEEP_AND_MASK(dash,FILL_ALL_FIELDS,SPARKWISE_MASK_FIELDS)));
		ret.put("widget_instances",ENTITIES_TO_OBJECTS(get_widgets_for_dashboard(dash, true)));
		ret.put("events",ENTITIES_TO_OBJECTS(get_events(project)));
		return ret;
	}

	/**
	 * Creates a new dashboard and clones the widgets within, but links the proxies...
	 * @param uctx
	 * @param orig_dash_id
	 * @param new_name
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	@TransactionProtect
	public OBJECT DuplicateDashboard(UserApplicationContext uctx, long orig_dash_id, String new_name) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity orig_dash = get_dashboard(orig_dash_id);
		Entity project = get_project_for_dashboard(orig_dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		@SuppressWarnings("unchecked")
		Entity new_dash = create_dashboard(user, project, new_name,
				(String)orig_dash.getAttribute(DASHBOARD_FIELD_DESCRIPTION),
				(List<String>)orig_dash.getAttribute(DASHBOARD_FIELD_TAGS));
		List<Entity> orig_widgets = get_widgets_for_dashboard(orig_dash,false);

		for(int i = 0;i < orig_widgets.size();i++)
		{
			Entity owi = orig_widgets.get(i);
			Entity nwi = clone_widget(owi);
			/* set the new parent */
			UPDATE(nwi,WIDGETINSTANCE_FIELD_PARENT,new_dash);
		}
		return ENTITY_TO_OBJECT(FILL_DEEP_AND_MASK(FORCE_EXPAND(new_dash), FILL_ALL_FIELDS, SPARKWISE_MASK_FIELDS));
	}

	private Entity clone_widget(Entity owi) throws PersistenceException
	{
		return CLONE_DEEP(owi, new clone_policy()
		{
			public int exec(Entity e, String fieldname, Entity reference_val)
			{
				if (fieldname.equals(FIELD_CREATOR) || fieldname.equals(WIDGETINSTANCE_FIELD_PROXY) || fieldname.equals(WIDGETINSTANCE_FIELD_PARENT))
					return LINK_REFERENCE;
				else if (fieldname.equals(WIDGETINSTANCE_FIELD_CHILDREN))
					return CLONE_REFERENCE;
				else
					return NULLIFY_REFERENCE;
			}
		});
	}

	/**
	 * Maintains a list of "tags" as an array in the dashboard.
	 * @param uctx
	 * @param dash_id
	 * @param tags Comma separated
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	@TransactionProtect
	public OBJECT TagDashboard(UserApplicationContext uctx, long dash_id, String tags) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity dash = get_dashboard(dash_id);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		String[] tt = tags.split(",");
		List<String> lt = new ArrayList<String>();
		for(int i = 0;i < tt.length;i++)
		{
			String t = tt[i].trim();
			if(t.length() > 0)
				lt.add(t);
		}
		dash.setAttribute(DASHBOARD_FIELD_TAGS, lt);
		return ENTITY_TO_OBJECT(dash);
	}

	/**
	 * Exports the dashboard as JSON. It should also include the widget proxy data for each widget TODO 
	 * It should also use RawCommunique TODO
	 * @param uctx
	 * @param dash_id
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public String Export(UserApplicationContext uctx, long dash_id)  throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity dash = get_dashboard(dash_id);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		List<Entity> wis = get_masked_widgets_for_dashboard(dash);
		StringBuilder s = new StringBuilder();
		for (Entity wi : wis)
		{
			s.append(ENTITY_TO_OBJECT(wi).toJSON());
		}
		//response.headers['Content-Type'] = "text/csv; charset=utf-8"
		//response.headers['Content-Disposition'] = 'attachment; filename=' + dash.name + ".csv"
		//response.headers['Pragma'] = 'no-cache'
		//response.headers['Cache-Control'] = 'no-cache,must-revalidate'
		return s.toString();
	}

	/**
	 * Returns a list of widget instances for a designated dashboard. This does not return the data
	 * collected on behalf of the widget data proxy. To do that, see GetDataForWidget.
	 * 
	 * @param uctx
	 * @param dash_id
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public List<OBJECT> GetWidgets(UserApplicationContext uctx, long dash_id) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity dash = get_dashboard(dash_id);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		List<Entity> widgets = get_masked_widgets_for_dashboard(dash);
		return ENTITIES_TO_OBJECTS(widgets);
	}

	/**
	 * Creates a widget within the dashboard. To do this, a widget definition and grid position are required.
	 * @param uctx
	 * @param dash_id
	 * @param widg_id
	 * @param x
	 * @param y
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	@TransactionProtect
	public OBJECT AddWidget(UserApplicationContext uctx, long dash_id, long widg_id, int x, int y)  throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity dash = get_dashboard(dash_id);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		Entity widget_def_info = get_widget_def_info(widg_id);
		
		WidgetDefinition widg = WidgetDefinition.forEntity(widget_def_info);
		if (widg == null)
			throw new WebApplicationException("There is no such widget");
		
		Entity wi = null;
		if(widget_def_info.getAttribute(WIDGET_FIELD_CLASS_NAME).equals("se.sparkwi.web.widget.Correlation_Correlate"))
		{
			wi = create_correlation_widget_instance(user, dash, widget_def_info, new Integer[]{x,y,1,1}, null);
		}
		else
		{
			wi = create_normal_widget_instance(user, dash, widget_def_info, new Integer[]{x,y,1,1}, null);
		}
		wi = state_machine.enter(wi);
		return ENTITY_TO_OBJECT(wi);
	}
	
	
	
	/**
	 * Repositions and/or resizes a widget instance.
	 * @param uctx
	 * @param wi_id
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT UpdateWidgetRect(UserApplicationContext uctx, long wi_id, int x, int y, int w, int h) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity wi = get_widget_instance(wi_id);
		Entity dash = get_dashboard_for_widget_instance(wi);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		UPDATE(wi, WIDGETINSTANCE_FIELD_RECT,  ARRAY(x,y,w,h));
		return ENTITY_TO_OBJECT(fill_and_mask(wi));
	}

	/**
	 * Deletes a widget instance from a board.
	 * 
	 * @param uctx
	 * @param wi_id
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	@TransactionProtect
	public OBJECT DeleteWidget(UserApplicationContext uctx, long wi_id)  throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity wi = get_widget_instance(wi_id);
		Entity dash = get_dashboard_for_widget_instance(wi);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		//WidgetDefinition def = get_definition_for_widget_instance(wi);
		Entity proxy = get_proxy_for_widget_instance(wi);
		if ((Integer)proxy.getAttribute(WIDGETDATAPROXY_FIELD_STATE) != PROXY_STATE_OK)//def.isOwned() must be deleted in 'connections'
		{
			proxy = delete_proxy(proxy);
			if(proxy.getAttribute("_was_in_correlation_") != null && (Boolean)proxy.getAttribute("_was_in_correlation_")  == true)
				wi.setAttribute("_was_in_correlation_", true); /* this is a message to the UI to update */
		}
		else
		{
			DELETE(wi);
		}
		
		return ENTITY_TO_OBJECT(wi);
	}

	/**
	 * Returns the data from the last x days for a widget instance. "Live data" like a Google Doc is computed on request.
	 * All other requests will come from the store. 
	 * 
	 * @param uctx
	 * @param wi_id
	 * @param last_x_days
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT GetDataForWidget(UserApplicationContext uctx, long wi_id, int last_x_days)  throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity wi = get_widget_instance(wi_id);
		Entity dash = get_dashboard_for_widget_instance(wi);
//TODO !!! add isPublic flag to the widget and add UI to manage explicity
//		boolean dash_is_public = (Boolean)dash.getAttribute(DASHBOARD_FIELD_PUBLIC);
//		if (!dash_is_public)
//		{
//			Entity project = get_project_for_dashboard(dash);
//			GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
//		}
		Entity proxy = get_proxy_for_widget_instance(wi);
		WidgetDefinition def = get_definition_for_proxy(proxy);
		try {
			if (def.isLiveDataWidget())
			{
				try 
				{
					return def.getData();
				} 
				catch (WebApplicationException e)
				{
					UPDATE(proxy, WIDGETDATAPROXY_FIELD_STATE, PROXY_STATE_FAILED_DATA_ERROR, WIDGETDATAPROXY_FIELD_LAST_FAILURE_MESSAGE, e.getMessage());
					return new OBJECT("error", e.getMessage());
				}
			}
			int type  = (Integer)wi.getAttribute(WIDGETINSTANCE_FIELD_TYPE);
			switch (type)
			{
				case WIDGETINSTANCE_TYPE_NORMAL:
					OBJECT data =  get_data_for_normal_widget(wi,last_x_days);
					@SuppressWarnings("unchecked")
					List<OBJECT> vals = (List<OBJECT>)data.get("values");
					if(last_x_days == 1 && vals.size() == 1)
					{
						OBJECT val = vals.get(0);
						String error = (String) val.get(WIDGETDATA_FIELD_ERROR_CODE);
						if(error != null)
						{
							Entity last_valid_data_point = get_last_valid_widget_data_point(wi,(String) val.get(WIDGETDATA_FIELD_DATEKEY));
							if(last_valid_data_point != null)
							{
								val.put(WIDGETDATA_FIELD_DATE_UTC,last_valid_data_point.getAttribute(WIDGETDATA_FIELD_DATE_UTC));
								val.put(WIDGETDATA_FIELD_DATEKEY,last_valid_data_point.getAttribute(WIDGETDATA_FIELD_DATEKEY));
								val.put(WIDGETDATA_FIELD_DATA,OBJECT.decode((String)last_valid_data_point.getAttribute(WIDGETDATA_FIELD_DATA)));
							}
						}
					}
					return data;
				case WIDGETINSTANCE_TYPE_CORRELATION:
					return get_data_for_correlation_widget(wi,last_x_days);
				default:
					return null;
			}
		}
		catch (OutOfMemoryError e){
			System.out.println(e);
			return OBJECT("oom",proxy.getId());
		}
	}
	
	/**
	 * This version of GetDataForWidget allows widget instance data to be returned within a particular date range. It should
	 * not be used for live data.
	 * @param uctx
	 * @param wi_id
	 * @param from
	 * @param to
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT GetDataForWidget(UserApplicationContext uctx, long wi_id, Double from, Double to)  throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity wi = get_widget_instance(wi_id);
		Entity dash = get_dashboard_for_widget_instance(wi);
		boolean dash_is_public = (Boolean)dash.getAttribute(DASHBOARD_FIELD_PUBLIC);
		if (!dash_is_public)
		{
			Entity project = get_project_for_dashboard(dash);
			GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		}
		if (from == null || to == null)
			throw new WebApplicationException("date range is required");
		Entity proxy = get_proxy_for_widget_instance(wi);
		WidgetDefinition def = get_definition_for_proxy(proxy);
		if (def.isLiveDataWidget())
			return GetDataForWidget(uctx, wi_id, 1);
		int type 	= (Integer)wi.getAttribute(WIDGETINSTANCE_FIELD_TYPE);
		switch (type)
		{
			case WIDGETINSTANCE_TYPE_NORMAL:
				OBJECT data =  get_data_in_range_for_normal_widget(wi,new Date(from.longValue()),new Date(to.longValue()));
				@SuppressWarnings("unchecked")
				List<OBJECT> vals = (List<OBJECT>)data.get("values");
				if(vals.size() == 1)
				{
					OBJECT val = vals.get(0);
					String error = (String) val.get(WIDGETDATA_FIELD_ERROR_CODE);
					if(error != null)
					{
						Entity last_valid_data_point = get_last_valid_widget_data_point(wi,(String) val.get(WIDGETDATA_FIELD_DATEKEY));
						if(last_valid_data_point != null)
						{
							val.put(WIDGETDATA_FIELD_DATE_UTC,last_valid_data_point.getAttribute(WIDGETDATA_FIELD_DATE_UTC));
							val.put(WIDGETDATA_FIELD_DATEKEY,last_valid_data_point.getAttribute(WIDGETDATA_FIELD_DATEKEY));
							val.put(WIDGETDATA_FIELD_DATA,last_valid_data_point.getAttribute(WIDGETDATA_FIELD_DATA));
						}
					}
				}
				return data;
			case WIDGETINSTANCE_TYPE_CORRELATION:
				return get_data_in_range_for_correlation_widget(wi,new Date(from.longValue()),new Date(to.longValue()));
			default:
				return null;
		}
	}
	
	private Entity get_last_valid_widget_data_point(Entity wi,String to_datekey) throws PersistenceException,WebApplicationException
	{
		Entity proxy = (Entity)wi.getAttribute(WIDGETINSTANCE_FIELD_PROXY);
		String datekey = to_datekey;
		int page_size = 20;
		int offset = 0;
		while(true)
		{
			Query q = new Query(WIDGETDATA_ENTITY);
			q.idx(WIDGETDATA_IDX_BY_PROXY_BY_DATEKEY);
			q.lt(q.list(proxy,datekey));
			q.pageSize(page_size);
			q.offset(offset);
			List<Entity> results = QUERY(q).getEntities();
			for(int i = 0;i < results.size();i++)
			{
				Entity datum = results.get(i);
				if(datum.getAttribute(WIDGETDATA_FIELD_ERROR_CODE) == null)
					return datum;
			}
			if(results.size() < page_size)
				return null;
			offset += results.size();
		}
		
	}
	
	/**
	 * Updates the display properties for a widget instance. If the widget definition owns it data,
	 * then the props are also passed to the proxy.
	 * @param uctx
	 * @param wi_id
	 * @param props
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT UpdatePropsForWidget(UserApplicationContext uctx, long wi_id, OBJECT props) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity wi = get_widget_instance(wi_id);
		Entity dash = get_dashboard_for_widget_instance(wi);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		
		if(props.get("display-style") != null && ((String)props.get("display-style")).startsWith("_"))
			return ENTITY_TO_OBJECT(fill_and_mask(wi));
			
		wi = UPDATE(wi, WIDGETINSTANCE_FIELD_PROPS, ENCODE(props));
		//validate updated wi
		Entity proxy = get_proxy_for_widget_instance(wi);
		proxy = UPDATE(proxy, WIDGETDATAPROXY_FIELD_PROPS, ENCODE(props));
		state_machine.reset(wi);
		state_machine.enter(wi,proxy);
		return ENTITY_TO_OBJECT(fill_and_mask(wi));
	}

	/**
	 * Sets the OAuth connection for the widget instance.
	 * @param uctx
	 * @param wi_id
	 * @param conn_id
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT SetConnectionForWidget(UserApplicationContext uctx, long wi_id, long conn_id) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity wi = get_widget_instance(wi_id);
		Entity dash = get_dashboard_for_widget_instance(wi);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		Entity conn  = get_connection(conn_id);
		Entity proxy = get_proxy_for_widget_instance(wi);
		Entity widg  = get_widget_def_info_for_proxy(proxy);		
		proxy = create_widget_data_proxy(user, project, widg, conn);
		proxy.setAttribute(WIDGETDATAPROXY_FIELD_STATE, PROXY_STATE_REQUIRES_SERVICE_SELECTION);
		wi.setAttribute(WIDGETINSTANCE_FIELD_PROXY, proxy);
		wi.setAttribute(WIDGETINSTANCE_FIELD_STATE, WIDGET_STATE_INIT);
		return ENTITY_TO_OBJECT(state_machine.enter(wi));
	}

	/**
	 * Returns selector values for a particular proxy selector. Selectors are named properties of a proxy
	 * that the proxy requires to make a data query. Often the proxy will provide a list of acceptable values
	 * for the selector. For example, with Google Analytics, a 'site' must be chosen. This method will
	 * provide the list of valid sites for the proxy within the specified widget instance.
	 * @param uctx
	 * @param wi_id
	 * @param selector_name
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT GetSelectorDataForWidget(UserApplicationContext uctx, long wi_id, String selector_name) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity wi = get_widget_instance(wi_id);
		Entity dash = get_dashboard_for_widget_instance(wi);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		WidgetDefinition def = get_definition_for_widget_instance(wi);
		Entity proxy = EXPAND(get_proxy_for_widget_instance(wi));
		OBJECT selector_vals = OBJECT.decode((String)proxy.getAttribute(WIDGETDATAPROXY_FIELD_SELECTOR_VALUES));
		if(selector_vals == null)
			selector_vals = new OBJECT();
		OBJECT sel_data = def.getSelectorData(selector_name,selector_vals);
		return sel_data;
	}

	/**
	 * Sets the selector value for a widget instance (its proxy). It requires that a key value and human readable display value be provided.
	 * @param uctx
	 * @param wi_id
	 * @param selector_name
	 * @param value
	 * @param display_value
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT SetSelectorValueForWidget(UserApplicationContext uctx, long wi_id, String selector_name, String value, String display_value) throws PersistenceException, WebApplicationException
	{
		Entity user 	= get_user(uctx);
		Entity wi 		= FILL_DEEP(get_widget_instance(wi_id)); //TODO replace w/ fill and mask?
		Entity dash 	= get_dashboard_for_widget_instance(wi);
		Entity project 	= get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		
		if(display_value.startsWith("Choose"))
			return ENTITY_TO_OBJECT(wi);
		if(value.startsWith("_waiting"))
			return ENTITY_TO_OBJECT(wi);
		if(display_value.equals(""))
			return ENTITY_TO_OBJECT(wi);

		Entity proxy 		 	= get_proxy_for_widget_instance(wi);
		int pstate 				= (Integer)proxy.getAttribute(WIDGETDATAPROXY_FIELD_STATE);
		WidgetDefinition def 	= get_definition_for_proxy(proxy);
		String[] sel_names 		= def.get_service_selector_names();


		set_dict_attr(proxy, WIDGETDATAPROXY_FIELD_SELECTOR_VALUES, selector_name,value);
		set_dict_attr(proxy, WIDGETDATAPROXY_FIELD_SELECTOR_DISPLAY_VALUES,selector_name,display_value);
		
		/* if someone is changing a selector value we may have to create a new proxy if one doesn't exist or find an existing one*/
		if (/*!def.isOwned() &&*/ pstate != PROXY_STATE_REQUIRES_SERVICE_SELECTION)
		{
			int sel_idx = 0;
			for(int i = 0;i < sel_names.length;i++)
			{
				if(sel_names[i].equals(selector_name))
				{
					sel_idx = i;
					break;
				}
			}
			
			/* reset values below */
			boolean need_to_create_temp = false;
			for(int i = sel_idx +1; i < sel_names.length;i++)
			{
				need_to_create_temp = true;
				set_dict_attr(proxy, WIDGETDATAPROXY_FIELD_SELECTOR_VALUES, sel_names[i], null);
				set_dict_attr(proxy, WIDGETDATAPROXY_FIELD_SELECTOR_DISPLAY_VALUES, sel_names[i], null);
			}
			
			
			if(need_to_create_temp)
			{
				if(!def.isOwned())
					proxy = clone_proxy(proxy);
				System.out.println("!!CURRENT SELECTOR VALUES ARE "+(OBJECT.decode((String)proxy.getAttribute(WIDGETDATAPROXY_FIELD_SELECTOR_VALUES)).toJSON()));
				proxy.setAttribute(WIDGETDATAPROXY_FIELD_STATE, PROXY_STATE_REQUIRES_SERVICE_SELECTION);
				wi.setAttribute(WIDGETINSTANCE_FIELD_PROXY, proxy);
				state_machine.reset(wi);
				return ENTITY_TO_OBJECT(state_machine.enter(wi));	
			}
			else
			{
				Entity match = get_matching_proxy(proxy);
				
				if(match != null)
				{
					proxy = match;
					proxy.setAttribute(WIDGETDATAPROXY_FIELD_STATE, PROXY_STATE_OK);
					wi.setAttribute(WIDGETINSTANCE_FIELD_PROXY, proxy);
					state_machine.reset(wi);
					return ENTITY_TO_OBJECT(state_machine.enter(wi));	
				}
				else
				{
					if(!def.isOwned())
						proxy = clone_proxy(proxy);
					proxy.setAttribute(WIDGETDATAPROXY_FIELD_STATE, PROXY_STATE_REQUIRES_SERVICE_SELECTION);
					wi.setAttribute(WIDGETINSTANCE_FIELD_PROXY, proxy);
					state_machine.reset(wi);
					return ENTITY_TO_OBJECT(state_machine.enter(wi));			
				}
			}
		}
		else
		{
			state_machine.save(proxy);
			return ENTITY_TO_OBJECT(state_machine.enter(wi));
		}

	
	}

	/**
	 * Forces the widget to go through the state machine, causing it to be revalidated.
	 * In the process, its data for the date window will be collected.
	 * @param uctx
	 * @param wi_id
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT RefreshWidgetData(UserApplicationContext uctx, long wi_id) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity wi = FILL_DEEP(get_widget_instance(wi_id));
		Entity dash = get_dashboard_for_widget_instance(wi);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		state_machine.reset(wi);
		return ENTITY_TO_OBJECT(state_machine.enter(wi));
	}

	/**
	 * Forces the widget instance through the statemachine without revalidating it. Evaluate: Does UI access?
	 * @param uctx
	 * @param wi_id
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT SyncWidget(UserApplicationContext uctx, long wi_id) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity wi = FILL_DEEP(get_widget_instance(wi_id));
		Entity dash = get_dashboard_for_widget_instance(wi);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		return ENTITY_TO_OBJECT(state_machine.enter(wi));
	}
	
	/**
	 * Is this used by the UI?
	 * @param uctx
	 * @param proxy_id
	 * @return
	 * @throws WebApplicationException
	 * @throws PersistenceException
	 */
	@Export
	@TransactionProtect
	public OBJECT RepairProxy(UserApplicationContext uctx,long proxy_id) throws WebApplicationException,PersistenceException
	{
		Entity user = get_user(uctx);
		Entity proxy = GET(WIDGETDATAPROXY_ENTITY,proxy_id);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, proxy.getAttribute(WIDGETDATAPROXY_FIELD_PARENT));
		return ENTITY_TO_OBJECT(repair_proxy(proxy));
	}
	
	private Entity repair_proxy(Entity proxy) throws WebApplicationException,PersistenceException
	{
		try{
			create_data_for_proxy(proxy);
			UPDATE(proxy,
					WIDGETDATAPROXY_FIELD_STATE,PROXY_STATE_OK,
					WIDGETDATAPROXY_FIELD_LAST_FAILURE_MESSAGE,null);
		}catch(Exception e)
		{
			ERROR(e);
			try{
				UPDATE(proxy,
						WIDGETDATAPROXY_FIELD_STATE,PROXY_STATE_FAILED_DATA_ERROR,
						WIDGETDATAPROXY_FIELD_LAST_FAILURE_MESSAGE,e.getMessage());
			}catch(Exception pe)
			{
				ERROR(pe);
			}
		}
		return proxy;		
	}
	
	/**
	 * Is this used by the UI?
	 * @param uctx
	 * @param proxy_id
	 * @return
	 * @throws WebApplicationException
	 * @throws PersistenceException
	 */
	@Export
	@TransactionProtect
	public OBJECT RefreshProxy(UserApplicationContext uctx,long proxy_id) throws WebApplicationException,PersistenceException
	{
		Entity user = get_user(uctx);
		Entity proxy = GET(WIDGETDATAPROXY_ENTITY,proxy_id);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, proxy.getAttribute(WIDGETDATAPROXY_FIELD_PARENT));
		return ENTITY_TO_OBJECT(refresh_proxy(proxy));
	}
	
	private Entity refresh_proxy(Entity proxy)
	{
		try{
			create_data_for_proxy(proxy);
			if((Integer)proxy.getAttribute(WIDGETDATAPROXY_FIELD_STATE) != PROXY_STATE_OK)
			{
				UPDATE(proxy,
						WIDGETDATAPROXY_FIELD_STATE,PROXY_STATE_OK,
						WIDGETDATAPROXY_FIELD_LAST_FAILURE_MESSAGE,null);
			}
		}catch(Exception e)
		{
			ERROR(e);
			try{
				UPDATE(proxy,
						WIDGETDATAPROXY_FIELD_STATE,PROXY_STATE_FAILED_DATA_ERROR,
						WIDGETDATAPROXY_FIELD_LAST_FAILURE_MESSAGE,e.getMessage());
			}catch(Exception pe)
			{
				ERROR(pe);
			}
		}
		return proxy;	
	
	}
	
	/**
	 * Adds a widget instance to the correlation.
	 * @param uctx
	 * @param corr_id
	 * @param wi_id
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	@TransactionProtect
	public OBJECT AddWidgetToCorrelation(UserApplicationContext uctx, long corr_id, long wi_id) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity corr = get_widget_instance(corr_id);
		Entity dash = get_dashboard_for_widget_instance(corr);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		int corr_type = (Integer) corr.getAttribute(WIDGETINSTANCE_FIELD_TYPE);
		if (corr_type != WIDGETINSTANCE_TYPE_CORRELATION)
			throw new WebApplicationException("Requires a correlation parent");
		Entity wi = get_widget_instance(wi_id);
		Entity proxy = get_proxy_for_widget_instance(wi);
		//GUARD : verify proxy is part of the project or owned or ...?
		@SuppressWarnings("unchecked") 
		List<Entity> children = (List<Entity>)corr.getAttribute(WIDGETINSTANCE_FIELD_CHILDREN);
		if (!children.contains(proxy))
		{
			children.add(proxy);
			UPDATE(corr, WIDGETINSTANCE_FIELD_CHILDREN, children);
		}
		return ENTITY_TO_OBJECT(fill_and_mask(state_machine.enter(corr)));
	}

	/**
	 * Removes a widget from a correlation.
	 * @param uctx
	 * @param corr_id
	 * @param proxy_id
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	@TransactionProtect
	public OBJECT RemoveWidgetFromCorrelation(UserApplicationContext uctx, long corr_id, long proxy_id) throws PersistenceException,
			WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity corr = get_widget_instance(corr_id);
		Entity dash = get_dashboard_for_widget_instance(corr);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		int corr_type = (Integer) corr.getAttribute(WIDGETINSTANCE_FIELD_TYPE);
		if (corr_type != WIDGETINSTANCE_TYPE_CORRELATION)
			throw new WebApplicationException("Requires a correlation parent");
		Entity proxy = get_proxy(proxy_id);
		// GUARD : verify proxy is part of the project or owned or ...?

		@SuppressWarnings("unchecked")
		List<Entity> children = (List<Entity>) corr.getAttribute(WIDGETINSTANCE_FIELD_CHILDREN);
		if (children.contains(proxy))
		{
			children.remove(proxy);
			UPDATE(corr, WIDGETINSTANCE_FIELD_CHILDREN, children);
		}

		corr.setAttribute(WIDGETINSTANCE_FIELD_STATE, WIDGET_STATE_CORRELATION_REQUIRES_MEMBERS);
		corr = FILL_DEEP_AND_MASK(state_machine.enter(corr), FILL_ALL_FIELDS, SPARKWISE_MASK_FIELDS);
		return ENTITY_TO_OBJECT(corr);
	}

	/**
	 * My set is a collection of widget instances (props & configured proxies). It is used to easily
	 * copy widgets from one place to another. GetMySet returns the wrapper for the list of saved widget instances.
	 * @param uctx
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT GetMySet(UserApplicationContext uctx) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		return ENTITY_TO_OBJECT(get_myset_for_user(user));
	}
	
	/**
	 * Adds a widget instance to "My Set".
	 * @param uctx
	 * @param wi_id
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT SaveToMySet(UserApplicationContext uctx, long wi_id) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity wi = get_widget_instance(wi_id);
		Entity dash = get_dashboard_for_widget_instance(wi);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		Entity myset = get_myset_for_user(user);
		int type = (Integer) wi.getAttribute(WIDGETINSTANCE_FIELD_TYPE);
		Entity proxy = (Entity) wi.getAttribute(WIDGETINSTANCE_FIELD_PROXY);
		Integer[] rect = new Integer[]{0,0,1,1};
		@SuppressWarnings("unchecked")
		List<Entity> children = (List<Entity>) wi.getAttribute(WIDGETINSTANCE_FIELD_CHILDREN);
		OBJECT props = OBJECT.decode((String) wi.getAttribute(WIDGETINSTANCE_FIELD_PROPS));
		Entity newwi = create_widget_instance(user, null, type, proxy, children, rect, props);
		@SuppressWarnings("unchecked")
		List<Entity> wis = (List<Entity>)myset.getAttribute(MYSET_FIELD_WIDGETS);
		wis.add(newwi);
		myset = UPDATE(myset, MYSET_FIELD_WIDGETS, wis);
		return ENTITY_TO_OBJECT(FILL_DEEP(newwi));
	}
	
	/**
	 * Removes a widget instance from my set.
	 * @param uctx
	 * @param wi_id
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT RemoveFromMySet(UserApplicationContext uctx, long wi_id) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity wi = get_widget_instance(wi_id);
//		Entity dash = get_dashboard_for_widget_instance(wi);
//		Entity project = get_project_for_dashboard(dash);
//		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		GUARD(user, IS_OWNER, GUARD_INSTANCE, wi);
		Entity myset = get_myset_for_user(user);
		@SuppressWarnings("unchecked")
		List<Entity> wis = (List<Entity>)myset.getAttribute(MYSET_FIELD_WIDGETS);
		if (wis.contains(wi))
		{
			wis.remove(wi);
			myset = UPDATE(myset, MYSET_FIELD_WIDGETS, wis);
		}
		return ENTITY_TO_OBJECT(FILL_DEEP(wi));
	}
	
	
	/**
	 * This method uses a widget instance id for creating a new widget instance on the dashboard. It not only uses
	 * the proxy of the referenced widget instance, but also it children and visual properties. 
	 * @param uctx
	 * @param dash_id
	 * @param wi_id
	 * @param x
	 * @param y
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	@TransactionProtect
	public OBJECT AddWidgetInstance(UserApplicationContext uctx, long dash_id, long wi_id, int x, int y)  throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity wi = FILL_DEEP(get_widget_instance(wi_id));
		Entity dash = get_dashboard(dash_id);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		int type = (Integer) wi.getAttribute(WIDGETINSTANCE_FIELD_TYPE);
		Entity proxy = (Entity) wi.getAttribute(WIDGETINSTANCE_FIELD_PROXY);
		Integer[] rect = new Integer[]{x,y,1,1};
		@SuppressWarnings("unchecked")
		List<Entity> children = (List<Entity>) wi.getAttribute(WIDGETINSTANCE_FIELD_CHILDREN);
		OBJECT props = OBJECT.decode((String) wi.getAttribute(WIDGETINSTANCE_FIELD_PROPS));
		Entity new_wi = create_widget_instance(user, dash, type, proxy, children, rect, props);
		new_wi = state_machine.enter(new_wi);
		return ENTITY_TO_OBJECT(new_wi);
	}



	/***************
	 *
	 *  AUTHORIZATION & CONNECTION
	 *
	 */
	
	
	/**
	 * When the UI needs a Connection, it interacts with DoAuthorization. A
	 * WidgetInstance key is required from which the Connection type & scope is
	 * derived. Also, a Connection can be created by type, and will be placed in
	 * the ctx project. This page then redirects to the authorization service
	 * with type & scope as well as the parent Project, eventually arriving at
	 * DoAuthorizationReturn. This page must be requested in a pop up window.
	 * 
	 * @param uctx
	 * @param c
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public void DoAuthorization(UserApplicationContext uctx,RawCommunique c) throws PersistenceException,WebApplicationException
	{
		HttpServletRequest  request  = (HttpServletRequest)c.getRequest();
		HttpServletResponse response = (HttpServletResponse)c.getResponse();

		Entity user = (Entity)uctx.getUser();
		Entity project;
		String connection_type;
		String[] scopes;
		OBJECT user_obj = new OBJECT();

		String wid_param 		= request.getParameter("wid");
		String conn_type_param 	= request.getParameter("type");

		if (wid_param!=null)
		{
			long wid 			= Long.parseLong(wid_param);
			Entity wi 			= get_widget_instance(wid);
			Entity dash 		= get_dashboard_for_widget_instance(wi);
			project 			= get_project_for_dashboard(dash);
			GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
			WidgetDefinition def 	= get_definition_for_widget_instance(wi);
			connection_type			= def.get_required_connection();
			scopes					= def.get_scopes();
			user_obj.put("wid", wid);
		}
		else if (conn_type_param!=null)
		{
			project					= get_ctx_project(uctx);
			connection_type			= conn_type_param;
			scopes					= new String[]{};
			user_obj.put("type", conn_type_param);
		}
		else
		{
			throw new WebApplicationException("DoAuthorization requires wid or type");
		}

		if(request.getParameter("js_ok_f")!=null)
			user_obj.put("js_ok_f",request.getParameter("js_ok_f"));
		if(request.getParameter("js_err_f")!=null)
			user_obj.put("js_err_f",request.getParameter("js_err_f"));


		String bp           = getApplication().getConfig().getWebRootUrl();
		String return_to    = bp + "/" + getName() + "/DoAuthorizationReturn/.raw";
		String redirect_url = bp + authorization_module.getRelativeAuthorizationURL(connection_type, scopes, PROJECT_ENTITY, project.getId(), return_to, user_obj);
		String preconn_prefix = bp + "/tool/preconnect/";
		try
		{
			if (connection_type.equals(ConnectionApi.CONNECTION_TYPE_FACEBOOK))
			{
				String app_id = FacebookAuthorizationHandler.facebook_app_id;
				response.sendRedirect(preconn_prefix + "fb_pre_connect.fhtml?app_id=" + app_id + "&next=" + Text.encodeURIComponent(redirect_url));
				return;
			}
			else if (connection_type.equals(ConnectionApi.CONNECTION_TYPE_VIMEO))
			{
				response.sendRedirect(preconn_prefix + "vimeo_pre_connect.fhtml?next=" + Text.encodeURIComponent(redirect_url));
				return;
			}
			else if (connection_type.equals(ConnectionApi.CONNECTION_TYPE_GOOGLE_ANALYTICS))
			{
				response.sendRedirect(preconn_prefix + "gdata_pre_connect.fhtml?next=" + Text.encodeURIComponent(redirect_url));
				return;
			}
			else if (connection_type.equals(ConnectionApi.CONNECTION_TYPE_YOUTUBE))
			{
				response.sendRedirect(preconn_prefix + "youtube_pre_connect.fhtml?next=" + Text.encodeURIComponent(redirect_url));
				return;
			}
			response.sendRedirect(redirect_url);
		}
		catch (IOException ioe)
		{
			ERROR(ioe);
		}
	
	}

	@Export
	public void DoAuthorizationReturn(UserApplicationContext uctx, RawCommunique c) throws PersistenceException,WebApplicationException, IOException
	{
		HttpServletRequest  request  = (HttpServletRequest)c.getRequest();
		HttpServletResponse response = (HttpServletResponse)c.getResponse();

		OBJECT authstuff 	= authorization_module.decodeAuthorizationResponse(request);
		OBJECT user_obj	 	= (OBJECT)authstuff.get("user_object");
		String js_ok_f_name  = "authorization_complete";
		String js_err_f_name = "authorization_error";
		if(user_obj.get("js_ok_f")  != null)
			js_ok_f_name = (String)user_obj.get("js_ok_f");
		if(user_obj.get("js_err_f") != null)
			js_ok_f_name = (String)user_obj.get("js_err_f");

		if(authstuff.get("error") == null)
		{
			Entity connection  	= (Entity)authstuff.get("connection");
			if (user_obj.containsKey("wid"))
			{
				Entity wi 			= get_widget_instance(user_obj.I("wid"));
				Entity proxy 		= get_proxy_for_widget_instance(wi);
				state_machine.addConnectionToProxy(proxy, connection);
				state_machine.enter(wi, proxy);
			}
			else if (user_obj.containsKey("type"))
			{
				List<Entity> proxies = get_proxies_for_connection(connection);
				for (Entity proxy : proxies)
				{
					int pstate = (Integer) proxy.getAttribute(WIDGETDATAPROXY_FIELD_STATE);
					if (pstate == PROXY_STATE_FAILED_DATA_ERROR)
					try {
						create_data_for_proxy(proxy);
						UPDATE(proxy, WIDGETDATAPROXY_FIELD_STATE, PROXY_STATE_OK);
					} catch (Exception e)
					{
						ERROR(e);
						UPDATE(proxy, WIDGETDATAPROXY_FIELD_LAST_FAILURE_MESSAGE, e.getMessage());
					}
				}
			}
			else
				throw new WebApplicationException("DoAuthorizationReturn - invalid return params");

			response.getOutputStream().println("<script>window.opener."+js_ok_f_name+"("+
								ENTITY_TO_OBJECT(connection).toJSON()+");this.close()</script>");
		}
		else
		{
			response.getOutputStream().println("<script>window.opener."+js_err_f_name+"('"+authstuff.get("error")+"');this.close()</script>");
		}
	}

	/**
	 * Lists all user established Connections
	 * @param uctx
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public List<OBJECT> ListConnections(UserApplicationContext uctx) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		if (user==null)
			throw new WebApplicationException("Log in first");
		return ENTITIES_TO_OBJECTS(authorization_module.get_connections(user));
	}

	/**
	 * Lists every proxies for all the user's connections! Wow!
	 * @param uctx
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public List<OBJECT> ListConnectionProxies(UserApplicationContext uctx) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		if (user==null)
			throw new WebApplicationException("Log in first");
		List<Entity> connections = authorization_module.get_connections(user);
		for (Entity conn : connections)
		{
			List<Entity> proxies = get_proxies_for_connection(conn);
			conn.setAttribute("children", proxies);
		}
		return ENTITIES_TO_OBJECTS(connections);
	}
	
	/**
	 * Adds all proxies as "children" of their connection (except none/raw connections).
	 * @param uctx
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	public OBJECT ListConnectionData(UserApplicationContext uctx) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		if (user==null)
			throw new WebApplicationException("Log in first");
		Entity project = get_ctx_project(uctx);
		
		OBJECT ret = new OBJECT();
		for(int i = 0;i < ConnectionApi.CONNECTION_TYPES.length;i++)
			ret.put(ConnectionApi.CONNECTION_TYPES[i], new ArrayList<OBJECT>());
		
		List<Entity> connections = authorization_module.get_connections_by_parent(project);

		for (Entity conn : connections)
		{
			if(conn.getAttribute(OAuthorizationModule.CONNECTION_FIELD_TYPE).equals(ConnectionApi.CONNECTION_TYPE_NONE) || 
			   conn.getAttribute(OAuthorizationModule.CONNECTION_FIELD_TYPE).equals(ConnectionApi.CONNECTION_TYPE_RAW) 
			)
				continue;
			List<Entity> proxies = get_proxies_for_connection(conn);
			for(int i = 0;i < proxies.size();i++)
			{
				Entity p = proxies.get(i);
				//dont show these weird ones..they will be pruned if they stay around too long//
				if((Integer)p.getAttribute(WIDGETDATAPROXY_FIELD_STATE) < PROXY_STATE_OK)
					proxies.remove(p);

			}
			conn.setAttribute("children", proxies);
			@SuppressWarnings("unchecked")
			List<OBJECT> cc = (List<OBJECT>)ret.get(conn.getAttribute(OAuthorizationModule.CONNECTION_FIELD_TYPE));
			cc.add(ENTITY_TO_OBJECT(conn));
		}
		return ret;
	}


	/**
	 * Deletes a connection and all related proxies.
	 * 
	 * @param uctx
	 * @param conn_id
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	@Export
	@TransactionProtect
	public OBJECT DeleteConnection(UserApplicationContext uctx, long conn_id) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity conn = get_connection(conn_id);
		Entity project = get_project_for_connection(conn);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		List<Entity> proxies = get_proxies_for_connection(conn);
		for (Entity proxy : proxies)
		{
			delete_proxy(proxy);
		}
		return ENTITY_TO_OBJECT(DELETE(conn));
	}
	
	/**
	 * Deletes an individual proxy.
	 * 
	 * @param uctx
	 * @param proxy_id
	 * @return
	 * @throws WebApplicationException
	 * @throws PersistenceException
	 */
	@Export
	@TransactionProtect
	public OBJECT DeleteProxy(UserApplicationContext uctx,long proxy_id) throws WebApplicationException,PersistenceException
	{
		Entity user = get_user(uctx);
		Entity proxy = GET(WIDGETDATAPROXY_ENTITY,proxy_id);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, proxy.getAttribute(WIDGETDATAPROXY_FIELD_PARENT));
		return ENTITY_TO_OBJECT(delete_proxy(FILL_REFS(proxy)));
	}


	/**
	 * This will delete the data and all widget instances pointing to it
	 * @param proxy
	 * @return
	 * @throws PersistenceException
	 * @throws WebApplicationException
	 */
	private Entity delete_proxy(Entity proxy) throws PersistenceException,WebApplicationException
	{
		return delete_proxy(proxy,true);
	}
	
	private Entity delete_proxy(Entity proxy, boolean update_correlations) throws PersistenceException,WebApplicationException
	{
		List<Entity> wis = get_widget_instances_for_proxy(proxy);
		for (Entity wi : wis)
			DELETE(wi);

		if (update_correlations)
		{
			List<Entity> corrs = get_corr_instances_for_proxy(proxy);
			for (Entity corr : corrs)
			{
				@SuppressWarnings("unchecked") List<Entity> children =
					(List<Entity>)corr.getAttribute(WIDGETINSTANCE_FIELD_CHILDREN);
				children.remove(proxy);
				UPDATE(corr, WIDGETINSTANCE_FIELD_CHILDREN, children);
				corr.setAttribute(WIDGETINSTANCE_FIELD_STATE, WIDGET_STATE_CORRELATION_REQUIRES_MEMBERS);
				state_machine.enter(corr);
				proxy.setAttribute("_was_in_correlation_", true);
			}
		}
		
		delete_widget_data_for_proxy(proxy);
		
		return DELETE(proxy);	
	}

	/* EVENTS */


	@Export
	public List<OBJECT> ListEvents(UserApplicationContext uctx) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity project = get_ctx_project(uctx);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		return ENTITIES_TO_OBJECTS(get_events(project));
	}
	


	@Export
	public OBJECT AddEvent(UserApplicationContext uctx, String name, Double date, String description) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity project = get_ctx_project(uctx);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);

		if(date == null)
			throw new WebApplicationException("Date is required for event.");
		if(empty(name))
			throw new WebApplicationException("Name is required for event.");

		return ENTITY_TO_OBJECT(create_event(user, project, name, new Date(date.longValue()), description));
	}
	
	@Export
	public OBJECT UpdateEvent(UserApplicationContext uctx,long evt_id, String name, Double date, String description) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity evt = get_event(evt_id);
		Entity project = get_project_for_event(evt);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);

		if(date == null)
			throw new WebApplicationException("Date is required for event.");
		if(empty(name))
			throw new WebApplicationException("Name is required for event.");

		return ENTITY_TO_OBJECT(update_event(evt, name, new Date(date.longValue()), description));
	}

	@Export
	@TransactionProtect
	public Entity DeleteEvent(UserApplicationContext uctx, long evt_id) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity evt = get_event(evt_id);
		Entity project = get_project_for_event(evt);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		return DELETE(evt);
	}










	/*******************
	 *
	 *  INTERVAL DATA COLLECTION
	 *
	 */
	private static boolean collect_debug = false;
	private int collect_data_hour_of_day;
	private int collect_data_minute_of_hour;
	private AlarmManager mgr;

	private void init_data_collect(Map<String,Object> config) throws InitializationException
	{
		if (collect_debug)
		{
			new Timer().scheduleAtFixedRate(new TimerTask()
			{
				@Override
				public void run()
				{
					do_data_collect();
				}
			}, 0, date_quantize_resolution*1000*60);
		}
		else
		{
			collect_data_hour_of_day    = GET_REQUIRED_INT_CONFIG_PARAM(PARAM_COLLECT_DATA_HOUR, config);
			collect_data_minute_of_hour = GET_REQUIRED_INT_CONFIG_PARAM(PARAM_COLLECT_DATA_MINUTE, config);
			/* dont run at all -- we do this on staging */
			if(collect_data_hour_of_day == -1)
				return;
			mgr = new AlarmManager();
			try {
		    	INFO("Scheduling Data Collect time for "+collect_data_hour_of_day+":"+collect_data_minute_of_hour);
		    	mgr.addAlarm(getName()+"DataCollector",new int[]{collect_data_minute_of_hour}, new int[]{collect_data_hour_of_day}, new int[]{-1}, new int[]{-1}, new int[]{-1}, -1,
		    		new AlarmListener()
						{
							public void handleAlarm(AlarmEntry entry)
							{
								do_data_collect();
							}
						}
				);

		    	System.out.println("MGR IS "+mgr.getAllAlarms());
				System.out.println("MGR IS "+mgr);
			} catch (PastDateException e) {
				ERROR(e);
			}
		}

	}

	@Export
	public void DoDataCollect(UserApplicationContext uctx,RawCommunique c) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		if(!PermissionEvaluator.IS_ADMIN(user))
			throw new WebApplicationException("No Permission");
		
		do_data_collect();
	}
	
	static int num_collect_errors = 0;
	static int num_collect_proxies = 0;
	private void do_data_collect()
	{
		synchronized (getApplication().getApplicationLock())
		{
			INFO("START DATA COLLECTION");
			long t1 = System.currentTimeMillis();
			num_collect_errors = 0;
			num_collect_proxies = 0;
			//TODO multiplex!
			try
			{
				PAGE_APPLY(PROJECT_ENTITY, new CALLBACK()
				{
					@Override
					public Object exec(Object...objects)
					{
						Entity project = (Entity)objects[0];
						List<Entity> proxies;
						try
						{
							proxies = get_unmasked_proxies_for_project(project);
						} catch (PersistenceException e)
						{
							ERROR("DATA COLLECTION GET PROXIES FOR PROJECT ERROR",e);
							return CALLBACK_VOID;
						}
						INFO("GETTING "+proxies.size()+" PROXY DATA FOR "+project);
						for (Entity proxy : proxies)
						{

							try{
								//proxy = FILL_DEEP(proxy);
								if((Integer)proxy.getAttribute(WIDGETDATAPROXY_FIELD_STATE) < PROXY_STATE_OK)
									continue;
								create_data_for_proxy(proxy);
								num_collect_proxies++;
							}catch(Exception e)
							{
								ERROR(e);
								try{
									UPDATE(proxy,WIDGETDATAPROXY_FIELD_STATE,PROXY_STATE_FAILED_DATA_ERROR,
											WIDGETDATAPROXY_FIELD_LAST_FAILURE_MESSAGE,e.getMessage());
									//create_data_for_proxy(proxy,info.utc_date,null);
									num_collect_errors++;
								}catch(Exception pe)
								{
									ERROR(pe);
									continue;
								}

							}
						}
						return CALLBACK_VOID;
					}
				});
			} catch (Exception e)
			{
				ERROR("DATA COLLECTION PAGE APPLY ERROR",e);
			}
			long total_time = System.currentTimeMillis() - t1;
			total_time = total_time/1000/60;//change to minutes
			IEmailModule email_module = (IEmailModule)getApplication().getModule("Email");
			Map<String,Object> params = new HashMap<String,Object>();
			params.put("message", "Total time to run was "+total_time+" minutes.\nNumber of proxies processed was "+num_collect_proxies+".\nNumber of bad proxy requests was "+num_collect_errors+".");
			try{email_module.sendEmail("support@sparkwi.se", new String[]{"topher@topher.com","david@posttool.com","gbrink@tomorrowpartners.com"}, "Sparkwi.se Data Collect", "generic.fm", params);}catch(Exception e){e.printStackTrace();}
		}

	}

	
	/* data retry stuff */	
	private Thread  data_retry_thread;
	private boolean dr_running = false;
	private Map<Long,Integer> retry_fail_count_map = new HashMap<Long,Integer>();
	private void start_data_rety_thread()
	{
		
		/* dont run at all -- we do this on staging */
		if(collect_data_hour_of_day == -1)
			return;
		
		dr_running 		  = true;
		data_retry_thread = new Thread(getName())
		{
			public void run()
			{
				while(dr_running)
				{
					synchronized (getApplication().getApplicationLock()) 
					{
						INFO("DATA RETRY THREAD RUNNING");
						Query q = new Query(WIDGETDATAPROXY_ENTITY);
						q.idx(IDX_BY_STATE);
						q.eq(PROXY_STATE_FAILED_DATA_ERROR);
						q.pageSize(20);
						try{
							PAGE_APPLY(q, new CALLBACK()
							{
								public Object exec(Object... args) throws Exception
								{
									Entity proxy = (Entity)args[0];
									Integer fail_count = retry_fail_count_map.get(proxy.getId());
									if(fail_count == null)
										fail_count = 0;
									if(fail_count >= 3)
										return CALLBACK_VOID;

									long start_time = System.currentTimeMillis();;
									try{
									
										create_data_for_proxy(proxy);
									
										UPDATE(proxy,WIDGETDATAPROXY_FIELD_STATE,PROXY_STATE_OK,
													WIDGETDATAPROXY_FIELD_LAST_FAILURE_MESSAGE,null);
										retry_fail_count_map.remove(proxy.getId());
									}catch(Exception e)
									{
										ERROR(e);
										if(System.currentTimeMillis() - start_time > 10000)//really long ones only get one chance
											retry_fail_count_map.put(proxy.getId(),3);
										else
											retry_fail_count_map.put(proxy.getId(),++fail_count);
									}
									
									if(!dr_running)
										return PAGE_APPLY_BREAK;
									else
										return CALLBACK_VOID;
								}
							});
						}catch(Exception e)
						{
							
						}
					
						/* get rid of orphaned require service selection ones */
						q = new Query(WIDGETDATAPROXY_ENTITY);
						q.idx(IDX_BY_STATE);
						q.lt(PROXY_STATE_OK);
						q.pageSize(20);
						try{
							PAGE_APPLY(q, new CALLBACK()
							{
								public Object exec(Object... args) throws Exception
								{
									Entity proxy = (Entity)args[0];

									try{
										long now = System.currentTimeMillis();
										long t1 = ((Date)proxy.getAttribute(FIELD_DATE_CREATED)).getTime();
										//if it is older than 15 minutes kill it
										//intermediate proxies should be hanging around that long
										if(t1 - now > 1000 * 60 * 15)
										{
											INFO("DELETING STUCK PROXY "+proxy);
											delete_proxy(proxy);
											
										}
									}catch(Exception e)
									{

									}
									
									if(!dr_running)
										return PAGE_APPLY_BREAK;
									else
										return CALLBACK_VOID;
								}
							});
						}catch(Exception e)
						{
							
						}
					
					
					
					}					
					INFO("DATA RETRY THREAD COMPLETE");
					try{
						
						Thread.sleep(15000 * 60);
					}catch(InterruptedException ie)
					{
						
					}
				}
			}
		};
		data_retry_thread.start();
	}
	
	
	public void onDestroy()
	{
		super.onDestroy();
		if (mgr!=null)
			mgr.removeAllAlarmsAndStop();
	
		dr_running = false;
		data_retry_thread.interrupt();
	}





	/********************
	 *
	 *  WIDGET STATE MACHINE
	 *
	 */
	class WidgetStateMachine
	{

		public Entity enter(Entity wi) throws PersistenceException, WebApplicationException
		{
			Entity proxy	      = get_proxy_for_widget_instance(wi);
			return enter(wi, proxy);
		}

		public Entity enter(Entity wi, Entity proxy) throws PersistenceException, WebApplicationException
		{
			Entity dashboard	  = get_dashboard_for_widget_instance(wi);
			Entity project		  = get_project_for_dashboard(dashboard);

			Entity widget         = get_widget_def_info_for_proxy(proxy);
			WidgetDefinition def  = get_definition_for_meta_info(proxy, widget);
			return enter_widget_instance(project, dashboard, wi, proxy, def);
		}

		private Entity enter_widget_instance(Entity project, Entity dashboard, Entity wi, Entity proxy, WidgetDefinition def) throws PersistenceException, WebApplicationException
		{
			int state = (Integer) wi.getAttribute(WIDGETINSTANCE_FIELD_STATE);
			switch (state)
			{

				case WIDGET_STATE_INIT:
					if (is_correlation(wi))
						wi.setAttribute(WIDGETINSTANCE_FIELD_STATE, WIDGET_STATE_CORRELATION_REQUIRES_MEMBERS);
					else
						wi.setAttribute(WIDGETINSTANCE_FIELD_STATE, WIDGET_STATE_REQUIRES_DATA_PROXY);
					return enter_widget_instance(project,dashboard,wi,proxy,def);

				case WIDGET_STATE_REQUIRES_DATA_PROXY:
					proxy = enter_proxy(project,dashboard,proxy,def);
					wi.setAttribute(WIDGETINSTANCE_FIELD_PROXY, proxy);
					int pstate = (Integer)proxy.getAttribute(WIDGETDATAPROXY_FIELD_STATE);
					if (pstate == PROXY_STATE_OK)
					{
						wi.setAttribute(WIDGETINSTANCE_FIELD_STATE, WIDGET_STATE_REQUIRES_PROPS);
						return enter_widget_instance(project,dashboard,wi,proxy,def);
					}
					return save(wi);
				case WIDGET_STATE_REQUIRES_PROPS:
					String[] required_props = def.get_required_props();
					OBJECT props = OBJECT.decode((String)wi.getAttribute(WIDGETINSTANCE_FIELD_PROPS));
					if(props == null)
						props = new OBJECT();

					for(int i = 0;i < required_props.length;i++)
					{
						if(empty((String)props.get(required_props[i])))
							return save(wi);
					}
					wi.setAttribute(WIDGETINSTANCE_FIELD_STATE, WIDGET_STATE_OK);
					return enter_widget_instance(project,dashboard,wi,proxy,def);					

				case WIDGET_STATE_CORRELATION_REQUIRES_MEMBERS:
					@SuppressWarnings("unchecked") List<Entity> children =
						(List<Entity>) wi.getAttribute(WIDGETINSTANCE_FIELD_CHILDREN);
					if (children.size()<2)
					{
						return save(wi);
					}
					else
					{
						wi.setAttribute(WIDGETINSTANCE_FIELD_STATE, WIDGET_STATE_OK);
						UPDATE(proxy,WIDGETDATAPROXY_FIELD_STATE, PROXY_STATE_OK);
						return enter_widget_instance(project,dashboard,wi,proxy,def);
					}
				case WIDGET_STATE_OK:	
				default:
					return save(wi);
			}
		}


		private Entity enter_proxy(Entity project, Entity dashboard, Entity proxy, WidgetDefinition def) throws PersistenceException,WebApplicationException
		{
			int state = (Integer) proxy.getAttribute(WIDGETDATAPROXY_FIELD_STATE);
			switch (state)
			{

				case PROXY_STATE_INIT:
					if (def.returnsVoid() || !def.requiresConnection())
					{
						set_dict_attr(proxy, WIDGETDATAPROXY_FIELD_SELECTOR_VALUES, "pid", Long.toHexString(proxy.getId()));
						proxy.setAttribute(WIDGETDATAPROXY_FIELD_STATE, PROXY_STATE_REQUIRES_SERVICE_SELECTION);
					}
					else if (def.requiresRawConnection())
					{
						Entity conn = authorization_module.createRawConnection(project);
						proxy.setAttribute(WIDGETDATAPROXY_FIELD_CONNECTION, conn);
						proxy.setAttribute(WIDGETDATAPROXY_FIELD_STATE, PROXY_STATE_REQUIRES_SERVICE_SELECTION);
					}
					else // use the authorization module to create a connection... or get it from the project
					{
						proxy.setAttribute(WIDGETDATAPROXY_FIELD_STATE, PROXY_STATE_REQUIRES_AUTH);
					}
					return enter_proxy(project,dashboard,proxy,def);

				case PROXY_STATE_REQUIRES_AUTH:
					String required_conn = def.get_required_connection();
					Entity existing_conn = (Entity)proxy.getAttribute(WIDGETDATAPROXY_FIELD_CONNECTION);
					if (existing_conn != null)
					{
						String type = (String)existing_conn.getAttribute(OAuthorizationModule.CONNECTION_FIELD_TYPE);
						if (required_conn.equals(type))
						{
							proxy.setAttribute(WIDGETDATAPROXY_FIELD_STATE, PROXY_STATE_REQUIRES_SERVICE_SELECTION);
							return enter_proxy(project,dashboard,proxy,def);
						}
					}
					//do we look in the project here or is it really by user//
					Entity connection = get_connection_for_project_by_type(project, required_conn);
					if (connection == null)
					{
						return save(proxy);
					}
					else
					{
						proxy.setAttribute(WIDGETDATAPROXY_FIELD_CONNECTION, connection);
						proxy.setAttribute(WIDGETDATAPROXY_FIELD_STATE, PROXY_STATE_REQUIRES_SERVICE_SELECTION);
						return enter_proxy(project,dashboard,proxy,def);
					}

				case PROXY_STATE_REQUIRES_SERVICE_SELECTION:
					if (check_proxy_selector_values_and_copy_from_siblings(proxy,def,dashboard))
					{
						proxy.setAttribute(WIDGETDATAPROXY_FIELD_STATE, PROXY_STATE_OK);
						return enter_proxy(project,dashboard,proxy,def);
					}
					else
					{
						return save(proxy);
					}

			case PROXY_STATE_OK:
				if (def.isLiveDataWidget())
				{
					return save(proxy);
				}
				
				if (def.isOwned())
				{
					try{
						create_data_for_proxy(proxy);
					}catch(Exception e)
					{
						ERROR(e);
						try{
							proxy.setAttribute(WIDGETDATAPROXY_FIELD_STATE,PROXY_STATE_FAILED_DATA_ERROR);
							proxy.setAttribute(WIDGETDATAPROXY_FIELD_LAST_FAILURE_MESSAGE,e.getMessage());

						}catch(Exception pe)
						{
							ERROR(pe);
						}
					}
					return save(proxy);
				}

				Entity match = get_matching_proxy(proxy);
				if (match==null)
				{
					System.out.println("!!!! DIDNT FIND A MATCH FOR "+proxy);
					try{
						create_data_for_proxy(proxy);
					}catch(Exception e)
					{
						ERROR(e);
						try{
							proxy.setAttribute(WIDGETDATAPROXY_FIELD_STATE,PROXY_STATE_FAILED_DATA_ERROR);
							proxy.setAttribute(WIDGETDATAPROXY_FIELD_LAST_FAILURE_MESSAGE,e.getMessage());
						}catch(Exception pe)
						{
							ERROR(pe);
						}
					}

					return save(proxy);
				}
				else
				{
					System.out.println("!!!! FOUND A MATCH FOR "+proxy);
					DELETE(proxy);//temp proxy//
					return match;
				}
				
			case PROXY_STATE_FAILED_DATA_ERROR:
				proxy.setAttribute(WIDGETDATAPROXY_FIELD_STATE, PROXY_STATE_INIT);
				return enter_proxy(project,dashboard,proxy,def);
				
				
			default:
				throw new WebApplicationException("unknown proxy state");
			}
		}

		private boolean check_proxy_selector_values_and_copy_from_siblings(Entity proxy, WidgetDefinition def, Entity dashboard) throws PersistenceException, WebApplicationException
		{
			OBJECT[] info		= def.get_service_selector_info();
			int s 				= info.length;
			int c = 0; //used to count complete selector values

			List<String> real_selector_lookup_keys = new ArrayList<String>();
			boolean has_freetext_selector = false;
			for (int i=0; i<s; i++)
			{
				int type = (Integer)info[i].get(WidgetDefinition.SERVICE_SELECTOR_TYPE);
				/* if we want to start dealing with freetext selectors and grabbing them
				 * automatically we need to match on name and value.
				 */
				if(type == WidgetDefinition.SELECTOR_TYPE_FREE_TEXT)
					has_freetext_selector = true;
				real_selector_lookup_keys.add((String)info[i].get(WidgetDefinition.SERVICE_SELECTOR_NAME));
			}
			
			for (int i=0; i<real_selector_lookup_keys.size(); i++)
			{
				String name = real_selector_lookup_keys.get(i);
				if(has_selector_value(proxy, name) || is_optional(info, name))
					c++;
			}
			
			if(c == real_selector_lookup_keys.size())
				return true;
			
			if(c == 0 && real_selector_lookup_keys.size() < 2 && !has_freetext_selector)//if we are new. otherhwise if some are set but not others don't look for a match which happens in the case of page->album when you change page//
			{
				Entity oproxy = get_first_proxy_for_connection_with_matching_selectors_set(dashboard,(Entity)proxy.getAttribute(WIDGETDATAPROXY_FIELD_CONNECTION), real_selector_lookup_keys);
				if(oproxy != null)
				{
					for(int i = 0;i < real_selector_lookup_keys.size();i++)
					{
						String oval = (String) get_dict_attr(oproxy, WIDGETDATAPROXY_FIELD_SELECTOR_VALUES, real_selector_lookup_keys.get(i));
						set_dict_attr(proxy, WIDGETDATAPROXY_FIELD_SELECTOR_VALUES, real_selector_lookup_keys.get(i), oval);
						String odisplayval = (String) get_dict_attr(oproxy, WIDGETDATAPROXY_FIELD_SELECTOR_DISPLAY_VALUES, real_selector_lookup_keys.get(i));
						set_dict_attr(proxy, WIDGETDATAPROXY_FIELD_SELECTOR_DISPLAY_VALUES, real_selector_lookup_keys.get(i), odisplayval);
					}
					return true;
				}
			}
			return false;
		}

		
		private boolean is_optional(com.pagesociety.util.OBJECT[] info, String name)
		{
			for (int i=0; i<info.length; i++)
			{
				String sname = info[i].S(WidgetDefinition.SERVICE_SELECTOR_NAME);
				if (name.equals(sname))
				{
					Object optional = info[i].get(WidgetDefinition.SERVICE_SELECTOR_OPTIONAL);
					if (optional != null)
						return (Boolean)optional;
				}
			}
			return false;
		}

		private Entity get_first_proxy_for_connection_with_matching_selectors_set(Entity dashboard, Entity connection, List<String> names) throws PersistenceException, WebApplicationException
		{
			if(connection == null)
				return null;
			List<Entity> sib_wis = get_widgets_for_dashboard(dashboard);
			for (Entity sib_wi : sib_wis)
			{
				Entity proxy = get_proxy_for_widget_instance(sib_wi);
				Entity pconn = get_connection_for_proxy(proxy);
				if (pconn==null)
					continue;
				String pconn_type = (String)pconn.getAttribute(OAuthorizationModule.CONNECTION_FIELD_TYPE);
				if (pconn_type.equals(connection.getAttribute(OAuthorizationModule.CONNECTION_FIELD_TYPE))&& pconn.getAttribute(OAuthorizationModule.CONNECTION_FIELD_UID) != null && pconn.getAttribute(OAuthorizationModule.CONNECTION_FIELD_UID).equals(connection.getAttribute(OAuthorizationModule.CONNECTION_FIELD_UID)) && has_selector_values(proxy, names))
					return proxy;
			}

			return null;
		}

		private boolean has_selector_value(Entity p,String name) throws WebApplicationException
		{
			String v = (String) get_dict_attr(p, WIDGETDATAPROXY_FIELD_SELECTOR_VALUES, name);
			return !empty(v);
		}
		
		private boolean has_selector_values(Entity p,List<String> names) throws WebApplicationException
		{
			for(int i = 0;i< names.size();i++)
			{
				String v = (String) get_dict_attr(p, WIDGETDATAPROXY_FIELD_SELECTOR_VALUES, names.get(i));
				if(empty(v))
					return false;
			}
			return true;
		}

		private Entity save(Entity e) throws PersistenceException
		{
			return UPDATE(e, e.getAttributes());
		}

		public void addConnectionToProxy(Entity proxy, Entity conn) throws PersistenceException
		{
			proxy.setAttribute(WIDGETDATAPROXY_FIELD_CONNECTION, conn);
			proxy.setAttribute(WIDGETDATAPROXY_FIELD_STATE, PROXY_STATE_REQUIRES_SERVICE_SELECTION);
			save(proxy);
		}

		public Entity updateSelectorInfo(Entity wi, Entity proxy, String selector_name, String value, String display_value) throws WebApplicationException, PersistenceException
		{
			WidgetDefinition def = get_definition_for_proxy(proxy);
			int pstate = (Integer)proxy.getAttribute(WIDGETDATAPROXY_FIELD_STATE);
			/* if someone is changing a selector value we may have to create a new proxy if one doesn't exist or find an existing one*/
			if (!def.isOwned() && pstate == PROXY_STATE_OK)
			{
				set_dict_attr(proxy, WIDGETDATAPROXY_FIELD_SELECTOR_VALUES, selector_name, value);
				set_dict_attr(proxy, WIDGETDATAPROXY_FIELD_SELECTOR_DISPLAY_VALUES, selector_name, display_value);
				Entity match = get_matching_proxy(proxy);
				if(match != null)
				{
					INFO("!!! FOUND MATCHING PROXY IN UPDATE SELECTOR "+match);
					proxy = match;
				}
				else
				{
					INFO("!!! DIDNT FIND PROXY IN UPDATE SELECTOR FOR "+proxy);
					proxy = clone_proxy(proxy);					
				}

				wi.setAttribute(WIDGETINSTANCE_FIELD_PROXY, proxy);
				wi.setAttribute(WIDGETINSTANCE_FIELD_STATE, WIDGET_STATE_REQUIRES_DATA_PROXY);
				save(wi);
			}
			
			set_dict_attr(proxy, WIDGETDATAPROXY_FIELD_SELECTOR_VALUES, selector_name, value);
			set_dict_attr(proxy, WIDGETDATAPROXY_FIELD_SELECTOR_DISPLAY_VALUES, selector_name, display_value);
			proxy.setAttribute(WIDGETDATAPROXY_FIELD_STATE, PROXY_STATE_REQUIRES_SERVICE_SELECTION);
			save(proxy);
			reset(wi);
			return wi;
		}

		public void reset(Entity wi)
		{
			wi.setAttribute(WIDGETINSTANCE_FIELD_STATE, WIDGET_STATE_INIT);
		}
	}







	/*******************
	 *
	 *  USER & REGISTRATION MODULE LISTENER
	 *
	 */

	@Override
	public void onEvent(Module src, ModuleEvent e)
			throws WebApplicationException {

		switch(e.type)
		{
		case UserModule.EVENT_USER_LOGGED_IN:
			{
				try{
					setup_initial_user_project_context(getApplication().getCallingUserContext());
				}catch(PersistenceException pe)
				{
					pe.printStackTrace();
					throw new WebApplicationException("Unable to setup initial editing context "+pe.getMessage());
				}
			}
			case RegistrationModule.REGISTRATION_EVENT_ACCOUNT_ACTIVATED:
			case ForgotPasswordModule.EVENT_USER_FORGOT_PASSWORD_LOGGED_IN:
				try{
					setup_initial_user_project_context(getApplication().getCallingUserContext());
				}catch(PersistenceException pe)
				{
					pe.printStackTrace();
					throw new WebApplicationException("Unable to setup initial editing context "+pe.getMessage());
				}
				break;
		}

	}

	private void delete_data_for_user(final Entity user)
	{
		try{
			PAGE_APPLY(WIDGETDATAPROXY_ENTITY, new CALLBACK()
			{
				public Object exec(Object... args) throws Exception
				{
					Entity e = (Entity)args[0];
					if(user.equals(e.getAttribute(FIELD_CREATOR)))
						DELETE(e);
					return null;
				}
			});

			PAGE_APPLY(WIDGETINSTANCE_ENTITY, new CALLBACK()
			{
				public Object exec(Object... args) throws Exception
				{
					Entity e = (Entity)args[0];
					if(user.equals(e.getAttribute(FIELD_CREATOR)))
						DELETE(e);
					return null;
				}
			});

			PAGE_APPLY(OAuthorizationModule.CONNECTION_ENTITY, new CALLBACK()
			{
				public Object exec(Object... args) throws Exception
				{
					Entity e = (Entity)args[0];
					if(user.equals(e.getAttribute(FIELD_CREATOR)))
						DELETE(e);
					return null;
				}
			});

		}catch(Exception e)
		{
			ERROR(e);
		}

	}




	/**************************
	 *
	 *  GETS
	 *
	 */

	private WidgetDefinition get_definition_for_widget_instance(Entity wi) throws PersistenceException, WebApplicationException
	{
		Entity proxy	      = EXPAND( (Entity) wi.getAttribute(WIDGETINSTANCE_FIELD_PROXY) );
		FILL_REF(proxy, WIDGETDATAPROXY_FIELD_CONNECTION);
		Entity widget         = EXPAND( (Entity) proxy.getAttribute(WIDGETDATAPROXY_FIELD_WIDGET) );
		return get_definition_for_meta_info(proxy, widget);
	}

	private WidgetDefinition get_definition_for_proxy(Entity proxy) throws PersistenceException, WebApplicationException
	{
		FILL_REF(proxy, WIDGETDATAPROXY_FIELD_CONNECTION);
		Entity widget         = EXPAND( (Entity) proxy.getAttribute(WIDGETDATAPROXY_FIELD_WIDGET) );
		return get_definition_for_meta_info(proxy, widget);
	}

	private WidgetDefinition get_definition_for_meta_info(Entity proxy, Entity widget) throws PersistenceException, WebApplicationException
	{
		return WidgetDefinition.create(proxy,widget);
	}

	private Entity get_user(UserApplicationContext uctx)
	{
		return (Entity) uctx.getUser();
	}

	private Entity get_dashboard(long dash_id) throws PersistenceException
	{
		return GET(DASHBOARD_ENTITY, dash_id);
	}
	
	private Entity get_dashboard(String uuid) throws PersistenceException,WebApplicationException
	{
		return GET_ONE(DASHBOARD_ENTITY, IDX_BY_UUID, uuid);
	};

	private Entity get_dashboard_for_widget_instance(Entity wi) throws PersistenceException
	{
		return EXPAND( (Entity) wi.getAttribute(WIDGETINSTANCE_FIELD_PARENT) );
	}

	private Entity get_widget_instance(long wi_id) throws PersistenceException
	{
		return GET(WIDGETINSTANCE_ENTITY, wi_id);
	}
	
	private Entity get_widget_instance(String uuid) throws PersistenceException,WebApplicationException
	{
		return GET_ONE(WIDGETINSTANCE_ENTITY, IDX_BY_UUID, uuid);
	};


	private Entity fill_and_mask(Entity e) throws PersistenceException
	{
		return FILL_DEEP_AND_MASK(e,FILL_ALL_FIELDS,SPARKWISE_MASK_FIELDS);
	}

//	private Entity fill_deep(Entity e) throws PersistenceException
//	{
//		return FILL_DEEP(e);
//	}

	private Entity get_ctx_project(UserApplicationContext uctx) throws PersistenceException, WebApplicationException
	{
		Entity project = (Entity)uctx.get(PROJECT_CTX_KEY);
		return project;
	}

	private void set_ctx_project(UserApplicationContext uctx,Entity project) throws PersistenceException, WebApplicationException
	{
		uctx.setProperty(PROJECT_CTX_KEY,project);
	}

	private Entity get_project(String uuid) throws WebApplicationException,PersistenceException
	{
		return GET_ONE(PROJECT_ENTITY,IDX_BY_UUID,uuid);
	}
	
	private Entity get_project_for_dashboard(Entity dashboard) throws PersistenceException
	{
		return EXPAND( (Entity) dashboard.getAttribute(DASHBOARD_FIELD_PARENT) );
	}

	private Entity get_project_for_connection(Entity conn) throws PersistenceException
	{
		return EXPAND( (Entity) conn.getAttribute(OAuthorizationModule.CONNECTION_FIELD_PARENT) );
	}

	private Entity get_widget_def_info(long wid_def_id) throws PersistenceException
	{
		return GET(WIDGET_ENTITY, wid_def_id);
	}

	private Entity get_widget_def_info_for_proxy(Entity proxy) throws PersistenceException
	{
		return EXPAND( (Entity) proxy.getAttribute(WIDGETDATAPROXY_FIELD_WIDGET) );
	}

	private Entity get_connection_for_proxy(Entity proxy) throws PersistenceException
	{
		return EXPAND( (Entity) proxy.getAttribute(WIDGETDATAPROXY_FIELD_CONNECTION) );
	}

	private Entity get_connection(long conn_id) throws PersistenceException
	{
		return GET(OAuthorizationModule.CONNECTION_ENTITY, conn_id);
	}

	private Entity get_event(long evt_id) throws PersistenceException
	{
		return GET(EVENT_ENTITY, evt_id);
	}

	private Entity get_project_for_event(Entity evt) throws PersistenceException
	{
		return EXPAND( (Entity) evt.getAttribute(EVENT_FIELD_PARENT) );
	}

	private Entity get_proxy_for_widget_instance(Entity wi) throws PersistenceException
	{
		return EXPAND( (Entity) wi.getAttribute(WIDGETINSTANCE_FIELD_PROXY) );
	}

	private Entity get_proxy(long id) throws PersistenceException
	{
		return GET(WIDGETDATAPROXY_ENTITY, id);
	}

	private List<Entity> get_proxies_for_project(Entity project) throws PersistenceException
	{
		Query q = new Query(WIDGETDATAPROXY_ENTITY);
		q.idx(IDX_BY_PARENT);
		q.eq(project);
		return QUERY_FILL_DEEP_AND_MASK(q, FILL_ALL_FIELDS,SPARKWISE_MASK_FIELDS).getEntities();
	}
	
	private List<Entity> get_unmasked_proxies_for_project(Entity project) throws PersistenceException
	{
		Query q = new Query(WIDGETDATAPROXY_ENTITY);
		q.idx(IDX_BY_PARENT);
		q.eq(project);
		return QUERY_FILL_DEEP(q).getEntities();
	}

	private List<Entity> get_proxies_for_connection(Entity conn) throws PersistenceException
	{
		Query q = new Query(WIDGETDATAPROXY_ENTITY);
		q.idx(WIDGETDATAPROXY_IDX_BY_EVERYTHING);
		q.eq(Query.l(conn.getAttribute(FIELD_CREATOR), conn, Query.VAL_GLOB, Query.VAL_GLOB));
		return QUERY_FILL_DEEP_AND_MASK(q, FILL_ALL_FIELDS,SPARKWISE_MASK_FIELDS).getEntities();
	}

	private Entity get_matching_proxy(Entity proxy) throws PersistenceException
	{
		Query q = new Query(WIDGETDATAPROXY_ENTITY);
		q.idx(WIDGETDATAPROXY_IDX_BY_EVERYTHING);
		q.eq(Query.l(proxy.getAttribute(FIELD_CREATOR),
				proxy.getAttribute(WIDGETDATAPROXY_FIELD_CONNECTION),
				proxy.getAttribute(WIDGETDATAPROXY_FIELD_WIDGET),
				proxy.getAttribute(WIDGETDATAPROXY_FIELD_SELECTOR_VALUES)));
		List<Entity> r = QUERY(q).getEntities();
		r.remove(proxy);
		if (r.isEmpty())
			return null;
		else
			return FILL_DEEP(r.get(0));
	}


	private List<Entity> get_widget_instances_for_proxy(Entity proxy) throws PersistenceException
	{
		Query q = new Query(WIDGETINSTANCE_ENTITY);
		q.idx(WIDGETINSTANCE_IDX_BY_PROXY);
		q.eq(proxy);
		return QUERY(q).getEntities();
	}
	
	private List<Entity> get_corr_instances_for_proxy(Entity proxy) throws PersistenceException
	{
		Query q = new Query(WIDGETINSTANCE_ENTITY);
		q.idx(WIDGETINSTANCE_IDX_BY_CHILDREN);
		q.setContainsAny(Query.l(proxy));
		return QUERY(q).getEntities();
	}
	
	private void delete_widget_data_for_proxy(Entity proxy) throws PersistenceException,WebApplicationException
	{
		Query q = new Query(WIDGETDATA_ENTITY);
		q.idx(WIDGETDATA_IDX_BY_PROXY_BY_DATEKEY);
		q.eq(q.list(proxy,Query.VAL_GLOB));
		q.pageSize(50);
		try{
		PAGE_APPLY(q, new CALLBACK() 
		{
				public Object exec(Object... args) throws Exception
				{
					INFO("DELETEING "+args[0]);
					DELETE((Entity)args[0]);
					return null;
				}
		});
		}catch(Exception e)
		{
			ERROR(e);
			throw new WebApplicationException("FAILED IN DELETE WIDGET DATA FOR PROXY");
		}
	}

	public List<Entity> get_dashboards_for_project(Entity project) throws PersistenceException
	{
		Query q = new Query(DASHBOARD_ENTITY);
		q.idx(DASHBOARD_IDX_BY_PROJECT);
		q.eq(project);
		q.orderBy(FIELD_DATE_CREATED);
		q.cacheResults(false);
		return QUERY(q).getEntities();
	}

	private List<Entity> get_masked_widgets_for_dashboard(Entity dashboard) throws PersistenceException
	{
		Query q = new Query(WIDGETINSTANCE_ENTITY);
		q.idx(WIDGETINSTANCE_IDX_BY_DASHBOARD);
		q.eq(dashboard);
		return QUERY_FILL_DEEP_AND_MASK(q,FILL_ALL_FIELDS,SPARKWISE_MASK_FIELDS).getEntities();
	}

	private List<Entity> get_widgets_for_dashboard(Entity dashboard) throws PersistenceException
	{
		return get_widgets_for_dashboard(dashboard, true);
	}

	private List<Entity> get_widgets_for_dashboard(Entity dashboard,boolean fill) throws PersistenceException
	{
		Query q = new Query(WIDGETINSTANCE_ENTITY);
		q.idx(WIDGETINSTANCE_IDX_BY_DASHBOARD);
		q.eq(dashboard);
		if(fill) /*TODO: might want to use sparkwiuse mask */
			return QUERY_FILL_DEEP_AND_MASK(q,FILL_ALL_FIELDS,MASK_NO_FIELDS).getEntities();
		else
			return QUERY(q).getEntities();
	}

//	private List<Entity> get_projects_for_user(Entity user) throws PersistenceException
//	{
//		Query q = new Query(PROJECT_ENTITY);
//		q.idx(PROJECT_IDX_BY_USER_BY_NAME);
//		q.eq(Query.l(user,Query.VAL_GLOB));
//		return QUERY_FILL_DEEP_AND_MASK(q,FILL_ALL_FIELDS,SPARKWISE_MASK_FIELDS).getEntities();
//	}

	private List<Entity> get_proxy_data(Entity proxy, int size) throws PersistenceException
	{
		Query q = new Query(WIDGETDATA_ENTITY);
		q.idx(WIDGETDATA_IDX_BY_PROXY_BY_DATEKEY);
		q.betweenDesc(Query.l(proxy,Query.VAL_MAX),Query.l(proxy,Query.VAL_MIN));
		q.setPageSize(size);
		return QUERY(q).getEntities();
	}

	private List<Entity> get_proxy_data_in_range(Entity proxy,Date from,Date to) throws PersistenceException
	{
		Query q = new Query(WIDGETDATA_ENTITY);
		q.idx(WIDGETDATA_IDX_BY_PROXY_BY_DATEKEY);
		q.betweenDesc(Query.l(proxy,getFlooredDateString(to)),Query.l(proxy,getFlooredDateString(from)));
		return QUERY(q).getEntities();
	}

	private Entity get_widget_data_for_timekey(Entity proxy, String timekey) throws PersistenceException
	{
		Query q = new Query(WIDGETDATA_ENTITY);
		q.idx(WIDGETDATA_IDX_BY_PROXY_BY_DATEKEY);
		q.eq(Query.l(proxy,timekey));
		List<Entity> r = QUERY(q).getEntities();
		if (r.isEmpty())
			return null;
		else
			return r.get(0);
	}
	
	private Entity get_myset_for_user(Entity user) throws PersistenceException
	{
		Query q = new Query(MYSET_ENTITY);
		q.idx(MYSET_IDX_BY_USER);
		q.eq(user);
		List<Entity> r = QUERY(q).getEntities();
		if (r.isEmpty())
			return NEW(MYSET_ENTITY, user);
		else
		{
			// bug sure makes a lot of work... wish there was a way to make persistence less strict about 'data integrity'
			Entity myset = r.get(0);
			@SuppressWarnings("unchecked")
			List<Entity> widgets = (List<Entity>)myset.getAttribute(MYSET_FIELD_WIDGETS);
			List<Entity> ewidgets = new ArrayList<Entity>();
			boolean update = false;
			for (int i=0; i<widgets.size(); i++)
			{
				Entity widget = widgets.get(i);
				try {
					widget = GET(widget.getType(),widget.getId());
				}catch(PersistenceException e)
				{
					update = true;
					continue;
				}
				ewidgets.add(FILL_DEEP(widget));
			}
			if (update)
				UPDATE(myset, MYSET_FIELD_WIDGETS, ewidgets);
			myset.setAttribute(MYSET_FIELD_WIDGETS, ewidgets);
			return myset;
		}
	}


	private List<Entity> get_events(Entity project) throws PersistenceException
	{
		Query q = new Query(EVENT_ENTITY);
		q.idx(EVENT_IDX_BY_PROJECT_BY_DATE);
		q.betweenDesc(Query.l(project,Query.VAL_MAX), Query.l(project,Query.VAL_MIN));
		return QUERY(q).getEntities();
	}

	public Entity get_project_by_user_by_name(Entity user,Object name) throws PersistenceException,WebApplicationException
	{
		return FILL_DEEP_AND_MASK(GET_ONE(PROJECT_ENTITY, PROJECT_IDX_BY_USER_BY_NAME, user,name),FILL_ALL_FIELDS,MASK_EMAIL_PASSWORD);
	}

	private Entity get_widget_collection_by_user_by_name(Entity user, String name) throws PersistenceException,WebApplicationException
	{
		return FILL_DEEP_AND_MASK(GET_ONE(WIDGET_COLLECTION_ENTITY, IDX_BY_USER_BY_NAME, user,name),FILL_ALL_FIELDS,MASK_EMAIL_PASSWORD);
	}

	private List<Entity> get_connections_for_project_by_type(Entity project, String conn_type) throws PersistenceException
	{
		return authorization_module.get_connections((Entity)project.getAttribute(FIELD_CREATOR), conn_type);
	}

	private Entity get_connection_for_project_by_type(Entity project, String conn_type) throws PersistenceException
	{
		List<Entity> conns = get_connections_for_project_by_type(project,conn_type);
		if (conns.isEmpty())
			return null;
		else
			return conns.get(0);
	}

	private OBJECT get_data_for_normal_widget(Entity wi,int latest_x_days) throws PersistenceException, WebApplicationException
	{
		int num_data_points = latest_x_days;
		List<OBJECT> data = ENTITIES_TO_OBJECTS(get_proxy_data(get_proxy_for_widget_instance(wi), num_data_points));
		String displayStyle = (String)get_dict_attr(wi, WIDGETINSTANCE_FIELD_PROPS, "display-style");
		if (!displayStyle.equals("map"))
		{
			for (OBJECT e : data)
			{
				OBJECT d = e.O(WIDGETDATA_FIELD_DATA);
				System.out.println(d);
				d.remove("geo_values");
			}
		}
		return  OBJECT("values",data);
	}
	
	private OBJECT get_data_in_range_for_normal_widget(Entity wi,Date from,Date to) throws PersistenceException, WebApplicationException
	{
		return  OBJECT("values",ENTITIES_TO_OBJECTS(get_proxy_data_in_range(get_proxy_for_widget_instance(wi), from,to)));
	}


	private OBJECT get_data_for_correlation_widget(Entity wi,int latest_x_days) throws PersistenceException, WebApplicationException
	{
		@SuppressWarnings("unchecked") List<Entity> children =
				(List<Entity>)wi.getAttribute(WIDGETINSTANCE_FIELD_CHILDREN);
		ARRAY corr_data = new ARRAY();
		for (Entity c : children)
		{
			c = EXPAND(c);
			Entity widget =EXPAND( (Entity)c.getAttribute(WIDGETDATAPROXY_FIELD_WIDGET));
			if (widget==null)
				continue;//SORRY ABOUT THIS CHECK//TODO gateway should handle the null pointer better com.pagesociety.web.ErrorMessage.<init>(ErrorMessage.java:22)
			String widget_name = (String) widget.getAttribute(WIDGET_FIELD_NAME);
			corr_data.add(OBJECT("name", widget_name, "values", ENTITIES_TO_OBJECTS(get_proxy_data(c, latest_x_days))));
		}
		return OBJECT("corr", corr_data);
	}
	
	private OBJECT get_data_in_range_for_correlation_widget(Entity wi,Date from ,Date to) throws PersistenceException, WebApplicationException
	{
		@SuppressWarnings("unchecked") 
		List<Entity> children =
				(List<Entity>)wi.getAttribute(WIDGETINSTANCE_FIELD_CHILDREN);
		ARRAY corr_data = new ARRAY();
		for (Entity c : children)
		{
			c = EXPAND(c);
			Entity widget =EXPAND( (Entity)c.getAttribute(WIDGETDATAPROXY_FIELD_WIDGET));
			String widget_name = (String) widget.getAttribute(WIDGET_FIELD_NAME);
			corr_data.add(OBJECT("name", widget_name, "values", ENTITIES_TO_OBJECTS(get_proxy_data_in_range(c,from,to))));
		}
		return OBJECT("corr", corr_data);
	}

	private int get_trend_prop_values(Entity wi) throws WebApplicationException
	{
		String time_span = (String)get_dict_attr(wi, WIDGETINSTANCE_FIELD_PROPS, "time-span");
		if (time_span==null)
			time_span = "7";
		int num_data_points = 10;
		try {
			num_data_points = Integer.parseInt(time_span);
		}catch(Exception e){}
		return num_data_points;
	}









	/**********************
	 *
	 *  CREATES
	 *
	 */

	private Entity setup_initial_user_project_context(UserApplicationContext uctx) throws PersistenceException, WebApplicationException
	{

		Entity user	   = (Entity)uctx.getUser();
		Entity project = get_project_by_user_by_name(user, DEFAULT_PROJECT_NAME);
		if(project == null)
		{
			INFO("Couldn't find default project for user "+user+" creating one..");
			project = create_project(user, DEFAULT_PROJECT_NAME);
			create_dashboard(user, project, MASTER_DASHBOARD_NAME, "", new ArrayList<String>());
			uctx.setProperty("show_welcome", true);
		}
		else
		{
			INFO("Found default project for user "+user);
			INFO("Project is "+project);
		}
		set_ctx_project(uctx, project);
		return project;
	}
	
	@Export
	public void ClearWelcome(UserApplicationContext uctx)
	{
		uctx.removeProperty("show_welcome");
	}


	private Entity create_project(Entity creator,String name) throws WebApplicationException,PersistenceException
	{
		Entity project = NEW(PROJECT_ENTITY, creator,
							PROJECT_FIELD_NAME,name,
							PROJECT_FIELD_UUID,new UUID().toString());
		return project;
	}

	private Entity create_dashboard(Entity creator,Entity parent,String name,String description,List<String> tags) throws WebApplicationException,PersistenceException
	{
		Entity dashboard = NEW(DASHBOARD_ENTITY,
	    						creator,
		 						DASHBOARD_FIELD_PARENT,parent,
		 						DASHBOARD_FIELD_NAME,name,
		 						DASHBOARD_FIELD_DESCRIPTION,description,
		 						DASHBOARD_FIELD_TAGS,tags,
		 						DASHBOARD_FIELD_UUID,new UUID().toString()
		);
		return dashboard;
	}

	private Entity clone_proxy(Entity proxy) throws WebApplicationException,PersistenceException
	{

		Entity widgetdataproxy = NEW(WIDGETDATAPROXY_ENTITY,
				(Entity)proxy.getAttribute(FIELD_CREATOR),
					WIDGETDATAPROXY_FIELD_PARENT,(Entity)proxy.getAttribute(WIDGETDATAPROXY_FIELD_PARENT),
					WIDGETDATAPROXY_FIELD_WIDGET,(Entity)proxy.getAttribute(WIDGETDATAPROXY_FIELD_WIDGET),
					WIDGETDATAPROXY_FIELD_CONNECTION,(Entity)proxy.getAttribute(WIDGETDATAPROXY_FIELD_CONNECTION),
					WIDGETDATAPROXY_FIELD_SELECTOR_VALUES,(String)proxy.getAttribute(WIDGETDATAPROXY_FIELD_SELECTOR_VALUES),
					WIDGETDATAPROXY_FIELD_SELECTOR_DISPLAY_VALUES,(String)proxy.getAttribute(WIDGETDATAPROXY_FIELD_SELECTOR_DISPLAY_VALUES)
		);
		return widgetdataproxy;
	}

	private Entity create_widget_data_proxy(Entity creator, Entity project, Entity widget) throws WebApplicationException,PersistenceException
	{
		Entity widgetdataproxy = NEW(WIDGETDATAPROXY_ENTITY,
	    						creator,
		 						WIDGETDATAPROXY_FIELD_PARENT,project,
		 						WIDGETDATAPROXY_FIELD_WIDGET,widget,
		 						WIDGETDATAPROXY_FIELD_PROPS,ENCODE(new OBJECT())
		);
		return widgetdataproxy;
	}
	
	private Entity create_widget_data_proxy(Entity creator, Entity project, Entity widget,Entity connection) throws WebApplicationException,PersistenceException
	{
		Entity widgetdataproxy = NEW(WIDGETDATAPROXY_ENTITY,
	    						creator,
		 						WIDGETDATAPROXY_FIELD_PARENT,project,
		 						WIDGETDATAPROXY_FIELD_WIDGET,widget,
		 						WIDGETDATAPROXY_FIELD_CONNECTION,connection,
		 						WIDGETDATAPROXY_FIELD_PROPS,ENCODE(new OBJECT())
		);
		return widgetdataproxy;
	}

	private Entity create_normal_widget_instance(Entity creator, Entity dashboard, Entity widget_def_info, Integer[] rect,OBJECT props) throws WebApplicationException,PersistenceException
	{
		Entity project 			= get_project_for_dashboard(dashboard);
		Entity proxy 			= create_widget_data_proxy(creator,project,widget_def_info);
		WidgetDefinition def 	= WidgetDefinition.forEntity(widget_def_info);
		OBJECT dprops 			= def.get_default_props();
		if(props != null)
			dprops.putAll(props);
		return create_widget_instance(creator, dashboard, WIDGETINSTANCE_TYPE_NORMAL, proxy, null, rect, dprops);
	}

	private Entity create_correlation_widget_instance(Entity creator, Entity dashboard, Entity widget_def_info, Integer[] rect, OBJECT props) throws WebApplicationException,PersistenceException
	{
		Entity project = get_project_for_dashboard(dashboard);
		Entity proxy = create_widget_data_proxy(creator,project,widget_def_info);
		WidgetDefinition def 	= WidgetDefinition.forEntity(widget_def_info);
		OBJECT dprops 			= def.get_default_props();
		if(props != null)
			dprops.putAll(props);
		return create_widget_instance(creator, dashboard, WIDGETINSTANCE_TYPE_CORRELATION, proxy,new ArrayList<Entity>(), rect, dprops);
	}

	private Entity create_widget_instance(Entity creator, Entity dashboard, int type, Entity proxy, List<Entity> children, Integer[] rect,OBJECT props) throws WebApplicationException,PersistenceException
	{
		List<Integer> rect_list = new ArrayList<Integer>(Arrays.asList(rect));
		return create_widget_instance(creator, dashboard, type, proxy, children, rect_list, props);
	}

	private Entity create_widget_instance(Entity creator, Entity dashboard, int type, Entity proxy, List<Entity> children, List<Integer> rect_list,OBJECT props) throws WebApplicationException,PersistenceException
	{
		String sprops = null;
		if(props != null)
			sprops = OBJECT.encode(props);
		Entity widgetinstance =	NEW(WIDGETINSTANCE_ENTITY,
	    						creator,
		 						WIDGETINSTANCE_FIELD_PARENT,dashboard,
		 						WIDGETINSTANCE_FIELD_TYPE,type,
		 						WIDGETINSTANCE_FIELD_RECT,rect_list,
		 						WIDGETINSTANCE_FIELD_PROPS,sprops,
		 						WIDGETINSTANCE_FIELD_PROXY,proxy,
		 						WIDGETINSTANCE_FIELD_CHILDREN,children,
								WIDGETINSTANCE_FIELD_UUID, new UUID().toString()

		);
		return widgetinstance;
	}

	private void create_data_for_proxy(Entity proxy) throws WebApplicationException, PersistenceException
	{
		INFO("!!! create data for proxy "+proxy);
		INFO("!!! create data for user "+EXPAND(((Entity)proxy.getAttribute(FIELD_CREATOR))).getAttribute("email"));
		WidgetDefinition def = get_definition_for_proxy(proxy);

		if (def.returnsVoid())
			return;
		
		UTCDateInfo info = DateUtil.getUTCDateInfo(new Date());
		OBJECT data = null;
		
		
		try{
			data 	= def.getData();
			Date data_date 	= info.utc_date;
			if(data.get("data_date") != null)
				data_date =  (Date)data.get("data_date");
			create_data_for_proxy(proxy,data_date,data);
			UPDATE(proxy, WIDGETDATAPROXY_FIELD_LAST_UPDATED, info.utc_date);		
		}catch(WebApplicationException wae)
		{
			create_data_error_for_proxy(proxy, String.valueOf(wae.getCode()),wae.getMessage());
			UPDATE(proxy,
					WIDGETDATAPROXY_FIELD_LAST_UPDATED,info.utc_date);		
			throw wae;
		}

	}

	private Entity create_data_for_proxy(Entity proxy, Date data_date, OBJECT data) throws WebApplicationException, PersistenceException
	{
		UTCDateInfo info 	= DateUtil.getUTCDateInfo(new Date());
		Date utc_date 		= info.utc_date;
		String timekey 		= getFlooredDateString(utc_date);
		Entity wd 			= get_widget_data_for_timekey(proxy,timekey);
		
		String encoded_data = OBJECT.encode(data);
		if (wd != null)
			return UPDATE(wd, WIDGETDATA_FIELD_DATA, encoded_data);
		else
			return NEW(WIDGETDATA_ENTITY, (Entity)proxy.getAttribute(FIELD_CREATOR),
					WIDGETDATA_FIELD_DATE_UTC,data_date,
					WIDGETDATA_FIELD_PARENT, proxy,
					WIDGETDATA_FIELD_DATEKEY, timekey,
					WIDGETDATA_FIELD_DATA, encoded_data);
	}

	private Entity create_data_error_for_proxy(Entity proxy, String error_code,String error_message) throws WebApplicationException, PersistenceException
	{
		UTCDateInfo info 	= DateUtil.getUTCDateInfo(new Date());
		Date utc_date 		= info.utc_date;
		String timekey 		= getFlooredDateString(utc_date);
		Entity wd 			= get_widget_data_for_timekey(proxy,timekey);
		if (wd != null)
			return UPDATE(wd, 
					  WIDGETDATA_FIELD_ERROR_CODE, error_code,
					  WIDGETDATA_FIELD_ERROR_MESSAGE, error_message
						);
		else
			return NEW(WIDGETDATA_ENTITY, (Entity)proxy.getAttribute(FIELD_CREATOR),
				WIDGETDATA_FIELD_ERROR_CODE, error_code,
				WIDGETDATA_FIELD_ERROR_MESSAGE, error_message,
				WIDGETDATA_FIELD_DATE_UTC,utc_date,
				WIDGETDATA_FIELD_PARENT, proxy,
				WIDGETDATA_FIELD_DATEKEY, timekey);
	}


	private Entity create_event(Entity creator, Entity project, String name, Date date, String description) throws PersistenceException
	{

		Entity widgetinstance =	NEW(EVENT_ENTITY,
				creator,
					EVENT_FIELD_PARENT,project,
					EVENT_FIELD_NAME,name,
					EVENT_FIELD_DATE,date,
					EVENT_FIELD_DESCRIPTION,description
		);
		return widgetinstance;
	}
	
	private Entity update_event(Entity event,String name, Date date, String description) throws PersistenceException
	{

		Entity widgetinstance =	UPDATE(event,
					EVENT_FIELD_NAME,name,
					EVENT_FIELD_DATE,date,
					EVENT_FIELD_DESCRIPTION,description
		);
		return widgetinstance;
	}


	@Export
	@TransactionProtect
	public OBJECT Delete(UserApplicationContext uctx, long dash_id) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Entity dash = get_dashboard(dash_id);
		Entity project = get_project_for_dashboard(dash);
		GUARD(user, CAN_MANAGE_PROJECT, GUARD_INSTANCE, project);
		List<Entity> wi = get_widgets_for_dashboard(dash,false);
		for(int i = 0;i < wi.size();i++ )
		{
			DELETE(wi.get(i));
		}

		return ENTITY_TO_OBJECT(DELETE(dash));
	}
	
	@Export
	@TransactionProtect
	public OBJECT DeleteAccount(UserApplicationContext uctx) throws PersistenceException, WebApplicationException
	{
		Entity user = get_user(uctx);
		Query q = new Query(PROJECT_ENTITY);
		q.idx(PROJECT_IDX_BY_USER_BY_NAME);
		q.eq(q.list(user,Query.VAL_GLOB));
		
		List<Entity> projects = QUERY(q).getEntities();
		for(int i = 0;i < projects.size();i++)
		{
			Entity project 			 = projects.get(i);
			List<Entity> dashboards = get_dashboards_for_project(project);
			for(int ii = 0; ii < dashboards.size();ii++)
			{
				Entity dashboard = dashboards.get(ii);
				List<Entity> wi = get_widgets_for_dashboard(dashboard,false);
				for(int iii = 0;iii < wi.size();iii++ )
					DELETE(wi.get(iii));

				DELETE(dashboard);
			}
			
			List<Entity> proxies = get_proxies_for_project(project);
			for(int ii = 0;ii < proxies.size();ii++)
			{
				Entity proxy = proxies.get(ii);
				delete_proxy(proxy,false);
			}
			DELETE(project);			
		}
		
		List<Entity> connections = authorization_module.get_connections(user);
		for(int i = 0;i < connections.size();i++)
		{
			Entity connection = connections.get(i);
			DELETE(connection);
		}

		OBJECT ret = ENTITY_TO_OBJECT(((UserModule)getApplication().getModule("User")).deleteUser(user));
		INFO("SUCCESSFUL DELETE OF "+user);
		return ret;
	}
	
	
 /*****************
  * JSON SOURCE
  * 
  * */

	@Export
	public int GetNumberOfDashboards(UserApplicationContext uctx) throws PersistenceException
	{
		Query q = new Query(DASHBOARD_ENTITY);
		q.idx(Query.PRIMARY_IDX);
		q.gt(0);
		return COUNT(q);
	}
	
	@Export
	public int GetNumberOfUsers(UserApplicationContext uctx) throws PersistenceException
	{
		Query q = new Query(UserModule.USER_ENTITY);
		q.idx(Query.PRIMARY_IDX);
		q.gt(0);
		return COUNT(q);
	}







	/* CONVERT ENTITIES TO OBJECTS */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected List<OBJECT> ENTITIES_TO_OBJECTS(List ee)
	{
		for(int i = 0;i < ee.size();i++)
		{
			ee.set(i,ENTITY_TO_OBJECT((Entity)ee.get(i)));
		}
		return ee;
	}


	protected OBJECT ENTITY_TO_OBJECT(Entity e)
	{
		HashSet<String> string_object_fields = new HashSet<String>();
		string_object_fields.add(WIDGETDATAPROXY_FIELD_SELECTOR_DISPLAY_VALUES);
		string_object_fields.add(WIDGETDATAPROXY_FIELD_SELECTOR_VALUES);
		string_object_fields.add(WIDGETINSTANCE_FIELD_PROPS);
		string_object_fields.add(WIDGETDATA_FIELD_DATA);
		return ENTITY_TO_OBJECT(e, new HashMap<Object, Object>(),string_object_fields);
	}

	protected OBJECT ENTITY_TO_OBJECT(Entity e,Map<Object,Object> seen, Set<String>obj_fields)
	{
		OBJECT o = new OBJECT();
		o.put("type",e.getType());
		for(Map.Entry<String,Object> entry: e.getAttributes().entrySet())
		{
			String key = entry.getKey();
			o.put(key,entity_to_object_get_val(key,entry.getValue(),seen,obj_fields) );
		}
		if(e.getType().equals(WIDGET_ENTITY))
		{
			WidgetDefinition def = null;
			try{
				def = WidgetDefinition.forEntity(e);
				if (def!=null)
				{
					o.put("return_types",to_list(def.getReturnTypes()));
					o.put("required_connection_type", def.getRequiredConnectionType());
					o.put("service_selector_info", new ArrayList<OBJECT>(Arrays.asList(def.get_service_selector_info())));
					o.put("service_selector_names", new ArrayList<String>(Arrays.asList(def.get_service_selector_names())));
					o.put("service_selector_types", to_list(def.get_service_selector_types()));
					o.put("is_correlation",e.getAttribute(WIDGET_FIELD_CLASS_NAME).equals("se.sparkwi.web.widget.Correlation_Correlate"));
					o.put("required_properties",def.get_required_props());
				}
			}catch(WebApplicationException we)
			{
				ERROR(we);
			}
			
		}
		
		
		return o;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object entity_to_object_get_val(String key,Object val,Map<Object,Object> seen,  Set<String>obj_fields)
	{
		if( val == null)
			return null;
		else if(val instanceof Entity)
		{
			if(seen.containsKey(val))
				return seen.get(val);
			Object o = ENTITY_TO_OBJECT((Entity)val);
			seen.put(val, o);
			return o;
		}
		else if(val instanceof List)
		{
			List ll = (List) val;
			for(int i = 0;i < ll.size();i++)
				ll.set(i, entity_to_object_get_val(key,ll.get(i),seen,obj_fields));
			return ll;
		}
		else if (val instanceof String && obj_fields.contains(key))
		{
			try {
				return OBJECT.decode((String)val); //NOTE: Some fields are 'Objects' serialized into a String... this converts them
				// we could instead create a map of type/fields that are these kinds of special strings...
			} catch (Exception e)
			{
				return val;
			}
		}
		else
			return val;
	}
	
	private void make_uuids() throws InitializationException
	{
		
		try
		{
			PAGE_APPLY(DASHBOARD_ENTITY, new CALLBACK()
			{
				public Object exec(Object... args) throws Exception
				{

					Entity board = (Entity) args[0];
					if (board.getAttribute(DASHBOARD_FIELD_UUID) == null)
					{
						String uuid = new UUID().toString();
						UPDATE(board, DASHBOARD_FIELD_UUID, uuid);
					}
					if (board.getAttribute(DASHBOARD_FIELD_PUBLIC) == null)
					{
						UPDATE(board, DASHBOARD_FIELD_PUBLIC, false);
					}
					return null;
				}
			});

			PAGE_APPLY(PROJECT_ENTITY, new CALLBACK()
			{
				public Object exec(Object... args) throws Exception
				{
					Entity project = (Entity) args[0];
					if (project.getAttribute(PROJECT_FIELD_UUID) == null)
						UPDATE(project, PROJECT_FIELD_UUID, new UUID().toString());

					return null;
				}
			});

			PAGE_APPLY(WIDGETINSTANCE_ENTITY, new CALLBACK()
			{
				public Object exec(Object... args) throws Exception
				{
					Entity widgetinstance = (Entity) args[0];
					if (widgetinstance.getAttribute(WIDGETINSTANCE_FIELD_UUID) == null)
						UPDATE(widgetinstance, WIDGETINSTANCE_FIELD_UUID, new UUID().toString());

					return null;
				}
			});

		}
		catch (Exception e)
		{
			throw new InitializationException("FAILED DURING MAKE UUID", e);
		}
	}



	/* DATE FLOOR STUFF */

	private String getFlooredDateString(Date d)
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
		return formatter.format(floorDate(d));
	}

	private Date floorDate(Date d)
	{
		return DateUtil.floorDate(d,date_quantize_unit,date_quantize_resolution);
	}





	/************************
	 *
	 *  ENTITY DEFINITIONS & INDEXES
	 *
	 */

	public static final String PROJECT_ENTITY 			= "Project";
	public static final String PROJECT_FIELD_NAME 		= "name";
	public static final String PROJECT_FIELD_UUID 		= "uuid";

	public static final String DASHBOARD_ENTITY 			= "Dashboard";
	public static final String DASHBOARD_FIELD_PARENT 		= "parent";
	public static final String DASHBOARD_FIELD_NAME 		= "name";
	public static final String DASHBOARD_FIELD_DESCRIPTION 	= "description";
	public static final String DASHBOARD_FIELD_TAGS 		= "tags";
	public static final String DASHBOARD_FIELD_PUBLIC 	 	= "public";
	public static final String DASHBOARD_FIELD_PUBLIC_NAME 	 	= "public_name";
	public static final String DASHBOARD_FIELD_PUBLIC_ORG 	 	= "public_org";
	public static final String DASHBOARD_FIELD_UUID 		= "uuid";

	public static final int PROXY_STATE_INIT							= 0x00;
	public static final int PROXY_STATE_REQUIRES_AUTH					= 0x10;
	public static final int PROXY_STATE_REQUIRES_SERVICE_SELECTION		= 0x20;
	public static final int PROXY_STATE_OK						        = 0x30;
	public static final int PROXY_STATE_FAILED_DATA_ERROR 				= 0x40;

	public static final String WIDGETDATAPROXY_ENTITY 			   				= "WidgetDataProxy";
	public static final String WIDGETDATAPROXY_FIELD_PARENT 		   			= "parent";
	public static final String WIDGETDATAPROXY_FIELD_STATE 		   				= "state";
	public static final String WIDGETDATAPROXY_FIELD_LAST_FAILURE_MESSAGE   	= "last_failure_message";
	public static final String WIDGETDATAPROXY_FIELD_SELECTOR_VALUES 			= "selector_values";
	public static final String WIDGETDATAPROXY_FIELD_SELECTOR_DISPLAY_VALUES 	= "selector_display_values";
	public static final String WIDGETDATAPROXY_FIELD_WIDGET 					= "widget";
	public static final String WIDGETDATAPROXY_FIELD_CONNECTION 				= "connection";
	public static final String WIDGETDATAPROXY_FIELD_PROPS						= "props";
	public static final String WIDGETDATAPROXY_FIELD_LAST_UPDATED				= "last_updated";


	public static final String WIDGETDATA_ENTITY 					= "WidgetData";
	public static final String WIDGETDATA_FIELD_DATE_UTC 			= "date_utc";
	public static final String WIDGETDATA_FIELD_PARENT 				= "parent";
	public static final String WIDGETDATA_FIELD_DATEKEY 			= "datekey";
	public static final String WIDGETDATA_FIELD_DATA 				= "data";
	public static final String WIDGETDATA_FIELD_ERROR_CODE 			= "error_code";
	public static final String WIDGETDATA_FIELD_ERROR_MESSAGE 		= "error_message";
	public static final String WIDGETDATA_FIELD_ANNOTATION	 		= "annotation";
	
	public static final int WIDGETINSTANCE_TYPE_NORMAL				= 0;
	public static final int WIDGETINSTANCE_TYPE_CORRELATION			= 1;

	public static final int WIDGET_STATE_INIT 							= 0x00;
	public static final int WIDGET_STATE_REQUIRES_DATA_PROXY 			= 0x10;
	public static final int WIDGET_STATE_REQUIRES_PROPS					= 0x15;
	public static final int WIDGET_STATE_CORRELATION_REQUIRES_MEMBERS	= 0x20;
	public static final int WIDGET_STATE_OK 							= 0x40;

	public static final String WIDGETINSTANCE_ENTITY 			= "WidgetInstance";
	public static final String WIDGETINSTANCE_FIELD_PARENT 		= "parent";
	public static final String WIDGETINSTANCE_FIELD_STATE 		= "state";
	public static final String WIDGETINSTANCE_FIELD_TYPE 		= "type";
	public static final String WIDGETINSTANCE_FIELD_NAME 		= "name";
	public static final String WIDGETINSTANCE_FIELD_RECT 		= "rect";
	public static final String WIDGETINSTANCE_FIELD_PROPS 		= "props";
	public static final String WIDGETINSTANCE_FIELD_PROXY 		= "proxy";
	public static final String WIDGETINSTANCE_FIELD_CHILDREN 	= "children";
	public static final String WIDGETINSTANCE_FIELD_UUID 		= "uuid";
	public static final String WIDGETINSTANCE_FIELD_PUBLIC 		= "public";

	public static final String MYSET_ENTITY 					= "MySet";
	public static final String MYSET_FIELD_WIDGETS 				= "widgets";
	
	public static final String WIDGET_COLLECTION_ENTITY 				= "WidgetCollection";
	public static final String WIDGET_COLLECTION_FIELD_NAME 			= "name";
	public static final String WIDGET_COLLECTION_FIELD_CATEGORIES 	    = "categories";

	public static final String WIDGET_COLLECTION_CATEGORY_ENTITY	    	= "WidgetCollectionCategory";
	public static final String WIDGET_COLLECTION_CATEGORY_FIELD_NAME		= "name";
	public static final String WIDGET_COLLECTION_CATEGORY_FIELD_WIDGETS	  	= "widgets";

	public static final String WIDGET_ENTITY 				= "Widget";
	public static final String WIDGET_FIELD_NAME 			= "name";
	public static final String WIDGET_FIELD_DESCRIPTION 	= "description";
	public static final String WIDGET_FIELD_CLASS_NAME 		= "class_name";

	public static final String EVENT_ENTITY 				= "Event";
	public static final String EVENT_FIELD_DATE_UTC 		= "date_utc";
	public static final String EVENT_FIELD_PARENT 			= "parent";
	public static final String EVENT_FIELD_NAME 			= "name";
	public static final String EVENT_FIELD_DESCRIPTION 		= "description";
	public static final String EVENT_FIELD_DATE 			= "date";



	protected void defineEntities(Map<String,Object> config) throws PersistenceException, InitializationException
	{
		DEFINE_ENTITY(PROJECT_ENTITY,
			PROJECT_FIELD_NAME,Types.TYPE_STRING,null,
			PROJECT_FIELD_UUID,Types.TYPE_STRING,null	
		);
		DEFINE_ENTITY(DASHBOARD_ENTITY,
			DASHBOARD_FIELD_PARENT,Types.TYPE_REFERENCE, PROJECT_ENTITY,null,
			DASHBOARD_FIELD_NAME,Types.TYPE_STRING,null,
			DASHBOARD_FIELD_DESCRIPTION,Types.TYPE_TEXT,null,
			DASHBOARD_FIELD_TAGS,Types.TYPE_ARRAY|Types.TYPE_STRING,null,
			DASHBOARD_FIELD_PUBLIC,Types.TYPE_BOOLEAN,false,
			DASHBOARD_FIELD_PUBLIC_NAME,Types.TYPE_STRING,null,
			DASHBOARD_FIELD_PUBLIC_ORG,Types.TYPE_STRING,null,
			DASHBOARD_FIELD_UUID,Types.TYPE_STRING,null
			);
		
		
		DEFINE_ENTITY(WIDGETDATAPROXY_ENTITY,
			WIDGETDATAPROXY_FIELD_PARENT,Types.TYPE_REFERENCE, PROJECT_ENTITY,null,
			WIDGETDATAPROXY_FIELD_STATE,Types.TYPE_INT,PROXY_STATE_INIT,
			WIDGETDATAPROXY_FIELD_SELECTOR_VALUES,Types.TYPE_STRING,null,
			WIDGETDATAPROXY_FIELD_LAST_FAILURE_MESSAGE,Types.TYPE_STRING,null,
			WIDGETDATAPROXY_FIELD_SELECTOR_DISPLAY_VALUES,Types.TYPE_STRING,null,
			WIDGETDATAPROXY_FIELD_WIDGET,Types.TYPE_REFERENCE, WIDGET_ENTITY,null,
			WIDGETDATAPROXY_FIELD_CONNECTION,Types.TYPE_REFERENCE, OAuthorizationModule.CONNECTION_ENTITY,null,
			WIDGETDATAPROXY_FIELD_PROPS,Types.TYPE_STRING,null,
			WIDGETDATAPROXY_FIELD_LAST_UPDATED,Types.TYPE_DATE,null

			);
		
		DEFINE_ENTITY(WIDGETDATA_ENTITY,
			WIDGETDATA_FIELD_DATE_UTC,Types.TYPE_DATE,null,
			WIDGETDATA_FIELD_PARENT,Types.TYPE_REFERENCE, WIDGETDATAPROXY_ENTITY,null,
			WIDGETDATA_FIELD_DATEKEY,Types.TYPE_STRING,null,
			WIDGETDATA_FIELD_DATA,Types.TYPE_STRING,null,
			WIDGETDATA_FIELD_ERROR_CODE,Types.TYPE_STRING,null,
			WIDGETDATA_FIELD_ERROR_MESSAGE,Types.TYPE_STRING,null);

		
		DEFINE_ENTITY(WIDGETINSTANCE_ENTITY,
			WIDGETINSTANCE_FIELD_PARENT,Types.TYPE_REFERENCE, DASHBOARD_ENTITY,null,
			WIDGETINSTANCE_FIELD_TYPE,Types.TYPE_INT,null,
			WIDGETINSTANCE_FIELD_STATE,Types.TYPE_INT,WIDGET_STATE_INIT,
			WIDGETINSTANCE_FIELD_NAME,Types.TYPE_STRING,null,
			WIDGETINSTANCE_FIELD_RECT,Types.TYPE_ARRAY|Types.TYPE_INT,null,
			WIDGETINSTANCE_FIELD_PROPS,Types.TYPE_STRING,null,
			WIDGETINSTANCE_FIELD_PROXY,Types.TYPE_REFERENCE, WIDGETDATAPROXY_ENTITY,null,
			WIDGETINSTANCE_FIELD_CHILDREN,Types.TYPE_ARRAY|Types.TYPE_REFERENCE, WIDGETDATAPROXY_ENTITY,null,
			WIDGETINSTANCE_FIELD_UUID,Types.TYPE_STRING,null,
			WIDGETINSTANCE_FIELD_PUBLIC,Types.TYPE_BOOLEAN,false);
		
		DEFINE_ENTITY(MYSET_ENTITY,
				MYSET_FIELD_WIDGETS, Types.TYPE_REFERENCE|Types.TYPE_ARRAY, WIDGETINSTANCE_ENTITY, EMPTY_LIST);

		DEFINE_ENTITY(WIDGET_COLLECTION_ENTITY,
				WIDGET_COLLECTION_FIELD_NAME,Types.TYPE_STRING,null,
				WIDGET_COLLECTION_FIELD_CATEGORIES,Types.TYPE_REFERENCE|Types.TYPE_ARRAY,WIDGET_COLLECTION_CATEGORY_ENTITY,EMPTY_LIST);

		DEFINE_ENTITY(WIDGET_COLLECTION_CATEGORY_ENTITY,
				WIDGET_COLLECTION_CATEGORY_FIELD_NAME,Types.TYPE_STRING,null,
				WIDGET_COLLECTION_CATEGORY_FIELD_WIDGETS,Types.TYPE_REFERENCE|Types.TYPE_ARRAY,WIDGET_ENTITY,EMPTY_LIST);

		DEFINE_ENTITY(WIDGET_ENTITY,
				WIDGET_FIELD_NAME,Types.TYPE_STRING,null,
				WIDGET_FIELD_DESCRIPTION,Types.TYPE_TEXT,null,
				WIDGET_FIELD_CLASS_NAME,Types.TYPE_STRING,null);
		DEFINE_ENTITY(EVENT_ENTITY,
				EVENT_FIELD_DATE_UTC,Types.TYPE_DATE,null,
				EVENT_FIELD_PARENT,Types.TYPE_REFERENCE, PROJECT_ENTITY,null,
				EVENT_FIELD_NAME,Types.TYPE_STRING,null,
				EVENT_FIELD_DESCRIPTION,Types.TYPE_TEXT,null,
				EVENT_FIELD_DATE,Types.TYPE_DATE,null);

	}

	public static final String PROJECT_IDX_BY_USER_BY_NAME 				= "Project_ByUserByName";
	public static final String DASHBOARD_IDX_BY_PROJECT 				= "Dashboard_ByProject";
	public static final String WIDGETINSTANCE_IDX_BY_DASHBOARD 		 	= "WidgetInstance_byDashboard";
	public static final String WIDGETINSTANCE_IDX_BY_PROXY 				= "WidgetInstance_byProxy";
	public static final String WIDGETINSTANCE_IDX_BY_CHILDREN 			= "byChildren";
	public static final String IDX_BY_USER_BY_NAME						= "byUserByName";
	public static final String IDX_BY_PARENT 							= "byParent";

	public static final String IDX_BY_STATE 							= "byState";
	public static final String WIDGETDATAPROXY_IDX_BY_EVERYTHING 		= "byUserByConnectionByWidgetBySelectorVals";
	public static final String WIDGETDATA_IDX_BY_PROXY_BY_DATEKEY		= "byProxyByDateKey";
	public static final String EVENT_IDX_BY_PROJECT_BY_DATE				= "byProjectByDate";
	public static final String WIDGET_IDX_BY_CLASSNAME				    = "byClassname";
	public static final String MYSET_IDX_BY_USER					    = "byUser";

	
	public static final String IDX_BY_UUID 								= "byUUID";
	
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException, InitializationException
	{
		DEFINE_ENTITY_INDEX(WIDGET_ENTITY,WIDGET_IDX_BY_CLASSNAME,EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX,WIDGET_FIELD_CLASS_NAME);
		DEFINE_ENTITY_INDEX(PROJECT_ENTITY, PROJECT_IDX_BY_USER_BY_NAME, EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_CREATOR,PROJECT_FIELD_NAME);
		DEFINE_ENTITY_INDEX(DASHBOARD_ENTITY, DASHBOARD_IDX_BY_PROJECT, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, DASHBOARD_FIELD_PARENT);
		DEFINE_ENTITY_INDEX(WIDGETINSTANCE_ENTITY, WIDGETINSTANCE_IDX_BY_DASHBOARD, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, WIDGETINSTANCE_FIELD_PARENT);
		DEFINE_ENTITY_INDEX(WIDGETINSTANCE_ENTITY, WIDGETINSTANCE_IDX_BY_PROXY, EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, WIDGETINSTANCE_FIELD_PROXY);
		DEFINE_ENTITY_INDEX(WIDGETINSTANCE_ENTITY, WIDGETINSTANCE_IDX_BY_CHILDREN, EntityIndex.TYPE_ARRAY_MEMBERSHIP_INDEX, WIDGETINSTANCE_FIELD_CHILDREN);
		DEFINE_ENTITY_INDEX(WIDGETDATA_ENTITY, WIDGETDATA_IDX_BY_PROXY_BY_DATEKEY, EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, WIDGETDATA_FIELD_PARENT, WIDGETDATA_FIELD_DATEKEY);
		DEFINE_ENTITY_INDEX(WIDGET_COLLECTION_ENTITY, IDX_BY_USER_BY_NAME,EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_CREATOR,PROJECT_FIELD_NAME);
		DEFINE_ENTITY_INDEX(WIDGETDATAPROXY_ENTITY, WIDGETDATAPROXY_IDX_BY_EVERYTHING,EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, FIELD_CREATOR, WIDGETDATAPROXY_FIELD_CONNECTION, WIDGETDATAPROXY_FIELD_WIDGET, WIDGETDATAPROXY_FIELD_SELECTOR_VALUES);
		DEFINE_ENTITY_INDEX(WIDGETDATAPROXY_ENTITY, IDX_BY_PARENT,EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, WIDGETDATAPROXY_FIELD_PARENT);
		DEFINE_ENTITY_INDEX(WIDGETDATAPROXY_ENTITY, IDX_BY_STATE,EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, WIDGETDATAPROXY_FIELD_STATE);
		DEFINE_ENTITY_INDEX(EVENT_ENTITY, EVENT_IDX_BY_PROJECT_BY_DATE,EntityIndex.TYPE_SIMPLE_MULTI_FIELD_INDEX, EVENT_FIELD_PARENT, EVENT_FIELD_DATE);
		DEFINE_ENTITY_INDEX(PROJECT_ENTITY, IDX_BY_UUID,EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, PROJECT_FIELD_UUID);
		DEFINE_ENTITY_INDEX(DASHBOARD_ENTITY, IDX_BY_UUID,EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, DASHBOARD_FIELD_UUID);
		DEFINE_ENTITY_INDEX(WIDGETINSTANCE_ENTITY, IDX_BY_UUID,EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, WIDGETINSTANCE_FIELD_UUID);
		DEFINE_ENTITY_INDEX(MYSET_ENTITY, MYSET_IDX_BY_USER,EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, FIELD_CREATOR);
	}


	
	
	
}
