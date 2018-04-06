import syntaxtree.*;
import visitor.*;
import java.io.*;

public class Main {
    public static void main (String [] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Main [file1] [file2] ... [fileN]");
            System.exit(1);
        }
        FileInputStream fis = null;
        for (int i = 0; i < args.length; i++) {
            try {
				System.out.println("\n\nChecking file " + args[i]);
                // Don't forget to place input files in the "input" folder
                String inputDir = "../input/" + args[i];
                fis = new FileInputStream(inputDir);

                // Parse file
                MiniJavaParser parser = new MiniJavaParser(fis);
                Goal root = parser.Goal();
                System.err.println("Program parsed successfully.");

                // Create and populate Symbol table (using STPopulatorVisitor)
                STPopulatorVisitor stPopVisitor = new STPopulatorVisitor();
                SymbolTable symTable = new SymbolTable();
                root.accept(stPopVisitor, symTable);

                // Perform type check (using TypeCheckVisitor)
                TypeCheckVisitor tyCheVisitor = new TypeCheckVisitor();
                root.accept(tyCheVisitor, symTable);

                // If there are no errors, print offsets stored in the Symbol Table
                symTable.printOffsets();
            } catch (ParseException ex) {
                System.out.println(ex.getMessage());
            } catch (FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            } finally {
                try {
                    if (fis != null) fis.close();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}
