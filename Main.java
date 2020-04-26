import java.util.*;

import java.io.File;
import java.io.FileNotFoundException;

class Main {
    private static Map<String, Var> varMap = new HashMap();
    private static Integer lineCount = 0;
    private static String parseMode = "STATEMENT"; // STATEMENT or PROCEDURE
    // key: procedure name (uppercase), value: procedure
    private static Map<String, Procedure> procedureMap = new HashMap();
    // procedure currently being parsed
    private static Procedure procedureBeingParsed; // don't initialize here

    public static Procedure getProcedureBeingParsed() {
        return procedureBeingParsed;
    }

    public static void setProcedureBeingParsed(Procedure procedure) {
        procedureBeingParsed = procedure;
    }

    public static String getParseMode() {
        return parseMode;
    }

    public static void setParseMode(String parseMode) {
        Main.parseMode = parseMode;
    }

    public static Map<String, Var> getVarMap() {
        return varMap;
    }

    public static Procedure getProcedureByName(String name) {
        return procedureMap.get(name.toUpperCase());
    }

    public static void addProcedureToList(Procedure procedure) {
        String name = procedure.getName().toUpperCase();
        procedureMap.put(name, procedure);
    }

    public static void main(String args[]) {
        if ((args.length == 2) && args[1].equals("ttt")) {
            Main.test(args);
        } else {
            Main.run(args);
        }
    }

    private static void test(String args[]) {
        System.out.println("Welcome to the test method.");
    }

    private static void run(String args[]) {
        Msg msg = Utils.validateArgs(args);
        if (!msg.getValidationPassed()) {
            Utils.printErrorAndExit(msg);
        }

        while (msg.getScanner().hasNextLine()) {
            lineCount += 1;
            Line line = new Line(msg.getScanner().nextLine(), lineCount);
            executeLine(line);
        }
        // Utils.printVars();
    }

