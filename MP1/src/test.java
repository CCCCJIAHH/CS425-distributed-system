import javax.script.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class test {
    public static void main(String[] args) throws IOException, ScriptException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
            }
        }
    }
}
