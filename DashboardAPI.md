
```
AddDashboard(String name)
```
Adds a new dashboard to the user's collection.

```
AddEvent(String name, Double date, String description) 
```
Adds an event at the designated date with a name, description.

```
AddWidget(long dash_id, long widg_id, int x, int y) 
```
Creates a widget within the specified dashboard at the designated x,y with a width and height of 1.

```
AddWidgetInstance(long dash_id, long wi_id, int x, int y) 
```
This method uses a widget instance id for creating a new widget instance on the dashboard.

```
AddWidgetToCorrelation(long corr_id, long wi_id) 
```
Adds a widget instance to the correlation.

```
DeleteAccount(com.pagesociety.web.UserApplicationContext uctx) 
```
Cleans up all data created by the user.

```
DeleteConnection(long conn_id) 
```
Deletes a connection and all related proxies as well as widget instances that refer to them.

```
DeleteDashboard(long dash_id) 
```
Deletes a dashboard and all of its widget instances, but not their proxies.

```
DeleteEvent(long evt_id) 
```
Deletes the event.

```
DeleteProxy(long proxy_id) 
```
Deletes an individual proxy and widget instances that refer to it.

```
DeleteWidget(long wi_id) 
```
Deletes a widget instance from a board.

```
DoAuthorization(RawCommunique c) 
```
When the UI needs a Connection, it interacts with DoAuthorization.

```
DoAuthorizationReturn(RawCommunique c) 
```
This is the callback url for OAuth connections.

```
DoDataCollect(RawCommunique c) 
```
Runs the data collection service for all proxies.

```
DuplicateDashboard(long orig_dash_id, String new_name) 
```
Creates a new dashboard and clones the widgets within, but links the proxies...

```
Export(long dash_id) 
```
Exports the dashboard as JSON.

```
GetDashboardEmbed(long dash_id) 
```
This service returns the dashboard data for a public or embedded dashboard.

```
GetDataForWidget(long wi_id, Double from, Double to) 
```
This version of GetDataForWidget allows widget instance data to be returned within a particular date range.

```
GetDataForWidget(long wi_id, int last_x_days) 
```
Returns the data from the last x days for a widget instance.

```
GetMySet(com.pagesociety.web.UserApplicationContext uctx) 
```
My set is a collection of widget instances (props & configured proxies).

```
GetPublishedDashboard(String uuid) 
```
GetPublishedDashboard aggregates everything a page needs to know about a published dashboard.

```
GetSelectorDataForWidget(long wi_id, String selector_name) 
```
Returns selector values for a particular proxy selector.

```
GetWidgetCollectionByName(String name) 
```
Returns a requested widget collection.

```
GetWidgetEmbed(String uuid) 
```
GetWidgetEmbed returns the widget data for a widget with a uuid.

```
GetWidgets(long dash_id) 
```
Returns a list of widget instances for a designated dashboard.

```
ListConnectionData() 
```
Adds all proxies as "children" of their connection (except none/raw connections).

```
ListConnectionProxies() 
```
Lists every proxies for all the user's connections! Wow!

```
ListConnections() 
```
Lists all user established Connections

```
ListDashboards() 
```
Returns a dashboard for a particular user.

```
ListEvents() 
```

```
PublishDashboard(long dash_id, String public_name, String public_org) 
```
Publish dashboard adds a uuid string and published flag to the dashboard data object so that it can be accessed by a url pattern.

```
RefreshProxy(long proxy_id)
```
Is this used by the UI?

```
RefreshWidgetData(long wi_id) 
```
Forces the widget to go through the state machine, causing it to be revalidated.

```
RemoveFromMySet(long wi_id)
```
Removes a widget instance from my set.

```
RemoveWidgetFromCorrelation(long corr_id, long proxy_id) 
```
Removes a widget from a correlation.

```
RenameDashboard(long dash_id, String new_name) 
```
Rename dashboard.

```
SaveToMySet(long wi_id) 
```
Adds a widget instance to "My Set".

```
SetConnectionForWidget(long wi_id, long conn_id) 
```
Sets the OAuth connection for the widget instance.

```
SetSelectorValueForWidget(long wi_id, String selector_name, String value, String display_value) 
```
Sets the selector value for a widget instance (its proxy).

```
SyncWidget(long wi_id) 
```
Forces the widget instance through the statemachine without revalidating it.

```
TagDashboard(long dash_id, String tags) 
```
Maintains a list of "tags" as an array in the dashboard.

```
UnpublishDashboard(long dash_id) 
```
UnpublishDashboard removes the publication flag.

```
UpdateEvent(long evt_id, String name, Double date, String description) 
```

```
UpdatePropsForWidget(long wi_id, Object props) 
```
Updates the display properties for a widget instance.

```
UpdateWidgetRect(long wi_id, int x, int y, int w, int h) 
```