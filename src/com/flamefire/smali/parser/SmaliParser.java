/*******************************************************************************
 * Copyright (C) 2014 Flamefire
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.flamefire.smali.parser;

import com.flamefire.importsmalinames.utils.Util;
import com.flamefire.smali.types.SmaliClass;
import com.flamefire.smali.types.SmaliMethod;
import com.flamefire.smali.types.SmaliVariable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SmaliParser {
    private final Map<String, SmaliClass> classes = new HashMap<String, SmaliClass>();
    private SmaliClass curClass;
    private SmaliMethod curMethod;
    private boolean waitForEnclosingMethod;
    private boolean isStaticMethod;
    private boolean isEnum;
    private boolean isInnerClass;

    public Map<String, SmaliClass> getResult() {
        Map<String, SmaliClass> result = new HashMap<String, SmaliClass>();
        for (SmaliClass c : classes.values()) {
            c.finalName = c.finalName.replace('$', '.');
            c.name = c.finalName;
            result.put(c.finalName, c);
        }
        return result;
    }

    public boolean parseFile(File file) {
        curClass = null;
        curMethod = null;
        waitForEnclosingMethod = false;
        isInnerClass = false;
        isEnum = false;
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            System.err.println("File '" + file.getAbsolutePath() + "' not found");
            return false;
        }
        try {
            for (String line; (line = br.readLine()) != null;) {
                try {
                    processLine(line.trim());
                } catch (Exception e) {
                    System.err.println(e.getLocalizedMessage());
                    System.err.println("In line " + line + " of file " + file.getAbsolutePath());
                    e.printStackTrace();
                    br.close();
                    return false;
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (curClass != null) {
            // Inner classes may have parent as first param
            if (isInnerClass) {
                for (SmaliMethod m : curClass.methods) {
                    if (m.isConstructor() && m.parameters.size() > 0 && m.parameters.get(0) == null)
                        m.parameters.remove(0);
                }
            }
            // Fix long params
            for (SmaliMethod m : curClass.methods) {
                m.cleanUpParameters();
                // Enum constructors contain parameters for the enum itself,
                // they are NOT shown in the code
                if (isEnum && m.isConstructor()) {
                    m.removeLeadingNullParams();
                }
                if (m.containsNullParams())
                    System.err.println("Method " + curClass.finalName + "." + m.name + " contains unknown parameters");
            }
            // Update real containing class name for classes in this class
            for (SmaliClass c : classes.values()) {
                if (c.finalName.startsWith(curClass.name + "->")) {
                    c.finalName = curClass.finalName + c.finalName.substring(curClass.name.length() + 2);
                }
            }
        }
        return true;
    }

    private static String smaliTypeToJavaType(String type) {
        if (type.endsWith(";"))
            type = type.substring(0, type.length() - 1);
        if (type.startsWith("["))
            type = type.substring(1) + "[]";
        if (type.startsWith("L"))
            type = type.substring(1);
        type = type.replace('/', '.');
        type = Util.removePrefix(type, "java.lang.");
        return type;
    }

    // Removes a prefix (a word including an implied following space)
    private static String removePrefix(String str, String prefix) {
        return Util.removePrefix(str, prefix, 1);
    }

    private static boolean isNameValid(String name) {
        return !name.contains(" ");
    }

    private void processLine(String line) {
        int p = line.indexOf(' ');
        if (p < 0)
            return;
        String id = line.substring(0, p);
        String rest = line.substring(p + 1);
        if (id.equals(".class")) {
            // Skip visibility
            String className = rest.substring(rest.indexOf(' ') + 1);
            // Strip modifiers
            // DO NOT CHANGE ORDER of all of them
            className = removePrefix(className, "interface");
            className = removePrefix(className, "abstract");
            className = removePrefix(className, "final");
            className = removePrefix(className, "annotation");
            className = removePrefix(className, "enum");
            className = smaliTypeToJavaType(className);
            if (!isNameValid(className)) {
                System.err.println("Invalid class name '" + className + "' in line " + line);
                return;
            }
            curClass = new SmaliClass(className);
            classes.put(className, curClass);
            return;
        }
        if (curClass == null)
            return;
        if (id.equals(".annotation")) {
            if (rest.equals("system Ldalvik/annotation/InnerClass;"))
                isInnerClass = true;
            else if (rest.equals("system Ldalvik/annotation/EnclosingMethod;"))
                waitForEnclosingMethod = true;
        } else if (id.equals(".super")) {
            if (rest.equals("Ljava/lang/Enum;"))
                isEnum = true;
        } else if (id.equals("value")) {
            if (!waitForEnclosingMethod)
                return;
            int start = rest.indexOf("->");
            int end = rest.indexOf("(");
            String methodName = rest.substring(start, end);
            String fName = curClass.finalName;
            start = fName.lastIndexOf('$');
            String topName = fName.substring(0, start);
            // Find real containing class name
            if (classes.containsKey(topName))
                topName = classes.get(topName).finalName;
            curClass.finalName = topName + methodName + fName.substring(start);
            waitForEnclosingMethod = false;
        } else if (id.equals(".method")) {
            String methodName = rest.substring(0, rest.indexOf('('));
            // Strip modifiers
            // DO NOT CHANGE ORDER of all of them
            methodName = removePrefix(methodName, "public");
            methodName = removePrefix(methodName, "private");
            methodName = removePrefix(methodName, "protected");
            // Handle static methods
            if (methodName.startsWith("static")) {
                isStaticMethod = true;
                methodName = removePrefix(methodName, "static");
            } else
                isStaticMethod = false;
            methodName = removePrefix(methodName, "bridge");
            // Handle compiler magic (Getter, Setter)
            if (methodName.startsWith("synthetic"))
                return;
            // Handle other modifiers
            methodName = removePrefix(methodName, "final");
            boolean isVarArgs = false;
            if (methodName.startsWith("varargs")) {
                isVarArgs = true;
                methodName = removePrefix(methodName, "varargs");
            }
            methodName = removePrefix(methodName, "abstract");
            methodName = removePrefix(methodName, "constructor");
            methodName = removePrefix(methodName, "declared-synchronized");
            if (!isNameValid(methodName)) {
                System.err.println("Invalid method name '" + methodName + "' in line " + line);
                return;
            }
            curMethod = new SmaliMethod(methodName, isVarArgs);
            curClass.methods.add(curMethod);
        } else if (id.equals(".end")) {
            if (rest.equals("method"))
                curMethod = null;
        } else if (id.equals(".param") || id.equals(".local")) {
            // Skip if we are in invalid methods
            if (curMethod == null)
                return;
            char parOrVar = rest.charAt(0);
            int num = Integer.valueOf(rest.substring(1, rest.indexOf(',')));
            rest = rest.substring(rest.indexOf(", \"") + 3);
            String name = rest.substring(0, rest.indexOf('"'));
            int start = rest.indexOf("#");
            if (start < 0)
                start = rest.indexOf(':');
            int end = rest.indexOf(',') - 1;
            if (end < 0)
                end = rest.length();
            String type = rest.substring(start + 1, end).trim();
            type = smaliTypeToJavaType(type);
            SmaliVariable var = new SmaliVariable(name, type);
            if (parOrVar == 'p') {
                // Non-static methods have a hidden "this" param
                if (!isStaticMethod) {
                    if (num == 0) {
                        if (!name.equals("this"))
                            System.err.println("Found p0 '" + name + "' in line " + line);
                        return;
                    }
                    num--;
                }
                while (curMethod.parameters.size() <= num)
                    curMethod.parameters.add(null);
                curMethod.parameters.set(num, var);
            } else {
                curMethod.variables.add(var);
            }
        }
    }
}
