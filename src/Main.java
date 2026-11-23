import javax.swing.SwingUtilities;

import view.JanelaPrincipal;

public class Main {
    public static void main(String[] args) {
        String usuario = System.getenv("MYSQL_USER");
        String senha = System.getenv("MYSQL_PASSWORD");

        SwingUtilities.invokeLater(() -> new JanelaPrincipal(usuario, senha).setVisible(true));
    }
}
