package ptest;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import com.soletta.beanprocessor.JAXBMemberType;
import com.soletta.beanprocessor.SBean;
import com.soletta.beanprocessor.SProperty;

@XmlRootElement
@SBean(properties={
        @SProperty(name="title"),
        @SProperty(name="running", type=boolean.class, javadoc="Determines if the job is running."),
        @SProperty(name="started", type=Date.class, jaxbType=JAXBMemberType.ELEMENT),
        @SProperty(name="number", type=double.class, jaxbType=JAXBMemberType.TRANSIENT)
}, bound=true, predicates=true, extractors=true, fluent=true, jaxbType=JAXBMemberType.ATTRIBUTE)
public class TestJob extends TestJobBase {     

}     