    private static void executeLine(Line line) {
        // Line may not correspond to one physical line in the program text file.
        // a line is rather a 'statement', as used in programming
        // for example, in a program's line for a 'for' loop, there are multiple
        // statements and hence multiple lines

        // System.out.println("line: " + line.getLineNumber() + " " + line.getType() + "
        // " + line.getTokens());

        if (Main.parseMode.equals("PROCEDURE")) {
            if (line.getType().equals("PROC_END")) {
                Main.setParseMode("STATEMENT");
            } else {
                Procedure pbp = Main.getProcedureBeingParsed();
                List<Line> lineList = pbp.getLineList();
                lineList.add(line);
                pbp.setLineList(lineList);

            }
        } else { // parseMode is "STATEMENT"
            switch (line.getType()) {
            case "VAR_DECLARATION":
                String varName = line.getTokensUpperCase().get(1);

                if (Utils.getVarByName(varName) != null) { // variable already exists
                    Utils.printErrorAndExit(new Msg(line, "DUPLICATE VARIABLE DECLARATION"));
                }

                // check if variable matches any of the reserved words
                if (Utils.isReservedWord(varName)) {
                    Utils.printErrorAndExit(new Msg(line, "RESERVED WORD USED FOR VARIABLE DECLARATION"));
                }

                // everything okay, proceed
                Var var = new Var();
                var.setName(varName);
                var.setType(line.getTokensUpperCase().get(0));
                varMap.put(var.getName(), var); // to ensure uppercase is used, retrieve from map
                break;

            case "ASSIGNMENT":
                // make sure that variable exists
                String varNameL = line.getTokensUpperCase().get(0); // LHS variable's name

                Var varL = Utils.getVarByName(varNameL); // LHS variable
                if (varL == null) {
                    Utils.printErrorAndExit(new Msg(line, "INVALID INITIALIZATIION ATTEMPT"));
                }

                String stringR; // RHS value
                // STRING needs to be handled differently, it may have multiple tokens
                if (varL.getType().equals("STRING")) {
                    stringR = Utils.makeSubstringFromTokenList(line.getTokens(), 2, -2, true);
                } else {
                    stringR = line.getTokens().get(2);
                }

                Var varR = Utils.getVarByName(stringR);
                if (varR != null) { // RHS is a name of a variable, not a value
                    Utils.confirmCompatibilityAndEvaluate("=", varL, varR, line);
                } else { // RHS is a value, not a variable
                    if (!Utils.validateTypeVsValue(varL.getType(), stringR)) {
                        Utils.printErrorAndExit(new Msg(line, "INVALID INITIALIZATION ATTEMPT"));
                    }
                    // everything is okay, assign the value
                    varL.setValues(stringR);
                }
                break;

            case "COMP_ASSIGNMENT_+=":
                // make sure LHS variable exists
                varNameL = line.getTokens().get(0);
                varL = Utils.getVarByName(varNameL);
                if (varL == null) {
                    Utils.printErrorAndExit(new Msg(line, "ASSIGNMENT TO UNDECLARED VARIABLE"));
                }

                stringR = null;
                if (varL.getType().equals("STRING")) {
                    stringR = Utils.makeSubstringFromTokenList(line.getTokens(), 2, -2, true);
                } else if (varL.getType().equals("INT")) {
                    stringR = line.getTokens().get(2);
                } else if (varL.getType().equals("BOOL")) {
                    Utils.printErrorAndExit(new Msg(line, "OPERATION NOT SUPPORTED"));
                }

                varR = Utils.getVarByName(stringR);
                if (varR != null) { // RHS token is a variable
                    Utils.confirmCompatibilityAndEvaluate("+=", varL, varR, line);
                } else { // RHS token is a value (not a variable)
                    Utils.confirmCompatibilityAndEvaluateWithStringForVarR("+=", varL, stringR, line);
                }
                break;

            case "COMP_ASSIGNMENT_-=":
                // make sure LHS variable exists
                varNameL = line.getTokens().get(0);
                varL = Utils.getVarByName(varNameL);
                if (varL == null) {
                    Utils.printErrorAndExit(new Msg(line, "ASSIGNMENT TO UNDECLARED VARIABLE"));
                }

                stringR = null;
                if (varL.getType().equals("INT")) {
                    stringR = line.getTokens().get(2);
                } else { // operation not supported for STRING or BOOL
                    Utils.printErrorAndExit(new Msg(line, "OPERATION NOT SUPPORTED"));
                }

                varR = Utils.getVarByName(stringR);
                if (varR != null) { // RHS token is a variable
                    Utils.confirmCompatibilityAndEvaluate("-=", varL, varR, line);
                } else { // RHS token is a value (not a variable)
                    Utils.confirmCompatibilityAndEvaluateWithStringForVarR("-=", varL, stringR, line);
                }
                break;

            case "COMP_ASSIGNMENT_*=":
                // make sure LHS variable exists
                varNameL = line.getTokens().get(0);
                varL = Utils.getVarByName(varNameL);
                if (varL == null) {
                    Utils.printErrorAndExit(new Msg(line, "ASSIGNMENT TO UNDECLARED VARIABLE"));
                }

                stringR = null;
                if (varL.getType().equals("INT")) {
                    stringR = line.getTokens().get(2);
                } else { // operation not supported for STRING or BOOL
                    Utils.printErrorAndExit(new Msg(line, "OPERATION NOT SUPPORTED"));
                }

                varR = Utils.getVarByName(stringR);
                if (varR != null) { // RHS token is a variable
                    Utils.confirmCompatibilityAndEvaluate("*=", varL, varR, line);
                } else { // RHS token is a value (not a variable)
                    Utils.confirmCompatibilityAndEvaluateWithStringForVarR("*=", varL, stringR, line);
                }
                break;

            case "PRINT":
                String argument = line.getTokens().get(1); // value to be printed
                var = Utils.getVarByName(argument);
                if (var != null) { // argument is a variable's name
                    Utils.printVar(var, line);
                } else { // argument is a value
                    System.out.println(argument);
                }
                break;

            case "FOR_LOOP":
                List<String> statements = new ArrayList();
                String[] semicolonTokens = line.getText().split(";");

                // extract the first statement from the first token
                // eg: FOR 5 B += A --> B += A
                // and append to statements
                String ft = semicolonTokens[0]; // first token
                String[] spaceTokens = ft.split(" "); // tokens of the first token
                String firstStmt = Utils.makeSubstringFromTokenArray(spaceTokens, 2, -1, false);
                firstStmt += " ;";
                statements.add(firstStmt);

                // append other statements
                for (Integer i = 1; i <= semicolonTokens.length - 2; i++) {
                    String stmt = semicolonTokens[i];
                    stmt += " ;";
                    statements.add(stmt);
                }

                // In a loop, process each statement, as if they were an entire line
                Integer loopCount = Integer.parseInt(line.getTokens().get(1));
                for (Integer i = 0; i < loopCount; i++) {
                    for (String stmt : statements) {
                        Line innerLine = new Line(stmt, line.getLineNumber());
                        Main.executeLine(innerLine);
                    }
                }
                break;

            case "NESTED_FOR_LOOP":
                loopCount = Integer.parseInt(line.getTokens().get(1)); // outer loop count
                Integer nTokens = line.getNTokens();

                statements = new ArrayList();

                // the ending delimeters for a for loop is 'ENDFOR'
                // while that for a normal statement is ';'
                // So, we define 'mode' to track what (statement or for-loop)
                // is being parsed
                String mode = "STATEMENT"; // STATEMENT / FOR_LOOP
                String stmt = "";

                // start from index 2, i.e. first statement
                // escape the last token (ENDFOR)
                for (Integer i = 2; i < nTokens - 1; i++) {
                    String token = line.getTokens().get(i);
                    String tokenU = line.getTokensUpperCase().get(i);
                    stmt += " " + token;
                    if (mode.equals("STATEMENT") && token.equals(";")) {
                        statements.add(stmt);
                        stmt = ""; // reset statement
                    }
                    if (mode.equals("FOR_LOOP") && tokenU.equals("ENDFOR")) {
                        statements.add(stmt);
                        stmt = ""; // reset statement
                    }

                    if (tokenU.equals("FOR")) {
                        mode = "FOR_LOOP";
                    }
                    if (tokenU.equals("ENDFOR")) {
                        mode = "STATEMENT";
                    }

                }
                for (Integer i = 0; i < loopCount; i++) {
                    for (String stmt1 : statements) {
                        Line innerLine = new Line(stmt1, line.getLineNumber());
                        Main.executeLine(innerLine);
                    }
                }
                break;

            case "PROC_START":
                String procName = line.getTokensUpperCase().get(1); // use UPPERCASE name as key
                Procedure procedure = Main.getProcedureByName(procName);
                // there should be no previously parsed procedure with the same name 
                if (procedure == null) {
                    Main.setParseMode("PROCEDURE");
                    procedure = new Procedure(); 
                    procedure.setName(line.getTokensUpperCase().get(1)); // use UPPERCASE name as key
                    Main.addProcedureToList(procedure);
                    Main.setProcedureBeingParsed(procedure);
                } else {
                    Utils.printErrorAndExit(new Msg(line, "NON-UNIQUE PROCEDURE NAME"));
                }

                break;

            case "PROC_CALL":
                String procedureName = line.getTokens().get(1);
                procedure = Main.getProcedureByName(procedureName);
                for (Line ll : procedure.getLineList()) {
                    Main.executeLine(ll);
                }
                break;
            }
        }
    }

}

