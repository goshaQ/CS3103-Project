import java.util.Scanner;

public class Main {
    public static final int LISTENING_PORT = 5678;

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        boolean  isSelected = false;
        while (!isSelected) {
            System.out.println("Enter the mode:");
            System.out.println("1. Client");
            System.out.println("2. Server\n");

            int option = scanner.nextInt();
            switch (option) {
                case 1:
                    new Client();

                    isSelected = true;
                    break;
                case 2:
                    new Server();

                    isSelected = true;
                    break;
            }
        }
    }
}
