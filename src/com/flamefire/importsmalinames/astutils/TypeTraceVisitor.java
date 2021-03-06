
package com.flamefire.importsmalinames.astutils;

import com.flamefire.importsmalinames.types.JavaClass;
import com.flamefire.importsmalinames.types.JavaMethod;
import com.flamefire.importsmalinames.types.JavaVariable;
import com.flamefire.types.CMethod;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.util.List;
import java.util.Map;
import java.util.Stack;

public abstract class TypeTraceVisitor extends ASTVisitor {
    private final Stack<String> parentStack = new Stack<String>();
    private final Stack<JavaClass> classStack = new Stack<JavaClass>();
    private final Stack<JavaMethod> methodStack = new Stack<JavaMethod>();
    private final Stack<Integer> anonCtStack = new Stack<Integer>();
    protected JavaClass curClass = null;
    protected JavaMethod curMethod = null;
    private int curAnonCt = 0;
    private String curParent = "";
    public final Map<String, JavaClass> classes;
    private String pck = "";

    protected TypeTraceVisitor(Map<String, JavaClass> classes) {
        this.classes = classes;
    }

    @Override
    public boolean visit(PackageDeclaration node) {
        pck = node.getName() + ".";
        return false;
    }

    // Gets a just found class
    // Returns the class that will be new curClass
    protected JavaClass handleNewClass(ASTNode node, JavaClass jClass) {
        classes.put(jClass.name, jClass);
        return jClass;
    }

    protected void newClassFound(ASTNode node, String name) {
        parentStack.push(curParent);
        classStack.push(curClass);
        if (!curParent.equals(""))
            curParent += "$";
        curParent += name;
        JavaClass jClass = new JavaClass(pck + curParent);
        curClass = handleNewClass(node, jClass);
    }

    protected void classEnd() {
        curParent = parentStack.pop();
        curClass = classStack.pop();
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        newClassFound(node, node.getName().toString());
        return true;
    }

    @Override
    public void endVisit(EnumDeclaration node) {
        classEnd();
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        newClassFound(node, node.getName().toString());
        return true;
    }

    @Override
    public void endVisit(TypeDeclaration node) {
        classEnd();
    }

    @Override
    public boolean visit(AnonymousClassDeclaration node) {
        // Anonymous classes in classes are not supported and are most likely
        // wrong enums
        if (curParent.lastIndexOf('.') >= curParent.lastIndexOf("->"))
            return false;
        curAnonCt++;
        newClassFound(node, String.valueOf(curAnonCt));
        return true;
    }

    @Override
    public void endVisit(AnonymousClassDeclaration node) {
        // Anonymous classes in classes are not supported and are most likely
        // wrong enums
        if (curParent.lastIndexOf('.') >= curParent.lastIndexOf("->"))
            return;
        classEnd();
    }

    protected String getArrayDim(int dim) {
        String res = "";
        for (int i = 0; i < dim; i++) {
            res += "[]";
        }
        return res;
    }

    protected String getTypeFromParam(SingleVariableDeclaration v) {
        String type = v.getType() + getArrayDim(v.getExtraDimensions());
        if (v.isVarargs())
            type = type + "...";
        return type;
    }

    // Gets a just found method (With parameters set)
    // Returns the method that will be new curMethod
    protected JavaMethod handleNewMethod(MethodDeclaration node, JavaMethod jMethod) {
        curClass.methods.add(jMethod);
        return jMethod;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        parentStack.push(curParent);
        methodStack.push(curMethod);
        anonCtStack.push(curAnonCt);
        String name = (node.isConstructor()) ? CMethod.CONSTRUCTOR : node.getName().toString();
        if (!curParent.equals(""))
            curParent += "->";
        curParent += name;
        JavaMethod jMethod = new JavaMethod(name, Modifier.isAbstract(node.getModifiers()));
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> params = node.parameters();
        for (SingleVariableDeclaration p : params) {
            jMethod.parameters.add(new JavaVariable(p.getName().toString(), getTypeFromParam(p)));
        }
        curMethod = handleNewMethod(node, jMethod);
        curAnonCt = 0;
        return true;
    }

    @Override
    public void endVisit(MethodDeclaration node) {
        curParent = parentStack.pop();
        curMethod = methodStack.pop();
        curAnonCt = anonCtStack.pop();
    }

    protected abstract void handleVarDecl(String type, List<VariableDeclarationFragment> fragments);

    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(VariableDeclarationStatement node) {
        handleVarDecl(node.getType().toString(), node.fragments());
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean visit(VariableDeclarationExpression node) {
        handleVarDecl(node.getType().toString(), node.fragments());
        return true;
    }
}