class Line {
    private String text;
    private ArrayList<String> tokens;
    private ArrayList<String> tokensLowerCase;
    private ArrayList<String> tokensUpperCase;
    private Integer nTokens;
    private String type; // 'VAR_DECLARATION', 'ASSIGNMENT', 'FOR_LOOP', 'PRINT'
    private Integer lineNumber;

    public Line(String text, Integer lineNumber) {
        text = text.strip();
        this.text = text;
        this.tokens = new ArrayList(Arrays.asList(text.split(" ")));
        this.tokensLowerCase = new ArrayList();
        for (String s : this.tokens) {
            this.tokensLowerCase.add(s.toLowerCase());
        }
        this.tokensUpperCase = new ArrayList();
        for (String s : this.tokens) {
            this.tokensUpperCase.add(s.toUpperCase());
        }
        this.nTokens = this.tokens.size();
        // todo: it was found that even for empty line, tokens.size() returns 1, so this
        // fix is introduced.
        if (this.tokens.get(0).equals("")) {
            this.nTokens = 0;
        }
        this.lineNumber = lineNumber;
        this.setType();
    }

    public String getText() {
        return this.text;
    }

    public ArrayList<String> getTokens() {
        return this.tokens;
    }

    public ArrayList<String> getTokensLowerCase() {
        return this.tokensLowerCase;
    }

    public ArrayList<String> getTokensUpperCase() {
        return this.tokensUpperCase;
    }

    public Integer getNTokens() {
        return this.nTokens;
    }

    public String getType() {
        return this.type;
    }

    public Integer getLineNumber() {
        return this.lineNumber;
    }

