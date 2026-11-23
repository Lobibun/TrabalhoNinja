package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.text.Collator;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import dao.NinjaDAO;
import dao.NinjaDAO.Opcao;
import dao.NinjaDAO.OpcoesCadastro;
import dao.NinjaDAO.TipoOpcao;
import model.Ninja;

public final class TelaCadastroNinja extends JFrame {
    private final JTextField campoNome = new JTextField(25);
    private final JComboBox<Opcao> campoVila = new JComboBox<>();
    private final JComboBox<Opcao> campoCla = new JComboBox<>();
    private final JComboBox<Opcao> campoRank = new JComboBox<>();
    private final JButton campoNatureza = new JButton(Ninja.SEM_NATUREZA);
    private final JComboBox<Opcao> campoStatus = new JComboBox<>();
    private final JButton adicionarVila = new JButton("+");
    private final JButton adicionarCla = new JButton("+");
    private final JButton adicionarNatureza = new JButton("+");
    private final JButton botaoSalvar = new JButton("Cadastrar ninja");
    private final JButton novo = new JButton("Novo ninja");
    private final JButton editarOpcoes = new JButton("Editar opções");
    private final JLabel modoFormulario = ComponentesCadastro.rotuloFormulario("Novo ninja");
    private final JLabel mensagem = new JLabel("Carregando ninjas...");

