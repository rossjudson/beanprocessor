package ptest;

import java.util.Date;

import com.soletta.beanprocessor.SBean;
import com.soletta.beanprocessor.SProperty;

@SBean(fxbean=true, protectedScope=true,
        properties={
        @SProperty(name="visible", type=boolean.class),
        @SProperty(name="count", type=int.class),
        @SProperty(name="hello", type=String.class),
        @SProperty(name="when", type=Date.class),
        @SProperty(name="notfx", nofxbean=true, type=boolean.class)
})  
public class FXBean extends FXBeanBase {
 
}   
   