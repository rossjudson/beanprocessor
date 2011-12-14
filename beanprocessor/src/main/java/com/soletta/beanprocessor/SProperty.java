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

/** SProperty is nested inside an SBean to indicate and control the properties to generate.
 * 
 * @author rjudson
 *
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface SProperty {
    /** The name of the property.
     * 
     * @return
     */
    String name();
    /** The type of property.
     * 
     * @return
     */
    Class<?> type() default String.class;
    /** A fully qualitifed string to use as the type for this property, primarily useful if you want to supply a parameterized type,
     * such as ListenableFuture<String>.
     * 
     * @return
     */
    String typeString() default "";
    /** The kind of the property, which can be used to do collections generation.
     * 
     * @return
     */
    SKind kind() default SKind.SIMPLE;
    /** If the SBean has bound set, this will override it and ensure that this property is NOT bound.
     * 
     * @return
     */
    boolean unbound() default false;
    /** Forces this property to be a bound one.
     * 
     * @return
     */
    boolean bound() default false;
    /** Triggers generation of a guava-compatible predicate for this property. Non-boolean 
     * properties create a HAS_ predicate.
     * @return
     */
    boolean predicate() default false;
    /** If the SBean has predicate generation set as the default, this overrides it and ensures
     * this that property does NOT have a predicate generated for it.
     * 
     * @return
     */
    boolean nopredicate() default false;
    /** Generates a guava-compatible function that can be used to extract a single field from
     * the object.
     * @return
     */
    boolean extractor() default false;
    /** If the SBean has extractor function generation turned on by default, this overrides
     * that setting and ensures that this property does NOT have an extractor generated for it.
     * @return
     */
    boolean noextractor() default false;
    
    /** If supplied, the text string is used to initialize the property.
     * 
     * @return
     */
    String init() default "";
    
    /** Generate a fluent setter for this property.
     * 
     */
    boolean fluent() default false;
    
    /** Indicates that this property should NOT have a fluent API generated for it, when
     * fluent generation is turned on at the bean (or package) level.
     */
    boolean nofluent() default false;

    /** Trigger generation of appropriate jaxb annotations.
     * 
     * @return
     */
    JAXBMemberType jaxbType() default JAXBMemberType.UNSET;
    
    /** Adds javadoc to the generated property.
     * 
     */
    String javadoc() default "";
    
}
