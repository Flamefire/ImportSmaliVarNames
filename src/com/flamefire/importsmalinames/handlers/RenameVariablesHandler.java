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

package com.flamefire.importsmalinames.handlers;

import com.flamefire.importsmalinames.refactoring.RefactoringController;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.ICompilationUnit;
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

    private void doRename(Shell shell, ICompilationUnit cu) {
        try {
            System.out.println("Class: " + cu.getCorrespondingResource().getName());
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
        String lastFolder = null;
        try {
            lastFolder = com.flamefire.importsmalinames.utils.Util.getPersistentProperty(cu.getCorrespondingResource(),
                    LASTFOLDER);
        } catch (JavaModelException e) {
        }
        JFileChooser j = new JFileChooser();
        j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        j.setDialogTitle("Select smali directory");
        if (lastFolder != null)
            j.setSelectedFile(new File(lastFolder));
        if (j.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
            return;
        try {
            com.flamefire.importsmalinames.utils.Util.setPersistentProperty(cu.getCorrespondingResource(), LASTFOLDER, j
                    .getSelectedFile().getAbsolutePath());
        } catch (JavaModelException e) {
        }
        File smaliFolder = j.getSelectedFile();

        RefactoringController controller = new RefactoringController(smaliFolder);
        controller.renameParametersInFile(cu);
    }

}
