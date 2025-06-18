import java.io.*;
import java.sql.*;
import java.util.Scanner;

public class FileDBExecutor4 {
    public static void main(String[] args) {
        String[] menuOptions = {
            "1. Create Tables",
            "2. Insert into DEPT",
            "3. Insert into EMP",
            "4. Insert into SALGRADE",
            //"5. Delete from DEPT",
            //"6. Delete from EMP",
            //"7. Delete from SALGRADE",
            //"8. Update EMP",
            //"9. Update DEPT",
            //"10. Update SALGRADE",
            "5. Drop Tables"
        };

        String[] filePaths = {
            "D:\\java\\commands.txt",
            "D:\\java\\dept.txt",
            "D:\\java\\emp.txt",
            "D:\\java\\sal.txt",
            //"D:\\java\\deptdel.txt",
            //"D:\\java\\empdel.txt",
            //"D:\\java\\saldel.txt",
            //"D:\\java\\empup.txt",
            //"D:\\java\\deptup.txt",
            //"D:\\java\\salup.txt",
            "D:\\java\\drop.txt"
        };

        String url = "jdbc:mysql://localhost:3306/companydb";
        String user = "root";
        String password = "madhuri@123";

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\nSelect the SQL file to execute:");
            for (String option : menuOptions) {
                System.out.println(option);
            }

            System.out.print("Enter your choice (1-5): ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            if (choice < 1 || choice > filePaths.length) {
                System.out.println("Invalid choice. Try again.");
                continue;
            }

            String selectedFile = filePaths[choice - 1];

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");

                try (
                    Connection conn = DriverManager.getConnection(url, user, password);
                    Statement stmt = conn.createStatement();
                    BufferedReader reader = new BufferedReader(new FileReader(selectedFile))
                ) {
                    System.out.println("Connected to database.");
                    System.out.println("Executing SQL from: " + selectedFile + "\n");

                    StringBuilder queryBuilder = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;

                        queryBuilder.append(line).append(" ");

                        if (line.endsWith(";")) {
                            String query = queryBuilder.toString().trim();
                            query = query.substring(0, query.length() - 1); // remove ;

                            System.out.println("Query: " + query);

                            try {
                                if (query.toLowerCase().startsWith("select")) {
                                    ResultSet rs = stmt.executeQuery(query);
                                    ResultSetMetaData rsmd = rs.getMetaData();
                                    int columnCount = rsmd.getColumnCount();

                                    while (rs.next()) {
                                        for (int i = 1; i <= columnCount; i++) {
                                            System.out.print(rs.getString(i) + "\t");
                                        }
                                        System.out.println();
                                    }
                                    rs.close();

                                } else if (query.toLowerCase().startsWith("insert into emp")) {
                                    String valuesPart = query.toLowerCase().replace("insert into emp", "").trim();
                                    valuesPart = valuesPart.substring(valuesPart.indexOf("(") + 1, valuesPart.indexOf(")"));
                                    String[] values = valuesPart.split(",");
                                    int empno = Integer.parseInt(values[0].trim());

                                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM EMP WHERE EMPNO = " + empno);
                                    rs.next();
                                    int count = rs.getInt(1);
                                    rs.close();

                                    if (count > 0) {
                                        String dupQuery = query.replaceFirst("(?i)insert into emp", "INSERT INTO EMP_DUPLICATES");
                                        int dupRows = stmt.executeUpdate(dupQuery);
                                        System.out.println("Duplicate found! Inserted into EMP_DUPLICATES (" + dupRows + " row).");
                                    } else {
                                        int rowsAffected = stmt.executeUpdate(query);
                                        System.out.println("Query OK, " + rowsAffected + " row(s) affected.");
                                    }

                                } else {
                                    int rowsAffected = stmt.executeUpdate(query);
                                    System.out.println("Query OK, " + rowsAffected + " row(s) affected.");
                                }

                            } catch (SQLException e) {
                                System.err.println("SQL Error: " + e.getMessage());
                            }

                            System.out.println("--------------------------------------------------");
                            queryBuilder.setLength(0); // reset builder
                        }
                    }

                }

            } catch (ClassNotFoundException e) {
                System.err.println("MySQL JDBC Driver not found.");
            } catch (SQLException e) {
                System.err.println("Database connection error: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("File read error: " + e.getMessage());
            }

            System.out.print("\nDo you want to continue? (Y/N): ");
            String again = scanner.nextLine().trim();
            if (again.equalsIgnoreCase("N")) {
                System.out.println("Exiting program.");
                break;
            }
        }

        scanner.close();
    }
}