    private void setType() {
        this.type = "UNKNOWN"; // default

        // EMPTY LINE
        if (this.nTokens == 0) {
            this.type = "EMPTY_LINE";
            return;
        }

        String firstTokenUc = this.tokensUpperCase.get(0);

        // PRINT
        if (firstTokenUc.equals("PRINT")) {
            this.type = "PRINT";
            return;
        }
        // VAR_DECLARATION
        if (firstTokenUc.equals("STRING") || firstTokenUc.equals("INT") || firstTokenUc.equals("BOOL")) {
            this.type = "VAR_DECLARATION";
            return;
        }
        // FOR_LOOP / NESTED_FOR_LOOP
        if (firstTokenUc.equals("FOR")) {
            Integer forTokenCount = 0; // total number of 'FOR' tokens
            for (String t : this.tokensUpperCase) {
                if (t.equals("FOR")) {
                    forTokenCount += 1;
                }
            }
            if (forTokenCount > 1) {
                this.type = "NESTED_FOR_LOOP";
            } else {
                this.type = "FOR_LOOP";
            }
            return;
        }
        if (this.nTokens >= 2) {

            // all assignment operations
            if (this.tokens.get(1).equals("=")) {
                this.type = "ASSIGNMENT";
            } else if (this.tokens.get(1).equals("+=")) {
                this.type = "COMP_ASSIGNMENT_+=";
            } else if (this.tokens.get(1).equals("-=")) {
                this.type = "COMP_ASSIGNMENT_-=";
            } else if (this.tokens.get(1).equals("*=")) {
                this.type = "COMP_ASSIGNMENT_*=";
            }

        }
        // PROCEDURE START
        if (firstTokenUc.equals("PROC")) {
            this.type = "PROC_START";
        }
        // PROCEDURE END
        if (firstTokenUc.equals("ENDPROC")) {
            this.type = "PROC_END";
        }
        // PROCEDURE CALL
        if (firstTokenUc.equals("CALL")) {
            this.type = "PROC_CALL";
        }
    }

}

class Var {
    private String name;
    private String type; // "INT" / "STRING" / "BOOL"
    // type STRING has stringValue only
    // type BOOL has all stringValue, intValue and boolValue
    // type INT has stringalue and intValue
    private String stringValue;
    private Integer intValue;
    private Boolean boolValue;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStringValue() {
        return this.stringValue;
    }

    public Integer getIntValue() {
        return this.intValue;
    }

    public Boolean getBoolValue() {
        return this.boolValue;
    }

    public void setValues(String stringValue) {
        // set all 3 values (stringValue, integerValue and booleanValue)
        // ASSUMPTION: stringValue is valid (for the variable's type)

        this.stringValue = stringValue;
        String uc = stringValue.toUpperCase();
        switch (this.type) {
        case "STRING":
            // fallthrough
            break;
        case "BOOL":
            this.boolValue = uc.equals("TRUE") ? true : false;
            this.intValue = uc.equals("TRUE") ? 1 : 0;
            break;
        case "INT":
            if (uc.equals("TRUE")) {
                this.intValue = 1;
                break;
            }
            if (uc.equals("FALSE")) {
                this.intValue = 0;
                break;
            }
            this.intValue = Integer.parseInt(stringValue);
            break;
        }
    }

    public boolean isInitialized() {
        return this.stringValue == null ? false : true;
    }

    public String toString() {
        String str = "\nname        : " + this.name;
        str += "\ntype        : " + this.type;
        str += "\nstringValue : " + this.stringValue;
        str += "\nintValue    : " + this.intValue;
        str += "\nboolValue   : " + this.boolValue;
        return str;
    }
}

class Msg {
    private String msg;
    private Boolean validationPassed;
    private Scanner scanner;
    private Line line;

    public Msg(Line line, String msg) {
        this.line = line;
        this.msg = msg;
    }

    public Msg(Line line) {
        this.line = line;
    }

    public Msg() {
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Boolean getValidationPassed() {
        return this.validationPassed;
    }

    public void setValidationPassed(Boolean validationPassed) {
        this.validationPassed = validationPassed;
    }

    public Scanner getScanner() {
        return this.scanner;
    }

    public void setScanner(Scanner scanner) {
        this.scanner = scanner;
    }

    public Line getLine() {
        return this.line;
    }

    public void setLine(Line line) {
        this.line = line;
    }

}

class Procedure {
    String name;
    String nameUc; // upper case
    private List<Line> lineList = new ArrayList();

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        this.nameUc = name.toUpperCase();
    }

