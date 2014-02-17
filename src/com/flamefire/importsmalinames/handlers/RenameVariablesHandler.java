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

import com.flamefire.importsmalinames.refactoring.RefactoringController;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import java.io.File;

import javax.swing.JFileChooser;

@SuppressWarnings({
    "restriction"
})
public class RenameVariablesHandler extends AbstractHandler {
    private final static QualifiedName LASTFOLDER = new QualifiedName("flamefire", "LASTFOLDER");

    public RenameVariablesHandler() {
        super();
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);
        ISelection sel = HandlerUtil.getActiveMenuSelection(event);
        ICompilationUnit cu = null;
        if (sel instanceof IStructuredSelection) {
            IStructuredSelection selection = (IStructuredSelection) sel;
            Object el = selection.getFirstElement();
            if (el instanceof ICompilationUnit)
                cu = (ICompilationUnit) el;
        } else if (HandlerUtil.getActivePartId(event).equals("org.eclipse.jdt.ui.CompilationUnitEditor")) {
            IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            cu = (ICompilationUnit) EditorUtility.getEditorInputJavaElement(editor, false);
        }
        if (cu != null) {
            doRename(shell, cu);
        } else {
            MessageDialog.openInformation(shell, "Info", "Please select a Java source file");
        }
        return null;
    }

    public IJavaProject getProject(ICompilationUnit cu) {
        try {
            // Get the root of the workspace
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IWorkspaceRoot root = workspace.getRoot();
            // Get all projects in the workspace
            IProject[] projects = root.getProjects();
            // Loop over all projects
            for (IProject project : projects) {
                if (!project.isNatureEnabled("org.eclipse.jdt.core.javanature"))
                    continue;
                IJavaProject javaProject = JavaCore.create(project);
                IPackageFragment[] packages = javaProject.getPackageFragments();
                for (IPackageFragment mypackage : packages) {
                    if (mypackage.getKind() != IPackageFragmentRoot.K_SOURCE)
                        continue;
                    for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
                        if (cu.equals(unit))
                            return javaProject;
                    }
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void doRename(Shell shell, ICompilationUnit cu) {
        IJavaProject proj = getProject(cu);
        String cClassName = null;
        String pName = null;
        try {
            cClassName = cu.getCorrespondingResource().getName();
            if (proj != null)
                pName = proj.getElementName();
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
        if (cClassName == null || pName == null || cClassName.length() == 0 || pName.length() == 0) {
            MessageDialog.openError(shell, "Error", "Could not access project and class names");
            return;
        }
        boolean classOnly = MessageDialog.openQuestion(shell, "Apply smali names only to current class?",
                "Do you want to apply the names only to current java file (" + cClassName + ") ->YES\n"
                        + "Or to the whole project (" + pName + ") -> NO");
        String lastFolder = null;
        IResource res = null;
        try {
            res = proj.getCorrespondingResource();
            lastFolder = com.flamefire.importsmalinames.utils.Util.getPersistentProperty(res, LASTFOLDER);
        } catch (JavaModelException e) {
        }
        JFileChooser j = new JFileChooser();
        j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        j.setDialogTitle("Select smali directory");
        if (lastFolder != null)
            j.setSelectedFile(new File(lastFolder));
        if (j.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
            return;
        com.flamefire.importsmalinames.utils.Util.setPersistentProperty(res, LASTFOLDER, j.getSelectedFile()
                .getAbsolutePath());
        File smaliFolder = j.getSelectedFile();
        RefactoringController controller = new RefactoringController();
        if (!controller.init(smaliFolder)) {
            MessageDialog.openError(shell, "Error", "Could not parse smali classes");
            return;
        }
        String msg = "This will apply the smali names to the ";
        if (classOnly)
            msg += "java file " + cClassName;
        else
            msg += "whole project " + pName;
        msg += "\n\nPlease note that you HAVE to check the changes without a preview and it is better to apply this to the decompiled source before you do any changes yourself.";
        msg += "\nThis might fail under certain circumstances e.g. with nested classes and certain name combinations.";
        msg += "\n\nProceed?";
        if (!MessageDialog.openConfirm(shell, "Apply smali names", msg))
            return;
        if (classOnly) {
            if (!controller.renameVariablesInFile(cu)) {
                MessageDialog.openError(shell, "Error", "Applying the changes failed. Please check output.");
                return;
            }
        } else {
            try {
                IPackageFragment[] packages = proj.getPackageFragments();
                for (IPackageFragment mypackage : packages) {
                    if (mypackage.getKind() != IPackageFragmentRoot.K_SOURCE)
                        continue;
                    for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
                        if (!controller.renameVariablesInFile(unit)) {
                            if (!MessageDialog.openConfirm(shell, "Error in "
                                    + unit.getCorrespondingResource().getName(), "Applying the changes to "
                                    + unit.getCorrespondingResource().getName()
                                    + " failed. Please check output.\n\nContinue?"))
                                return;
                        }
                    }
                }
            } catch (JavaModelException e) {
                e.printStackTrace();
            }
        }

        MessageDialog.openInformation(shell, "Success",
                "Names were changed. Please have a look at the output in the console for warnings and errors");
    }
}
