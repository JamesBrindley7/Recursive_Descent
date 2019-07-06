import java.io.IOException;
import java.util.ArrayList;
/**
 * 
 * @author James Brindley
 *
 */

public class SyntaxAnalyser extends AbstractSyntaxAnalyser{
	String indent = ("    ");
	int identnum = 0; //counts the number of indents in
	ArrayList<Variable> variables = new ArrayList<Variable>(); //Stores all variables loaded
	ArrayList<ArrayList<String>> loopcontents = new ArrayList<ArrayList<String>>(); //stores all variables in loops
	int loopcounter = -1; //counts the loop its currently in
	
	/**
	 * Constructor
	 * @param fileName
	 * @throws IOException
	 */
	public SyntaxAnalyser(String fileName) throws IOException {
		lex = new LexicalAnalyser(fileName);
	}
	/**
	 * Calls the statement parse method
	 */
	@Override
	public void _statementPart_() throws IOException, CompilationException {
		statementpartparse();
	}
	/**
	 * Recursively indent 
	 * @param num
	 */
	public void printindent(int num) {
		if(num <= 0) {
			return;
		}
		System.out.print(indent);
		printindent(num - 1);
	}
	/**
	 * Stores the varaible into a list
	 * @param v Variable
	 */
	public void storevariable(Variable v) {
		variables.add(v);
	}
	/**
	 * Removes the varaible from the varaible list 
	 * @param name name of the varaible
	 * @param num location it looks in
	 */
	public void removevariable(String name, int num) {
		if(variables.size() != 0) {
			if(variables.get(num).identifier.equals(name)) { //if it equals the name then remove
				variables.remove(num);
				return;
			} //if its not at the end of the list then call it again
			if(num+1 < variables.size()) {
				removevariable(name,num+1);
			}
		}
	}
	/**
	 * Checks if a varaible exists in the list
	 * @param v Variable
	 * @param num Number
	 * @return
	 */
	public boolean checkexists(Variable v, int num) {
		if(variables.size() != 0) {
			if(variables.get(num).identifier.equals(v.identifier) && variables.get(num).type.equals(v.type)) {
				return true;
			}
			if(num+1 < variables.size()) {
				boolean suc = checkexists(v,num+1);
				return suc;
			}
		}
		return false;
	}
	/**
	 * Checks if the variable name is located in the varaible storage and return the data type it is
	 * 0 means bad data type
	 * 1 means number
	 * 2 means string
	 * 3 means not created
	 * @param name
	 * @param num
	 * @return
	 */
	public int checkvaraiblename(String name, int num) {
		if(variables.size() != 0) {
			if(variables.get(num).identifier.equals(name)) {
				return variables.get(num).type.value;
			}
			if(num+1 < variables.size()) {
				int suc = checkvaraiblename(name,num+1);
				return suc;
			}
		}
		return 3;
	}
	/**
	 * Returns the varaible with the given name
	 * @param name name of the variable
	 * @param num number
	 * @return
	 */
	public Variable getvariables(String name, int num) {
		if(variables.size() != 0) {
			if(variables.get(num).identifier.equals(name)) {
				return variables.get(num);
			}
			if(num+1 < variables.size()) {
				Variable suc = getvariables(name,num+1);
				return suc;
			}
		}
		return null;
	}
	/**
	 * Checks a loops list to see if it contains a varaible with the name
	 * @param name name
	 * @param loopnum loop number to check
	 * @param num number
	 * @return
	 */
	public boolean checkloopvariables(String name, int loopnum, int num) {
		if(loopcontents.get(loopnum).size() != 0) {
			if(loopcontents.get(loopnum).get(num).equals(name)) {
				return true;
			}
			if(num+1 < loopcontents.get(loopnum).size()) {
				boolean suc = checkloopvariables(name,loopnum,num+1);
				return suc;
			}
		}
		return false;
	}
	/**
	 * Remove all varaibles contained within the loop varaibles list only used in it
	 * @param loopvariables 
	 * @param num
	 */
	public void loopthrough(ArrayList<String> loopvariables, int num) {
		if(variables.size() != 0) {
			Variable v = getvariables(loopvariables.get(num),0);
			printindent(identnum);
			myGenerate.removeVariable(v);
			removevariable(loopvariables.get(num),0);
			if(num+1 < loopvariables.size()) {
				loopthrough(loopvariables, num+1);
			}
		}
	}
	/**
	 * Reads the grammar of statementpart := begin <statement list> end
	 * checks the first token is begin, if so enter the statement list grammar
	 * After finished check there is an end token
	 * Then check if there is a End Of File token
	 * If passes then the syntax is correct
	 * @throws IOException
	 * @throws CompilationException
	 */
	void statementpartparse() throws IOException, CompilationException { 
		printindent(identnum);
		myGenerate.commenceNonterminal("StatementPart"); //begin StatementPart
		identnum++;
		
		if(nextToken.symbol == 2) {
			printindent(identnum);
			myGenerate.insertTerminal(nextToken);//print begin
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken, "Begin Expected ");
		}
		
