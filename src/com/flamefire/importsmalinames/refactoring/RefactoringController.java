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
import com.flamefire.importsmalinames.astutils.RenameVariablesVisitor;
import com.flamefire.importsmalinames.types.JavaClass;
import com.flamefire.importsmalinames.types.JavaMethod;
import com.flamefire.importsmalinames.types.JavaVariable;
import com.flamefire.importsmalinames.utils.Util;
import com.flamefire.smali.parser.SmaliParser;
import com.flamefire.smali.types.SmaliClass;
import com.flamefire.smali.types.SmaliMethod;
import com.flamefire.smali.types.SmaliVariable;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RefactoringController {
    private Map<String, SmaliClass> smaliClasses = null;
    private Map<JavaMethod, Map<String, String>> curRenamings;
    private final Map<ICompilationUnit, RenamingEntry> renamings = new HashMap<ICompilationUnit, RenamingEntry>();

    private class RenamingEntry {
        public final Map<JavaMethod, Map<String, String>> renamings;
        public final Map<String, JavaClass> classes;

        public RenamingEntry(Map<JavaMethod, Map<String, String>> renamings, Map<String, JavaClass> classes) {
            this.renamings = renamings;
            this.classes = classes;
        }
    }

    public boolean init(File smaliFolder) {
        List<File> smaliFiles = FileUtil.getFilesRecursive(smaliFolder, new SuffixFilter(TypeFilter.FILE, "smali"));
        // Do this in order to keep anonymous classes the same same as they were
        // before
        Collections.sort(smaliFiles);
        SmaliParser parser = new SmaliParser();
        for (File f : smaliFiles)
            if (!parser.parseFile(f))
                return false;
        smaliClasses = parser.getResult();
        renamings.clear();
        return true;
    }

    public boolean renameVariablesInFile(ICompilationUnit cu) {
        if (renamings.containsKey(cu))
            return false;
        curRenamings = new HashMap<JavaMethod, Map<String, String>>();
        Map<String, JavaClass> classes = RefactoringHelper.getTypesInCU(cu);
        for (String sClass : classes.keySet()) {
            renameVariablesInClass(sClass, classes.get(sClass));
        }
        RenamingEntry entry = new RenamingEntry(curRenamings, classes);
        renamings.put(cu, entry);
        return true;
    }

    public boolean applyRenamings(Shell shell) {
        boolean res = true;
        try {
            for (ICompilationUnit cu : renamings.keySet()) {
                RenamingEntry entry = renamings.get(cu);
                CompilationUnit unit = Util.createCU(cu, true);
                unit.recordModifications();
                ASTRewrite astRewrite = ASTRewrite.create(unit.getAST());
                RenameVariablesVisitor v = new RenameVariablesVisitor(entry.classes, entry.renamings, astRewrite);
                unit.accept(v);
                if (!RefactoringHelper.rewriteAST(cu, astRewrite)) {
                    if (renamings.size() == 1
                            || !MessageDialog.openConfirm(shell, "Error in " + cu.getCorrespondingResource().getName(),
                                    "Applying the changes to " + cu.getCorrespondingResource().getName()
                                            + " failed. Please check output.\n\nContinue?"))
                        return false;
                    else
                        res = false;
                }
            }
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
        return res;
    }

    private void renameVariablesInClass(String sClass, JavaClass curClass) {
        System.out.println("Processing class " + sClass);
        SmaliClass smClass = getSmaliClass(sClass, curClass);
        try {
            if (smClass == null) {
                System.err.println("Error: Smali class " + sClass + " not found");
                return;
            }
            for (JavaMethod method : curClass.methods) {
                renameVariablesInMethod(method, smClass);
            }
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
    }

    private SmaliClass getSmaliClass(String sClass, JavaClass curClass) {
        SmaliClass smClass = smaliClasses.get(sClass);
        if (smClass == null)
            return null;
        int p = sClass.lastIndexOf('$');
        if (p < 0)
            return smClass;
        // Possible anonymous class
        int i = 0;
        try {
            i = Integer.valueOf(sClass.substring(p + 1));
        } catch (NumberFormatException e) {
        }
        if (i == 0)
            return smClass;
        SmaliClass first = smClass;
        String baseName = sClass.substring(0, p + 1);
        if (!smaliClasses.containsKey(baseName + (i + 1)))
            return smClass;
        do {
            boolean hasDefConst = smClass.hasDefaultConstructor();
            boolean exactMCt = smClass.methods.size() == curClass.methods.size();
            if (exactMCt || (hasDefConst && smClass.methods.size() - 1 == curClass.methods.size())) {
                boolean ok = true;
                for (JavaMethod m : curClass.methods) {
                    boolean found = false;
                    for (SmaliMethod m2 : smClass.methods) {
                        if (m.name.equals(m2.name) && m.parameters.size() == m2.parameters.size()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found || (!exactMCt && m.isDefaultConstructor())) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    // We found a class
                    if (!smClass.equals(first)) {
                        List<SmaliClass> renameNew = new ArrayList<SmaliClass>();
                        List<SmaliClass> renamefirst = new ArrayList<SmaliClass>();
                        for (String n : smaliClasses.keySet()) {
                            if (n.startsWith(smClass.name + "->"))
                                renameNew.add(smaliClasses.get(n));
                            if (n.startsWith(first.name + "->"))
                                renamefirst.add(smaliClasses.get(n));
                        }
                        // Do this separate to avoid collisions
                        for (SmaliClass c : renameNew)
                            smaliClasses.remove(c.name);
                        for (SmaliClass c : renamefirst)
                            smaliClasses.remove(c.name);
                        for (SmaliClass c : renameNew) {
                            c.name = first.name + c.name.substring(smClass.name.length());
                            smaliClasses.put(c.name, c);
                        }
                        for (SmaliClass c : renamefirst) {
                            c.name = smClass.name + c.name.substring(first.name.length());
                            smaliClasses.put(c.name, c);
                        }

                        // Exchange classes
                        first.name = smClass.name;
                        smaliClasses.put(first.name, first);
                        smClass.name = sClass;
                        smaliClasses.put(smClass.name, smClass);
                    }
                    return smClass;
                }
            }
            i++;
            smClass = smaliClasses.get(baseName + i);
        } while (smClass != null);
        return first;
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
            if (!method.isAbstract)
                System.err.println("Error: Smali method " + method.name + " not found");
            return;
        }
        if (methods.size() > 1) {
            System.err.println("Error: To many smali methods match " + method.name);
            return;
        }
        curRenamings.put(method, new HashMap<String, String>());
        renameParameters(method, methods.get(0));
        renameLocalVariables(method, methods.get(0));
    }

    private void renameParameters(JavaMethod method, SmaliMethod smMethod) {
        Map<String, String> rename = curRenamings.get(method);
        List<JavaVariable> p = method.parameters;
        for (int j = 0; j < p.size(); j++) {
            addRenameVar(p.get(j), smMethod.parameters.get(j).name, rename);
        }
    }

    private String matchLocalVar(JavaVariable locVar, SmaliMethod smMethod, Map<String, String> rename) {
        for (SmaliVariable v : smMethod.variables) {
            if (RefactoringHelper.typeIsEqual(locVar.getType(),
                    RefactoringHelper.convertSignatureToType(v.getTypeSignature()))) {
                if (!rename.containsValue(v.name))
                    return v.name;
            }
        }
        return null;
    }

    private void renameLocalVariables(JavaMethod method, SmaliMethod smMethod) {
        Map<String, String> rename = curRenamings.get(method);
        for (JavaVariable v : method.variables) {
            String newName = matchLocalVar(v, smMethod, rename);
            if (newName != null) {
                addRenameVar(v, newName, rename);
            }
        }
    }

    private void addRenameVar(JavaVariable var, String newName, Map<String, String> rename) {
        rename.put(var.name, newName);
        System.out.println(var.name + "->" + newName);
    }

}
