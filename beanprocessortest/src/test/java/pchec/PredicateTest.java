/* BeanProcessor -- a JavaBean generator.
 * 
 * Copyright 2012 Ross Judson
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License. You may obtain a copy of the license at http://www.apache.org/licenses/LICENSE-2.0.
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. 
 */
package pchec;

import static com.google.common.collect.Collections2.filter;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ptest.TestJob;

import com.google.common.collect.Collections2;

public class PredicateTest {

    @Test
    public void usePredicates() {
        TestJob job = new TestJob();
        job.setTitle("I have a title");
        
        List<TestJob> jobs = asList(job, new TestJob());
        
        assertEquals(1, filter(jobs, TestJob.HAS_TITLE).size());
        
        Collection<Double> helloValues = Collections2.transform(jobs, TestJob.NUMBER);
        for (Double v: helloValues)
            System.out.println(v);
        
    }
    
    @Test
    public void testFiltering() {
        assertEquals(1, filter(asList(new TestJob(), new TestJob().running(true), new TestJob()), TestJob.IS_RUNNING).size());
    }
    
    @Test
    public void usePropertyChange() {
        final TestJob job = new TestJob();
        job.setTitle("Title 1");
        job.setRunning(false);
        job.setNumber(0);
        
        final boolean [] fired = { false };
        
        job.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                fired[0] = true;
                Assert.assertSame(job, evt.getSource());
                if (evt.getPropertyName().equals("title")) {
                    assertEquals("Original value not correct", "Title 1", evt.getOldValue());
                    assertEquals("New value not correct", "Title 2", evt.getNewValue());
                } else if (evt.getPropertyName().equals("running")) {
                    assertEquals(false, evt.getOldValue());
                    assertEquals(true, evt.getNewValue());
                } else if (evt.getPropertyName().equals("number")) {
                    assertEquals(0.0, evt.getOldValue());
                    assertEquals(0.1, evt.getNewValue());
                }
            }
        });
        
        job.setTitle("Title 2");
        job.setRunning(true);
        job.setNumber(0.1);
        
        assertEquals("Property change listener didn't fire.", true, fired[0]);
    }
    
    
    @Test
    public void testFluent() {
        TestJob job = new TestJob().title("Fluent Title").running(true).number(0.1).listen(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
            }
        });
    
        assertEquals("Fluent Title", job.getTitle());
        assertEquals(true, job.isRunning());
        assertEquals(0.1, job.getNumber(), 0);
        
    }
    
}
