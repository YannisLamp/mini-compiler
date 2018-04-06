import java.util.*;

/**
 * Class used for storing the necessary data to perform a semantic check
 */

public class SymbolTable {
    // Declared class data
    private Map<String, ClassInfo> classMap;

    // Data used to store current score
    private ClassInfo currClass;
    private MethodInfo currMethod;

    // Data used for method call parameter checks
    private MethodInfo chkMethInfo;
    private Stack<MethodInfo> chkMethStack;
    private int chkParamIndex;
    private Stack<Integer> chkParamIndexStack;
    private int chkCurrLine;


    public SymbolTable(){
        classMap = new LinkedHashMap<>();
        currClass = null;
        currMethod = null;

        chkMethInfo = null;
        chkMethStack = null;
        chkParamIndex = 0;
        chkParamIndexStack = null;
        chkCurrLine = 0;
    }

    /**
    *  Methods used only by STPopulatorVisitor, to create and populate a symbol table for the input program
    */

    // Insert a class (that does not extend another class) in the Map
    public boolean insertClass(String classId) {
        if (!classMap.containsKey(classId)) {
            ClassInfo toInsert = new ClassInfo(classId);
            classMap.put(classId, toInsert);
            return true;
        }
        else
            return false;
    }

    // Insert a class (that extends another class) in the Map
    public boolean insertClass(String classId, String parentClassId) {
        if (!classMap.containsKey(classId)) {
            ClassInfo toInsert = new ClassInfo(classId, parentClassId);
            classMap.put(classId, toInsert);
            return true;
        }
        else
            return false;
    }

    // Insert a method in the current class (current scope)
    public boolean insertMethod(String methodRetType, String methodId) {
        if (currClass != null)
            return currClass.insertMethod(methodRetType, methodId);
        else
            return false;
    }

    // Add a parameter to the current method (to be used in the middle of a method declaration,
    // current scope is inside the method)
    public boolean insertParamType(String paramType) {
        if (currMethod != null)
            return currMethod.insertParamType(paramType);
        else
            return false;
    }

    // Insert a variable in the current scope (auto checks for current scope)
    public boolean insertVar(String varId, String varType) {
        if (currMethod != null)
            return currMethod.insertVar(varId, varType);
        else if (currClass != null)
            return currClass.insertVar(varId, varType);
        else
            return false;
    }

    // Check if current method is  inherently polymorphic
    public boolean chkCurrMethodPoly() {
        // Get the first other possible method definition from parent classes
        if ((classMap.get(currMethod.getMethodOf())).getParentId() != null) {
            loadChkMethInfo((classMap.get(currMethod.getMethodOf())).getParentId(), currMethod.getId(), 0);
            // If anything is found, compare them
            if (chkMethInfo != null) {
                if (!currMethod.getRetType().equals(chkMethInfo.getRetType()))
                    return false;
                if (currMethod.getParamNum() != chkMethInfo.getParamNum())
                    return false;
                for (int i = 0; i < currMethod.getParamNum(); i++)
                    if (!currMethod.getParamType(i).equals(chkMethInfo.getParamType(i)))
                        return false;
            }

            resetChkMethInfo();
        }
        return true;
    }

    /**
     *  Methods used only by TypeCheckVisitor, to perform the type checks
     */

    // Manage data used for method call parameter checks
    public boolean chkMethFound() {
        if (chkMethInfo != null)
            return true;
        else
            return false;
    }

    public String getChkMethRetType() { return chkMethInfo.getRetType(); }

    public String getChkMethNextArgType() { return chkMethInfo.getParamType(chkParamIndex++); }

    public boolean chkMethodCorrectArgNum() {
        if (chkParamIndex == chkMethInfo.getParamNum())
            return true;
        else
            return false;
    }

    // Recursively searches for method occurrence
    public void loadChkMethInfo(String classId, String methodId, int inCurrLine) {
        ClassInfo strtClass = classMap.get(classId);
        if (chkMethInfo != null) {
            if (chkMethStack == null) {
                chkMethStack = new Stack<>();
                chkParamIndexStack = new Stack<>();
            }
            chkMethStack.push(chkMethInfo);
            chkParamIndexStack.push(chkCurrLine);
            chkParamIndexStack.push(chkParamIndex);
        }
        chkMethInfo = strtClass.getMethodInfo(methodId);
        chkCurrLine = inCurrLine;
        if (chkMethInfo == null) {
            if (strtClass.getParentId() != null)
                loadChkMethInfo(strtClass.getParentId(), methodId, inCurrLine);
        }
    }

