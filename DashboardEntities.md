
```
Project
	name : String
	uuid : String
	dashboards : Dashboard[]

Dashboard
	parent : Project
	widgets : WidgetInstance[]
	name : String
	description : String
	tags : String[]
	is_public : boolean
	public_name : String
	public_org : String
	uuid : String

WidgetInstance
	parent : Dashboard
	proxy : WidgetDataProxy
	type : enum(init, requiresProxy, requiresMembers, ok)
	state : enum()
	name : String
	rect : int[]
	display_props : Map
	children : WidgetDataProxy[]
	is_public : boolean
	uuid : String

Widget (Definition)
	name : String
	description : String
	class_name : String

WidgetDataProxy
	parent : Project
	widget : Widget
	connection : Connection
	state : enum(init, requiresAuth, requiresServiceSelection, ok, error)
	selector_values : Map
	selector_display_values : Map
	props : Map
	last_failure_message : String
	last_updated : Date

WidgetData
	parent : WidgetDataProxy
	date_utc : Date
	date_key : String
	field_data : Map
	error_code : String
	error_message : String

Connection
	type : String
	token : String
	scopes : String

MySet
	widgets : WidgetInstance[]

WidgetCollection
	name : String
	categories : WidgetCollectionCategory[]		

WidgetCollectionCategory
	name : String
	widgets : Widget[]

Event
	parent : Project
	name : String
	description : String
	date_utc : Date

```