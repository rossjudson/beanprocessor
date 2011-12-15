## BeanProcessor JavaBean Generator

## UNDER CONSTRUCTION!!!

##What does it do?

beanprocessor creates boilerplate for JavaBeans by generating a superclass. It
works with Eclipse's annotation support as a factory, and also works with 
maven 3's compiler plugin, which supports pluggable annotation processors. beanprocessor
can create getters and setters. It can also create a "fluent" setting API,
generate Guava-compatible predicates and extractor functions, and emit some
JAXB XML annotations.

beanprocessor does the things that I am interested in, right now. It's not a replacement
for the javadude annotation processor yet, if you use a lot of the features of that.


Your build section needs to set the compiler to use source and targe versions of at least 1.6, and add the 
bean processor as a dependency to the compiler plugin. It's also useful to insert build-helper-maven-plugin
so Eclipse will add the generated annotation source to a source directory. Here's an example of the build 
section:

    <build>
      	<plugins>
    			<plugin>
    				<groupId>org.apache.maven.plugins</groupId>
    				<artifactId>maven-compiler-plugin</artifactId>
    				<version>2.3.2</version>
    				<configuration>
    					<source>1.6</source>
    					<target>1.6</target>
    				</configuration>
    				<dependencies>
    					<dependency>
    						<groupId>com.soletta</groupId>
    						<artifactId>beanprocessor</artifactId>
    						<version>0.1.4-SNAPSHOT</version>
    					</dependency>
    				</dependencies>
    			</plugin>
    			<plugin>
    				<groupId>org.codehaus.mojo</groupId>
    				<artifactId>build-helper-maven-plugin</artifactId>
    				<version>1.7</version>
    				<executions>
    					<execution>
    						<id>add-source</id>
    						<phase>generate-sources</phase>
    						<goals>
    							<goal>add-source</goal>
    						</goals>
    						<configuration>
    							<sources>
    								<source>target/generated-sources/annotations</source>
    							</sources>
    						</configuration>
    					</execution>
    				</executions>
    			</plugin>
    		</plugins>
    	</build>

I usually set up a separate project to contain the generated beans, then include that project in others that
want to *use* the beans. 