    public void resetChkMethInfo() {
        if (chkMethStack != null && !chkMethStack.empty()) {
            chkMethInfo = chkMethStack.pop();
            chkParamIndex = chkParamIndexStack.pop();
            chkCurrLine = chkParamIndexStack.pop();
        }
        else {
            chkMethInfo = null;
            chkParamIndex = 0;
        }
    }

    // Is class "isClass" a subclass of "ofClass"?
    public boolean isSubclassOf(String isClassId, String ofClassId) {
        if (classMap.containsKey(isClassId)) {
            ClassInfo isClassInfo = classMap.get(isClassId);
            if (isClassInfo.getParentId() != null) {
                if (isClassInfo.getParentId().equals(ofClassId))
                    return true;
                else
                    return isSubclassOf(isClassInfo.getParentId(), ofClassId);
            } else
                return false;
        }
        else
            return false;
    }

    public String lookup(String varId) {
        // First if we are inside a method scope, look in there
        if (currMethod != null) {
            if (currMethod.getVarType(varId) != null)
                return currMethod.getVarType(varId);

            // If not, or if nothing is found, search for variables inside the method's class and its superclasses
            return recFindClassVarType(currMethod.getMethodOf(), varId);
        }
        else
            return null;
    }

    private String recFindClassVarType(String classId, String varId) {
        ClassInfo tempClass = classMap.get(classId);
        String retType = tempClass.getVarType(varId);

        if (retType == null)
            if (tempClass.getParentId() != null)
                return recFindClassVarType(tempClass.getParentId(), varId);
            else
                // Didn't find anything
                return null;
        else
            return retType;
    }

    public String getCurrMethRetType() { return currMethod.getRetType(); }

    public String getCurrClassId() { return currClass.getId(); }

    public int getChkCurrLine() {return chkCurrLine; }

    /**
     *  Methods used by both visitors
     */

    // Enter designated scope
    public boolean enter(String mapKey) {
        if (currMethod != null)
            return false;

        if (currClass == null) {
            currClass = classMap.get(mapKey);
            if (currClass != null)
                return true;
            else
                return false;
        }
        else {
            currMethod = currClass.getMethodInfo(mapKey);
            if (currMethod != null)
                return true;
            else
                return false;
        }
    }

    // Exit current scope
    public void exit() {
        if (currMethod != null)
            currMethod = null;
        else if (currClass != null)
            currClass = null;
    }

    public boolean classExists(String classId) { return classMap.containsKey(classId); }


    /**
     *  Methods used to print program offsets, not used by visitors
     */

    // Actual public method called to print offsets
    public void printOffsets() {
        Set<String> classIdSet = classMap.keySet();
        int iter = 0;
        for (String classId : classIdSet) {
            // Don't print offsets for the first class (main method is in there)
            if (iter != 0) {
                System.out.println("\n-----------Class " + classId + "-----------");
                ClassInfo tempClass = classMap.get(classId);

                // Print variable offsets
                System.out.println("---Variables---");
                // Get parent variables offsets, if there is one
                int currOffset = 0;
                if (tempClass.getParentId() != null)
                    currOffset = calcParentVarOffset(tempClass.getParentId());

                Set<String> varIdSet = tempClass.getAllVarIds();
                if (varIdSet != null) {
                    for (String varId : varIdSet) {
                        System.out.println(classId + "." + varId + " : " + currOffset);
                        String varType = tempClass.getVarType(varId);
                        // Increment currOffset
                        if (varType.equals("int"))
                            currOffset += 4;
                        else if (varType.equals("boolean"))
                            currOffset += 1;
                        else
                            currOffset += 8;
                    }
                }

                // Print method offsets
                System.out.println("---Methods---");
                Set<String> methIdSet = tempClass.getAllMethIds();
                currOffset = 0;
                if (methIdSet != null) {
                    // Get parent method ids, if there is one
                    if (tempClass.getParentId() != null) {
                        Set<String> parMethSet = getParentMethods(tempClass.getParentId());
                        int parMethNum = parMethSet.size();
                        // Main method is static
                        if (parMethSet.contains("main"))
                            parMethNum--;
                        currOffset += 8 * parMethNum;
                        for (String methId : methIdSet) {
                            if (!parMethSet.contains(methId)) {
                                System.out.println(classId + "." + methId + " : " + currOffset);
                                currOffset += 8;
                            }
                        }
                    } else {
                        for (String methId : methIdSet) {
                            System.out.println(classId + "." + methId + " : " + currOffset);
                            currOffset += 8;
                        }
                    }
                }
            }
            iter++;
        }
    }

