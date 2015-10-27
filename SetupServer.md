Here is a basic install procedure. Many of the details are up to your configuration requirements...

1. Install the required software:

```
#java,tomcat,ant
apt-get install openjdk-6-jdk
apt-get install tomcat6
apt-get install ant

#bdb with java bindings
apt-get install libdb4.7-java

#svn, imagemagick
apt-get install subversion
apt-get install imagemagick
```

2. Create a root directory for the application, say `/home/sparkwise`

3. Create a shell script to checkout, build and deploy the application. Something like the following as `/home/sparkwise/build.sh`. Most of the actual build happens in the ant task [build\_server](http://code.google.com/p/sparkwise/source/browse/trunk/build.xml).

```
APP_NAME=SparkwiseServer
APP_HOME=/home/sparkwise
WEB_RES=$APP_HOME
WEB_APPS=/var/lib/tomcat6/webapps
SVN_ROOT=https://sparkwise.googlecode.com/svn/trunk/  #your fork of the project
TOMCAT_SERVICE="/etc/init.d/tomcat6"

case $1 in

        checkout)
                cd $APP_HOME/checkout
                svn checkout $SVN_ROOT
                ;;
        build)
                cd $APP_HOME/checkout
                ant build_server
                ;;
        deploy)
                rm -rf $WEB_APPS/ROOT
                mkdir $WEB_APPS/ROOT
                cp $PS_HOME/checkout/$APP_NAME/ROOT.war $WEB_APPS/ROOT
                ;;
        everything)
                $0 checkout
                $0 build
                $TOMCAT_SERVICE stop
                $0 deploy
                $TOMCAT_SERVICE start
                ;;

        evolution)
                $0 checkout
                $0 build
                $TOMCAT_SERVICE stop
                $0 deploy
                $TOMCAT_SERVICE run
                ;;
esac

```

4. Before you `./build.sh everything`, make sure that your deployment properties are set to reflect these base directories too. Take a look at SetupKeys and [deployment.properties](http://code.google.com/p/sparkwise/source/browse/trunk/WebContent/WEB-INF/config/). In the sample below, the question marks should all be replaced with the keys you collected from the third party sites.

```
DB_ROOT_DIR                   = /home/sparkwise/sparkwise-db
DB_BACKUP_DIR                 = /home/sparkwise/sparkwise-db-backup

WEB_ROOT_DIR                  = /var/lib/tomcat6/webapps/ROOT
RESOURCE_BASE_DIR             = /var/lib/tomcat6/webapps/ROOT/resources

WEB_ROOT_URL                  = http://myserv.er
WEB_ROOT_URL_SECURE           = https://myserv.er

IMAGE_MAGICK_PATH             = /usr/bin

#required to send email confirmations of registration
SMTP_SERVER                   = smtp.mailserv.er
SMTP_SERVER_PORT              = 465
SMTP_SERVER_USERNAME          = sender@serv.er
SMTP_SERVER_PASSWORD          = ?

#application data root directories
MODULE_DATA_DIRECTORY         = /home/sparkwise/checkout/sparkwise/module-data
EMAIL_TEMPLATE_DIR            = /home/sparkwise/checkout/sparkwise/email-templates
WIDGET_CONFIG                 = /home/sparkwise/checkout/sparkwise/config/widget.conf.json

#data collection policy. -1 turns it off.
DATA_COLLECT_HOUR             = -1
DATA_COLLECT_MINUTE           = -1
DATE_QUANTIZE_UNIT            = day
DATE_QUANTIZE_RESOLUTION      = 1

#oauth (and other types of) keys
TWITTER_CONSUMER_KEY          = ?
TWITTER_CONSUMER_SECRET_KEY   = ?
VIMEO_CONSUMER_KEY            = ?
VIMEO_CONSUMER_SECRET_KEY     = ?
GDATA_CONSUMER_KEY            = ? 
GDATA_CONSUMER_SECRET_KEY     = ? 
YOUTUBE_DEVELOPER_KEY         = ?
FACEBOOK_APP_ID               = ?
FACEBOOK_SECRET_KEY           = ?
FLICKR_CONSUMER_KEY           = ?
FLICKR_SECRET_KEY             = ?
BING_APP_ID                   = ?

#s3 is used for storing images from the 'upload logo' widget
S3_USER_IMAGE_BUCKET          = mys3bucket
S3_API_KEY                    = ?
S3_SECRET_KEY                 = ?
```

5. The first time you run the application, execute `./build.sh evolution`. The output from the server starting should be routed to the console. After the database is created, you will be asked to set up an admin user. After completing this step, restart the server using the normal `start` argument. If you have a problem with "running" the tomcat service with output to the console, get in touch...