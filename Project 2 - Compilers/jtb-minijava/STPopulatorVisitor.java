import syntaxtree.*;
import visitor.GJDepthFirst;

/**
 * Populates the Symbol table needed for the semantic check, also performs
 * checks for duplicate variable and class declarations in the same scope,
 * shadowed methods with different parameter types and inheritance errors
 * (parent class should be declared before the child class)
 */

public class STPopulatorVisitor extends GJDepthFirst<String, SymbolTable> {

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    public String visit(MainClass n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);

        String classId = n.f1.accept(this, symTable);
        if(!symTable.insertClass(classId))
            throw new Exception("Error at line " + n.f0.beginLine + ": Class named " + classId + " already exists");

        //n.f2.accept(this, symTable);
        symTable.enter(classId);

        //n.f3.accept(this, symTable);
        //n.f4.accept(this, symTable);

        String mainRetType = n.f5.toString();
        String mainName = n.f6.toString();

        if(!symTable.insertMethod(mainRetType, mainName))
            throw new Exception("Error at line " + n.f4.beginLine + ": A method named " + mainName +
                    " already exists in this scope");

        //n.f7.accept(this, symTable);
        symTable.enter(mainName);

        String mainParamType = n.f8.toString() + n.f9.toString() + n.f10.toString();
        String mainParamId = n.f11.accept(this, symTable);
        symTable.insertParamType(mainParamType);
        // The fact that the parameter's type is String[] does not matter (does not pose a problem)
        symTable.insertVar(mainParamId, mainParamType);

        //n.f12.accept(this, symTable);
        //n.f13.accept(this, symTable);
        n.f14.accept(this, symTable);
        n.f15.accept(this, symTable);

        symTable.exit();
        //n.f16.accept(this, symTable);

        symTable.exit();
        //n.f17.accept(this, symTable);
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public String visit(ClassDeclaration n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);

        String classId = n.f1.accept(this, symTable);
        if(!symTable.insertClass(classId))
            throw new Exception("Error at line " + n.f0.beginLine + ": A class named " + classId + " already exists");

        //n.f2.accept(this, symTable);
        symTable.enter(classId);

        n.f3.accept(this, symTable);
        n.f4.accept(this, symTable);

        symTable.exit();
        //n.f5.accept(this, symTable);
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    public String visit(ClassExtendsDeclaration n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);

        String classId = n.f1.accept(this, symTable);
        //n.f2.accept(this, symTable);
        String parentClassId = n.f3.accept(this, symTable);
        // Check for extends errors
        if (!symTable.classExists(parentClassId))
            throw new Exception("Error at line " + n.f0.beginLine + ": Class " + parentClassId +
                    " must be declared before class " + classId);

        if(!symTable.insertClass(classId, parentClassId))
            throw new Exception("Error at line " + n.f0.beginLine + ": A class named " + classId + " already exists");

        //n.f4.accept(this, symTable);
        symTable.enter(classId);

        n.f5.accept(this, symTable);
        n.f6.accept(this, symTable);

        symTable.exit();
        //n.f7.accept(this, symTable);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, SymbolTable symTable) throws Exception {
        String varType = n.f0.accept(this, symTable);
        String varId = n.f1.accept(this, symTable);
        if(!symTable.insertVar(varId, varType))
            throw new Exception("Error at line " + n.f2.beginLine + ": A variable named " + varId +
                    " already exists in this scope");

        //n.f2.accept(this, symTable);
        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public String visit(MethodDeclaration n, SymbolTable symTable) throws Exception {
        //n.f0.accept(this, symTable);

        String methodRetType = n.f1.accept(this, symTable);
        String methodId = n.f2.accept(this, symTable);
        if(!symTable.insertMethod(methodRetType, methodId))
            throw new Exception("Error at line " + n.f0.beginLine + ": A method named " + methodId +
                    " already exists in this scope");

        symTable.enter(methodId);
        //n.f3.accept(this, symTable);
        n.f4.accept(this, symTable);
        if (!symTable.chkCurrMethodPoly())
            throw new Exception("Error at line " + n.f0.beginLine + ": Method " + methodId +
                    " should have the same return and argument types as superclass methods with the same name");

        //n.f5.accept(this, symTable);
        //n.f6.accept(this, symTable);
        n.f7.accept(this, symTable);
        n.f8.accept(this, symTable);
        //n.f9.accept(this, symTable);
        n.f10.accept(this, symTable);
        //n.f11.accept(this, symTable);

        symTable.exit();
        //n.f12.accept(this, symTable);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public String visit(FormalParameter n, SymbolTable symTable) throws Exception {
        // Add to list of method parameters
        String parType = n.f0.accept(this, symTable);
        String parId = n.f1.accept(this, symTable);
        symTable.insertParamType(parType);
        symTable.insertVar(parId, parType);

        return null;
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(ArrayType n, SymbolTable symTable) throws Exception {
        return n.f0.toString() + n.f1.toString() + n.f2.toString();
    }

    /**
     * f0 -> "boolean"
     */
    public String visit(BooleanType n, SymbolTable symTable) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "int"
     */
    public String visit(IntegerType n, SymbolTable symTable) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, SymbolTable symTable) throws Exception {
        return n.f0.toString();
    }
}