    // Method
    private int calcParentVarOffset(String classId) {
        if (classMap.containsKey(classId)) {
            ClassInfo tempClass = classMap.get(classId);
            int currOffset = 0;
            if (tempClass.getParentId() != null)
                currOffset = calcParentVarOffset(tempClass.getParentId());

            Set<String> varIdSet = tempClass.getAllVarIds();
            if (varIdSet != null) {
                for (String varId : varIdSet) {
                    String varType = tempClass.getVarType(varId);
                    // Increment currOffset
                    if (varType.equals("int"))
                        currOffset += 4;
                    else if (varType.equals("boolean"))
                        currOffset += 1;
                    else
                        currOffset += 8;
                }
            }

            return currOffset;
        }
        else
            return 0;
    }

    private Set<String> getParentMethods(String parClassId) {
        // tempClass will never be null, there will always be a parent class
        ClassInfo tempClass = classMap.get(parClassId);
        Set<String> retSet;
        // Crete or get set from upper class calls
        if (tempClass.getParentId() != null)
            retSet = getParentMethods(tempClass.getParentId());
        else
            retSet = new HashSet<>();

        // Then add methods of this class to the set
        Set<String> methIdSet = tempClass.getAllMethIds();
        for (String methId : methIdSet)
            if (!retSet.contains(methId))
                retSet.add(methId);

        return retSet;
    }
}


/**
 * Class used for storing a declared class information, such as its name, its superclass' name,
 * (if it has one), along with all the variables and methods (MethodInfo) declared in its scope
 */

class ClassInfo {
    private final String id;
    private final String parentId;

    private Map<String, String> varMap;
    private Map<String, MethodInfo> methodMap;

    public ClassInfo(String classId) {
        id = classId;
        parentId = null;
        varMap = null;
        methodMap = null;
    }
    // Constructor to be used if class has a parent class
    public ClassInfo(String classId, String parentClassId) {
        id = classId;
        varMap = null;
        methodMap = null;
        parentId = parentClassId;
    }

    /**
     * Inserts
     */

    public boolean insertVar(String varId, String varType) {
        if (varMap == null)
            varMap = new LinkedHashMap<>();

        if (!varMap.containsKey(varId)) {
            varMap.put(varId, varType);
            return true;
        }
        else
            return false;
    }

    public boolean insertMethod(String methodRetType, String methodId) {
        if (methodMap == null)
            methodMap = new LinkedHashMap<>();

        if (!methodMap.containsKey(methodId)) {
            MethodInfo toInsert = new MethodInfo(methodRetType, methodId, id);
            methodMap.put(methodId, toInsert);
            return true;
        }
        else
            return false;
    }

    /**
     * Getters
     */

    public String getId() { return id; }

    public String getParentId() { return parentId; }

    public Set<String> getAllVarIds() {
        if (varMap != null)
            return varMap.keySet();
        else
            return null;
    }

    public Set<String> getAllMethIds() {
        if (methodMap != null)
            return methodMap.keySet();
        else
            return null;
    }

    public String getVarType(String varId) {
        if (varMap == null)
            return null;
        // Returns null if there is no key with varId value
        return varMap.get(varId);
    }

    public MethodInfo getMethodInfo(String methodId) {
        if (methodMap == null)
            return null;
        // Returns null if there is no key with methodId value
        return methodMap.get(methodId);
    }
}


/**
 * Class used for storing a declared method information, such as its name, its return type, what class it belongs to,
 * all its parameter types, along with all the variables declared in its scope
 */

class MethodInfo {
    private final String retType;
    private final String id;
    private final String methodOf;

    private Map<String, String> varMap;
    private ArrayList<String> paramTypes;


    public MethodInfo(String methodRetType, String methodId, String classId) {
        retType = methodRetType;
        id = methodId;
        methodOf = classId;
        varMap = null;
        paramTypes = null;
    }

    /**
     * Inserts
     */

    public boolean insertVar(String varId, String toInsert) {
        if (varMap == null)
            varMap = new LinkedHashMap<>();

        if (!varMap.containsKey(varId)) {
            varMap.put(varId, toInsert);
            return true;
        }
        else
            return false;
    }

    public boolean insertParamType(String Type) {
        if (paramTypes == null)
            paramTypes = new ArrayList<>();

        return paramTypes.add(Type);
    }

    /**
     * Getters
     */

    public int getParamNum() {
        if (paramTypes != null)
            return paramTypes.size();
        else
            return 0;
    }

    public String getRetType() { return retType; }

    public String getId() { return id; }

    public String getVarType(String varId) {
        if (varMap == null)
            return null;
        // Returns null if there is no key with varId value
        return varMap.get(varId);
    }

    public String getParamType(int index) {
        if (paramTypes != null) {
            if (index < paramTypes.size())
                return paramTypes.get(index);
            else
                return null;
        }
        else
            return null;
    }

    public String getMethodOf() { return this.methodOf; }
}
