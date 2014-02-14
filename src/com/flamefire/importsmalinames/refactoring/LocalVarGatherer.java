
package com.flamefire.importsmalinames.refactoring;

import com.flamefire.importsmalinames.utils.Util;

import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LocalVarGatherer extends ASTVisitor {
    private final IMethod method;
    public final List<LocalVar> vars = new ArrayList<LocalVar>();

    public LocalVarGatherer(IMethod method) {
        this.method = method;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        try {
            if (!node.getName().toString().equals(method.getElementName()))
                return false;

            @SuppressWarnings("unchecked")
            List<SingleVariableDeclaration> nodeParams = node.parameters();
            ILocalVariable[] methodParams = method.getParameters();

            if (nodeParams.size() != methodParams.length)
                return false;
            for (int i = 0; i < nodeParams.size(); i++) {
                ILocalVariable v = methodParams[i];
                SingleVariableDeclaration v2 = nodeParams.get(i);
                if (!v.getElementName().equals(v2.getName().toString()))
                    return false;
                String vType = RefactoringHelper.convertSignatureToType(v.getTypeSignature());
                // We do not have array notations in the signature
                vType = Util.removeSuffix(vType, "[]");
                if (!RefactoringHelper.typeIsEqual(vType, v2.getType().toString())) {
                    System.out.println("Different Types: " + vType + "!=" + v2.getType());
                    return false;
                }
            }
            return true;

        } catch (JavaModelException e) {
            return false;
        }
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        String type = node.getType().toString();
        for (@SuppressWarnings("unchecked")
        Iterator<VariableDeclarationFragment> iter = node.fragments().iterator(); iter.hasNext();) {
            VariableDeclarationFragment fragment = iter.next();
            // VariableDeclarationFragment: is the plain variable declaration
            // part.
            // Example: "int x=0, y=0;" contains two
            // VariableDeclarationFragments, "x=0" and "y=0"
            vars.add(new LocalVar(fragment.getName().toString(), type));
        }
        return false;
    }
}
