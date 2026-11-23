package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import dao.NinjaDAO;
import model.Ninja;

final class PainelConsultaNinja extends JPanel {
    private final JTextField id = new JTextField(8);
    private final JTextField nome = new JTextField(16);
    private final Filtro vila = new Filtro("Vila", "Todas as vilas");
    private final Filtro cla = new Filtro("Clã", "Todos os clãs");
    private final Filtro rank = new Filtro("Rank", "Todos os ranks");
    private final Filtro natureza = new Filtro("Natureza do chakra", "Qualquer natureza");
    private final Filtro status = new Filtro("Status", "Todos os status");
    private final List<Filtro> filtros = List.of(vila, cla, rank, natureza, status);
    private final JButton buscar = new JButton("Buscar");
    private final JButton limpar = new JButton("Limpar filtros");
    private final JButton atualizar = new JButton("Atualizar lista");
    private final JLabel mensagem = new JLabel(" ");

    private final DefaultTableModel modelo = new DefaultTableModel(
        new String[] {"ID", "Nome", "Vila", "Clã", "Rank", "Naturezas do chakra", "Status"}, 0
    ) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int coluna) {
            return coluna == 0 ? Integer.class : String.class;
        }
    };
    private final TableRowSorter<DefaultTableModel> ordenacao = new TableRowSorter<>(modelo);
    private final NinjaDAO dao;
    private final String usuario;
    private final String senha;
    private List<Ninja> ninjas = List.of();
    private boolean carregando;
    private boolean dadosCarregados;
    private Filtro filtroAberto;

    private static final class Filtro {
        private final String titulo;
        private final String todos;
        private final JButton botao;
        private final JPanel campo = new JPanel(new BorderLayout(0, 2));
        private List<String> opcoes = List.of();
        private String valor;

        private Filtro(String titulo, String todos) {
            this.titulo = titulo;
            this.todos = todos;
            botao = new JButton(todos);
            campo.add(botao, BorderLayout.NORTH);
        }

        private void selecionar(String valor) {
            this.valor = valor;
            String texto = valor == null ? todos : valor;
            botao.setText(texto);
            botao.setToolTipText(texto);
        }

        private boolean aceita(String valorRegistro) {
            return valor == null || valor.equals(valorRegistro);
        }
    }

    PainelConsultaNinja(NinjaDAO dao, String usuario, String senha) {
        super(new BorderLayout(0, 12));
        this.dao = dao;
        this.usuario = usuario;
        this.senha = senha;

        JPanel campos = new JPanel(new GridBagLayout());
        List<JPanel> grupos = new ArrayList<>();
        grupos.add(ComponentesCadastro.campoIdentificado("ID do ninja", id, id));
        grupos.add(ComponentesCadastro.campoIdentificado("Nome", nome, nome));
        for (Filtro filtro : filtros) {
            grupos.add(ComponentesCadastro.campoIdentificado(filtro.titulo, filtro.botao, filtro.campo));
            filtro.botao.addActionListener(evento -> mostrarOpcoes(filtro));
        }
        for (int indice = 0; indice < grupos.size(); indice++) {
            GridBagConstraints posicao = new GridBagConstraints();
            posicao.gridx = indice % 3;
            posicao.gridy = indice / 3;
            posicao.weightx = 1;
            posicao.fill = GridBagConstraints.HORIZONTAL;
            posicao.anchor = GridBagConstraints.FIRST_LINE_START;
            posicao.insets = new Insets(0, 0, 8, posicao.gridx == 2 ? 0 : 12);
            campos.add(grupos.get(indice), posicao);
        }

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        acoes.add(buscar);
        acoes.add(limpar);
        acoes.add(atualizar);

        JPanel formulario = new JPanel(new BorderLayout(0, 10));
        formulario.add(campos, BorderLayout.NORTH);
        formulario.add(acoes, BorderLayout.SOUTH);

        JTable tabela = new JTable(modelo);
        tabela.setRowSorter(ordenacao);
        tabela.getColumnModel().getColumn(0).setMaxWidth(70);
        ComponentesCadastro.colorirStatus(tabela);

        add(formulario, BorderLayout.NORTH);
        add(ComponentesCadastro.painelTabela("Consulta de ninjas", tabela), BorderLayout.CENTER);
        add(mensagem, BorderLayout.SOUTH);

        buscar.addActionListener(evento -> filtrar());
        id.addActionListener(evento -> filtrar());
        nome.addActionListener(evento -> filtrar());
        limpar.addActionListener(evento -> {
            id.setText("");
            nome.setText("");
            filtros.forEach(filtro -> filtro.selecionar(null));
            fecharOpcoes();
            filtrar();
        });
        atualizar.addActionListener(evento -> carregarDados());
        habilitarFiltros(false);
    }

    void carregarDados() {
        if (carregando) {
            return;
        }
        if (usuario == null || usuario.isBlank() || senha == null) {
            mensagem.setText("Configure MYSQL_USER e MYSQL_PASSWORD no arquivo .env.");
            return;
        }

        carregando = true;
        fecharOpcoes();
        habilitarFiltros(false);
        atualizar.setEnabled(false);
        mensagem.setText("Carregando ninjas...");

        new SwingWorker<List<Ninja>, Void>() {
            @Override
            protected List<Ninja> doInBackground() throws SQLException {
                return dao.listar(usuario, senha);
            }

            @Override
            protected void done() {
                try {
                    ninjas = get();
                    atualizarOpcoes(vila, ninjas.stream().map(Ninja::getVila).toList());
                    atualizarOpcoes(cla, ninjas.stream().map(Ninja::getCla).toList());
                    atualizarOpcoes(rank, ninjas.stream().map(Ninja::getRank).toList());
                    List<String> naturezas = new ArrayList<>(ninjas.stream()
                        .map(Ninja::getNaturezasChakra).flatMap(List::stream).toList());
                    if (ninjas.stream().anyMatch(ninja -> ninja.getNaturezasChakra().isEmpty())) {
                        naturezas.add(Ninja.SEM_NATUREZA);
                    }
                    atualizarOpcoes(natureza, naturezas);
                    atualizarOpcoes(status, ninjas.stream().map(Ninja::getStatus).toList());
                    ordenacao.setRowFilter(null);
                    modelo.setRowCount(0);
                    for (Ninja ninja : ninjas) {
                        modelo.addRow(new Object[] {ninja.getId(), ninja.getNome(), ninja.getVila(),
                            ninja.getCla(), ninja.getRank(), ninja.getNaturezaChakra(), ninja.getStatus()});
                    }
                    dadosCarregados = true;
                    filtrar();
                } catch (ExecutionException erro) {
                    mensagem.setText(erro.getCause() instanceof SQLException sql && sql.getErrorCode() == 1146
                        ? "Base antiga: execute o script 04 da pasta sql/migracoes."
                        : "Não foi possível atualizar a consulta. Verifique a conexão com o MySQL.");
                } catch (InterruptedException erro) {
                    Thread.currentThread().interrupt();
                    mensagem.setText("A atualização da consulta foi interrompida.");
                } finally {
                    carregando = false;
                    habilitarFiltros(dadosCarregados);
                    atualizar.setEnabled(true);
                }
            }
        }.execute();
    }

    private void habilitarFiltros(boolean habilitado) {
        id.setEnabled(habilitado);
        nome.setEnabled(habilitado);
        filtros.forEach(filtro -> filtro.botao.setEnabled(habilitado));
        buscar.setEnabled(habilitado);
        limpar.setEnabled(habilitado);
    }

    private void atualizarOpcoes(Filtro filtro, List<String> valores) {
        var ordem = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        filtro.opcoes = valores.stream().distinct().sorted(ordem).toList();
        if (filtro.valor != null && !filtro.opcoes.contains(filtro.valor)) {
            filtro.selecionar(null);
        }
    }

    private void mostrarOpcoes(Filtro filtro) {
        if (filtroAberto == filtro) {
            fecharOpcoes();
            return;
        }
        fecharOpcoes();
        filtroAberto = filtro;
        String[] opcoes = new String[filtro.opcoes.size() + 1];
        opcoes[0] = filtro.todos;
        for (int indice = 0; indice < filtro.opcoes.size(); indice++) {
            opcoes[indice + 1] = filtro.opcoes.get(indice);
        }
        JList<String> lista = new JList<>(opcoes);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.getAccessibleContext().setAccessibleName(filtro.titulo);
        lista.addListSelectionListener(evento -> {
            int indice = lista.getSelectedIndex();
            if (!evento.getValueIsAdjusting() && indice >= 0) {
                filtro.selecionar(indice == 0 ? null : filtro.opcoes.get(indice - 1));
                fecharOpcoes();
                filtro.botao.requestFocusInWindow();
                filtrar();
            }
        });

        JScrollPane painel = new JScrollPane(lista);
        painel.setPreferredSize(new Dimension(0, Math.min(opcoes.length, 5) * lista.getFontMetrics(lista.getFont())
            .getHeight() + 12));
        filtro.campo.add(painel, BorderLayout.CENTER);
        filtro.campo.revalidate();
        filtro.campo.repaint();
        lista.requestFocusInWindow();
    }

    private void fecharOpcoes() {
        if (filtroAberto != null) {
            JPanel campo = filtroAberto.campo;
            campo.remove(((BorderLayout) campo.getLayout()).getLayoutComponent(BorderLayout.CENTER));
            filtroAberto = null;
            campo.revalidate();
            campo.repaint();
        }
    }

    private void filtrar() {
        Integer idBuscado;
        try {
            String texto = id.getText().strip();
            idBuscado = texto.isEmpty() ? null : Integer.valueOf(texto);
            if (idBuscado != null && idBuscado <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException erro) {
            mensagem.setText("Digite um ID inteiro maior que zero ou deixe o campo vazio.");
            return;
        }
        String nomeBuscado = normalizarNome(nome.getText().strip());
        ordenacao.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> linha) {
                Ninja ninja = ninjas.get(linha.getIdentifier());
                return (idBuscado == null || idBuscado == ninja.getId())
                    && (nomeBuscado.isEmpty() || normalizarNome(ninja.getNome()).contains(nomeBuscado))
                    && vila.aceita(ninja.getVila()) && cla.aceita(ninja.getCla())
                    && rank.aceita(ninja.getRank()) && status.aceita(ninja.getStatus())
                    && (natureza.valor == null || ninja.getNaturezasChakra().contains(natureza.valor)
                        || (Ninja.SEM_NATUREZA.equals(natureza.valor) && ninja.getNaturezasChakra().isEmpty()));
            }
        });
        int total = ordenacao.getViewRowCount();
        mensagem.setText(total == 0 ? "Nenhum ninja encontrado." : total + " de " + modelo.getRowCount() + " ninjas.");
    }

    private String normalizarNome(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT);
    }
}
