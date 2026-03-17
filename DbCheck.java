import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbCheck {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://127.0.0.1:5433/postgres";
        String user = "postgres";
        String password = "mysecretpassword";

        System.out.println("Connecting to " + url);
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connection successful!");
            System.out.println("Driver: " + conn.getMetaData().getDriverName());
            System.out.println("Version: " + conn.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            System.err.println("Connection failed!");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
