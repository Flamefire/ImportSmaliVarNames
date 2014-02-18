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

import com.flamefire.importsmalinames.astutils.JavaTypesGatherVisitor;
import com.flamefire.importsmalinames.astutils.TypeTraceVisitor;
import com.flamefire.importsmalinames.types.JavaClass;
import com.flamefire.importsmalinames.utils.Util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEdit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class RefactoringHelper {
    private static IProgressMonitor NULL_MON = new NullProgressMonitor();
    private static String[] SigTypesFrom = {
            "B", "C", "D", "F", "I", "J", "S", "Z"
    };
    private static String[] SigTypesTo = {
            "byte", "char", "double", "float", "int", "long", "short", "boolean"
    };

    // Based on Code of
    // http://stackoverflow.com/questions/14408614/refactor-parameter-names-programmatically
    // Renames a local variable or parameter without confirmation
    public static void renameVariable(String task, IJavaElement element, String new_name) {
        RefactoringStatus status = new RefactoringStatus();

        RefactoringContribution contrib = RefactoringCore
                .getRefactoringContribution(IJavaRefactorings.RENAME_LOCAL_VARIABLE);
        RenameJavaElementDescriptor rnDesc = (RenameJavaElementDescriptor) contrib.createDescriptor();
        rnDesc.setFlags(JavaRefactoringDescriptor.JAR_MIGRATION | JavaRefactoringDescriptor.JAR_REFACTORING);
        rnDesc.setProject(element.getJavaProject().getProject().getName());
        rnDesc.setUpdateReferences(true);
        rnDesc.setJavaElement(element);
        rnDesc.setNewName(new_name);

        Refactoring ref;
        try {
            ref = rnDesc.createRefactoring(status);
            ref.checkInitialConditions(NULL_MON);
            ref.checkFinalConditions(NULL_MON);

            Change change = ref.createChange(NULL_MON);
            change.perform(NULL_MON);
        } catch (CoreException e) {
            e.printStackTrace();
        }

    }

    // Makes a java type standard (No "Q...;")
    public static String standardizeJavaType(String type) {
        int p = type.indexOf('<');
        if (p >= 0) {
            int p2 = type.lastIndexOf('>') + 1;
            if (p2 < type.length())
                type = type.substring(0, p) + type.substring(p2);
            else
                type = type.substring(0, p);
        }
        p = type.indexOf(';');
        if (p >= 0) {
            if (p + 1 < type.length())
                type = type.substring(0, p) + type.substring(p + 1);
            else
                type = type.substring(0, p);
        }
        while (type.startsWith("["))
            type = type.substring(1) + "[]";
        if (type.startsWith("Q"))
            type = type.substring(1);
        return type;
    }

    private static String replaceSigType(String type, String from, String to) {
        // Do not forget we may have an array type
        if (type.equals(from) || type.startsWith(from + "...") || type.startsWith(from + "[]"))
            type = to + type.substring(1);
        return type;
    }

    public static String convertSignatureToType(String type) {
        while (type.startsWith("["))
            type = type.substring(1) + "[]";
        if (type.startsWith("."))
            type = type.substring(1) + "...";
        for (int i = 0; i < SigTypesFrom.length; i++) {
            type = replaceSigType(type, SigTypesFrom[i], SigTypesTo[i]);
        }
        if (type.startsWith("<"))
            type = type.substring(1);
        type = type.replace("$", ".");
        return standardizeJavaType(type);
    }

    public static boolean typeIsEqual(String javaType, String smaliType) {
        javaType = RefactoringHelper.standardizeJavaType(javaType);
        if (javaType.equals(smaliType))
            return true;
        if (smaliType.endsWith("." + javaType))
            return true;
        return false;
    }

    public static Map<String, JavaClass> getTypesInCU(ICompilationUnit icu) {
        CompilationUnit cu = Util.createCU(icu, false);
        TypeTraceVisitor lv = new JavaTypesGatherVisitor();
        cu.accept(lv);
        return lv.classes;
    }

    public static void write(String content, File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        out.write(content);

        out.close();
    }

    /**
     * Overwrite AST of given CompilationUnit
     * 
     * @param cu CompilationUnit to change
     * @param astRewrite ASTRewrite object with changes
     */
    public static boolean rewriteAST(ICompilationUnit cu, ASTRewrite astRewrite) {
        try {
            Document doc = new Document(cu.getSource());
            TextEdit edits = astRewrite.rewriteAST(doc, null);
            edits.apply(doc);
            cu.getBuffer().setContents(doc.get());
            File file = cu.getResource().getLocation().toFile();
            write(doc.get(), file);
            cu.getResource().refreshLocal(0, null);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
