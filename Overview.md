# Introduction #

SparkwiseOS is the open source code used to run the online free service http://sparkwi.se

It can be set up and run on any Linux, Windows or Mac in a development and production mode. It is developed in Java and Javascript, mostly. SparkwiseOS relies on plenty of other open source [dependencies](Dependencies.md) which are distributed with the project.

# Architecture #
The Sparkwise application is comprised of a user interface, the dashboard data services and third party integration points. The [Dashboard API](DashboardAPI.md) contains a list of the data services, which are intended to be client agnostic. The services can been called as JSON or AMF (other gateways could be added as well).

![https://docs.google.com/drawings/pub?id=16B8tyoIMSymMYhwxE0UAgX4JVRIGtjBl4oOmG0cxkrY&w=578&h=546&.png](https://docs.google.com/drawings/pub?id=16B8tyoIMSymMYhwxE0UAgX4JVRIGtjBl4oOmG0cxkrY&w=578&h=546&.png)

Once registered, users are able to _connect_ with third party sources. For each connection, the user can create instances of _widgets_ which are arranged in _dashboards_. Widgets are composed of display properties and a _data proxy_. The data proxy consists of of a connection, a widget definition and data _selector_ settings, for example: 'Google Analytics connection for Bob', 'Get Unique Visitors', 'subgenius.com'.

Take a look at the [entities](DashboardEntities.md) for a complete overview of the data structures returned from the API.

Widgets go through many states as they are set up and as they collect data over time. Following is a simplified overview of the process. More detail can be seen at [widget states](WidgetStates.md).

![https://docs.google.com/drawings/pub?id=1CQT_A9v7rupgMiOx4l2FccatPpbDqN0KCLgknLMD5fY&w=563&h=582&.png](https://docs.google.com/drawings/pub?id=1CQT_A9v7rupgMiOx4l2FccatPpbDqN0KCLgknLMD5fY&w=563&h=582&.png)



# Setup #

SparkwiseOS aggregates data from a variety of sources on behalf of registered users. In order to do this, the third party services require that we get API keys from them. Follow the [setup instructions](SetupKeys.md) to create  OAuth application keys with third party services, such as Google Analytics and Facebook. For more information on OAuth, see http://en.wikipedia.org/wiki/OAuth

Setting up a development environment or server is simple. Take a look at SetupDevelopment and SetupServer for more information.

# Code #

The server side code is written in Java and runs under the [Servlet 2.4](http://en.wikipedia.org/wiki/Java_Servlet) specification. Most of the data services are represented in `se.sparkwi.web.module.DashboardModule` which is loaded on startup by `WEB-INF/config/application.xml`.

_Please get in touch for any reason: David Karam dkaram@sparkwi.se_

