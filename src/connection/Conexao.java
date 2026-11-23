package connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexao {
    private static final String URL = "jdbc:mysql://localhost:3306/Trabalho_Ninja";

    public static Connection abrirConexao(String usuario, String senha) throws SQLException {
        return DriverManager.getConnection(URL, usuario, senha);
    }
}