    private final DefaultTableModel modeloTabela = new DefaultTableModel(
        new String[] {"ID", "Nome", "Vila", "Clã", "Rank", "Natureza do chakra", "Status"}, 0
    ) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }
    };
    private final JTable tabela = new JTable(modeloTabela);
    private final List<Ninja> ninjas = new ArrayList<>();
    private final List<Opcao> opcoesNatureza = new ArrayList<>();
    private List<Opcao> naturezas = List.of();
    private boolean disponivel;
    private Integer idEditando;

    private final NinjaDAO dao;
    private final String usuario;
    private final String senha;
    private Opcao statusAtivo;

    private record DadosTela(OpcoesCadastro opcoes, List<Ninja> ninjas) {}

    public TelaCadastroNinja(NinjaDAO dao, String usuario, String senha) {
        super("Cadastro de Ninjas");
        this.dao = dao;
        this.usuario = usuario;
        this.senha = senha;

        JPanel campos = new JPanel(new GridLayout(0, 2, 16, 12));
        ComponentesCadastro.adicionarCampo(campos, "Nome", campoNome);
        adicionarCampoComBotao(campos, "Vila", campoVila, adicionarVila, TipoOpcao.VILA);
        adicionarCampoComBotao(campos, "Clã", campoCla, adicionarCla, TipoOpcao.CLA);
        ComponentesCadastro.adicionarCampo(campos, "Rank", campoRank);
        adicionarNatureza.setToolTipText("Cadastrar natureza de chakra");
        adicionarNatureza.getAccessibleContext().setAccessibleName("Cadastrar natureza de chakra");
        adicionarNatureza.addActionListener(evento -> cadastrarOpcao("Natureza do chakra",
            TipoOpcao.NATUREZA_CHAKRA, criada -> {
                opcoesNatureza.add(criada);
                List<Opcao> selecionadas = new ArrayList<>(naturezas);
                selecionadas.removeIf(opcao -> opcao.nome().equalsIgnoreCase("Todas"));
                selecionadas.add(criada);
                naturezas = selecionadas;
                mostrarNaturezas();
            }));
        JPanel controlesNatureza = new JPanel(new BorderLayout(6, 0));
        controlesNatureza.add(campoNatureza, BorderLayout.CENTER);
        controlesNatureza.add(adicionarNatureza, BorderLayout.EAST);
        ComponentesCadastro.adicionarCampo(campos, "Naturezas do chakra", campoNatureza, controlesNatureza);
        ComponentesCadastro.adicionarCampo(campos, "Status", campoStatus);

        JPanel formulario = new JPanel(new BorderLayout(0, 12));
        formulario.add(modoFormulario, BorderLayout.NORTH);
        formulario.add(campos, BorderLayout.CENTER);
        JPanel acoes = new JPanel(new BorderLayout(12, 0));
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        botoes.add(botaoSalvar);
        botoes.add(novo);
        botoes.add(editarOpcoes);
        acoes.add(botoes, BorderLayout.WEST);
        formulario.add(acoes, BorderLayout.SOUTH);

        JPanel painel = new JPanel(new BorderLayout(0, 16));
        painel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        painel.add(formulario, BorderLayout.NORTH);
        ComponentesCadastro.colorirStatus(tabela);
        painel.add(ComponentesCadastro.painelTabela("Ninjas cadastrados", tabela),
            BorderLayout.CENTER);
        painel.add(mensagem, BorderLayout.SOUTH);
        setContentPane(painel);

        botaoSalvar.addActionListener(evento -> salvarNinja());
        campoNatureza.addActionListener(evento -> escolherNaturezas());
        novo.addActionListener(evento -> limparFormulario());
        editarOpcoes.addActionListener(evento -> editarOpcao());
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting() && disponivel && tabela.getSelectedRow() >= 0) {
                editarNinja(ninjas.get(tabela.convertRowIndexToModel(tabela.getSelectedRow())));
            }
        });
        getRootPane().setDefaultButton(botaoSalvar);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(860, 490);
        setMinimumSize(new Dimension(650, 420));
        carregarDados();
    }

    private void adicionarCampoComBotao(JPanel painel, String texto, JComboBox<Opcao> campo,
                                        JButton botao, TipoOpcao tipo) {
        String descricao = "Adicionar " + texto.toLowerCase(Locale.ROOT);
        botao.setToolTipText(descricao);
        botao.getAccessibleContext().setAccessibleName(descricao);
        botao.addActionListener(evento -> cadastrarOpcao(texto, tipo, criada -> inserirOpcao(campo, criada)));

        JPanel controles = new JPanel(new BorderLayout(6, 0));
        controles.add(campo, BorderLayout.CENTER);
        controles.add(botao, BorderLayout.EAST);
        ComponentesCadastro.adicionarCampo(painel, texto, campo, controles);
    }

    private void carregarDados() {
        setFormularioDisponivel(false);
        if (usuario == null || usuario.isBlank() || senha == null) {
            mensagem.setText("Configure MYSQL_USER e MYSQL_PASSWORD no arquivo .env.");
            return;
        }

        new SwingWorker<DadosTela, Void>() {
            @Override
            protected DadosTela doInBackground() throws SQLException {
                return new DadosTela(dao.carregarOpcoes(usuario, senha), dao.listar(usuario, senha));
            }

            @Override
            protected void done() {
                try {
                    DadosTela dados = get();
                    preencherOpcoes(campoVila, dados.opcoes().vilas());
                    preencherOpcoes(campoCla, dados.opcoes().clas());
                    preencherOpcoes(campoRank, dados.opcoes().ranks());
                    opcoesNatureza.clear();
                    opcoesNatureza.addAll(dados.opcoes().naturezas());
                    preencherOpcoes(campoStatus, dados.opcoes().status());
                    statusAtivo = dados.opcoes().status().stream()
                        .filter(opcao -> "Ativo".equalsIgnoreCase(opcao.nome()))
                        .findFirst().orElse(null);
                    ninjas.clear();
                    ninjas.addAll(dados.ninjas());
                    modeloTabela.setRowCount(0);
                    dados.ninjas().forEach(TelaCadastroNinja.this::adicionarLinha);
                    limparFormulario();
                    mensagem.setText(statusAtivo == null
                        ? "O status Ativo precisa estar cadastrado no MySQL."
                        : "Selecione um ninja na tabela para editar.");
                } catch (ExecutionException erro) {
                    mensagem.setText(erro.getCause() instanceof SQLException sql && sql.getErrorCode() == 1146
                        ? "Base antiga: execute o script 04 da pasta sql/migracoes."
                        : "Não foi possível carregar os dados do MySQL.");
                } catch (InterruptedException erro) {
                    Thread.currentThread().interrupt();
                    mensagem.setText("O carregamento foi interrompido.");
                }
            }
        }.execute();
    }

    private void preencherOpcoes(JComboBox<Opcao> campo, List<Opcao> opcoes) {
        campo.removeAllItems();
        opcoes.forEach(campo::addItem);
        campo.setSelectedIndex(-1);
    }

    private void cadastrarOpcao(String rotulo, TipoOpcao tipo, Consumer<Opcao> aoCriar) {
        String entrada = JOptionPane.showInputDialog(this, "Nome da nova opção para " + rotulo + ":",
            "Adicionar opção", JOptionPane.PLAIN_MESSAGE);
        if (entrada == null) {
            return;
        }
        String nome = entrada.strip();
        if (tipo == TipoOpcao.NATUREZA_CHAKRA
                && (nome.equalsIgnoreCase("Todas") || nome.equalsIgnoreCase(Ninja.SEM_NATUREZA))) {
            mensagem.setText("Cadastre uma natureza específica, como Fogo ou Raio.");
            return;
        }
        if (nome.isEmpty() || nome.codePointCount(0, nome.length()) > tipo.limiteNome()) {
            mensagem.setText("Digite um nome com até " + tipo.limiteNome() + " caracteres.");
            return;
        }

        setFormularioDisponivel(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        mensagem.setText("Salvando opção...");
        new SwingWorker<Opcao, Void>() {
            @Override
            protected Opcao doInBackground() throws SQLException {
                return dao.cadastrarOpcao(tipo, nome, usuario, senha);
            }

            @Override
            protected void done() {
                try {
                    Opcao criada = get();
                    mensagem.setText("Opção adicionada: " + criada.nome());
                    aoCriar.accept(criada);
                } catch (ExecutionException erro) {
                    if (erro.getCause() instanceof SQLException sql && sql.getErrorCode() == 1062) {
                        mensagem.setText("Já existe uma opção com esse nome.");
                    } else {
                        mensagem.setText("Não foi possível adicionar a opção no MySQL.");
                    }
                } catch (InterruptedException erro) {
                    Thread.currentThread().interrupt();
                    mensagem.setText("O cadastro da opção foi interrompido.");
                } finally {
                    setFormularioDisponivel(true);
                    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                }
            }
        }.execute();
    }

    private void editarOpcao() {
        String[] categorias = {"Vila", "Clã", "Natureza do chakra"};
        int categoria = JOptionPane.showOptionDialog(this, "Qual tipo de opção você quer renomear?",
            "Editar opções", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, categorias, categorias[0]);
        if (categoria < 0) {
            return;
        }
        TipoOpcao tipo;
        List<Opcao> opcoes = new ArrayList<>();
        if (categoria == 2) {
            tipo = TipoOpcao.NATUREZA_CHAKRA;
            opcoes.addAll(opcoesNatureza);
        } else {
            tipo = categoria == 0 ? TipoOpcao.VILA : TipoOpcao.CLA;
            JComboBox<Opcao> campo = categoria == 0 ? campoVila : campoCla;
            for (int indice = 0; indice < campo.getItemCount(); indice++) {
                opcoes.add(campo.getItemAt(indice));
            }
        }
        JList<Opcao> lista = new JList<>(opcoes.toArray(Opcao[]::new));
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setVisibleRowCount(8);
        if (JOptionPane.showConfirmDialog(this, new JScrollPane(lista), "Escolher opção",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION
                || lista.getSelectedValue() == null) {
            return;
        }
        Opcao escolhida = lista.getSelectedValue();
        String entrada = JOptionPane.showInputDialog(this, "Novo nome:", escolhida.nome());
        if (entrada == null) {
            return;
        }
        String nome = entrada.strip();
        if (nome.isEmpty() || nome.codePointCount(0, nome.length()) > tipo.limiteNome()) {
            mensagem.setText("Digite um nome com até " + tipo.limiteNome() + " caracteres.");
            return;
        }
        if (tipo == TipoOpcao.NATUREZA_CHAKRA
                && (nome.equalsIgnoreCase("Todas") || nome.equalsIgnoreCase(Ninja.SEM_NATUREZA)
                    || escolhida.nome().equalsIgnoreCase("Todas"))) {
            mensagem.setText("Troque a opção legada Todas pelas naturezas do ninja no formulário.");
            return;
        }

        setFormularioDisponivel(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        mensagem.setText("Renomeando opção...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws SQLException {
                dao.renomearOpcao(tipo, escolhida.id(), nome, usuario, senha);
                return null;
            }

            @Override
            protected void done() {
                boolean salvarOk = false;
                try {
                    get();
                    salvarOk = true;
                    mensagem.setText("Opção renomeada. Atualizando a lista...");
                } catch (ExecutionException erro) {
                    mensagem.setText(erro.getCause() instanceof SQLException sql && sql.getErrorCode() == 1062
                        ? "Já existe uma opção com esse nome." : "Não foi possível renomear a opção.");
                } catch (InterruptedException erro) {
                    Thread.currentThread().interrupt();
                    mensagem.setText("A atualização foi interrompida.");
                } finally {
                    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                    if (salvarOk) {
                        carregarDados();
                    } else {
                        setFormularioDisponivel(true);
                    }
                }
            }
        }.execute();
    }

    private void inserirOpcao(JComboBox<Opcao> campo, Opcao criada) {
        Collator ordem = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        int indice = 0;
        while (indice < campo.getItemCount()
                && ordem.compare(campo.getItemAt(indice).nome(), criada.nome()) < 0) {
            indice++;
        }
        campo.insertItemAt(criada, indice);
        campo.setSelectedItem(criada);
    }

    private void escolherNaturezas() {
        List<Opcao> opcoes = opcoesNatureza.stream()
            .filter(opcao -> !opcao.nome().equalsIgnoreCase("Todas"))
            .sorted(Comparator.comparing(Opcao::nome, Collator.getInstance(Locale.forLanguageTag("pt-BR"))))
            .toList();
        int atuais = (int) naturezas.stream().filter(opcoes::contains).count();
        JSpinner quantidade = new JSpinner(new SpinnerNumberModel(atuais, 0, opcoes.size(), 1));
        JPanel selecao = new JPanel();
        selecao.setLayout(new BoxLayout(selecao, BoxLayout.Y_AXIS));
        List<JCheckBox> caixas = new ArrayList<>();
        for (Opcao opcao : opcoes) {
            JCheckBox caixa = new JCheckBox(opcao.nome(), naturezas.contains(opcao));
            selecao.add(caixa);
            caixas.add(caixa);
        }
        quantidade.addChangeListener(evento -> {
            if ((Integer) quantidade.getValue() == 0) {
                caixas.forEach(caixa -> caixa.setSelected(false));
            }
        });
        JScrollPane lista = new JScrollPane(selecao);
        lista.setPreferredSize(new Dimension(280, 175));
        JPanel painel = new JPanel(new BorderLayout(0, 8));
        JPanel topo = new JPanel(new BorderLayout(8, 0));
        topo.add(new JLabel("Quantidade de naturezas (0 = sem natureza)"), BorderLayout.CENTER);
        topo.add(quantidade, BorderLayout.EAST);
        painel.add(topo, BorderLayout.NORTH);
        painel.add(lista, BorderLayout.CENTER);

        while (JOptionPane.showConfirmDialog(this, painel, "Naturezas do chakra",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                quantidade.commitEdit();
            } catch (ParseException erro) {
                JOptionPane.showMessageDialog(this, "Informe uma quantidade válida.");
                continue;
            }
            List<Opcao> marcadas = new ArrayList<>();
            for (int indice = 0; indice < caixas.size(); indice++) {
                if (caixas.get(indice).isSelected()) {
                    marcadas.add(opcoes.get(indice));
                }
            }
            if (marcadas.size() == (Integer) quantidade.getValue()) {
                naturezas = marcadas;
                mostrarNaturezas();
                return;
            }
            JOptionPane.showMessageDialog(this, "Marque exatamente a quantidade informada de naturezas.");
        }
    }

    private void mostrarNaturezas() {
        String nomes = String.join(", ", naturezas.stream().map(Opcao::nome).toList());
        campoNatureza.setText(nomes.isEmpty() ? Ninja.SEM_NATUREZA : nomes);
        campoNatureza.setToolTipText(nomes.isEmpty() ? null : nomes);
    }

    private void editarNinja(Ninja ninja) {
        idEditando = ninja.getId();
        campoNome.setText(ninja.getNome());
        selecionarOpcao(campoVila, ninja.getVila());
        selecionarOpcao(campoCla, ninja.getCla());
        selecionarOpcao(campoRank, ninja.getRank());
        selecionarOpcao(campoStatus, ninja.getStatus());
        naturezas = opcoesNatureza.stream().filter(opcao -> ninja.getNaturezasChakra().contains(opcao.nome())).toList();
        mostrarNaturezas();
        botaoSalvar.setText("Salvar alterações");
        modoFormulario.setText("Editando ninja #" + ninja.getId() + " — " + ninja.getNome());
        modoFormulario.setToolTipText(modoFormulario.getText());
        mensagem.setText(" ");
        setFormularioDisponivel(true);
    }

    private void selecionarOpcao(JComboBox<Opcao> campo, String nome) {
        campo.setSelectedIndex(-1);
        for (int indice = 0; indice < campo.getItemCount(); indice++) {
            if (campo.getItemAt(indice).nome().equals(nome)) {
                campo.setSelectedIndex(indice);
                return;
            }
        }
    }

    private void limparFormulario() {
        idEditando = null;
        tabela.clearSelection();
        modoFormulario.setText("Novo ninja");
        modoFormulario.setToolTipText(null);
        campoNome.setText("");
        campoVila.setSelectedIndex(-1);
        campoCla.setSelectedIndex(-1);
        campoRank.setSelectedIndex(-1);
        campoStatus.setSelectedItem(statusAtivo);
        naturezas = List.of();
        mostrarNaturezas();
        botaoSalvar.setText("Cadastrar ninja");
        mensagem.setText("Preencha os dados para cadastrar um ninja.");
        setFormularioDisponivel(true);
    }

    private void salvarNinja() {
        String nome = campoNome.getText().strip();
        if (nome.isEmpty() || nome.codePointCount(0, nome.length()) > 50) {
            mensagem.setText("Digite um nome com até 50 caracteres.");
            campoNome.requestFocusInWindow();
            return;
        }

        Opcao vila = (Opcao) campoVila.getSelectedItem();
        Opcao cla = (Opcao) campoCla.getSelectedItem();
        Opcao rank = (Opcao) campoRank.getSelectedItem();
        Opcao status = idEditando == null ? statusAtivo : (Opcao) campoStatus.getSelectedItem();
        if (vila == null || cla == null || rank == null || status == null) {
            mensagem.setText("Informe vila, clã, rank e status.");
            return;
        }

        Integer id = idEditando;
        List<Opcao> selecionadas = List.copyOf(naturezas);
        setFormularioDisponivel(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        mensagem.setText("Salvando ninja...");
        new SwingWorker<Ninja, Void>() {
            @Override
            protected Ninja doInBackground() throws SQLException {
                return dao.salvar(id, nome, vila, cla, rank, selecionadas, status, usuario, senha);
            }

            @Override
            protected void done() {
                try {
                    Ninja ninja = get();
                    if (id == null) {
                        ninjas.add(ninja);
                        adicionarLinha(ninja);
                    } else {
                        for (int linha = 0; linha < ninjas.size(); linha++) {
                            if (ninjas.get(linha).getId() == id) {
                                ninjas.set(linha, ninja);
                                modeloTabela.removeRow(linha);
                                modeloTabela.insertRow(linha, dadosLinha(ninja));
                                break;
                            }
                        }
                    }
                    limparFormulario();
                    mensagem.setText(id == null ? "Ninja cadastrado com ID " + ninja.getId() + "."
                        : "Alterações do ninja #" + ninja.getId() + " salvas.");
                } catch (ExecutionException erro) {
                    if (erro.getCause() instanceof SQLException sql && "02000".equals(sql.getSQLState())) {
                        mensagem.setText("Este ninja já foi removido. Reabra a tela para atualizar a lista.");
                    } else {
                        mensagem.setText(erro.getCause() instanceof SQLException sql && sql.getErrorCode() == 1048
                            && selecionadas.isEmpty() ? "Base antiga: execute o script 05 da pasta sql/migracoes."
                            : "Não foi possível salvar o ninja. Reabra a tela para conferir a lista.");
                    }
                } catch (InterruptedException erro) {
                    Thread.currentThread().interrupt();
                    mensagem.setText("O cadastro foi interrompido.");
                } finally {
                    setFormularioDisponivel(true);
                    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                    campoNome.requestFocusInWindow();
                }
            }
        }.execute();
    }

    private void setFormularioDisponivel(boolean disponivel) {
        this.disponivel = disponivel;
        tabela.setEnabled(disponivel);
        campoNome.setEditable(disponivel);
        campoVila.setEnabled(disponivel);
        campoCla.setEnabled(disponivel);
        campoRank.setEnabled(disponivel);
        campoNatureza.setEnabled(disponivel);
        adicionarVila.setEnabled(disponivel);
        adicionarCla.setEnabled(disponivel);
        adicionarNatureza.setEnabled(disponivel);
        campoStatus.setEnabled(disponivel && idEditando != null);
        novo.setEnabled(disponivel);
        editarOpcoes.setEnabled(disponivel);
        botaoSalvar.setEnabled(disponivel && (idEditando != null || statusAtivo != null));
    }

    private void adicionarLinha(Ninja ninja) {
        modeloTabela.addRow(dadosLinha(ninja));
    }

    private Object[] dadosLinha(Ninja ninja) {
        return new Object[] {
            ninja.getId(), ninja.getNome(), ninja.getVila(), ninja.getCla(),
            ninja.getRank(), ninja.getNaturezaChakra(), ninja.getStatus()
        };
    }
}
