# Sparkwise README #

_The code base is in the process of being checked in... Java code is available in truck/src, the Javascript/HTML is coming soon. Contact info is at the bottom of this document if you would like to discuss._

The Sparkwise application requires a server configured with the following software:

  * Java 6 or 7
  * BerkeleyDB 4.7 with Java bindings
  * ImageMagick
  * Tomcat 6 (or compatible web container)

The following configuration will work for Debian/Ubuntu:
```
# apt-get install openjdk-6-jdk
# apt-get install tomcat6
# apt-get install libdb4.7-java
# apt-get install imagemagick
```

Use Ant (build.xml) to create a deployable WAR file. Make sure to set up deployment.properties
with appropriate deployment values. S3 is used by default to host images for Upload widget. This
can be replaced with the local file system if S3 is not an option.

Contact David Karam dkaram@sparkwi.se for more information.