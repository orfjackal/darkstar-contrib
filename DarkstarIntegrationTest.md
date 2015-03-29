# Introduction #

Darkstar Integration Test contains utility classes for easily starting up the Darkstar Server in a separate process. Utilities for deleting the generated data files after running the tests are also provided.

The sources can be downloaded from the [downloads page](http://code.google.com/p/darkstar-contrib/downloads/list). You may also [browse](http://code.google.com/p/darkstar-contrib/source/browse/trunk/darkstar-integration-test/) the source repository.

Here is a short example of using the [DarkstarServer](http://code.google.com/p/darkstar-contrib/source/browse/trunk/darkstar-integration-test/src/main/java/net/orfjackal/darkstar/integration/DarkstarServer.java) and [DebugClient](http://code.google.com/p/darkstar-contrib/source/browse/trunk/darkstar-integration-test/src/main/java/net/orfjackal/darkstar/integration/DebugClient.java) classes to run a test where the client logs in and disconnects. Later this library might contain better JUnit integration, but for now this is how you can use it:

```
final int TIMEOUT = 5000;
DarkstarServer server;
TempDirectory tempDirectory;
Thread testTimeout;


/* SET UP */

tempDirectory = new TempDirectory();
tempDirectory.create();

server = new DarkstarServer(tempDirectory.getDirectory());
server.setAppName("MyApp");
server.setAppListener(MyAppListener.class);
server.start();
server.waitForApplicationReady(TIMEOUT);

// abort the tests if they take too long
testTimeout = TimedInterrupt.startOnCurrentThread(TIMEOUT);


/* TEST */

DebugClient client = new DebugClient("localhost", server.getPort(), "username", "password");
client.login();
String event = client.events.take();
assert event.startsWith(DebugClient.LOGGED_IN) : "event: " + event;
client.logout(false);
event = client.events.take();
assert event.startsWith(DebugClient.DISCONNECTED) : "event: " + event;


/* TEAR DOWN */

// Some debug output. You may also find StreamWaiter useful for examining SystemOut/Err
System.out.println(server.getSystemOut());
System.err.println(server.getSystemErr());

testTimeout.interrupt();
server.shutdown();
tempDirectory.dispose();
```


## Using Maven ##

To use darkstar-integration-test in a Maven project, add the following dependency to your pom.xml. You also need to have Darkstar Server's libraries in at least the "test" scope.

```
    <repositories>
        <repository>
            <id>orfjackal</id>
            <url>http://repo.orfjackal.net/maven2</url>
        </repository>
    </repositories>
    <dependencies>
        <dependency>
            <groupId>net.orfjackal.darkstar-contrib</groupId>
            <artifactId>darkstar-integration-test</artifactId>
            <version>1.0.1</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
```

You must use a relatively new version of maven-surefire-plugin, or the classpaths will not be right. At least version 2.4.3 works with darkstar-integration-test (and 2.3 will _not_ work). Add the following line to your pom.xml if the default maven-surefire-plugin causes classpath problems:

```
    <build>
        <plugins>
            <plugin>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.4.3</version>
            </plugin>
        </plugins>
    </build>
```

You also need to have the BDB native libraries somewhere along the path, or alternative you may set the java.library.path system property with maven-surefire-plugin: http://maven.apache.org/plugins/maven-surefire-plugin/howto.html


# Links #

  * SVN: http://darkstar-contrib.googlecode.com/svn/trunk/darkstar-integration-test/
  * Discussion: http://www.projectdarkstar.com/component/option,com_smf/Itemid,99999999/topic,527.0
  * Developer: [Esko Luontola](http://www.orfjackal.net/) (Jackal von ÖRF)