import java.io.InputStream;
import java.io.IOException;

class CalculatorEval {

    private int lookaheadToken;

    private InputStream in;

    private CalculatorEval(InputStream in) throws IOException {
		this.in = in;
		lookaheadToken = in.read();
    }

    private void consume(int symbol) throws IOException, ParseError {
		if (lookaheadToken != symbol)
	    	throw new ParseError();
		lookaheadToken = in.read();
    }

    private int evalNum(int num){
        return num - '0';
    }

    private int eval_prog() throws IOException, ParseError {
        int rv = exp();
        if (lookaheadToken != '\n' && lookaheadToken != -1)
            throw new ParseError();
        return rv;
    }


    private int exp() throws IOException, ParseError {
        // Check for invalid tokens to throw exceptions
        if ((lookaheadToken < '0' || lookaheadToken > '9') && lookaheadToken != '(')
            throw new ParseError();

        return exp2(term());
    }

    private int exp2(int interm) throws IOException, ParseError {
        if (lookaheadToken == ')' || lookaheadToken == '\n' || lookaheadToken == -1)
            return interm;
        // Check for invalid tokens to throw exceptions
        if (lookaheadToken != '+' && lookaheadToken != '-')
            throw new ParseError();

        int rt_val;
        if (lookaheadToken == '+') {
            consume('+');
            rt_val = exp2(interm + term());
        }
        else {
            consume('-');
            rt_val = exp2(interm - term());
        }
        return rt_val;
    }

    private int term() throws IOException, ParseError {
        // Check for invalid tokens to throw exceptions
        if ((lookaheadToken < '0' || lookaheadToken > '9') && lookaheadToken != '(')
            throw new ParseError();

        return term2(fact());
    }

    private int term2(int infact) throws IOException, ParseError {
        if (lookaheadToken == '+' || lookaheadToken == '-' || lookaheadToken == ')'
                || lookaheadToken == '\n' || lookaheadToken == -1  )
            return infact;
        // Check for invalid tokens to throw exceptions
        if (lookaheadToken != '*' && lookaheadToken != '/')
            throw new ParseError();

        int rt_val;
        if (lookaheadToken == '*') {
            consume('*');
            rt_val = term2(infact * fact());
        }
        else {
            consume('/');
            rt_val = term2(infact / fact());
        }
        return rt_val;
    }

    private int fact() throws IOException, ParseError {
        // Check for invalid tokens to throw exceptions
        if ((lookaheadToken < '0' || lookaheadToken > '9') && lookaheadToken != '(')
            throw new ParseError();

        int rt_val;
        if (lookaheadToken == '(') {
            consume('(');
            rt_val = exp();
            consume(')');
        }
        else {
            rt_val = evalNum(lookaheadToken);
            consume(lookaheadToken);
        }
        return rt_val;

    }


    public static void main(String[] args) {
	    try {
            System.out.println("Please type an arithmetic expression for evaluation:");
            CalculatorEval evaluate = new CalculatorEval(System.in);
	        System.out.println(evaluate.eval_prog());
	    }
	    catch (IOException e) {
	        System.err.println(e.getMessage());
	    }
	    catch(ParseError e){
	        System.err.println(e.getMessage());
	    }
    }
}

