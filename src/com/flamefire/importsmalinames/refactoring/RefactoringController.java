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

package com.flamefire.importsmalinames.refactoring;

import com.flamefire.fileutils.FileUtil;
import com.flamefire.fileutils.SuffixFilter;
import com.flamefire.fileutils.TypeFilter;
import com.flamefire.importsmalinames.types.JavaClass;
import com.flamefire.importsmalinames.types.JavaMethod;
import com.flamefire.importsmalinames.types.JavaVariable;
import com.flamefire.smali.parser.SmaliParser;
import com.flamefire.smali.types.SmaliClass;
import com.flamefire.smali.types.SmaliMethod;
import com.flamefire.smali.types.SmaliVariable;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RefactoringController {
    private Map<String, SmaliClass> smaliClasses = null;
    private final Map<JavaMethod, Map<String, String>> renamings = new HashMap<JavaMethod, Map<String, String>>();

    public boolean init(File smaliFolder) {
        List<File> smaliFiles = FileUtil.getFilesRecursive(smaliFolder, new SuffixFilter(TypeFilter.FILE, "smali"));
        SmaliParser parser = new SmaliParser();
        for (File f : smaliFiles)
            if (!parser.parseFile(f))
                return false;
        smaliClasses = parser.getResult();
        return true;
    }

    public void renameVariablesInFile(ICompilationUnit cu) {
        renamings.clear();
        Map<String, JavaClass> classes = RefactoringHelper.getTypesInCU(cu);
        for (String sClass : classes.keySet()) {
            renameVariablesInClass(sClass, classes.get(sClass));
        }
    }

    private void renameVariablesInClass(String sClass, JavaClass curClass) {
        System.out.println("Processing class " + sClass);
        SmaliClass smClass = smaliClasses.get(sClass);
        try {
            if (smClass == null) {
                System.out.println("Error: Smali class not found");
                return;
            }
            for (JavaMethod method : curClass.methods) {
                renameVariablesInMethod(method, smClass);
            }
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
    }

    private void renameVariablesInMethod(JavaMethod method, SmaliClass smClass) throws JavaModelException {
        System.out.println("Processing method " + method.name);

        List<SmaliMethod> methods = smClass.getMethods(method.name, method.getNumberOfParameters());
        for (int i = 0; i < methods.size(); i++) {
            boolean match = true;
            SmaliMethod smaliMethod = methods.get(i);
            List<JavaVariable> p = method.parameters;
            for (int j = 0; j < p.size(); j++) {
                String pType = p.get(j).getType();
                String p2Type = RefactoringHelper.convertSignatureToType(smaliMethod.parameters.get(j)
                        .getTypeSignature());
                if (!RefactoringHelper.typeIsEqual(pType, p2Type)) {
                    match = false;
                    break;
                }
            }
            if (!match) {
                methods.remove(i);
                i--;
            }
        }
        if (methods.size() == 0) {
            System.out.println("Error: Smali method not found");
            return;
        }
        if (methods.size() > 1) {
            System.out.println("Error: To many smali methods match");
            return;
        }
        renamings.put(method, new HashMap<String, String>());
        renameParameters(method, methods.get(0));
        renameLocalVariables(method, methods.get(0));
    }

    private void renameParameters(JavaMethod method, SmaliMethod smMethod) {
        Map<String, String> rename = renamings.get(method);
        List<JavaVariable> p = method.parameters;
        for (int j = 0; j < p.size(); j++) {
            rename.put(p.get(j).name, smMethod.parameters.get(j).name);
        }
    }

    private String matchLocalVar(JavaVariable locVar, SmaliMethod smMethod) {
        String res = null;
        for (SmaliVariable v : smMethod.variables) {
            if (RefactoringHelper.typeIsEqual(locVar.getType(),
                    RefactoringHelper.convertSignatureToType(v.getTypeSignature()))) {
                if (res != null)
                    return null;
                res = v.name;
            }
        }
        return res;
    }

    private void renameLocalVariables(JavaMethod method, SmaliMethod smMethod) {
        Map<String, String> rename = renamings.get(method);
        for (JavaVariable v : method.variables) {
            String newName = matchLocalVar(v, smMethod);
            if (newName != null)
                rename.put(v.name, newName);
        }
    }

}