    public String getNameUc() {
        return this.nameUc;
    }

    public List<Line> getLineList() {
        return this.lineList;
    }

    public void setLineList(List<Line> lineList) {
        this.lineList = lineList;
    }

}

class Utils {
    public static Msg validateArgs(String args[]) {
        // make sure that the z+- program filepath is privided as an argument
        if (args.length == 0) {
            Msg msg = new Msg();
            msg.setMsg("Program name missing");
            msg.setValidationPassed(false);
            return msg;
        }

        // make sure that the provided filepath is valid
        File file = new File(args[0]);
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            Msg msg = new Msg();
            msg.setMsg(e.toString());
            msg.setValidationPassed(false);
            return msg;
        }
        Msg msg = new Msg();
        msg.setMsg("SUCCESS");
        msg.setValidationPassed(true);
        msg.setScanner(scanner);
        return msg;
    }

    public static boolean isReservedWord(String word) {
        ArrayList<String> reservedWords = new ArrayList(
                Arrays.asList("PRINT", "FOR", "ENDFOR", "PROC", "CALL", "INT", "BOOL", "STRING", "TRUE", "FALSE"));
        if (reservedWords.contains(word.toUpperCase())) {
            return true;
        }
        return false;
    }

    public static void printErrorAndExit(Msg msg) {
        if (msg.getLine() != null) {
            // System.out.println("RUNTIME ERROR: line " + msg.getLine().getLineNumber());
            System.out.println("RUNTIME ERROR: line " + msg.getLine().getLineNumber() + " -- " + msg.getMsg());
        } else {
            System.out.println("*** ERROR: " + msg.getMsg());

        }
        System.exit(1);
    }

    public static boolean validateTypeVsValue(String type, String stringValue) {
        String uc = stringValue.toUpperCase();
        switch (type) {
        case "INT":
            // handle 'TRUE' and 'FALSE' values
            if (uc.equals("TRUE") || uc.equals("FALSE")) {
                return true;
            }
            try {
                Integer.parseInt(stringValue);
            } catch (NumberFormatException e) {
                return false;
            }
            return true;
        case "BOOL":
            if (uc.equals("TRUE") || uc.equals("FALSE")) {
                return true;
            }
            return false;
        case "STRING":
            return true;
        }
        return false;
    }

    public static void printVars() {
        System.out.println("\n=== ALL VARIABLES ===");
        for (String varName : Main.getVarMap().keySet()) {
            Var var = Main.getVarMap().get(varName);
            System.out.println(var);
        }
    }

    public static Var getVarByName(String varName) {
        // returns Var (if exists), else returns null
        return Main.getVarMap().get(varName.toUpperCase());
    }

    public static boolean isCompatibleOperation(String operator, Var varL, Var varR, Line line) {
        // prints error and exists, in case operation is not compatible

        // if operator is "=", varR must be initialized.
        // if operator is other than "=", both varL and varR must be initialized
        if ((operator.equals("=") && !varR.isInitialized())
                || (!operator.equals("=") && (!varL.isInitialized() || !varR.isInitialized()))) {
            Utils.printErrorAndExit(new Msg(line, "UNINITIALIZED VARIABLE(S) PASSED FOR OPERATION"));
        }

        switch (operator) {
        case "=":
            if (varL.getType().equals(varR.getType())) {
                return true;
            }
            if (varL.getType().equals("INT") && varR.getType().equals("BOOL")) {
                return true;
            }
            return false;

        case "+=":
            if (varL.getType().equals("STRING") && varR.getType().equals("STRING")) {
                return true;
            }
            if (varL.getType().equals("INT") && varR.getType().equals("INT")) {
                return true;
            }
            if (varL.getType().equals("INT") && varR.getType().equals("BOOL")) {
                return true;
            }
            return false;

        case "-=":
            if (varL.getType().equals("INT") && varR.getType().equals("INT")) {
                return true;
            }
            if (varL.getType().equals("INT") && varR.getType().equals("BOOL")) {
                return true;
            }
            return false;

        case "*=":
            if (varL.getType().equals("INT") && varR.getType().equals("INT")) {
                return true;
            }
            if (varL.getType().equals("INT") && varR.getType().equals("BOOL")) {
                return true;
            }
            return false;
        }

        return false;
    }

    public static void confirmCompatibilityAndEvaluate(String operator, Var varL, Var varR, Line line) {

        // computed value is assigned to valL, so return type is void
        // prints error and exits, in case the operation is not possible

        if (!Utils.isCompatibleOperation(operator, varL, varR, line)) {
            Utils.printErrorAndExit(new Msg(line, "INCOMPATIBLE OPERATION"));
        }

        switch (operator) {
        case "=":
            varL.setValues(varR.getStringValue());
            break;

        case "+=":
            if (varL.getType().equals("STRING")) { // varL:STRING, varR:STRING
                String s = varL.getStringValue();
                s += varR.getStringValue();
                varL.setValues(s);
            } else if (varL.getType().equals("INT")) { // varL:INT, varR:INT/BOOL
                Integer i = varL.getIntValue();
                i += varR.getIntValue();
                varL.setValues(i.toString());
            }
            break;

        case "-=": // varL: INT, varR: INT/BOOL
            Integer i = varL.getIntValue();
            i -= varR.getIntValue();
            varL.setValues(i.toString());
            break;

        case "*=": // varL: INT, varR: INT/BOOL
            i = varL.getIntValue();
            i *= varR.getIntValue();
            varL.setValues(i.toString());
            break;
        }
    }

    public static void confirmCompatibilityAndEvaluateWithStringForVarR(String operator, Var varL, String stringR,
            Line line) {

        // computed value is assigned to valL, so return type is void
        // prints error and exits, in case the operation is not possible

        String uc = stringR.toUpperCase();
        Var tempVar = new Var();
        tempVar.setName("__tempVar__");
        switch (varL.getType()) {
        case "INT":
            if (uc.equals("TRUE") || uc.equals("FALSE")) {
                tempVar.setType("BOOL");
                tempVar.setValues(stringR);
            } else {
                try {
                    Integer.parseInt(stringR);
                    // these lines execute only if the above line executes successfully
                    tempVar.setType("INT");
                    tempVar.setValues(stringR);
                } catch (NumberFormatException e) {
                    Utils.printErrorAndExit(new Msg(line, "INVALID OPERAND VALUE"));
                }
            }
            break;

        case "STRING":
            tempVar.setType("STRING");
            tempVar.setValues(stringR);
            break;

        case "BOOL":
            System.out.println("********** unexpected ***********");
            break;
        }
        Utils.confirmCompatibilityAndEvaluate(operator, varL, tempVar, line);
    }

    public static void printVar(Var var, Line line) {
        switch (var.getType()) {
        case "STRING":
            String string = var.getStringValue();
            if (string != null) {
                System.out.println(var.getStringValue());
            } else {
                Utils.printErrorAndExit(new Msg(line, "UNINITIALIZED VARIABLE PRINTING ATTEMPT"));
            }
            break;
        case "INT":
            Integer integer = var.getIntValue();
            if (integer != null) {
                System.out.println(var.getStringValue());
            } else {
                Utils.printErrorAndExit(new Msg(line, "UNINITIALIZED VARIABLE PRINTING ATTEMPT"));
            }
            break;
        case "BOOL":
            Boolean bb = var.getBoolValue();
            if (bb != null) {
                // boolean values must be displayed in all UPPERASE spelling
                System.out.println(var.getBoolValue().toString().toUpperCase());
            } else {
                Utils.printErrorAndExit(new Msg(line, "UNINITIALIZED VARIABLE PRINTING ATTEMPT"));
            }
            break;
        }
    }

    public static String makeSubstringFromTokenList(List tokenList, Integer indexFrom, Integer indexTo,
            Boolean removeQuotes) {
        // both indexFrom and indexTo are inclusive
        // -ve indexing is also supported
        if (indexTo < 0) {
            indexTo = tokenList.size() + indexTo + 1;
        } else {
            indexTo += 1;
        }
        String newStr = String.join(" ", tokenList.subList(indexFrom, indexTo));
        if (removeQuotes) {
            newStr = newStr.replace("\"", "");
        }
        return newStr;
    }

    public static String makeSubstringFromTokenArray(String[] tokenArray, Integer indexFrom, Integer indexTo,
            Boolean removeQuotes) {
        // both indexFrom and indexTo are inclusive
        // -ve indexing is also supported
        List<String> tokenList = new ArrayList(Arrays.asList(tokenArray));
        return Utils.makeSubstringFromTokenList(tokenList, indexFrom, indexTo, removeQuotes);
    }

}