		statementlistparse(); //enter statement list parser
		
		if(nextToken.symbol == 8) {
			myGenerate.insertTerminal(nextToken);//print end
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"end was Expected");
		}
		
		identnum--;
		printindent(identnum);
		myGenerate.finishNonterminal("StatementPart"); //end StatementPart
		
		nextToken = lex.getNextToken();
		if(nextToken.symbol == 10) { //checks if its the end of the file
			printindent(identnum);
			myGenerate.insertTerminal(nextToken);//print eof
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"End Of File Expected");
		}
		
	} 
	/**
	 * Reads the grammar of <statement list> ::= <statement> | <statement list> ; <statement>
	 * Is done by checking the token against the start of each known statement in the grammar, e.g. for, while, if etc
	 * Then checks if theres a semi colon indicating the start of another statement then loop and call statementlist parse
	 * , then check if its the end, if it isnt the end then report
	 * 
	 * @throws IOException
	 * @throws CompilationException
	 */
	void statementlistparse() throws IOException, CompilationException { //<statement list> ::= <statement> | <statement list> ; <statement>
		printindent(identnum);
		myGenerate.commenceNonterminal("StatementList"); //begin StatementList
		identnum++;
		
		nextToken = lex.getNextToken(); //check if the token is the start of a known statemnet
		if(nextToken.symbol == 16 || nextToken.symbol == 17 || nextToken.symbol == 36 || nextToken.symbol == 3|| nextToken.symbol == 7|| nextToken.symbol == 37 ) {
			statementparse();
		}
		
		if(nextToken.symbol == 9 || nextToken.symbol == 34 || nextToken.symbol == 34) { //checks if the symbol is unkown to the system
			printindent(identnum);
			myGenerate.reportError(nextToken,"Unexpected symbol "+nextToken.text);
		}
		else if(nextToken.symbol == 30) {//check semi colon
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print ; token
		}
		else {
			if(nextToken.symbol != 8) { //if its not the end then print it expected a ;
				printindent(identnum);
				myGenerate.reportError(nextToken,"; Expected");
			}
		}
		if(nextToken.symbol != 8 && nextToken.symbol != 10 ) { //if its not eof or end then get the next token and loop back into another statement list
			statementlistparse();
		}
		
		identnum--;
		printindent(identnum);
		myGenerate.finishNonterminal("StatementList"); //end StatementList
	} 
	/**
	 * Method checks // <statement> ::= <assignment statement> | <if statement> | <while statement> | <procedure statement> | <until statement> | <for statement>
	 * Checks to see what operation is being called then calls the ccorrect parser method for it
	 * @throws IOException
	 * @throws CompilationException
	 */
	void statementparse() throws IOException, CompilationException {	// <statement> ::= <assignment statement> | <if statement> | <while statement> | <procedure statement> | <until statement> | <for statement>
		printindent(identnum);
		myGenerate.commenceNonterminal("Statement"); //begin Statement
		
		identnum++;
		
		if(nextToken.symbol == 17) {//if starts with if statement print ifstatement
			ifstatementparse();
		}
		else if(nextToken.symbol == 36) {//if starts with while statement print whilestatement
			whilestatementparse();
		}
		else if(nextToken.symbol == 3) {//if starts with call then print procedurestatement
			procedurestatementparse();
		}
		else if(nextToken.symbol == 7) {//if starts with do statement print untilstatement
			untilstatementparse();
		}
		else if(nextToken.symbol == 37) {//if starts with for statement print forstatement
			forstatementparse();
		}
		else if(nextToken.symbol == 16) {//if it starts with an identifier print assignmentstatement
			assignmentstatementparse();
		}
		
		identnum--;
		printindent(identnum);
		myGenerate.finishNonterminal("Statement"); //end Statement
	} 
	/**
	 * Method for assignment statements <assignment statement> ::= identifier := <expression> | identifier := stringConstant
		Checks to see if theres an identifier e.g. x1 indicating a varaiblex and saves that token, if not then report error
		checks theres an equal sign in order for it to asign it
		The next token is then retrieved and a check is undergone to make sure that varaible can store the assgigned data type
	 * @throws IOException
	 * @throws CompilationException
	 */
	void assignmentstatementparse() throws IOException, CompilationException { //<assignment statement> ::= identifier := <expression> | identifier := stringConstant
		printindent(identnum);
		myGenerate.commenceNonterminal("AssignmentStatement");
		identnum++;
		
		Token identifert = nextToken;
		
		if(nextToken.symbol == 16) { //  identifier check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print identifier token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"identifier Expected");
		}
		
		nextToken = lex.getNextToken();
		if(nextToken.symbol == 1) { //  equals check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print equals token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,":= Expected");
		}
		nextToken = lex.getNextToken();
		
		Variable v = null;
		boolean nocheck = false;
		//check if already stored
		if(nextToken.symbol == 31) { //if the token is a string then store the string
			v = new Variable(identifert.text, Variable.Type.STRING);
		}
		else if(nextToken.symbol == 26 || nextToken.symbol ==13 || nextToken.symbol ==18|| nextToken.symbol ==13 || nextToken.symbol ==18) { //if its a number, create a variable for the number
			v = new Variable(identifert.text, Variable.Type.NUMBER);
		}
		else if(nextToken.symbol == 16){ //if its an identifier
			//check if it already exists as a variable if so check the next 
			int value = checkvaraiblename(nextToken.text, 0); //check if the varaible with this name already exists and what data type it is
			if(value != 0 && value != 3) { //if it hasnt been created or is a bad type then report else check the second
				int value2 = checkvaraiblename(identifert.text,0); //check the original identifier
				if(value2 == 3){ //check if its 3 meaning the vaariable doesnt exist then create one depending on the seconds data type
					if(value == 1) { 
						v = new Variable(identifert.text, Variable.Type.NUMBER);
					}
					else {
						v = new Variable(identifert.text, Variable.Type.STRING);
					}
				}
				else if(value != value2) { //if the values dont equal each other then there different varirables
					printindent(identnum);
					myGenerate.reportError(nextToken, nextToken.text+" and "+identifert.text+" are different variable types");
				}
				else if(value == value2){ //if they equal each other then mark to skip so the varaiable isnt created again
					nocheck = true;
				}
			}
			else {
				printindent(identnum);
				myGenerate.reportError(nextToken, "variable "+nextToken.text+" has not been declared or is a bad type");
			}
		}
		else {
			v = new Variable(identifert.text, Variable.Type.UNKNOWN);
		}
		
		if(nextToken.symbol == 31) { //if its a spring constant then print it
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print stringConstant
			nextToken = lex.getNextToken();
		}
		else { 
			expressionparse(); //if not then enter the expression grammar
		}
		if(!nocheck) { //if the varaibles don't exist already and are the same
			boolean check = checkexists(v,0); //check the created variable matches another
			if(check == false) { //if its already avaiable
				printindent(identnum);
				storevariable(v); //add the variable to the varaible list
				myGenerate.addVariable(v);
				if(loopcounter >= 0) { //check which loop its in
					boolean loopcheck = checkloopvariables(identifert.text,loopcounter,0); //check if its already stored in loops variables
					if(!loopcheck) {//if it doesnt already exist then add it to the loops varaibles so it can be deleted
						loopcontents.get(loopcounter).add(identifert.text);
					}
				}
			}
		}
		identnum--;
		printindent(identnum);
		myGenerate.finishNonterminal("AssignmentStatement"); //end AssignmentStatement
		
	} 
	/**
	 * checks <if statement> ::= if <condition> then <statement list> end if | if <condition> then <statement list> else <statement list> end if
	 * Starts by checking if the first token is an if token
	 * Then begins a condition parse
	 * Checks if theres a then token
	 * Then begins a statement list
	 * Checks if theres an else token, if not check if theres an end token then an if token . If neither are true then fail
	 * If its end if then end method
	 * If its an else then call statement list.
	 * @throws CompilationException
	 * @throws IOException
	 */
	void ifstatementparse() throws CompilationException, IOException {//<if statement> ::= if <condition> then <statement list> end if | if <condition> then <statement list> else <statement list> end if
		printindent(identnum);
		myGenerate.commenceNonterminal("IfStatement"); //begin IfStatement
		identnum++;
		
		if(nextToken.symbol == 17) { //check if
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print if token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"if Expected");
		}
		
		conditionparse(); //begin condition parse
		
		nextToken = lex.getNextToken();
		if(nextToken.symbol == 34) { //check then token
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print then token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"then Expected");
		}
		
		statementlistparse();//begin statement list
		nextToken = lex.getNextToken();
		
		if(nextToken.symbol == 9){ //check else token
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print else token
			statementlistparse(); //begin statement list
			nextToken = lex.getNextToken();
		}
		else {	
			if(nextToken.symbol != 8) { //if its end then skip
				printindent(identnum);
				myGenerate.reportError(nextToken,"end if / else Expected");
			}
		}
		if(nextToken.symbol == 8) { //check end
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print end token
			
			nextToken = lex.getNextToken();
			if(nextToken.symbol == 17) { //check if
				printindent(identnum);
				myGenerate.insertTerminal(nextToken); //print if token
			}
			else {
				printindent(identnum);
				myGenerate.reportError(nextToken,"if Expected");
			}
		}
		else {	
			printindent(identnum);
			myGenerate.reportError(nextToken,"end if Expected");
		}
		identnum--;
		printindent(identnum);
		myGenerate.finishNonterminal("IfStatement"); //end IfStatement
	} 
	/**
	 * <while statement> ::= while <condition> loop <statement list> end loop
	 * checks the statement begins with the token while
	 * Then start the condition parse
	 * Check the next tocken is loop
	 * Begin statement list parse
	 * Check the next token is end then if it is check if its followed by loop
	 * @throws IOException
	 * @throws CompilationException
	 */
	void whilestatementparse() throws IOException, CompilationException{ //<while statement> ::= while <condition> loop <statement list> end loop
		printindent(identnum);
		myGenerate.commenceNonterminal("WhileStatement"); //begin WhileStatement
		identnum++;
		
		if(nextToken.symbol == 36) {//while check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print while token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"while Expected");
		}
		
		conditionparse(); //begin condition
		
		nextToken = lex.getNextToken();
		if(nextToken.symbol == 23) {//loop check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print while token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"loop Expected");
		}
		
		statementlistparse(); //begin statement parse
		
		//nextToken = lex.getNextToken();
		if(nextToken.symbol == 8) { //check end
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print end token
			
			nextToken = lex.getNextToken();
			if(nextToken.symbol == 23) { //check loop so it makes end loop
				printindent(identnum);
				myGenerate.insertTerminal(nextToken); //print loop token
			}
			else {
				printindent(identnum);
				myGenerate.reportError(nextToken,"loop Expected after end");
			}
		}
		else {	
			printindent(identnum);
			myGenerate.reportError(nextToken,"end loop Expected");
		}
		nextToken = lex.getNextToken();
		
		identnum--;
		printindent(identnum);
		myGenerate.finishNonterminal("WhileStatement"); //end WhileStatement
	}
	/**
	 * <procedure statement> ::= call identifier ( <argument list> )
	 * Check first token is call
	 * Loads next token and checks its an identifier
	 * Loads next token and checks its a left bracket
	 * Begin argument list parsing
	 * Loads next token and checks its a right bracket
	 * @throws IOException
	 * @throws CompilationException
	 */
	void procedurestatementparse() throws IOException, CompilationException{ //<procedure statement> ::= call identifier ( <argument list> )
		printindent(identnum);
		myGenerate.commenceNonterminal("ProcedureStatement"); //begin ProcedureStatement
		identnum++;
		
		if(nextToken.symbol == 3) { // call check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print call token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"call Expected");
		}
		
		nextToken = lex.getNextToken();
		if(nextToken.symbol == 16) { //  identifier check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print identifier token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"identifier Expected");
		}
		
		nextToken = lex.getNextToken();
		if(nextToken.symbol == 20) { //left bracket check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print ( token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"( Expected");
		}
		
		argumentlistparse(); //enter argument list parser
		
		if(nextToken.symbol == 29) { // right bracket check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print ) token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,") Expected");
		}
		
		nextToken = lex.getNextToken();
		identnum--;
		printindent(identnum);
		myGenerate.finishNonterminal("ProcedureStatement"); //end ProcedureStatement
	}
	/**
	 * <until statement> ::= do <statement list> until <condition>
	 * Check the first token is do
	 * If it is then enter the statement list parse
	 * Check the next token is until
	 * The enter the condition parse
	 * @throws IOException
	 * @throws CompilationException
	 */
	void untilstatementparse() throws IOException, CompilationException{ //<until statement> ::= do <statement list> until <condition>
		printindent(identnum);
		myGenerate.commenceNonterminal("UntilStatement"); //begin UntilStatement
		identnum++;
		
		if(nextToken.symbol == 7) { // do check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print do token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"do Expected");
		}
		
		statementlistparse(); //enter statement list
		
		nextToken = lex.getNextToken();
		if(nextToken.symbol == 35) { // until check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print until token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"until Expected");
		}
		
		conditionparse(); //enter condition
		
		identnum--;
		printindent(identnum);
		myGenerate.finishNonterminal("UntilStatement"); //end UntilStatement
	}
	/**
	 * <for statement> ::= for ( <assignment statement> ; <condition> ; <assignment statement> ) do <statement list> end loop
	 * Checks if the token is a for token
	 * Checks it has a left bracket as the next token
	 * Load next token and check if the identifier already has a varaible, if not save it
	 * Begin assignment statement parse
	 * Check next token is semi colon
	 * Begin condition parse
	 * Check next token is semi colon
	 * Begin assignment statement parse
	 * Check right bracket
	 * Check token is do
	 * Begin statemnet list
	 * Check end
	 * Check loop
	 * @throws IOException
	 * @throws CompilationException
	 */
	void forstatementparse() throws IOException, CompilationException{//<for statement> ::= for ( <assignment statement> ; <condition> ; <assignment statement> ) do <statement list> end loop
		printindent(identnum);
		myGenerate.commenceNonterminal("ForStatement");//begin ForStatement	
		identnum++;
		
		ArrayList<String> loopvariables = new ArrayList<String>(); //creates a new loop list to store the loops varaibles
		loopcontents.add(loopvariables); //adds this to the loop list
		loopcounter++;
		
		if(nextToken.symbol == 37) { // for check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print for token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"for Expected");
		}
		
		nextToken = lex.getNextToken();
		if(nextToken.symbol == 20) { // left bracket check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print ( token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"( Expected");
		}
		
		nextToken = lex.getNextToken();
		
		int value = checkvaraiblename(nextToken.text, 0);
		if(value == 3) {//its never existed so new to this loop
			loopvariables.add(nextToken.text);
		}
		
		assignmentstatementparse(); //begin assignment statement
		
		//nextToken = lex.getNextToken();
		if(nextToken.symbol == 30) { //semi colon check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print ; token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"; Expected");
		}
		
		conditionparse(); //begin condition
		
		nextToken = lex.getNextToken();
		if(nextToken.symbol == 30) { //semi colon check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print ; token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"; Expected");
		}
		
		nextToken = lex.getNextToken();
		assignmentstatementparse(); //begin assignment statement
			
		//nextToken = lex.getNextToken();
		
		if(nextToken.symbol == 29) { //right bracket check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print ) token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,") Expected");
		}
		
		nextToken = lex.getNextToken();
		
		if(nextToken.symbol == 7) { //do check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print do token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"do Expected");
		}
		
		statementlistparse(); //begin statement list
		
		//nextToken = lex.getNextToken();
		
		if(nextToken.symbol == 8) { //end check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print end token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"end Expected");
		}
		
		nextToken = lex.getNextToken();
		if(nextToken.symbol == 23) { //loop check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print loop token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"loop Expected");
		}
		
		nextToken = lex.getNextToken();
		
		//delete the local variables
		int size = loopvariables.size();  //get size of the loops varaible list
		
		if(size >= 1) { //if its more than 0 then loop through and delete all of them
			loopthrough(loopvariables, 0);
			loopcontents.remove(loopcounter);
		}
		
		loopcounter--;
		identnum--;
		printindent(identnum);
		myGenerate.finishNonterminal("ForStatement"); //end ForStatement
	}
	/**
	 * Check first token is a identifier
	 * 
	 * @throws IOException
	 * @throws CompilationException
	 */
	void argumentlistparse() throws IOException, CompilationException{//<argument list> ::= identifier | <argument list> , identifier
		printindent(identnum);
		myGenerate.commenceNonterminal("ArgumentList");//begin ArgumentList
		identnum++;
		
		nextToken = lex.getNextToken();
		if(nextToken.symbol == 16) {
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print identifier
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"identifier Expected");
		}
		
		nextToken = lex.getNextToken();
		
		if(nextToken.symbol == 5) {
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print comma
			argumentlistparse();
		}
		
		identnum--;
		printindent(identnum);
		myGenerate.finishNonterminal("ArgumentList"); //end ArgumentList
	}
	/**
	 * <condition> ::= identifier <conditional operator> identifier | identifier <conditional operator> numberConstant | identifier <conditional operator> stringConstant
	 * Checks first token is an identifier
	 * Begin condition operator parse
	 * Checks if the next token is a identifier, numberConstant or stringConstant
	 * @throws IOException
	 * @throws CompilationException
	 */
	void conditionparse() throws IOException, CompilationException{//<condition> ::= identifier <conditional operator> identifier | identifier <conditional operator> numberConstant | identifier <conditional operator> stringConstant
		printindent(identnum);
		myGenerate.commenceNonterminal("Condition");//begin Condition
		identnum++;
		
		nextToken = lex.getNextToken();
		if(nextToken.symbol == 16) { //identifier check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print identifier token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"identifier Expected");
		}
		
		conditionaloperatorparse();
		
		nextToken = lex.getNextToken();
		if(nextToken.symbol == 16 || nextToken.symbol == 26 || nextToken.symbol == 31|| nextToken.symbol ==13 || nextToken.symbol ==18 ) { //identifier check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print identifier token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"identifier / numberConstant / stringConstant Expected");
		}
		
		identnum--;
		printindent(identnum);
		myGenerate.finishNonterminal("Condition"); //end Condition
	}
	/**
	 * Checks if next token is > | >= | = | /= | < | <=
	 * @throws IOException
	 * @throws CompilationException
	 */
	void conditionaloperatorparse() throws IOException, CompilationException{//<conditional operator> ::= > | >= | = | /= | < | <=
		printindent(identnum);
		myGenerate.commenceNonterminal("ConditionalOperator");//begin ConditionalOperator
		identnum++;
		
		nextToken = lex.getNextToken();
		if(nextToken.symbol == 15 || nextToken.symbol == 14 || nextToken.symbol == 11 || nextToken.symbol == 25 || nextToken.symbol == 22 || nextToken.symbol == 21) { //operator check
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print identifier token
		}
		else {
			printindent(identnum);
			myGenerate.reportError(nextToken,"'>' / '>=' / '=' / '/=' / '<' / '<=' Expected");
		}
		
		identnum--;
		printindent(identnum);
		myGenerate.finishNonterminal("ConditionalOperator"); //end ConditionalOperator
	}
	/**
	 * Saves first token, begins term parse
	 * Checks if theres a semi colon, right bracket or end 
	 * If it doesnt have any then check if its a plus or minus
	 * Check if the operation is on two data types that are the same
	 * If they are the same and not strings then load expression parses again
	 * @throws IOException
	 * @throws CompilationException
	 */
	void expressionparse() throws IOException, CompilationException{//<expression> ::= <term> | <expression> + <term> | <expression> - <term>
		boolean hassemi = false;
		printindent(identnum);
		myGenerate.commenceNonterminal("Expression");//begin Expression
		identnum++;
		Token identifert = nextToken; //gets first identifier/number
		
		
		termparse();
		
		if(nextToken.symbol == 30 || nextToken.symbol == 8 || nextToken.symbol == 29) { //checks if semi colon
			hassemi = true;
		}
		if(hassemi == false) {
			//nextToken = lex.getNextToken();
			if(nextToken.symbol == 27 || nextToken.symbol == 24) {
				int plusminus = 0;
				if(nextToken.symbol == 27) { //if plus then 1
					plusminus = 1;
				}
				printindent(identnum);
				myGenerate.insertTerminal(nextToken); //print + or -
				
				nextToken = lex.getNextToken();
				
				if(nextToken.symbol == 16 || identifert.symbol == 16){
					//check if it already exists as a variable if so check the next 
					int value = checkvaraiblename(nextToken.text, 0);
					int value2 = checkvaraiblename(identifert.text,0);
					if(plusminus == 1 ) {
						if(value == 2 && value2 == 2) {
							//can add if they are both strings
						}
						else if(value == 2 || value2 == 2) {
							printindent(identnum);
							myGenerate.reportError(nextToken, nextToken.text+" and "+identifert.text+" can't add or subract strings with non-string variables");
						}
					}
					else if(plusminus == 0 && (value == 2 || value2 == 2)){
						printindent(identnum);
						myGenerate.reportError(nextToken, nextToken.text+" and "+identifert.text+" can't subtract strings");
					}
				}
				
				expressionparse();
			}
			else {
				printindent(identnum);
				myGenerate.reportError(nextToken,"+ / - Expected");
			}
		}
		
		identnum--;
		printindent(identnum);
		myGenerate.finishNonterminal("Expression"); //end Expression
	}
	/**
	 * <term> ::= <factor> | <term> * <factor> | <term> / <factor>
	 * Enters factor parse
	 * Checks if the token is multiply or divide
	 * Checks if either varaible is a string value
	 * @throws IOException
	 * @throws CompilationException
	 */
	void termparse() throws IOException, CompilationException{//<term> ::= <factor> | <term> * <factor> | <term> / <factor>
		boolean hassemi = false;
		printindent(identnum);
		myGenerate.commenceNonterminal("Term");//begin Term
		identnum++;
		Token identifert = nextToken; //gets first identifier/number
		
		factorparse();
		
		nextToken = lex.getNextToken();
		if(nextToken.symbol == 30 || nextToken.symbol == 27 || nextToken.symbol == 24 || nextToken.symbol == 8 || nextToken.symbol == 29) { //checks the next symbol to see if it matchrd expression
			hassemi = true;
		}
		if(nextToken.symbol == 33 || nextToken.symbol == 6 && hassemi == false) { //checks if the next token is * or /
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print * or /
			nextToken = lex.getNextToken();
			
			if(nextToken.symbol == 16 || identifert.symbol == 16){
				//check if it already exists as a variable if so check the next 
				int value = checkvaraiblename(nextToken.text, 0);
				int value2 = checkvaraiblename(identifert.text,0);
				if(value == 2 || value2 == 2) {
					printindent(identnum);
					myGenerate.reportError(nextToken, nextToken.text+" and "+identifert.text+" can't multiply or divide strings");
				}
			}
			termparse();
		}
		else if(hassemi == false){
			printindent(identnum);
			myGenerate.reportError(nextToken,"'*' / '/' / ';' Expected");
			
		}
		
		identnum--;
		printindent(identnum);
		myGenerate.finishNonterminal("Term"); //end Term
	}
	/**
	 * <factor> ::= identifier | numberConstant | ( <expression> )
	 * Checks if the token is an identifer or number constant
	 * if it isnt then it must be an expression
	 * sCheck if it has a right bracket then begin the expression parse
	 * Check the baracket is closed
	 * @throws IOException
	 * @throws CompilationException
	 */
	void factorparse() throws IOException, CompilationException{//<factor> ::= identifier | numberConstant | ( <expression> )
		printindent(identnum);
		myGenerate.commenceNonterminal("Factor");//begin Factor
		identnum++;
		
		if(nextToken.symbol == 16 || nextToken.symbol == 26|| nextToken.symbol ==13 || nextToken.symbol ==18) { //checks if its an identifier or number constant
			printindent(identnum);
			myGenerate.insertTerminal(nextToken); //print identifier or numberConstant
		}
		else { //if not must be an expression
			if(nextToken.symbol == 20) {//if it is a bracket then do expression
				printindent(identnum);
				myGenerate.insertTerminal(nextToken); //print identifier or numberConstant
				expressionparse();
				
				if(nextToken.symbol == 29) {//bracket check
					printindent(identnum);
					myGenerate.insertTerminal(nextToken); //print )
				}
				else {
					printindent(identnum);
					myGenerate.reportError(nextToken,") Expected");
					
				}
			}
			else {
				printindent(identnum);
				myGenerate.reportError(nextToken,"identifier / numberConstant / ( Expected");
			
			}
		}
		
		identnum--;
		printindent(identnum);
		myGenerate.finishNonterminal("Factor"); //end Factor
	}

	@Override
	public void acceptTerminal(int symbol) throws IOException, CompilationException {
		
	}

}
