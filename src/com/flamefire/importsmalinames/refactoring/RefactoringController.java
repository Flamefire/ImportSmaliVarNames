/*******************************************************************************
 * Copyright (C) 2014 Flamefire
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.flamefire.importsmalinames.refactoring;

import com.flamefire.fileutils.FileUtil;
import com.flamefire.fileutils.SuffixFilter;
import com.flamefire.fileutils.TypeFilter;
import com.flamefire.importsmalinames.handlers.ClassGatherer;
import com.flamefire.smali.parser.SmaliParser;
import com.flamefire.smali.types.SmaliClass;
import com.flamefire.smali.types.SmaliMethod;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RefactoringController {
    private Map<String, SmaliClass> smaliClasses = new HashMap<String, SmaliClass>();

    public RefactoringController(File smaliFolder) {
        List<File> smaliFiles = FileUtil.getFilesRecursive(smaliFolder, new SuffixFilter(TypeFilter.FILE, "smali"));
        SmaliParser parser = new SmaliParser();
        for (File f : smaliFiles)
            if (!parser.parseFile(f))
                return;
        smaliClasses = parser.getResult();
    }

    public void renameParametersInFile(ICompilationUnit cu) {
        ClassGatherer gatherer = new ClassGatherer();
        Map<String, IType> classes = gatherer.getClasses(cu);
        for (String sClass : classes.keySet()) {
            renameParametersInClass(sClass, classes.get(sClass));
        }
    }

    private void renameParametersInClass(String sClass, IType curClass) {
        System.out.println("Processing class " + sClass);
        SmaliClass smClass = smaliClasses.get(sClass);
        if (smClass == null) {
            System.out.println("Error: Smali class not found");
            return;
        }
        try {
            IMethod[] methods = curClass.getMethods();
            for (IMethod method : methods) {
                renameParameters(method, smClass);
            }
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
    }

    private void renameParameters(IMethod method, SmaliClass smClass) throws JavaModelException {
        String mName = (method.isConstructor()) ? "<init>" : method.getElementName();
        System.out.println("Processing method " + mName);
        List<SmaliMethod> methods = smClass.getMethods(mName, method.getNumberOfParameters());
        for (int i = 0; i < methods.size(); i++) {
            boolean match = true;
            SmaliMethod smaliMethod = methods.get(i);
            ILocalVariable[] p = method.getParameters();
            for (int j = 0; j < p.length; j++) {
                String type = p[j].getTypeSignature();
                type = RefactoringHelper.convertType(type);
                if (!RefactoringHelper.typeIsEqual(type, smaliMethod.parameters.get(j).type)) {
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

    }

}
