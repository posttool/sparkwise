## Instructions for setting up a development environment on Mac OS X ##
(assumes Java 6 is installed)

### Eclipse ###

Get the Eclipse IDE for Java EE Developers
http://www.eclipse.org/downloads

Once installed, start it up and add subversion (subclipse) with these instructions (using the update manager) http://subclipse.tigris.org/servlets/ProjectProcess?pageID=p4wYuA

### Initial Import ###

Restart Eclipse (as prompted) go to File > Import > SVN > Checkout project from SVN

Add  the Google code repository and then checkout the project.

### Tomcat ###

  * Download tomcat 6 (core binary) at http://tomcat.apache.org/download-60.cgi
  * Extract it to you applications directory (or anywhere, really)
  * Now tell Eclipse about it- go to File > New > Other > server
  * In the wizard choose Apache Tomcat 6
  * Next to 'server runtime environment' click 'Add'
  * in the 'new runtime' popup, browse to the Tomcat installation
  * click next
  * from 'Add and Remove', add Sparkwise to the right side
  * click finish

### BDB ###

Download and extract
http://download.oracle.com/berkeley-db/db-4.7.25.tar.gz

From the command line / terminal

```
#cd /path/to/db-4.7.25 
#cd build_unix
#../dist/configure --enable-java
#make
#make install
```

You also might need to tell Java about BDB
```
#ln -s /usr/lib/BerkeleyDB/lib/libdb_java..  /Library/Java/Extentions
```

Windows gets an official installer...

### ImageMagick ###

Download
http://www.imagemagick.org/script/binary-releases.php#macosx
move the extracted directory 'ImageMagick-6.6.x' to Applications

### Want to run on port 80? ###

```
#sudo ipfw add 100 fwd 127.0.0.1,8080 tcp from any to any 80 in
```


## More Eclipse info ##

Add environment variables to the run configurations for resizing images
go to run > run configurations …
click on the tomcat v6 at localhost configuration
click the environment variables tab
add these:
MAGICK\_HOME               /Applications/ImageMagick-6.6.X
DYLD\_LIBRARY\_PATH     /Applications/ImageMagick-6.6.X/lib

### Deployment Properties ###

in the 'package explorer' navigate to and open
[deployment.properties](http://code.google.com/p/sparkwise/source/browse/trunk/WebContent/WEB-INF/config/deployment.properties)
the one that is there by default needs to be modified for your user
and your bdb environment, tomcat as well as image magick, etc.
modify this file.