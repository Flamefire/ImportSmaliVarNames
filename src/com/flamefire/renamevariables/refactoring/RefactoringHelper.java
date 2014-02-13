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
package com.flamefire.renamevariables.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class RefactoringHelper {
    private static IProgressMonitor NULL_MON = new NullProgressMonitor();

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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static String convertType(String type) {
        if (type.startsWith("Q"))
            type = type.substring(1);
        // if (type.startsWith("[B"))
        // type = type.substring(1);
        int p = type.indexOf('<');
        if (p >= 0)
            type = type.substring(0, p);
        if (type.endsWith(";"))
            type = type.substring(0, type.length() - 1);
        return type;
    }

    public static boolean typeIsEqual(String javaType, String smaliType) {
        if (javaType.equals(smaliType))
            return true;
        if (smaliType.endsWith("." + javaType))
            return true;
        return false;
    }
}
