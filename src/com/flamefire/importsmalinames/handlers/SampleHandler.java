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

package com.flamefire.importsmalinames.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.Document;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SampleHandler extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        // Get the root of the workspace
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        // Get all projects in the workspace
        IProject[] projects = root.getProjects();
        // Loop over all projects
        for (IProject project : projects) {
            try {
                printProjectInfo(project);
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void printProjectInfo(IProject project) throws CoreException, JavaModelException {
        System.out.println("Working in project " + project.getName());
        // check if we have a Java project
        if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
            IJavaProject javaProject = JavaCore.create(project);
            printPackageInfos(javaProject);
        }
    }

    private void printPackageInfos(IJavaProject javaProject) throws JavaModelException {
        IPackageFragment[] packages = javaProject.getPackageFragments();
        for (IPackageFragment mypackage : packages) {
            // Package fragments include all packages in the
            // classpath
            // We will only look at the package from the source
            // folder
            // K_BINARY would include also included JARS, e.g.
            // rt.jar
            if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
                System.out.println("Package " + mypackage.getElementName());
                printICompilationUnitInfo(mypackage);
            }

        }
    }

    private void printICompilationUnitInfo(IPackageFragment mypackage) throws JavaModelException {
        for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
            printCompilationUnitDetails(unit);
        }
    }

    private void printIMethods(ICompilationUnit unit) throws JavaModelException {
        IType[] allTypes = unit.getAllTypes();
        for (IType type : allTypes) {
            printIMethodDetails(type);
        }
    }

    private void printCompilationUnitDetails(ICompilationUnit unit) throws JavaModelException {
        System.out.println("Source file " + unit.getElementName());
        Document doc = new Document(unit.getSource());
        System.out.println("Has number of lines: " + doc.getNumberOfLines());
        printIMethods(unit);
    }

    private void printIMethodDetails(IType type) throws JavaModelException {
        IMethod[] methods = type.getMethods();
        for (IMethod method : methods) {

            System.out.println("Method name " + method.getElementName());
            System.out.println("Signature " + method.getSignature());
            System.out.println("Return Type " + method.getReturnType());

        }
    }
}
