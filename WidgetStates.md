
```
WIDGET_STATE_INIT 
WIDGET_STATE_REQUIRES_DATA_PROXY
WIDGET_STATE_CORRELATION_REQUIRES_MEMBERS
WIDGET_STATE_OK

PROXY_STATE_INIT
PROXY_STATE_REQUIRES_AUTH
PROXY_STATE_REQUIRES_SERVICE_SELECTION
PROXY_STATE_OK
PROXY_STATE_FAILED_DATA_ERROR


WidgetInstance

	case WIDGET_STATE_INIT
		if is_correlation
			widget state = WIDGET_STATE_CORRELATION_REQUIRES_MEMBERS
		else
			widget state = WIDGET_STATE_REQUIRES_DATA_PROXY

	case WIDGET_STATE_REQUIRES_DATA_PROXY
		Enter WidgetDataProxy State Transition
		if proxy state ok
			widget state = WIDGET_STATE_REQUIRES_PROPS

	case WIDGET_STATE_REQUIRES_PROPS
		if got props
			widget state = WIDGET_STATE_OK
				

	case WIDGET_STATE_CORRELATION_REQUIRES_MEMBERS
		if children.size > 1
			widget state = WIDGET_STATE_OK

	case WIDGET_STATE_OK
	


WidgetDataProxy


	case PROXY_STATE_INIT:
		if returns void || ! requires connection
			proxy state = PROXY_STATE_REQUIRES_SERVICE_SELECTION
		else if (def.requiresRawConnection())
			proxy state = PROXY_STATE_REQUIRES_SERVICE_SELECTION
		else 
			proxy state = PROXY_STATE_REQUIRES_AUTH

	case PROXY_STATE_REQUIRES_AUTH:
		if has a connection or can find one in db
			proxy state = PROXY_STATE_REQUIRES_SERVICE_SELECTION
						
	case PROXY_STATE_REQUIRES_SERVICE_SELECTION:
		if proxy_selector_values are valid or can copy_from_siblings
			proxy state = PROXY_STATE_OK

	case PROXY_STATE_OK:
		if error
			proxy state = PROXY_STATE_FAILED_DATA_ERROR
				
	case PROXY_STATE_FAILED_DATA_ERROR:
		proxy state = PROXY_STATE_INIT

```
