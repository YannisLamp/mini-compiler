import syntaxtree.*;
import visitor.GJDepthFirst;
import java.io.*;
import java.io.BufferedWriter;
import java.util.*;

public class LlvmGenVisitor extends GJDepthFirst<String, SymbolTable> {
    // BufferWriter (passed as an argument from main)
    private final BufferedWriter bwriter;
    // Store current temp variable and label numbers
    private int curr_temp;
    private int curr_label;
    private int tab_num;

    private Stack<String> priExprClassType;
    private Stack<String> formalParamStack;
    private Stack<String> exprListStack;
    private Stack<String> varDeclStack;

    private Map<String, LinkedList<String>> typesFromVtables;

    // Constructor to get buffered writer created in main
    public LlvmGenVisitor(BufferedWriter input) {
        bwriter = input;
        curr_temp = 0;
        curr_label = 0;
        tab_num = 0;
        priExprClassType = new Stack<>();
        formalParamStack = new Stack<>();
        exprListStack = new Stack<>();
        varDeclStack = new Stack<>();
        typesFromVtables = new HashMap<>();
    }

    // Emit function that takes a string as an argument
    private void emit(String out, boolean applyIndentation) {
        if (applyIndentation)
            for (int i = 0; i < tab_num; i++)
                out = "\t" + out;
        try {
            bwriter.write(out);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String applyInden() {
        String out = "";
        for (int i = 0; i < tab_num; i++)
            out = "\t" + out;
        return out;
    }

    private String new_temp() {
        String temp_name = "%_" + curr_temp;
        curr_temp++;
        return temp_name;
    }

    private String new_label(String customMes) {
        String label_name = customMes + curr_label;
        curr_label++;
        return label_name;
    }

    private void resetCurrCount() {
        curr_temp = 0;
        curr_label = 0;
    }

    private void increaseIndentation() { tab_num++; }
    private void decreaseIndentation() { tab_num--; }


    private String removeRegType(String inReg) {
        return inReg.replaceAll(".* ", "");
    }

    private void insertPriClassType(String inType) {
        priExprClassType.push(inType);
    }

    private Stack<String> getPriClassType() {
        if (priExprClassType == null)
            return null;

        return priExprClassType;
    }

    private String popPriClassType() {
        if (priExprClassType == null)
            return null;

        if (!priExprClassType.empty())
            return priExprClassType.pop();
        else
            return null;
    }

    private void pushFormalParam(String formalPar) {
        formalParamStack.push(formalPar);
    }

    private void addFormalParam(String add) {
        String temp;
        if (!formalParamStack.empty()) {
            temp = formalParamStack.pop();
            formalParamStack.push(temp + add);
        }
    }

    private String popFormalParam() {
        String retVal;
        if (!formalParamStack.empty())
            retVal = formalParamStack.pop();
        else
            retVal = null;

        return retVal;
    }

    private void pushExprListStack(String exprList) {
        exprListStack.push(exprList);
    }

    private void addExprListStack(String add) {
        String temp;
        if (!exprListStack.empty()) {
            temp = exprListStack.pop();
            exprListStack.push(temp + add);
        }
    }

    private String popExprListStack() {
        String retVal;
        if (!exprListStack.empty())
            retVal = exprListStack.pop();
        else
            retVal = null;

        return retVal;
    }

    private void pushVarDeclStack(String exprList) {
        varDeclStack.push(exprList);
    }

    private void addVarDeclStack(String add) {
        String temp;
        if (!varDeclStack.empty()) {
            temp = varDeclStack.pop();
            varDeclStack.push(temp + add);
        }
    }

    private String popVarDeclStack() {
        String retVal;
        if (!varDeclStack.empty())
            retVal = varDeclStack.pop();
        else
            retVal = null;

        return retVal;
    }


    private Map<String, LinkedList<String>> getTypesFromVtables() { return typesFromVtables; }

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
        // Start by emitting each v_table
        Set<String> classIdSet = symTable.getClassMap().keySet();
        int iter = 0;
        for (String classId : classIdSet) {
            // The first class id will always be the main class
            if (iter == 0)
                emit("@." + classId + "_vtable = global [0 x i8*] []\n", false);
            else {
                ClassInfo currClassInfo = symTable.getClassMap().get(classId);
                // New vtable to emit (one vtable for each class)
                String curr_vtable = "@." + classId + "_vtable = global ";
                // Get all method ids for this class
                Set<String> methIdSet = currClassInfo.getAllMethIds();
                // If this class has a parent class, get all the method ids of all its parent classes
                String parentId = currClassInfo.getParentId();
                if (parentId != null) {
                    Set<String> parMethIdSet = symTable.getParentMethods(parentId);
                    // Then combine the two (the actual class methods are added at the end of the Set)
                    for (String methId : methIdSet)
                        if (!parMethIdSet.contains(methId))
                            parMethIdSet.add(methId);
                    // Now the methIdSet contains all the methods of the class
                    methIdSet = parMethIdSet;
                }
                // Length of vtable
                curr_vtable = curr_vtable + "[" + methIdSet.size() + " x i8*] ";
                // If current class has any methods, add their info in the vtable
                if (methIdSet.size() > 0) {
                    curr_vtable = curr_vtable + "[";
                    for (String methId : methIdSet) {
                        MethodInfo currMethodInfo = currClassInfo.getMethodInfo(methId);
                        LinkedList<String> toSaveVtableTypes = new LinkedList<String>();
                        // First add the method return type
                        String currMethodType = currMethodInfo.getRetType();
                        if (currMethodType.equals("int")) {
                            curr_vtable = curr_vtable + "i8* bitcast (i32 ";
                            toSaveVtableTypes.add("i32");
                        }
                        else if (currMethodType.equals("boolean")) {
                            curr_vtable = curr_vtable + "i8* bitcast (i1 ";
                            toSaveVtableTypes.add("i1");
                        }
                        else if (currMethodType.equals("int[]")) {
                            curr_vtable = curr_vtable + "i8* bitcast (i32* ";
                            toSaveVtableTypes.add("i32*");
                        }
                        else {
                            curr_vtable = curr_vtable + "i8* bitcast (i8* ";
                            toSaveVtableTypes.add("i8*");
                        }
                        // Then add the argument types
                        // For this
                        curr_vtable = curr_vtable + "(i8*";
                        toSaveVtableTypes.add("i8*");
                        // And the rest, if there are any
                        int paramNum = currMethodInfo.getParamNum();
                        for (int i = 0; i < paramNum; i++) {
                            String currParamType = currMethodInfo.getParamType(i);
                            if (currParamType.equals("int")) {
                                curr_vtable = curr_vtable + ",i32";
                                toSaveVtableTypes.add("i32");
                            }
                            else if (currParamType.equals("boolean")) {
                                curr_vtable = curr_vtable + ",i1";
                                toSaveVtableTypes.add("i1");
                            }
                            else if (currParamType.equals("int[]")) {
                                curr_vtable = curr_vtable + ",i32*";
                                toSaveVtableTypes.add("i32*");
                            }
                            else {
                                curr_vtable = curr_vtable + ",i8*";
                                toSaveVtableTypes.add("i8*");
                            }
                        }
                        // Last part, method name
                        curr_vtable = curr_vtable + ")* @" + classId + "." + methId + " to i8*),\n\t\t";
                        // Save vtable types (needed for later
                        getTypesFromVtables().put(classId + "." + methId, toSaveVtableTypes);
                    }
                    // The current vtable ends here (there is an extra ",\n\t\t" at the end)
                    curr_vtable = curr_vtable.substring(0, curr_vtable.length() - 4) + "]\n\n";
                }
                // Else, the vtable is empty
                else
                    curr_vtable = curr_vtable + "[]\n\n";
                emit(curr_vtable, false);
            }
            iter++;
        }


        // Emit default declares and defines
        emit("declare i8* @calloc(i32, i32)\n" +
                "declare i32 @printf(i8*, ...)\n" +
                "declare void @exit(i32)\n\n" +

                "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n" +
                "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n" +
                "define void @print_int(i32 %i) {\n" +
                "\t%_str = bitcast [4 x i8]* @_cint to i8*\n" +
                "\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" +
                "\tret void\n" +
                "}\n\n" +

                "define void @throw_oob() {\n" +
                "\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n" +
                "\tcall i32 (i8*, ...) @printf(i8* %_str)\n" +
                "\tcall void @exit(i32 1)\n" +
                "\tret void\n" +
                "}\n\n"
            , true);

        // Now for the main method
        String classId = n.f1.accept(this, symTable);
        symTable.enter(classId);

        String mainName = n.f6.toString();
        n.f11.accept(this, symTable);

        symTable.enter(mainName);
        emit("define i32 @main() {\n", true);
        increaseIndentation();
        resetCurrCount();

        String varDeclEmit = n.f14.accept(this, symTable);
        n.f15.accept(this, symTable);
        emit("ret i32 0\n", true);

        decreaseIndentation();
        emit("}\n\n", true);
        symTable.exit();

        symTable.exit();
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

        String classId = n.f1.accept(this, symTable);
        symTable.enter(classId);

        n.f3.accept(this, symTable);
        n.f4.accept(this, symTable);

        symTable.exit();
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
        String classId = n.f1.accept(this, symTable);

        symTable.enter(classId);

        n.f5.accept(this, symTable);
        n.f6.accept(this, symTable);

        symTable.exit();
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, SymbolTable symTable) throws Exception {
        String varType = n.f0.accept(this, symTable);
        String llvmVarType = "";
        if (varType.equals("int"))
            llvmVarType = "i32";
        else if (varType.equals("boolean"))
            llvmVarType = "i1";
        else if (varType.equals("int[]"))
            llvmVarType = "i32*";
        else
            llvmVarType = "i8*";

        String varId = n.f1.accept(this, symTable);
        String varDeclForRet = applyInden() + "%" + varId + " = alloca " + llvmVarType + "\n";
        addVarDeclStack(varDeclForRet);

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
        String methodType = n.f1.accept(this, symTable);
        String methodId = n.f2.accept(this, symTable);

        symTable.enter(methodId);

        String def = "define ";
        if (methodType.equals("int"))
            def = def + "i32 ";
        else if (methodType.equals("boolean"))
            def = def + "i1 ";
        else if (methodType.equals("int[]"))
            def = def + "i32* ";
        else
            def = def + "i8* ";

        emit(def + "@" + symTable.getCurrClassId() + "." + methodId + "(i8* %this", true);
        resetCurrCount();
        increaseIndentation();
        String paramAllocasEmit = n.f4.accept(this, symTable);
        if (paramAllocasEmit == null)
            paramAllocasEmit = "";
        emit(") {\n", false);

        emit(paramAllocasEmit, false);
        pushVarDeclStack("");
        n.f7.accept(this, symTable);
        emit(popVarDeclStack(), false);

        n.f8.accept(this, symTable);

        String retExpr = n.f10.accept(this, symTable);
        emit("ret " + retExpr + "\n", true);
        decreaseIndentation();
        symTable.exit();
        emit("}\n\n", true);

        return "";
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterList n, SymbolTable symTable) throws Exception {
        pushFormalParam(n.f0.accept(this, symTable));
        n.f1.accept(this, symTable);

        return popFormalParam();
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public String visit(FormalParameter n, SymbolTable symTable) throws Exception {
        String paramType = n.f0.accept(this, symTable);
        String paramId = n.f1.accept(this, symTable);
        String paramForEmit = ", ";
        String retAlloca = "";
        if (paramType.equals("int")) {
            paramForEmit = paramForEmit + "i32 %." + paramId;
            retAlloca = applyInden() + "%" + paramId + " = alloca i32\n";
            retAlloca = retAlloca + applyInden() + "store i32 %." + paramId + ", i32* %" + paramId + "\n";
        }
        else if (paramType.equals("boolean")) {
            paramForEmit = paramForEmit + "i1 %." + paramId;
            retAlloca = applyInden() + "%" + paramId + " = alloca i1\n";
            retAlloca = retAlloca + applyInden() + "store i1 %." + paramId + ", i1* %" + paramId + "\n";
        }
        else if (paramType.equals("int[]")) {
            paramForEmit = paramForEmit + "i32* %." + paramId;
            retAlloca = applyInden() + "%" + paramId + " = alloca i32*\n";
            retAlloca = retAlloca + applyInden() + "store i32* %." + paramId + ", i32** %" + paramId + "\n";
        }
        else {
            paramForEmit = paramForEmit + "i8* " + paramId;
            retAlloca = applyInden() + "%" + paramId + " = alloca i8*\n";
            retAlloca = retAlloca + applyInden() + "store i8* %." + paramId + ", i8** %" + paramId + "\n";
        }

        emit(paramForEmit, false);

        return retAlloca;
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
     */
    public String visit(FormalParameterTail n, SymbolTable symTable) throws Exception {
        return n.f0.accept(this, symTable);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    public String visit(FormalParameterTerm n, SymbolTable symTable) throws Exception {
        addFormalParam(n.f1.accept(this, symTable));
        return null;
    }

    /**
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */
    public String visit(Type n, SymbolTable symTable) throws Exception {
        return n.f0.accept(this, symTable);
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
     * f0 -> Block()
     *       | AssignmentStatement()
     *       | ArrayAssignmentStatement()
     *       | IfStatement()
     *       | WhileStatement()
     *       | PrintStatement()
     */
    public String visit(Statement n, SymbolTable symTable) throws Exception {
        return n.f0.accept(this, symTable);
    }

    /**
     * f0 -> "{"
     * f1 -> ( Statement() )*
     * f2 -> "}"
     */
    public String visit(Block n, SymbolTable symTable) throws Exception {
        n.f0.accept(this, symTable);
        n.f1.accept(this, symTable);
        n.f2.accept(this, symTable);
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n, SymbolTable symTable) throws Exception {
        String priRegStr = n.f0.accept(this, symTable);
        String rightAsReg = n.f2.accept(this, symTable);
        String priType = symTable.lookup(priRegStr);
        String leftAsReg = "";
        // If lookup did not fail
        String llvmVarType = "";
        if (priType.equals("int"))
            llvmVarType = "i32";
        else if (priType.equals("boolean"))
            llvmVarType = "i1";
        else if (priType.equals("int[]"))
            llvmVarType = "i32*";
        else
            llvmVarType = "i8*";

        // If its a local method var
        if (symTable.getCurrMethod() != null) {
            String varInMethType = symTable.getCurrMethod().getVarType(priRegStr);
            if (varInMethType != null) {
                leftAsReg = "%" + priRegStr;
                emit("store " + llvmVarType + " " + removeRegType(rightAsReg) + ", " + llvmVarType + "* " + leftAsReg + "\n", true);
            }
            // else its a class field
            else {
                // Find field offset
                int fieldOffset = -1;
                String currClass = symTable.getCurrClassId();
                while (fieldOffset == -1) {
                    if (symTable.getClassMap().get(currClass).getVarOffsets().containsKey(priRegStr)) {
                        fieldOffset = symTable.getClassMap().get(currClass).getVarOffsets().get(priRegStr);
                    }
                        // Else get the class parent
                        else {
                            currClass = symTable.getClassMap().get(symTable.getClassMap().get(currClass).getParentId()).getId();
                        }
                    }
                    fieldOffset = fieldOffset + 8;
                    String getElemReg = new_temp();
                    emit(getElemReg + " = getelementptr i8, i8* %this, i32 " + fieldOffset + "\n", true);
                    String castReg = new_temp();
                    emit(castReg + " = bitcast i8* " + getElemReg + " to " + llvmVarType + "*\n", true);
                    String forOutReg = new_temp();
                    emit(  "store " + llvmVarType + forOutReg + ", " + llvmVarType + "* " + castReg + "\n", true);
                }
            }
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    public String visit(ArrayAssignmentStatement n, SymbolTable symTable) throws Exception {
        String _ret=null;
        String arrayId = n.f0.accept(this, symTable);

        String leftAsReg = new_temp();
        int fieldOffset = -1;
        // If lookup did not fail
        String llvmVarType = "i32*";
        if (symTable.getCurrMethod() != null) {
            String varInMethType = symTable.getCurrMethod().getVarType(arrayId);
            if (varInMethType != null) {
                String arrayPtr = "%" + arrayId;
                emit(leftAsReg + " load i32*, i32** " + arrayPtr + "\n",true);
            }
            // else its a class field
            else {
                // Find field offset
                String currClass = symTable.getCurrClassId();
                while (fieldOffset == -1) {
                    if (symTable.getClassMap().get(currClass).getVarOffsets().containsKey(arrayId)) {
                        fieldOffset = symTable.getClassMap().get(currClass).getVarOffsets().get(arrayId);
                    }
                    // Else get the class parent
                    else {
                        currClass = symTable.getClassMap().get(symTable.getClassMap().get(currClass).getParentId()).getId();
                    }
                }
                fieldOffset = fieldOffset + 8;
                String getElemReg = new_temp();
                emit(getElemReg + " = getelementptr i8, i8* %this, i32 " + fieldOffset + "\n", true);
                String castReg = new_temp();
                emit(castReg + " = bitcast i8* " + getElemReg + " to i32**\n", true);
                emit(leftAsReg + " load i32*, i32** " + castReg + "\n",true);
            }
        }

        // Check bounds
        String arrayIndex = n.f2.accept(this, symTable);
        // Load correct index
        String realIndex = new_temp();
        emit(realIndex + " = load i32, i32* " + leftAsReg + "\n", true);
        String boundsComp = new_temp();
        emit(boundsComp + " = icmp ult i32 " + removeRegType(arrayIndex) + ", " + realIndex + "\n", true);

        String oobLab = new_label("oob");
        String oobLab2 = new_label("oob");
        String oobLab3 = new_label("oob");

        emit("br i1 " + boundsComp + ", label %" + oobLab + ", label %" + oobLab2 + "\n\n", true);

        emit(oobLab + ":\n",false);
        String indexPlusOne = new_temp();
        emit(indexPlusOne + " = add i32 " + removeRegType(arrayIndex) + ", 1\n", true);
        String getElReg = new_temp();
        emit(getElReg + " = getelementptr i32, i32* " + leftAsReg + ", i32 " + indexPlusOne + "\n", true);
        String intToAs = n.f5.accept(this, symTable);
        emit("store " + intToAs + ", i32* " + getElReg + "\n", true);
        emit("br label %" + oobLab3 + "\n\n", true);

        emit(oobLab2 + ":\n",false);
        emit("call void throw_oob()\n",true);
        emit("br label %" + oobLab3 + "\n\n", true);

        emit(oobLab3 + ":\n", false);

        return null;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public String visit(IfStatement n, SymbolTable symTable) throws Exception {
        String exprReg = n.f2.accept(this, symTable);
        String ifLabel = new_label("if");
        String elseLabel = new_label("if");
        String endIfLabel = new_label("if");

        emit("br " + exprReg + ", label %" + ifLabel + ", label %" + elseLabel + "\n\n", true);

        increaseIndentation();
        emit(ifLabel + ":\n", false);
        n.f4.accept(this, symTable);
        emit("br label %" + endIfLabel + "\n\n", true);

        emit(elseLabel + ":\n", false);
        n.f6.accept(this, symTable);
        emit("br label %" + endIfLabel + "\n\n", true);
        emit(endIfLabel + ":\n", false);
        decreaseIndentation();

        return null;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public String visit(WhileStatement n, SymbolTable symTable) throws Exception {
        String loopCond = new_label("loop");
        String loopStart = new_label("loop");
        String loopEnd = new_label("loop");
        emit("br label %" + loopCond + "\n\n", true);

        emit(loopCond + ":\n", false);
        String whileExprReg = n.f2.accept(this, symTable);
        emit("br " + whileExprReg + ", label %" + loopStart + ", label %" + loopEnd + "\n\n", true);

        increaseIndentation();
        emit(loopStart + ":\n", false);
        n.f4.accept(this, symTable);
        emit("br label %" + loopCond + "\n\n", true);
        emit(loopEnd + ":\n", false);
        decreaseIndentation();

        return null;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public String visit(PrintStatement n, SymbolTable symTable) throws Exception {
        String printTargetRegister = n.f2.accept(this, symTable);
        // Check
        emit("call void (i32) @print_int(i32 " + printTargetRegister + ")\n", true);
        return null;
    }

    /**
     * f0 -> AndExpression()
     *       | CompareExpression()
     *       | PlusExpression()
     *       | MinusExpression()
     *       | TimesExpression()
     *       | ArrayLookup()
     *       | ArrayLength()
     *       | MessageSend()
     *       | Clause()
     */
    public String visit(Expression n, SymbolTable symTable) throws Exception {
        return n.f0.accept(this, symTable);
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n, SymbolTable symTable) throws Exception {


        String cLab = new_label("clause");
        String cLab2 = new_label("clause");
        String cLab3 = new_label("clause");

        emit("br i1 1, label %" + cLab + ", label %" + cLab2 + "\n",true);

        emit(cLab + ":\n",false);
        String clause1 = n.f0.accept(this, symTable);
        emit("br i1 " + clause1 + ", label %" + cLab2 + ", label %" + cLab3 + "\n",true);

        emit(cLab2 + ":\n",false);
        String clause2 = n.f2.accept(this, symTable);
        emit("br label %" + cLab3 + "\n", true);

        emit(cLab3 + ":\n", false);
        String retReg = new_temp();
        emit(retReg + " = phi i1 [" + clause1 + ", %" + cLab + "], [" + clause2 + ", %" + cLab2 + "]\n", true);

        return retReg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, SymbolTable symTable) throws Exception {
        String pri1 = n.f0.accept(this, symTable);
        String pri2 = n.f2.accept(this, symTable);
        String compReg = new_temp();

        emit(compReg + " = icmp slt " + pri1 + ", " + removeRegType(pri2) + "\n", true);

        return "i1 " + removeRegType(compReg);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, SymbolTable symTable) throws Exception {
        String pri1 = n.f0.accept(this, symTable);
        String pri2 = n.f2.accept(this, symTable);
        String plusReg = new_temp();

        emit(plusReg + " = add " + pri1 + ", " + removeRegType(pri2) + "\n", true);

        return "i32 " + removeRegType(plusReg);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, SymbolTable symTable) throws Exception {
        String pri1 = n.f0.accept(this, symTable);
        String pri2 = n.f2.accept(this, symTable);
        String subReg = new_temp();

        emit(subReg + " = sub " + pri1 + ", " + removeRegType(pri2) + "\n", true);

        return "i32 " + removeRegType(subReg);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, SymbolTable symTable) throws Exception {
        String pri1 = n.f0.accept(this, symTable);
        String pri2 = n.f2.accept(this, symTable);
        String multReg = new_temp();

        emit(multReg + " = mul " + pri1 + ", " + removeRegType(pri2) + "\n", true);

        return "i32 " + removeRegType(multReg);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, SymbolTable symTable) throws Exception {
        String arrReg = n.f0.accept(this, symTable);
        String indexReg = n.f2.accept(this, symTable);

        String bounds = new_temp();
        emit(bounds + " = load i32, " + arrReg + "\n", true);

        String compReg = new_temp();
        emit(compReg + " = icmp ult i32 " + removeRegType(indexReg) + ", " + removeRegType(bounds) + "\n", true);

        String oobLab = new_label("oob");
        String oobLab2 = new_label("oob");
        String oobLab3 = new_label("oob");

        emit("br i1 " + compReg + ", label %" + oobLab + ", label %" + oobLab2 + "\n\n",true);

        emit(oobLab + ":\n",false);
        String realIndexReg = new_temp();
        emit(realIndexReg + " = add " + removeRegType(indexReg) + ", 1\n", true);
        String getElemReg = new_temp();
        emit(getElemReg + " = getelementptr i32, " + arrReg + ", i32 " + realIndexReg + "\n", true);
        String forOutReg = new_temp();
        emit(forOutReg + " = load i32, i32* " + getElemReg + "\n", true);
        emit("br label %" + oobLab3 + "\n\n", true);

        emit(oobLab2 + ":\n", false);
        emit("call void @throw_oob()\n", true);
        emit("br label %" + oobLab3 + "\n\n", true);

        emit(oobLab3 + ":\n", false);


        return forOutReg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, SymbolTable symTable) throws Exception {
        n.f0.accept(this, symTable);
        String arrayId = n.f0.accept(this, symTable);

        String leftAsReg = new_temp();
        int fieldOffset = -1;
        // If lookup did not fail
        String llvmVarType = "i32*";
        if (symTable.getCurrMethod() != null) {
            String varInMethType = symTable.getCurrMethod().getVarType(arrayId);
            if (varInMethType != null) {
                String arrayPtr = "%" + arrayId;
                emit(leftAsReg + " load i32*, i32** " + arrayPtr + "\n",true);
            }
            // else its a class field
            else {
                // Find field offset
                String currClass = symTable.getCurrClassId();
                while (fieldOffset == -1) {
                    if (symTable.getClassMap().get(currClass).getVarOffsets().containsKey(arrayId)) {
                        fieldOffset = symTable.getClassMap().get(currClass).getVarOffsets().get(arrayId);
                    }
                    // Else get the class parent
                    else {
                        currClass = symTable.getClassMap().get(symTable.getClassMap().get(currClass).getParentId()).getId();
                    }
                }
                fieldOffset = fieldOffset + 8;
                String getElemReg = new_temp();
                emit(getElemReg + " = getelementptr i8, i8* %this, i32 " + fieldOffset + "\n", true);
                String castReg = new_temp();
                emit(castReg + " = bitcast i8* " + getElemReg + " to i32**\n", true);
                emit(leftAsReg + " load i32*, i32** " + castReg + "\n",true);
            }
        }

        // Load correct index
        String realLen = new_temp();
        emit(realLen + " = load i32, i32* " + leftAsReg + "\n", true);

        return  "i32 " + removeRegType(realLen);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public String visit(MessageSend n, SymbolTable symTable) throws Exception {
        // First find the class of the object whose method is called
        String objectRegStr = n.f0.accept(this, symTable);
        String priType = popPriClassType();
        String classType = null;
        if (priType == null) {
            classType = symTable.getCurrClassId();
        }
        else {
            classType = priType;
        }

        // Get method id, and call it
        String methodId = n.f2.accept(this, symTable);
        int methodOffset = symTable.getClassMap().get(classType).getMethodOffsets().get(methodId);
        emit("; " + classType + "." + methodId + " : " + methodOffset/8 + "\n", true);

        String tCastReg = new_temp();
        emit(tCastReg + " =  bitcast " + objectRegStr + " to i8***\n", true);

        String loadReg = new_temp();
        emit(loadReg + " = load i8**, i8*** " + tCastReg + "\n", true);

        String fGetElem = new_temp();
        emit(fGetElem + " = getelementptr i8*, i8** " + loadReg + ", i32 " + methodOffset/8 + "\n", true);

        String loadReg2 = new_temp();
        emit(loadReg2 + " = load i8*, i8** " + fGetElem + "\n", true);

        String tCastReg2 = new_temp();
        String methKey = classType + "." + methodId;
        String methRetType = getTypesFromVtables().get(methKey).get(0);
        String allTypes = methRetType + " (i8*";
        for (int i = 2; i < getTypesFromVtables().get(methKey).size(); i++)
            allTypes = allTypes + "," + getTypesFromVtables().get(methKey).get(i);
        allTypes = allTypes + ")*";
        emit(tCastReg2 + " bitcast i8* " + loadReg2 + " to " + allTypes + "\n", true);

        String retReg = new_temp();

        String exprList = n.f4.accept(this, symTable);
        if (exprList == null)
            exprList = "";
        else
            exprList = ", " + exprList;

        emit(retReg + " = call " + methRetType + " " + tCastReg2 + "(" + objectRegStr + exprList + ")\n", true);

        return retReg;
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public String visit(ExpressionList n, SymbolTable symTable) throws Exception {
        pushExprListStack(n.f0.accept(this, symTable));
        n.f1.accept(this, symTable);

        return popExprListStack();
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public String visit(ExpressionTail n, SymbolTable symTable) throws Exception {
        n.f0.accept(this, symTable);
        return null;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public String visit(ExpressionTerm n, SymbolTable symTable) throws Exception {
        addExprListStack(", " + n.f1.accept(this, symTable));
        return null;
    }

    /**
     * f0 -> NotExpression()
     *       | PrimaryExpression()
     */
    public String visit(Clause n, SymbolTable symTable) throws Exception {
        return n.f0.accept(this, symTable);
    }

    /**
     * f0 -> IntegerLiteral()
     *       | TrueLiteral()
     *       | FalseLiteral()
     *       | Identifier()
     *       | ThisExpression()
     *       | ArrayAllocationExpression()
     *       | AllocationExpression()
     *       | BracketExpression()
     */
    public String visit(PrimaryExpression n, SymbolTable symTable) throws Exception {
        String priRegStr = n.f0.accept(this, symTable);
        String priType = symTable.lookup(priRegStr);
        // If lookup did not fail
        String retReg = null;
        if (priType != null) {
            String llvmVarType = "";
            if (priType.equals("int"))
                llvmVarType = "i32";
            else if (priType.equals("boolean"))
                llvmVarType = "i1";
            else if (priType.equals("int[]"))
                llvmVarType = "i32*";
            else
                llvmVarType = "i8*";

            // If its a local method var
            if (symTable.getCurrMethod() != null) {
                String varInMethType = symTable.getCurrMethod().getVarType(priRegStr);
                if (varInMethType != null) {
                    String loadReg = new_temp();
                    emit(loadReg + " = load " + llvmVarType + ", " + llvmVarType + "* %" + priRegStr + "\n", true);
                    retReg = llvmVarType + " " + loadReg;
                    // If it is an object, save its class id
                    if (!priType.equals("int") && !priType.equals("boolean") && !priType.equals("int[]"))
                        insertPriClassType(priType);
                }
                // else its a class field
                else {
                    // Find field offset
                    int fieldOffset = -1;
                    String currClass = symTable.getCurrClassId();
                    while (fieldOffset == -1) {
                        if (symTable.getClassMap().get(currClass).getVarOffsets().containsKey(priRegStr)) {
                            fieldOffset = symTable.getClassMap().get(currClass).getVarOffsets().get(priRegStr);
                        }
                        // Else get the class parent
                        else {
                            currClass = symTable.getClassMap().get(symTable.getClassMap().get(currClass).getParentId()).getId();
                        }
                    }
                    fieldOffset = fieldOffset + 8;
                    String getElemReg = new_temp();
                    emit(getElemReg + " = getelementptr i8, i8* %this, i32 " + fieldOffset + "\n", true);
                    String castReg = new_temp();
                    emit(castReg + " = bitcast i8* " + getElemReg + " to " + llvmVarType + "*\n", true);
                    String forOutReg = new_temp();
                    emit(forOutReg + " = load " + llvmVarType + ", " + llvmVarType + "* " + castReg + "\n", true);
                    // If it is an object, save its class id
                    if (!priType.equals("int") && !priType.equals("boolean") && !priType.equals("int[]"))
                        insertPriClassType(priType);
                    retReg = llvmVarType + " " + forOutReg;
                }
            }
        }
        else
            retReg = priRegStr;

        return retReg;
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n, SymbolTable symTable) throws Exception {
        return "i32 " + n.f0.toString();
    }

    /**
     * f0 -> "true"
     */
    public String visit(TrueLiteral n, SymbolTable symTable) throws Exception {
        return "i1 1";
    }

    /**
     * f0 -> "false"
     */
    public String visit(FalseLiteral n, SymbolTable symTable) throws Exception {
        return "i1 0";
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, SymbolTable symTable) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n, SymbolTable symTable) throws Exception {
        return "i8* %" + n.f0.toString();
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(ArrayAllocationExpression n, SymbolTable symTable) throws Exception {
        String arraySizeReg = n.f3.accept(this, symTable);
        String cmpReg = new_temp();

        emit(cmpReg + " = icmp slt " + arraySizeReg + ", 0\n",true);
        String arrLab = new_label("arr_allloc");
        String arrLab2 = new_label("arr_allloc");

        emit("br i1 " + cmpReg + ", label %" + arrLab + ", label %" + arrLab2 + "\n\n", true);

        emit(arrLab + ":\n", false);
        emit("call void @throw_oob()\n", true);
        emit("br label %" + arrLab2 + "\n\n", true);

        emit(arrLab2 + ":\n", false);
        String realArrSize = new_temp();
        emit(realArrSize + " = add " + arraySizeReg + ", 1\n", true);
        String callocReg = new_temp();
        emit(callocReg + " = call i8* @calloc(i32 4, i32 " + realArrSize + ")\n", true);
        String castReg = new_temp();
        emit(castReg + " = bitcast i8* " + callocReg + " to i32*\n",true);
        emit("store i32 " + removeRegType(arraySizeReg) + ", i32* " + castReg + "\n",true);

        return castReg;
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public String visit(AllocationExpression n, SymbolTable symTable) throws Exception {
        String nRegPtr = new_temp();
        // Count variable offsets for calloc (plus 8 for vtable ptr)
        String classId = n.f1.accept(this, symTable);
        String parentId = symTable.getClassMap().get(classId).getParentId();
        int varOffset = 8 + symTable.calcParentVarOffset(parentId) + symTable.getClassMap().get(classId).getFieldSize();
        emit(nRegPtr + " = call i8* @calloc(i32 1, i32 " + varOffset + ")\n", true);

        String nCastedRegPtr = new_temp();
        emit(nCastedRegPtr + " = bitcast i8* " + nRegPtr + " to i8***\n", true);

        Set<String> methIdSet = symTable.getClassMap().get(classId).getAllMethIds();
        // If this class has a parent class, get all the method ids of all its parent classes
        if (parentId != null) {
            Set<String> parMethIdSet = symTable.getParentMethods(parentId);
            // Then combine the two (the actual class methods are added at the end of the Set)
            for (String methId : methIdSet)
                if (!parMethIdSet.contains(methId))
                    parMethIdSet.add(methId);
            // Now the methIdSet contains all the methods of the class
            methIdSet = parMethIdSet;
        }
        String regFGetElem = new_temp();
        String currVtableDim = "[" + methIdSet.size() + " x i8*]";
        emit(regFGetElem + " = getelementptr " + currVtableDim + ", " + currVtableDim +
                        "* @." + classId + "_vtable, i32 0, i32 0\n", true);

        emit("store i8** " + regFGetElem + ", i8*** " + nCastedRegPtr +"\n", true);

        insertPriClassType(classId);
        return "i8* " + removeRegType(nRegPtr);
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public String visit(NotExpression n, SymbolTable symTable) throws Exception {
        String retReg = new_temp();
        String notReg = n.f1.accept(this, symTable);
        emit(retReg + " = xor i1 1, " + removeRegType(notReg) + "\n", true);

        return retReg;
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, SymbolTable symTable) throws Exception {
        return n.f1.accept(this, symTable);
    }

}
