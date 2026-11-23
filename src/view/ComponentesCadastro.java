package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

final class ComponentesCadastro {
    private ComponentesCadastro() {}

    static void adicionarCampo(JPanel painel, String texto, JComponent campo) {
        adicionarCampo(painel, texto, campo, campo);
    }

    static void adicionarCampo(JPanel painel, String texto, JComponent campo, JComponent conteudo) {
        painel.add(campoIdentificado(texto, campo, conteudo));
    }

    static JPanel campoIdentificado(String texto, JComponent campo, JComponent conteudo) {
        JLabel rotulo = new JLabel(texto);
        rotulo.setLabelFor(campo);
        JPanel grupo = new JPanel(new BorderLayout(0, 4));
        grupo.add(rotulo, BorderLayout.NORTH);
        grupo.add(conteudo, BorderLayout.CENTER);
        return grupo;
    }

    static void colorirStatus(JTable tabela) {
        tabela.getColumn("Status").setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable origem, Object valor, boolean selecionado,
                                                           boolean foco, int linha, int coluna) {
                super.getTableCellRendererComponent(origem, valor, selecionado, foco, linha, coluna);
                if (selecionado) {
                    return this;
                }
                switch (String.valueOf(valor)) {
                    case "Ativo", "Concluída" -> aplicarCores(0xDDF2E3, 0x155333);
                    case "Inativo", "Cancelada" -> aplicarCores(0xF6E2E4, 0x792632);
                    case "Em andamento" -> aplicarCores(0xDDEBFD, 0x174678);
                    case "Aberta", "Pendente" -> aplicarCores(0xFFF0C9, 0x694900);
                    default -> {
                        setBackground(origem.getBackground());
                        setForeground(origem.getForeground());
                    }
                }
                return this;
            }

            private void aplicarCores(int fundo, int texto) {
                setBackground(new Color(fundo));
                setForeground(new Color(texto));
            }
        });
    }

    static JPanel painelTabela(String titulo, JTable tabela) {
        tabela.setFillsViewportHeight(true);
        tabela.setRowHeight(Math.max(26, tabela.getRowHeight()));
        tabela.setShowVerticalLines(false);
        tabela.setIntercellSpacing(new Dimension(0, 1));
        ajustarColunas(tabela);

        JLabel rotulo = new JLabel(titulo);
        rotulo.setLabelFor(tabela);
        rotulo.setFont(rotulo.getFont().deriveFont(Font.BOLD));
        JPanel painel = new JPanel(new BorderLayout(0, 8));
        painel.add(rotulo, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabela), BorderLayout.CENTER);
        return painel;
    }

    static void ajustarColunas(JTable tabela) {
        DefaultTableCellRenderer centralizado = new DefaultTableCellRenderer();
        centralizado.setHorizontalAlignment(SwingConstants.CENTER);
        var colunas = tabela.getColumnModel().getColumns();
        while (colunas.hasMoreElements()) {
            var coluna = colunas.nextElement();
            switch (coluna.getHeaderValue().toString()) {
                case "ID", "Quantidade" -> {
                    coluna.setPreferredWidth("ID".equals(coluna.getHeaderValue()) ? 55 : 115);
                    coluna.setMaxWidth("ID".equals(coluna.getHeaderValue()) ? 75 : 140);
                    coluna.setCellRenderer(centralizado);
                }
                case "Nome", "Ninja", "Título", "Missão", "Descrição" -> coluna.setPreferredWidth(200);
                case "Natureza do chakra", "Naturezas do chakra", "Data de geração" ->
                    coluna.setPreferredWidth(175);
                default -> {
                }
            }
        }
    }

    static JLabel rotuloFormulario(String texto) {
        JLabel rotulo = new JLabel(texto);
        rotulo.setFont(rotulo.getFont().deriveFont(Font.BOLD, 16f));
        return rotulo;
    }
}
