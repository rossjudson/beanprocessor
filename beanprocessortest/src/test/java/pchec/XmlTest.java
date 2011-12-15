package pchec;

import java.util.Date;

import javax.xml.bind.JAXB;

import org.junit.Test;

import ptest.TestJob;

public class XmlTest {

    @Test
    public void checkAttrs() {
        
        TestJob j = new TestJob().title("Hello").running(true).number(22.5).started(new Date());
        JAXB.marshal(j, System.out);
        
    }
}
