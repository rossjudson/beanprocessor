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

public enum SKind {
    /** A simple single-valued property (the default).
     * 
     */
    SIMPLE,
    /** A list property (implemented as ArrayList, unless an init is supplied.
     * 
     */
    LIST,
    /** Generates a JavaFX 2 SDK ObservableList.
     * 
     */
    OBSERVABLE_LIST
}
