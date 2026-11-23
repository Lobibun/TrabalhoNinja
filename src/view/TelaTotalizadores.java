package view;

import java.awt.BorderLayout;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import dao.TotalizadorDAO;
import model.Totalizador;

final class TelaTotalizadores extends JFrame {
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss");

    private final TotalizadorDAO dao = new TotalizadorDAO();
    private final String usuario;
    private final String senha;
    private final JButton gerar = new JButton("Gerar totalizadores");
    private final JLabel mensagem = new JLabel("Gere os totais para consultar e salvar esta apuração.");
    private final DefaultTableModel modelo = new DefaultTableModel(
        new String[] {"Descrição", "Quantidade", "Data de geração"}, 0
    ) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int coluna) {
            return coluna == 1 ? Integer.class : String.class;
        }
    };

    TelaTotalizadores(String usuario, String senha) {
        super("Totalizadores");
        this.usuario = usuario;
        this.senha = senha;

        JTable tabela = new JTable(modelo);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(360);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(115);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(175);
        DefaultTableCellRenderer centralizado = new DefaultTableCellRenderer();
        centralizado.setHorizontalAlignment(SwingConstants.CENTER);
        tabela.getColumnModel().getColumn(1).setCellRenderer(centralizado);
        tabela.getColumnModel().getColumn(2).setCellRenderer(centralizado);

        JPanel acoes = new JPanel(new BorderLayout(12, 0));
        acoes.add(gerar, BorderLayout.WEST);
        acoes.add(mensagem, BorderLayout.CENTER);

        JPanel painel = new JPanel(new BorderLayout(0, 16));
        painel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        painel.add(acoes, BorderLayout.NORTH);
        painel.add(ComponentesCadastro.painelTabela("Totais da última geração nesta tela", tabela),
            BorderLayout.CENTER);
        setContentPane(painel);

        gerar.addActionListener(evento -> gerarTotais());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(820, 490);
    }

    private void gerarTotais() {
        if (usuario == null || usuario.isBlank() || senha == null) {
            mensagem.setText("Configure MYSQL_USER e MYSQL_PASSWORD no arquivo .env.");
            return;
        }
        gerar.setEnabled(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        mensagem.setText("Calculando e salvando os totais...");

        new SwingWorker<List<Totalizador>, Void>() {
            @Override
            protected List<Totalizador> doInBackground() throws SQLException {
                return dao.gerarESalvar(usuario, senha);
            }

            @Override
            protected void done() {
                try {
                    List<Totalizador> totais = get();
                    modelo.setRowCount(0);
                    for (Totalizador total : totais) {
                        modelo.addRow(new Object[] {
                            total.descricao(), total.quantidade(), total.dataGeracao().format(FORMATO_DATA)
                        });
                    }
                    mensagem.setText(totais.size() + " resultados salvos no MySQL.");
                } catch (ExecutionException erro) {
                    if (erro.getCause() instanceof SQLException sql) {
                        mostrarErroSql(sql);
                    } else {
                        mensagem.setText("Não foi possível confirmar a geração. Verifique o MySQL.");
                    }
                } catch (InterruptedException erro) {
                    Thread.currentThread().interrupt();
                    mensagem.setText("A confirmação da geração foi interrompida.");
                } finally {
                    gerar.setEnabled(true);
                    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                }
            }
        }.execute();
    }

    private void mostrarErroSql(SQLException erro) {
        int codigo = erro.getErrorCode();
        String resumo = switch (codigo) {
            case 1045 -> "O MySQL recusou as credenciais configuradas.";
            case 1142 -> "O usuário do aplicativo não tem a permissão exigida.";
            case 1146 -> "O MySQL não encontrou uma tabela necessária.";
            default -> "O MySQL não confirmou a geração.";
        };
        mensagem.setText(codigo == 0 ? resumo : resumo + " Código " + codigo + ".");
        JOptionPane.showMessageDialog(this,
            resumo + "\n\nCódigo MySQL: " + codigo + "\nSQLState: " + erro.getSQLState()
                + "\n" + erro.getMessage(),
            "Erro ao gerar totalizadores", JOptionPane.ERROR_MESSAGE);
    }
}
