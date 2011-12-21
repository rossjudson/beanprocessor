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

import static java.lang.Character.toUpperCase;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

/**
 * A very simple annotation processor that generates JavaBean superclasses with
 * getters, setters, and other features. It can also create guava predicates,
 * extractor functions, and create JAXB annotations.
 * 
 * @author rjudson
 * 
 */
@SupportedOptions(value = {})
@SupportedAnnotationTypes({ "com.soletta.beanprocessor.SBean", "com.soletta.beanprocessor.SProperty" })
public class BeanProcessor extends AbstractProcessor {

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        processingEnv.getMessager().printMessage(Kind.NOTE, "Initialized BeanProcessor");
    }

//    @Override
//    public SourceVersion getSupportedSourceVersion() {
//        // Since we care very little about the structure of Java, we can allow
//        // this processing
//        // to occur with regard to language version.
//        return SourceVersion.latest();
//    }
//
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager()
                .printMessage(Kind.NOTE, String.format("Received %,d annotations in set.", annotations.size()));

        for (Element beanElement : roundEnv.getElementsAnnotatedWith(SBean.class)) {
            SBean sbean = beanElement.getAnnotation(SBean.class);
            TypeElement beanTypeElement = TypeElement.class.cast(beanElement);
            try {
                boolean generatePropertyChangeSupport = false, generateMXBeanInterface = sbean.mxbean();
                List<String> mxMethods = new ArrayList<String>();
                List<String> propertyNames = new ArrayList<String>();
                String generatedClassName = beanTypeElement.getSimpleName() + "Base";
                JavaFileObject source = processingEnv.getFiler().createSourceFile(beanTypeElement.getQualifiedName() + "Base",
                        beanElement);
                PrintWriter src = new PrintWriter(source.openOutputStream());
                try {
                    src.format("package %s;\n", packageElement(beanTypeElement).getQualifiedName());
                    src.println();

                    createClassDeclaration(src, sbean, generatedClassName);

                    // Generate a protected constructor
                    src.format("    protected %s() {}\n", generatedClassName);
                    src.println();

                    for (SProperty prop : sbean.properties()) {
                        propertyNames.add(prop.name());
                        String type = prop.typeString();
                        String boxed;
                        boolean isPrimitive;
                        if (type.length() == 0) {
                            TypeMirror mirror = mirrorType(prop);
                            type = mirror.toString();
                            boxed = type;
                            isPrimitive = mirror.getKind().isPrimitive();
                            if (isPrimitive)
                                boxed = processingEnv.getTypeUtils().boxedClass((PrimitiveType) mirror).toString();
                        } else {
                            isPrimitive = false;
                            boxed = type;
                        }

                        String capName = capitalize(prop);

                        switch (prop.kind()) {
                        case OBSERVABLE_LIST:
                            type = "javafx.rt.ObservableList<" + boxed + ">";
                            boxed = type;
                            break;
                        case LIST:
                            type = "java.util.List<" + boxed + ">";
                            boxed = type;
                            break;
                        default:
                            break;
                        }
                        boolean final_ = createField(src, sbean, prop, type, beanTypeElement, prop.kind());
                        createJavadoc(src, prop);
                        createJAXB(src, sbean, prop);
                        
                        String methodContent = createIsOrGet(src, prop, type, capName);
                        if (prop.mxbean() || (sbean.mxbean() && !prop.nomxbean())) {
                            generateMXBeanInterface = true;
                            mxMethods.add(methodContent);
                        }

                        if (!final_ && createSetter(src, sbean, prop, type, capName))
                            generatePropertyChangeSupport = true;

                        if (!final_ && (prop.fluent() || (sbean.fluent() && !prop.fluent())))
                            createFluentSetter(src, prop, type, capName, beanTypeElement);

                        if (prop.predicate() || (sbean.predicates() && !prop.nopredicate()))
                            createGuavaPredicate(src, type, capName, beanElement, beanTypeElement, isPrimitive);

                        if (prop.extractor() || (sbean.extractors() && !prop.noextractor()))
                            createGuavaExtractor(src, type, capName, beanElement, beanTypeElement, boxed);

                        src.println();
                    }

                    if (generatePropertyChangeSupport)
                        createPropertyChangeSupport(src, sbean, beanTypeElement);

                    if (generateMXBeanInterface) {
                        JavaFileObject mxbeanSource = processingEnv.getFiler().createSourceFile(beanTypeElement.getQualifiedName() + "BaseMXBean",
                                beanElement);
                        PrintWriter mxsrc = new PrintWriter(mxbeanSource.openOutputStream());
                        try {
                            mxsrc.format("package %s;\n", packageElement(beanTypeElement).getQualifiedName());
                            mxsrc.println();
                        } finally {
                            mxsrc.close();
                        }
                    }
                    
                    if (sbean.propertyEnum()) {
                        src.println();
                        src.println("    public enum Properties {");
                        
                        boolean first = true;
                        for (String propertyName: propertyNames) {
                            if (!first) 
                                src.println(",");
                            else
                                first = false;
                            src.print("        " + propertyName.toUpperCase() + "(\"" + propertyName + "\")");
                        }
                        src.println(";");
                        
                        src.println("        private String property;");
                        src.println("        private Properties(String property) { this.property = property; }");
                        src.println("        public String toString() { return property; }");
                        src.println("    }");
                    }
                    src.format("} // end of class definition\n");
                } finally {
                    src.close();
                }
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Kind.ERROR, "Unable to create source file");
            }

        }

        return true;
    }

    void createPropertyChangeSupport(PrintWriter src, SBean sbean, TypeElement typeElement) {
        src.println("    protected final java.beans.PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this); ");
        
        src.println(" /**\r\n" + 
        		"     * Add a PropertyChangeListener to the listener list.\r\n" + 
        		"     * The listener is registered for all properties.\r\n" + 
        		"     * The same listener object may be added more than once, and will be called\r\n" + 
        		"     * as many times as it is added.\r\n" + 
        		"     * If <code>listener</code> is null, no exception is thrown and no action\r\n" + 
        		"     * is taken.\r\n" + 
        		"     *\r\n" + 
        		"     * @param listener  The PropertyChangeListener to be added\r\n" + 
        		"     */");
        src.println("    public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) { propertyChangeSupport.addPropertyChangeListener(listener); }");
        
        src.println(" /**\r\n" + 
        		"     * Add a PropertyChangeListener for a specific property.  The listener\r\n" + 
        		"     * will be invoked only when a call on firePropertyChange names that\r\n" + 
        		"     * specific property.\r\n" + 
        		"     * The same listener object may be added more than once.  For each\r\n" + 
        		"     * property,  the listener will be invoked the number of times it was added\r\n" + 
        		"     * for that property.\r\n" + 
        		"     * If <code>propertyName</code> or <code>listener</code> is null, no\r\n" + 
        		"     * exception is thrown and no action is taken.\r\n" + 
        		"     *\r\n" + 
        		"     * @param propertyName  The name of the property to listen on.\r\n" + 
        		"     * @param listener  The PropertyChangeListener to be added\r\n" + 
        		"     */");
        src.println("    public void addPropertyChangeListener(String propertyName, java.beans.PropertyChangeListener listener) { propertyChangeSupport.addPropertyChangeListener(propertyName, listener); }");
        
        src.println(" /**\r\n" + 
        		"     * Remove a PropertyChangeListener from the listener list.\r\n" + 
        		"     * This removes a PropertyChangeListener that was registered\r\n" + 
        		"     * for all properties.\r\n" + 
        		"     * If <code>listener</code> was added more than once to the same event\r\n" + 
        		"     * source, it will be notified one less time after being removed.\r\n" + 
        		"     * If <code>listener</code> is null, or was never added, no exception is\r\n" + 
        		"     * thrown and no action is taken.\r\n" + 
        		"     *\r\n" + 
        		"     * @param listener  The PropertyChangeListener to be removed\r\n" + 
        		"     */");
        src.println("    public void removePropertyChangeListener(java.beans.PropertyChangeListener listener) { propertyChangeSupport.removePropertyChangeListener(listener); }");
        src.println("    public void removePropertyChangeListener(String propertyName, java.beans.PropertyChangeListener listener) { propertyChangeSupport.removePropertyChangeListener(propertyName, listener); }");
        
        src.println("/**\r\n" + 
        		"     * Check if there are any listeners for a specific property, including\r\n" + 
        		"     * those registered on all properties.  If <code>propertyName</code>\r\n" + 
        		"     * is null, only check for listeners registered on all properties.\r\n" + 
        		"     *\r\n" + 
        		"     * @param propertyName  the property name.\r\n" + 
        		"     * @return true if there are one or more listeners for the given property\r\n" + 
        		"     */");
        src.println("    public boolean hasListeners(String propertyName) { return propertyChangeSupport.hasListeners(propertyName); }");
        
        src.println(" /**\r\n" + 
        		"     * Returns an array of all the listeners that were added to the\r\n" + 
        		"     * PropertyChangeSupport object with addPropertyChangeListener().\r\n" + 
        		"     * <p>\r\n" + 
        		"     * If some listeners have been added with a named property, then\r\n" + 
        		"     * the returned array will be a mixture of PropertyChangeListeners\r\n" + 
        		"     * and <code>PropertyChangeListenerProxy</code>s. If the calling\r\n" + 
        		"     * method is interested in distinguishing the listeners then it must\r\n" + 
        		"     * test each element to see if it's a\r\n" + 
        		"     * <code>PropertyChangeListenerProxy</code>, perform the cast, and examine\r\n" + 
        		"     * the parameter.\r\n" + 
        		"     * \r\n" + 
        		"     * <pre>\r\n" + 
        		"     * PropertyChangeListener[] listeners = bean.getPropertyChangeListeners();\r\n" + 
        		"     * for (int i = 0; i < listeners.length; i++) {\r\n" + 
        		"     *   if (listeners[i] instanceof PropertyChangeListenerProxy) {\r\n" + 
        		"     *     PropertyChangeListenerProxy proxy = \r\n" + 
        		"     *                    (PropertyChangeListenerProxy)listeners[i];\r\n" + 
        		"     *     if (proxy.getPropertyName().equals(\"foo\")) {\r\n" + 
        		"     *       // proxy is a PropertyChangeListener which was associated\r\n" + 
        		"     *       // with the property named \"foo\"\r\n" + 
        		"     *     }\r\n" + 
        		"     *   }\r\n" + 
        		"     * }\r\n" + 
        		"     *</pre>\r\n" + 
        		"     *\r\n" + 
        		"     * @see PropertyChangeListenerProxy\r\n" + 
        		"     * @return all of the <code>PropertyChangeListeners</code> added or an \r\n" + 
        		"     *         empty array if no listeners have been added\r\n" + 
        		"     * @since 1.4\r\n" + 
        		"     */");
        src.println("    public java.beans.PropertyChangeListener[] getPropertyChangeListeners() { return propertyChangeSupport.getPropertyChangeListeners(); }");
        
        src.println(" /**\r\n" + 
        		"     * Returns an array of all the listeners which have been associated \r\n" + 
        		"     * with the named property.\r\n" + 
        		"     *\r\n" + 
        		"     * @param propertyName  The name of the property being listened to\r\n" + 
        		"     * @return all of the <code>PropertyChangeListeners</code> associated with\r\n" + 
        		"     *         the named property.  If no such listeners have been added,\r\n" + 
        		"     *         or if <code>propertyName</code> is null, an empty array is\r\n" + 
        		"     *         returned.\r\n" + 
        		"     * @since 1.4\r\n" + 
        		"     */");
        src.println("    public java.beans.PropertyChangeListener[] getPropertyChangeListeners(String propertyName) { return propertyChangeSupport.getPropertyChangeListeners(propertyName); }");
        
        if (sbean.fluent()) {
            src.format(
                    "    public %1$s listen(java.beans.PropertyChangeListener listener) { addPropertyChangeListener(listener);  return (%1$s)this;}\n",
                    typeElement.getSimpleName());
            src.format(
                    "    public %1$s listen(String propertyName, java.beans.PropertyChangeListener listener) { addPropertyChangeListener(propertyName, listener);  return (%1$s)this;}\n",
                    typeElement.getSimpleName());
        }
    }

    void createClassDeclaration(PrintWriter src, SBean sbean, String genName) {
        if (sbean.javadoc().length() > 0) {
            src.format("/** %s */\n", sbean.javadoc());
        }
        src.format("@javax.annotation.Generated(value=\"com.soletta.processor.BeanProcessor\")\n");

        String extend;
        TypeMirror extendMirror = mirrorExtend(sbean);
        if (extendMirror.toString().equals(Void.class.getName())) {
            extend = "";
        } else {
            extend = "extends " + extendMirror.toString();
        }

        src.format("abstract public class %s %s {\n", genName, extend);
        src.println();
    }

    void createMXInterfaceDeclaration(PrintWriter src, SBean sbean, String genName) {
        if (sbean.javadoc().length() > 0) {
            src.format("/** %s */\n", sbean.javadoc());
        }
        src.format("@javax.annotation.Generated(value=\"com.soletta.processor.BeanProcessor\")\n");

        src.format("public interface %s %s {\n", genName);
        src.println();
    }
    void createGuavaExtractor(PrintWriter src, String type, String capName, Element beanElement, TypeElement typeElement,
            String boxed) {
        String ptype = "com.google.common.base.Function<" + typeElement.getSimpleName() + "Base," + boxed + ">";
        src.format(
                "    public final static %1$s %2$s = new %1$s(){ public %5$s apply(%4$sBase value) { return value.%6$s%3$s(); }};\n",
                ptype, capName.toUpperCase(), capName, beanElement.getSimpleName(), boxed, isOrGet(type));
    }

    void createGuavaPredicate(PrintWriter src, String type, String capName, Element beanElement, TypeElement typeElement,
            boolean isPrimitive) {
        String ptype = "com.google.common.base.Predicate<" + typeElement.getSimpleName() + "Base>";
        String isOrHas;
        String body;
        if (type.equals(boolean.class.getName())) {
            isOrHas = "IS_";
            body = "return value.is" + capName + "();";
        } else if (!isPrimitive) {
            isOrHas = "HAS_";
            body = "return value.get" + capName + "() != null;";
        } else {
            body = null;
            isOrHas = null;
        }
        if (isOrHas != null) {
            src.format("    public final static %1$s %2$s = new %1$s(){ public boolean apply(%4$sBase value) { " + "%3$s\n"
                    + "} };\n", ptype, isOrHas + capName.toUpperCase(), body, beanElement.getSimpleName());
        }
    }

    void createFluentSetter(PrintWriter src, SProperty prop, String type, String capName, TypeElement typeElement) {
        src.format("    public %1$s %2$s(%3$s fluentValue) { set%4$s(fluentValue); return (%1$s)this; }\n",
                typeElement.getSimpleName(), prop.name(), type, capName);
    }

    boolean createSetter(PrintWriter src, SBean sbean, SProperty prop, String type, String capName) {
        boolean generatePropertyChangeSupport = false;
        src.format("    public void set%s(%s %s) {\n", capName, type, prop.name());
        if (prop.bound() || (sbean.bound() && !prop.unbound())) {
            generatePropertyChangeSupport = true;
            src.format("        %s oldValue = this.%s;\n", type, prop.name());
            src.format("        this.%s = %1$s;\n", prop.name());
            src.format("        propertyChangeSupport.firePropertyChange(\"%s\", oldValue, %1$s);\n", prop.name());
        } else {
            src.format("        this.%3$s = %3$s;\n", capName, type, prop.name());
        }
        src.println("    }");
        return generatePropertyChangeSupport;
    }

    String createIsOrGet(PrintWriter src, SProperty prop, String type, String capName) {
        String content = String.format("    public %s %s%s() { return %s; }\n", type, isOrGet(type), capName, prop.name());
        src.print(content);
        return content;
    }

    String capitalize(SProperty prop) {
        return toUpperCase(prop.name().charAt(0)) + prop.name().substring(1);
    }

    String isOrGet(String type) {
        return type.equals(boolean.class.getName()) ? "is" : "get";
    }

    void createJAXB(PrintWriter src, SBean sbean, SProperty prop) {
        JAXBMemberType jaxb = sbean.jaxbType();
        if (prop.jaxbType() != JAXBMemberType.UNSET)
            jaxb = prop.jaxbType();

        switch (jaxb) {
        case ATTRIBUTE:
            src.println("    @javax.xml.bind.annotation.XmlAttribute");
            break;
        case ELEMENT:
            break;
        case TRANSIENT:
            src.println("    @javax.xml.bind.annotation.XmlTransient");
            break;
        case LIST:
            src.println("    @javax.xml.bind.annotation.XmlList");
            break;
        default:
            break;
        }
    }

    void createJavadoc(PrintWriter src, SProperty prop) {
        if (prop.javadoc().length() > 0) {
            src.format("/** %s */\n", prop.javadoc());
        }
    }

    boolean createField(PrintWriter src, SBean bean, SProperty prop, String type, TypeElement beanTypeElement, SKind sKind) {
        boolean final_ = prop.final_() || (bean.final_() && !prop.notfinal());
        if (final_ && prop.init().isEmpty()) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "A generated final field must include an init string.", beanTypeElement);
        }
        String init = prop.init().isEmpty() ? "" : " = " + prop.init();
        src.format("    %sprivate %s %s%s;\n", final_? "final " : "", type, prop.name(), init);
        return final_;
    }

    TypeMirror mirrorExtend(SBean sbean) {
        try {
            sbean.extend().getName();
            throw new RuntimeException();
        } catch (MirroredTypeException mte) {
            return mte.getTypeMirror();
        }
    }

    TypeMirror mirrorType(SProperty sprop) {
        try {
            sprop.type().getName();
            throw new RuntimeException();
        } catch (MirroredTypeException mte) {
            return mte.getTypeMirror();
        }
    }

    private PackageElement packageElement(Element typeElement) {
        while (!(typeElement instanceof PackageElement))
            typeElement = typeElement.getEnclosingElement();
        return (PackageElement) typeElement;
    }

}
