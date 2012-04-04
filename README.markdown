# BeanProcessor JavaBean Generator

#What does it do?

beanprocessor creates boilerplate for JavaBeans by generating a superclass. It
works with Eclipse's annotation support as a factory, and also works with 
maven 3's compiler plugin, which supports pluggable annotation processors. beanprocessor
can create getters and setters. It can also create a "fluent" setting API,
generate Guava-compatible predicates and extractor functions, and emit some
JAXB XML annotations.

There's also a tuple generation facility, for both generic tuples and specialized
tuple types including primitives. Generic tuples look like this:

```java
public class Tuple2<A, B> {

  public final A _1;
  public final B _2;
  ...
}
```

Specialized tuples use primitives, so a tuple type that holds a double and a 
String can look like this:

```java
public class DoubleString {

  public final double _1;
  public final java.lang.String _2;
  ...
}
```

beanprocessor does the things that I am interested in, right now. It's not a replacement
for the javadude annotation processor yet, if you use a lot of the features of that.

#Usage

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

##Adding Annotations

Add annotations to your bean class to generate stuff:

```java
@XmlRootElement
@SBean(properties={
        @SProperty(name="title"),
        @SProperty(name="running", type=boolean.class, javadoc="Determines if the job is running."),
        @SProperty(name="started", type=Date.class, jaxbType=JAXBMemberType.ELEMENT),
        @SProperty(name="number", type=double.class, jaxbType=JAXBMemberType.TRANSIENT)
}, bound=true, predicates=true, extractors=true, fluent=true, jaxbType=JAXBMemberType.ATTRIBUTE)
public class TestJob extends TestJobBase {     

}
```

Each type of generator set at the bean level, has a corresponding setting at the property level,
plus a *no* setting, to override.

*bound* at the bean level makes every property a bound property, by default. At the property
level, you can turn it off with *nobound*. Even if *bound* isn't set on the *SBean* annotation, you 
can set it on the *SProperty* annotation.

Sample output is at the end of this document. 

## Generating Tuples

Create a package-info.java file, in the package you want tuple classes generated into. On the 
package declaration, add a Tuples annotation if you want generic tuples, and/or add a 
Specialized annotation to create particular optimized tuple types. Here's an example:

```java
@Tuples(20)
@Specialize({
    @Tuple(tupleTypeName="AllPrims", value={byte.class, short.class, int.class, long.class, char.class, float.class, double.class}),
    @Tuple(value={byte.class, short.class, int.class, long.class, char.class, float.class}),
    @Tuple(tupleTypeName="DoubleString", value={double.class, String.class})
    })
package ptest;
import com.soletta.beanprocessor.Specialize;
import com.soletta.beanprocessor.Tuple;
import com.soletta.beanprocessor.Tuples;
```

Note again that you must annotate the package, in the package-info.java file.

The generated tuples support equals and hashCode. They also have a toArray() (and 
a toArray(Object[])) method.

To construct a generic tuple, call Tuples.of(field1, field2, ...).

To construct a specialized tuple, call SpecializeTuple.of(field1, field2, ...);

##Using the Guava predicates

A *HAS_* predicate is constructed for each property that isn't a primitive. Boolean properties get
an *IS_* predicate. Use them like this:

```java
    TestJob job = new TestJob();
    job.setTitle("I have a title");
        
    List<TestJob> jobs = asList(job, new TestJob());
        
    assertEquals(1, filter(jobs, TestJob.HAS_TITLE).size());
```
    
#Using the Guava extractors

A Guava function is created for each property:

```java
    Collection<Double> helloValues = Collections2.transform(jobs, TestJob.NUMBER);
    for (Double v: helloValues)
        System.out.println(v);
```

##Output

TestJob's annotations will generate the following:

```java
package ptest;

@javax.annotation.Generated(value = "com.soletta.processor.BeanProcessor")
abstract public class TestJobBase {

    protected TestJobBase() {
    }

    private java.lang.String title;

    @javax.xml.bind.annotation.XmlAttribute
    public java.lang.String getTitle() {
        return title;
    }

    public void setTitle(java.lang.String title) {
        java.lang.String oldValue = this.title;
        this.title = title;
        propertyChangeSupport.firePropertyChange("title", oldValue, title);
    }

    public TestJob title(java.lang.String fluentValue) {
        setTitle(fluentValue);
        return (TestJob) this;
    }

    public final static com.google.common.base.Predicate<TestJobBase> HAS_TITLE = new com.google.common.base.Predicate<TestJobBase>() {
        public boolean apply(TestJobBase value) {
            return value.getTitle() != null;
        }
    };
    public final static com.google.common.base.Function<TestJobBase, java.lang.String> TITLE = new com.google.common.base.Function<TestJobBase, java.lang.String>() {
        public java.lang.String apply(TestJobBase value) {
            return value.getTitle();
        }
    };

    private boolean running;

    /** Determines if the job is running. */
    @javax.xml.bind.annotation.XmlAttribute
    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        boolean oldValue = this.running;
        this.running = running;
        propertyChangeSupport.firePropertyChange("running", oldValue, running);
    }

    public TestJob running(boolean fluentValue) {
        setRunning(fluentValue);
        return (TestJob) this;
    }

    public final static com.google.common.base.Predicate<TestJobBase> IS_RUNNING = new com.google.common.base.Predicate<TestJobBase>() {
        public boolean apply(TestJobBase value) {
            return value.isRunning();
        }
    };
    public final static com.google.common.base.Function<TestJobBase, java.lang.Boolean> RUNNING = new com.google.common.base.Function<TestJobBase, java.lang.Boolean>() {
        public java.lang.Boolean apply(TestJobBase value) {
            return value.isRunning();
        }
    };

    private java.util.Date started;

    public java.util.Date getStarted() {
        return started;
    }

    public void setStarted(java.util.Date started) {
        java.util.Date oldValue = this.started;
        this.started = started;
        propertyChangeSupport.firePropertyChange("started", oldValue, started);
    }

    public TestJob started(java.util.Date fluentValue) {
        setStarted(fluentValue);
        return (TestJob) this;
    }

    public final static com.google.common.base.Predicate<TestJobBase> HAS_STARTED = new com.google.common.base.Predicate<TestJobBase>() {
        public boolean apply(TestJobBase value) {
            return value.getStarted() != null;
        }
    };
    public final static com.google.common.base.Function<TestJobBase, java.util.Date> STARTED = new com.google.common.base.Function<TestJobBase, java.util.Date>() {
        public java.util.Date apply(TestJobBase value) {
            return value.getStarted();
        }
    };

    private double number;

    @javax.xml.bind.annotation.XmlTransient
    public double getNumber() {
        return number;
    }

    public void setNumber(double number) {
        double oldValue = this.number;
        this.number = number;
        propertyChangeSupport.firePropertyChange("number", oldValue, number);
    }

    public TestJob number(double fluentValue) {
        setNumber(fluentValue);
        return (TestJob) this;
    }

    public final static com.google.common.base.Function<TestJobBase, java.lang.Double> NUMBER = new com.google.common.base.Function<TestJobBase, java.lang.Double>() {
        public java.lang.Double apply(TestJobBase value) {
            return value.getNumber();
        }
    };

    protected final java.beans.PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);

    public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    public TestJob listen(java.beans.PropertyChangeListener listener) {
        addPropertyChangeListener(listener);
        return (TestJob) this;
    }

    public TestJob listen(String propertyName, java.beans.PropertyChangeListener listener) {
        addPropertyChangeListener(propertyName, listener);
        return (TestJob) this;
    }
} // end of class definition```



