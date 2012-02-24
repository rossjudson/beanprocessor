package com.soletta.beanprocessor;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

class TupleGenerator {

    void generateTupleClasses(PackageElement packageElement, ProcessingEnvironment processingEnv, RoundEnvironment roundEnv) throws IOException {

//        System.out.println("Tuple generation");
//        
        Tuples tuples = packageElement.getAnnotation(Tuples.class);
        Specialize specialize = packageElement.getAnnotation(Specialize.class);
        
        String dottedPackageName = packageElement.getQualifiedName().toString();
        JavaFileObject source = processingEnv.getFiler().createSourceFile(dottedPackageName + ".Tuples", packageElement);
        PrintWriter tw = new PrintWriter(source.openOutputStream());
        try {
            
            if (tuples != null) {
                packageHeader(tw, packageElement);
                tw.println("/** Tuples provides static construction methods for a varying lengths of tuple classes. */");
                tw.println("public class Tuples {");
                tw.println("  private Tuples() {}");
    
                // Create the construction functions, as well as the individual classes.
                for (int n = 2; n <= tuples.value(); n++) {

                    //System.out.println("Generate " + n);
                    
                    String [] letters = new String[n];
                    String [] paramNames = new String[n];
                    String [] params = new String[n];
                    boolean [] primitives = new boolean[n];
                    for (int i = 0; i < letters.length; i++) {
                        letters[i] = "" + (char)('A' + i);
                        paramNames[i] = "_" + (1+i);
                        params[i] = letters[i] + " " + paramNames[i];
                    }
                    String typeVars = "<" + join(letters) + ">";
                    String className = "Tuple" + n + typeVars;
                    String paramString = "(" + join(params) + ")";
                    String callString = "(" + join(paramNames) + ")";
                    
                    generateTupleSource(packageElement, processingEnv, tuples, dottedPackageName, false, letters, paramNames, params,
                            primitives, className, paramString, typeVars, callString);
                    
                    // Generate static builder for generic tuples
                    tw.format("public static %s %s of%s { return new %s%s; };\n", 
                            typeVars, className, paramString, className, callString);
                    
                }
            }
            
            if (specialize != null) {
                // Generate specialized tuples
                //System.out.println("Specializing");
                
                for (Tuple tuple: specialize.value()) {

                    List<? extends TypeMirror> mirrors = mirrorTypes(tuple);
                    int n = mirrors.size();
                    String[] typeNames = new String[n];
                    String [] paramNames = new String[n];
                    String [] params = new String[n];
                    boolean [] primitives = new boolean[n];
                    for (int i = 0; i < typeNames.length; i++) {
                        TypeMirror typeMirror = mirrors.get(i);
                        typeNames[i] = typeMirror.toString();//   tuple.value()[i].getName();
                        //System.out.println(typeNames[i]);
                        if (!typeMirror.getKind().isPrimitive()) {
                            // typeNames[i] = typeNames[i].substring(typeNames[i].lastIndexOf('.')+1, typeNames[i].length());
                        } else {
                            primitives[i] = true;
                            typeNames[i] = typeMirror.toString();//   tuple.value()[i].getName();
                        }
                        paramNames[i] = "_" + (1+i);
                        params[i] = typeNames[i] + " " + paramNames[i];
                    }
                    
                    //System.out.println("Generate " + join(typeNames));
                    
                    String typeVars = "";
                    String className = tuple.tupleTypeName();
                    if (className.equals("")) {
                        className = join("_", typeNames);
                        className = className.replace("java.lang.", "");
                        className = className.replace('.', '_');
                    }
                    String paramString = "(" + join(params) + ")";
                    String callString = "(" + join(paramNames) + ")";
    
                    generateTupleSource(packageElement, processingEnv, tuples, dottedPackageName, true, typeNames, paramNames, params,
                            primitives, className, paramString, typeVars, callString);
                    
                }
            }
            
            tw.println("}");
        } catch (Exception ex) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Unable to generate bean information: " + ex.getMessage());
        } finally {
            tw.close();
        }
        
    }

    private void generateTupleSource(PackageElement packageElement, ProcessingEnvironment processingEnv, Tuples tuples,
            String dottedPackageName, boolean specialize, String[] letters, String[] paramNames, String[] params, boolean [] primitive, String className,
            String paramString, String typeVars, String callString) throws IOException {
        
        int n = letters.length;
        int angle = className.indexOf('<');
        String nonParameterized = angle >= 0 ? className.substring(0, angle) : className;
        JavaFileObject tsource = processingEnv.getFiler().createSourceFile(dottedPackageName + "." + nonParameterized, packageElement);
        PrintWriter pw = new PrintWriter(tsource.openOutputStream());
        try {
            packageHeader(pw, packageElement);
            
            pw.format("public class %s {\n", className);
            pw.println();
            for (String p: params) 
                pw.format("  public final %s;\n", p);
            pw.println();
            pw.format("  public %s%s {\n", nonParameterized, paramString);
            for (int i = 0; i < letters.length; i++)
                pw.format("    this.%s = %<s;\n", paramNames[i]);
            pw.println("  }");
            pw.println();
            
            for (int i = 0; i < letters.length; i++) 
                pw.format("  public %s getField%d() { return %s; }\n", letters[i], i+1, paramNames[i]);
            pw.println();
            
            pw.println();
            pw.println("  public int hashCode() {");
            pw.println("    final int prime = 31; int result = 1;");
            
            for (int i = 0; i < paramNames.length; i++) {
                if (primitive[i]) {
                    if (letters[i].equals("long")) {
                        pw.format("    result = prime * result + (int)(%s ^ (%<s >>> 32));\n", paramNames[i]);
                    } else if (letters[i].equals("float")) {
                        pw.format("    result = prime * result + Float.floatToIntBits(%s);\n", paramNames[i]);
                    } else if (letters[i].equals("double")) {
                        pw.format("    long temp_%s = Double.doubleToLongBits(%<s);\n", paramNames[i]);
                        pw.format("    result = prime * result + (int)(temp_%s ^ (temp_%<s >>> 32));\n", paramNames[i]);
                    } else {
                        pw.format("    result = prime * result + (int)%s;\n", paramNames[i]);
                    }
                } else
                    pw.format("    result = prime * result + ((%s == null) ? 0 : %<s.hashCode());\n", paramNames[i]);
            }
            
            pw.println("    return result;");
            pw.println("  }");
            pw.println();

            pw.println("  public boolean equals(Object obj) {");
            pw.println("    if (this == obj)\r\n" + 
            		"        return true;\r\n" + 
            		"    if (obj == null)\r\n" + 
            		"        return false;\r\n" + 
            		"    if (getClass() != obj.getClass())\r\n" + 
            		"        return false;\r\n");
            if (!specialize)
                pw.println("    @SuppressWarnings(\"unchecked\")");
            pw.format("    %s other = (%<s)obj;\n", className);
            
            for (int i = 0; i < paramNames.length; i++) {
                if (primitive[i]) 
                    pw.format("    if (%s != other.%<s) return false;\n", paramNames[i]);
                else
                    pw.format("    if (%s == null) { if (other.%<s != null) return false; } else if (!%<s.equals(other.%<s)) return false;\n", paramNames[i]);
            }
            
            pw.println("    return true;");
            pw.println("  }");
            pw.println();
            
            pw.println("  public String toString() {");
            pw.println("    StringBuilder builder = new StringBuilder();");
            pw.println("    builder.append('(');");
            boolean first = true;
            for (String p: paramNames) {
                if (first)
                    first = false;
                else
                    pw.println("    builder.append(',');");
                pw.format("    builder.append(%s);\n", p);
            }
            pw.println("    builder.append(')');");
            pw.println("    return builder.toString();");
            pw.println("  }");
            pw.println();
            
            pw.println("  public Object[] toArray() {");
            pw.format("    return toArray(new Object[%d]);\n", n);
            pw.println("  }");
            pw.println();
            
            pw.println("  public Object[] toArray(Object[] ret) {");
            for (int i = 0; i < paramNames.length; i++)
                pw.format("    ret[%d] = %s;\n", i, paramNames[i]);
            pw.println("    return ret;");
            pw.println("  }");
            pw.println();
            
            // Generate static builder
            pw.format("public static %s %s of%s { return new %s%s; };\n", 
                    typeVars, className, paramString, className, callString);
            
            
//            if (n < tuples.value()) {
//                pw.format("  public <EX> Tuple%d<%s, EX> extend(EX ex) {\n", n+1, join(letters));
//                pw.format("    return Tuples.of(%s,ex);\n", join(paramNames));
//                pw.println("  }");
//            }
            
            pw.println("}");
            
        } finally {
            pw.close();
        }
    }
    
    private String join(Object items) {
        return join(", ", items);
    }

    private String join(String delim, Object items) {
        StringBuilder b = new StringBuilder();
        if (items instanceof Collection) {
            for (Object o: (Collection<?>)items) {
                if (b.length() > 0)
                    b.append(delim);
                b.append(o);
            }
            return b.toString();
        } else if (items.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(items); i++) {
                if (b.length() > 0)
                    b.append(delim);
                b.append(Array.get(items, i));
            }
            return b.toString();
        } else {
            return items.toString();
        }
    }

    private void packageHeader(PrintWriter tw, PackageElement packageElement) {
        tw.format("package %s;\n", packageElement.getQualifiedName());
        tw.println();
    }
    
    private List<? extends TypeMirror> mirrorTypes(Tuple sprop) {
        try {
            sprop.value()[0].getName();
            throw new RuntimeException();
        } catch (MirroredTypesException mte) {
            return mte.getTypeMirrors();
        }
    }
    

}
