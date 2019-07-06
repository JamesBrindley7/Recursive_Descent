
public class Generate extends AbstractGenerate {
	
	public Generate() {
		
	}
	
	/**
	 * Throws the error message to stop the syntax analyser
	 */
	@Override
	public void reportError(Token token, String explanatoryMessage) throws CompilationException {
		if(token.text.equals("")) {
			System.out.println("rggError " +explanatoryMessage+ " but no symbol was found on line "+token.lineNumber);	
		}
		else {
			System.out.println("rggError " +explanatoryMessage+ " but symbol "+token.text+" was found on line "+token.lineNumber);	
		}
		CompilationException c = new CompilationException(explanatoryMessage, token.lineNumber);
		throw c;
	}
}
