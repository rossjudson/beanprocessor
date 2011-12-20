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
package com.soletta.beanprocessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Indicates that JavaBean properties and other support mechanisms should be generated 
 * for the given class. The class should extend nnnBase, where nnn is the simple name of 
 * the class being generated for.
 * 
 * @author rjudson
 *
 */
@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.CLASS)
public @interface SBean {
    /** Indicates that bound properties should be generated by default. Override
     * this on a per-property basis with unbound, if you wish.
     * 
     * @return
     */
    boolean bound() default false;
    /** Sets the class that the generated base class should extend.
     * 
     * @return
     */
    Class<?> extend() default Void.class;
    /** Lists interfaces that the generated base class should implement.
     * 
     * @return
     */
    Class<?>[] implement() default {};
    /** Mandatory list of the properties to create for this JavaBean.
     * 
     * @return
     */
    SProperty[] properties();
    /** Create Guava-compatible predicates for boolean properties.
     * 
     * @return
     */
    boolean predicates() default false;
    /** Create Guava-compatible extractors for all properties not annotated 
     * with noextractor;
     * @return
     */
    boolean extractors() default false;
    
    /** Create a "fluent" API that allows chained setting of properties. 
     * 
     */
    boolean fluent() default false;
    
    /** Trigger generation of appropriate jaxb annotations.
     * 
     * @return
     */
    JAXBMemberType jaxbType() default JAXBMemberType.NONE;
    
    /** Adds javadoc to the generated bean class.
     * 
     */
    String javadoc() default "";
    
    /** Declares that properties are final by default; setters will not be generated for final properties,
     * and an init string must be specified.
     */
    boolean final_() default false;
}
