package se.sparkwi.web.module;

import java.util.List;
import java.util.Map;

import se.sparkwi.web.module.auth.OAuthorizationModule;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.user.UserModule;

public class AdminModule extends WebStoreModule
{
	private UserModule user_module;
	private SparkwiseRegistration reg_module;
	private DashboardModule dashboard_module;
	
	public void init(WebApplication app,Map<String,Object> config) throws InitializationException
	{
		super.init(app, config);
		user_module = (UserModule)getSlot("user-module");
		reg_module = (SparkwiseRegistration)getSlot("registration-module");
		dashboard_module = (DashboardModule)getSlot("dashboard-module");
	}
	
	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT("user-module", UserModule.class, true);
		DEFINE_SLOT("registration-module", SparkwiseRegistration.class, true);
		DEFINE_SLOT("dashboard-module", DashboardModule.class, true);
	}

	public void loadbang(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.loadbang(app, config);
	}
	
	@Export
	public PagingQueryResult GetInfo(UserApplicationContext uctx) throws PersistenceException, WebApplicationException
	{
		Entity u = (Entity)uctx.getUser();
		List<Integer>roles = (List<Integer>)u.getAttribute(UserModule.FIELD_ROLES);
		if (!roles.contains(UserModule.USER_ROLE_WHEEL))
			throw new WebApplicationException("NONO");
		PagingQueryResult r = user_module.getUsersByRole(UserModule.USER_ROLE_SYSTEM_USER, 0, 1000, FIELD_DATE_CREATED);
		for (Entity user : r.getEntities())
		{
			Entity project = dashboard_module.get_project_by_user_by_name(user,DashboardModule.DEFAULT_PROJECT_NAME);
			if (project == null)
				continue;
			List<Entity> boards = dashboard_module.get_dashboards_for_project(project);
			Entity info = reg_module.getUserInfo(user);
			user.setAttribute("boards", boards);
			if (info!=null)
				user.setAttribute("info", reg_module.getProps(info));
		}
		return r;
	}
}
