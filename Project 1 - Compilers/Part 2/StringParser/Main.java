import java_cup.runtime.*;
import java.io.*;

class Main {
    public static void main(String[] argv) throws Exception{
        PrintStream out = new PrintStream(new FileOutputStream("output.txt"));
        System.out.println("Please type a program you want to be translated into Java:");
        Parser p = new Parser(new Scanner(new InputStreamReader(System.in)));
        System.setOut(out);
        p.parse();
    }
}
