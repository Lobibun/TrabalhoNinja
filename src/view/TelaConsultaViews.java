package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import dao.ConsultaViewsDAO;
import dao.ConsultaViewsDAO.Consulta;

final class TelaConsultaViews extends JFrame {
    private final ConsultaViewsDAO dao = new ConsultaViewsDAO();
    private final String usuario;
    private final String senha;
    private final List<JToggleButton> opcoes = new ArrayList<>();
    private final JButton atualizar = new JButton("Atualizar");
    private final JLabel mensagem = new JLabel(" ");
    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }
    };
    private final JTable tabela = new JTable(modelo);
    private Consulta selecionada = Consulta.NINJAS_E_MISSOES;

    TelaConsultaViews(String usuario, String senha) {
        super("Consultar views");
        this.usuario = usuario;
        this.senha = senha;

        JPanel acoes = new JPanel(new BorderLayout(12, 0));
        JPanel consultas = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        ButtonGroup grupo = new ButtonGroup();
        for (Consulta consulta : Consulta.values()) {
            JToggleButton botao = new JToggleButton(consulta.titulo(), consulta == selecionada);
            botao.addActionListener(evento -> trocarConsulta(consulta));
            grupo.add(botao);
            opcoes.add(botao);
            consultas.add(botao);
        }
        atualizar.addActionListener(evento -> carregar());
        acoes.add(consultas, BorderLayout.CENTER);
        acoes.add(atualizar, BorderLayout.EAST);

        JPanel painel = new JPanel(new BorderLayout(0, 16));
        painel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        painel.add(acoes, BorderLayout.NORTH);
        painel.add(ComponentesCadastro.painelTabela("Resultados da consulta", tabela), BorderLayout.CENTER);
        painel.add(mensagem, BorderLayout.SOUTH);
        setContentPane(painel);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(960, 490);
        setMinimumSize(new Dimension(730, 380));
        configurarTabela();
        carregar();
    }

    private void trocarConsulta(Consulta consulta) {
        if (consulta != selecionada) {
            selecionada = consulta;
            configurarTabela();
            carregar();
        }
    }

    private void configurarTabela() {
        modelo.setRowCount(0);
        modelo.setColumnIdentifiers(selecionada.colunas().toArray());
        tabela.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        ComponentesCadastro.ajustarColunas(tabela);
        if (selecionada.colunas().contains("Status")) {
            ComponentesCadastro.colorirStatus(tabela);
        }
    }

    private void carregar() {
        if (usuario == null || usuario.isBlank() || senha == null) {
            mensagem.setText("Configure MYSQL_USER e MYSQL_PASSWORD no arquivo .env.");
            return;
        }
        Consulta consulta = selecionada;
        habilitarAcoes(false);
        mensagem.setText("Consultando " + consulta.titulo() + "...");

        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws SQLException {
                return dao.consultar(consulta, usuario, senha);
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> linhas = get();
                    modelo.setRowCount(0);
                    linhas.forEach(modelo::addRow);
                    mensagem.setText(linhas.isEmpty() ? "Nenhum resultado para esta consulta."
                        : linhas.size() + (linhas.size() == 1 ? " resultado." : " resultados."));
                } catch (ExecutionException erro) {
                    if (erro.getCause() instanceof SQLException sql) {
                        mensagem.setText(switch (sql.getErrorCode()) {
                            case 1146 -> "View ausente. Execute o script 03_criar_views.sql no MySQL.";
                            case 1142 -> "O usuário do aplicativo precisa de permissão de leitura para a view.";
                            case 1356, 1449 -> "O MySQL não conseguiu ler a view. Confira sua definição e permissões.";
                            default -> "Erro ao consultar o MySQL (código " + sql.getErrorCode() + ").";
                        });
                    } else {
                        mensagem.setText("Não foi possível concluir a consulta.");
                    }
                } catch (InterruptedException erro) {
                    Thread.currentThread().interrupt();
                    mensagem.setText("A consulta foi interrompida.");
                } finally {
                    habilitarAcoes(true);
                }
            }
        }.execute();
    }

    private void habilitarAcoes(boolean habilitadas) {
        opcoes.forEach(botao -> botao.setEnabled(habilitadas));
        atualizar.setEnabled(habilitadas);
    }
}
