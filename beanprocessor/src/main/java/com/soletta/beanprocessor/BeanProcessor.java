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
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
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
@SupportedAnnotationTypes({ "com.soletta.processor.SBean", "com.soletta.processor.SProperty" })
public class BeanProcessor extends AbstractProcessor {

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        processingEnv.getMessager().printMessage(Kind.NOTE, "Initialized BeanProcessor");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        // Since we care very little about the structure of Java, we can allow
        // this processing
        // to occur with regard to language version.
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager()
                .printMessage(Kind.NOTE, String.format("Received %,d annotations in set.", annotations.size()));

        for (Element beanElement : roundEnv.getElementsAnnotatedWith(SBean.class)) {
            SBean sbean = beanElement.getAnnotation(SBean.class);
            TypeElement beanTypeElement = TypeElement.class.cast(beanElement);
            try {
                boolean generatePropertyChangeSupport = false;
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
                        switch (prop.kind()) {
                        case LIST:
                            throw new UnsupportedOperationException("LIST not yet implemented");
                        case SIMPLE:
                            String type = prop.typeString();
                            String capName = capitalize(prop);

                            createField(src, prop, type);
                            createJavadoc(src, prop);
                            createJAXB(src, sbean, prop);
                            createIsOrGet(src, prop, type, capName);

                            if (createSetter(src, sbean, prop, type, capName))
                                generatePropertyChangeSupport = true;

                            if (prop.fluent() || (sbean.fluent() && !prop.fluent()))
                                createFluentSetter(src, prop, type, capName, beanTypeElement);

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

                            if (prop.predicate() || (sbean.predicates() && !prop.nopredicate()))
                                createGuavaPredicate(src, type, capName, beanElement, beanTypeElement, isPrimitive);

                            if (prop.extractor() || (sbean.extractors() && !prop.noextractor()))
                                createGuavaExtractor(src, type, capName, beanElement, beanTypeElement, boxed);

                            src.println();
                            break;
                        }
                    }

                    if (generatePropertyChangeSupport)
                        createPropertyChangeSupport(src, sbean, beanTypeElement);

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
        src.println("    public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) { propertyChangeSupport.addPropertyChangeListener(listener); }");
        src.println("    public void addPropertyChangeListener(String propertyName, java.beans.PropertyChangeListener listener) { propertyChangeSupport.addPropertyChangeListener(propertyName, listener); }");
        src.println("    public void removePropertyChangeListener(java.beans.PropertyChangeListener listener) { propertyChangeSupport.removePropertyChangeListener(listener); }");
        src.println("    public void removePropertyChangeListener(String propertyName, java.beans.PropertyChangeListener listener) { propertyChangeSupport.removePropertyChangeListener(propertyName, listener); }");

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

    void createIsOrGet(PrintWriter src, SProperty prop, String type, String capName) {
        src.format("    public %s %s%s() { return %s; }\n", type, isOrGet(type), capName, prop.name());
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
        default:
            break;
        }
    }

    void createJavadoc(PrintWriter src, SProperty prop) {
        if (prop.javadoc().length() > 0) {
            src.format("/** %s */\n", prop.javadoc());
        }
    }

    void createField(PrintWriter src, SProperty prop, String type) {
        String init = prop.init().isEmpty() ? "" : " = " + prop.init();
        src.format("    private %s %s%s;\n", type, prop.name(), init);
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
