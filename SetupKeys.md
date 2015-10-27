## OAuth Keys Setup ##

Sparkwise integrates with many OAuth enabled data sources. It provides a mechanism to authenticate with these sources on a users and gather data on their behalf. In order to provide this mechanism, the application developer must register with each of the sources. The sources will provide an application key as well as other "tokens" that must be added to a configuration file.

In all cases, once your application has been registered, the keys can be copied to your application [deployment.properties](http://code.google.com/p/sparkwise/source/browse/trunk/WebContent/WEB-INF/config/)


### Google - a.k.a Gdata ###

Register your domain and verify ownership:
https://www.google.com/accounts/ManageDomains

Once the domain is verified, you can get your keys by clicking **Manage domain** at the bottom of the page. Make sure the "Target URL path prefix" shows your domain name. If not, add it and **Save**.

### You Tube Developer Key ###

http://code.google.com/apis/youtube/dashboard/gwt/index.html

### Twitter ###

https://dev.twitter.com/apps/new
Callback URL: http://yourdomain/authorize/Twitter


### Vimeo ###

http://vimeo.com/api/applications/new
Application Callback URL: -empty-

### Facebook ###

http://developers.facebook.com/setup/

Facebook is the only service in the list that requires separate keys be created for development and production. It is the only service that verifies the application domain. So generally we setup a key for Facebook on localhost for development.

### Flickr ###

http://www.flickr.com/services/apps





