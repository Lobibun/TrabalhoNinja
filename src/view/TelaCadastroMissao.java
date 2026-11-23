package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import dao.MissaoDAO;
import dao.MissaoDAO.Opcao;
import dao.MissaoDAO.OpcoesCadastro;
import dao.NinjaMissaoDAO;
import model.Missao;
import model.NinjaMissao;
import service.NinjaMissaoService;

public final class TelaCadastroMissao extends JFrame {
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private final MissaoDAO dao = new MissaoDAO();
    private final NinjaMissaoDAO vinculosDAO = new NinjaMissaoDAO();
    private final String usuario;
    private final String senha;
    private List<Missao> missoes = List.of();
    private boolean dadosCarregados;
    private boolean ocupado;
    private SwingWorker<List<NinjaMissao>, Void> consultaEquipe;

    private final JTextField titulo = new JTextField();
    private final JTextArea descricao = new JTextArea(3, 30);
    private final JComboBox<Opcao> rank = new JComboBox<>();
    private final JComboBox<Opcao> vila = new JComboBox<>();
    private final JComboBox<Opcao> status = new JComboBox<>();
    private final JButton novo = new JButton("Nova missão");
    private final JButton cadastrar = new JButton("Cadastrar");
    private final JButton editar = new JButton("Salvar alterações");
    private final JButton excluir = new JButton("Excluir");
    private final JButton atualizar = new JButton("Atualizar lista");
    private final JButton vincular = new JButton("Vincular ninja");
    private final JLabel modoFormulario = ComponentesCadastro.rotuloFormulario("Nova missão");
    private final JLabel mensagem = new JLabel(" ");
    private final JLabel mensagemEquipe = new JLabel("Selecione uma missão para ver a equipe.");

    private final DefaultTableModel modeloTabela = new DefaultTableModel(
        new String[] {"ID", "Título", "Descrição", "Rank", "Vila de origem", "Status"}, 0
    ) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }
    };
    private final JTable tabela = new JTable(modeloTabela);
    private final DefaultTableModel modeloEquipe = new DefaultTableModel(
        new String[] {"Ninja", "Função", "Data da participação"}, 0
    ) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }
    };

    private record DadosTela(OpcoesCadastro opcoes, List<Missao> missoes) {}

    @FunctionalInterface
    private interface OperacaoBanco<T> {
        T executar() throws SQLException;
    }

    public TelaCadastroMissao(String usuario, String senha, IntConsumer abrirVinculos) {
        super("Cadastro de Missões");
        this.usuario = usuario;
        this.senha = senha;

        JPanel campos = new JPanel(new GridLayout(0, 2, 16, 12));
        ComponentesCadastro.adicionarCampo(campos, "Título", titulo);
        ComponentesCadastro.adicionarCampo(campos, "Rank", rank);
        ComponentesCadastro.adicionarCampo(campos, "Vila de origem", vila);
        ComponentesCadastro.adicionarCampo(campos, "Status", status);

        descricao.setLineWrap(true);
        descricao.setWrapStyleWord(true);
        JPanel campoDescricao = new JPanel(new BorderLayout(0, 4));
        JLabel rotuloDescricao = new JLabel("Descrição (opcional)");
        rotuloDescricao.setLabelFor(descricao);
        campoDescricao.add(rotuloDescricao, BorderLayout.NORTH);
        campoDescricao.add(new JScrollPane(descricao), BorderLayout.CENTER);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        acoes.add(novo);
        acoes.add(cadastrar);
        acoes.add(editar);
        acoes.add(atualizar);

        JPanel formulario = new JPanel(new BorderLayout(0, 10));
        JPanel camposFormulario = new JPanel(new BorderLayout(0, 10));
        camposFormulario.add(campos, BorderLayout.NORTH);
        camposFormulario.add(campoDescricao, BorderLayout.CENTER);
        formulario.add(modoFormulario, BorderLayout.NORTH);
        formulario.add(camposFormulario, BorderLayout.CENTER);
        formulario.add(acoes, BorderLayout.SOUTH);

        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting() && !ocupado) {
                mostrarMissaoSelecionada();
            }
        });

        JPanel painel = new JPanel(new BorderLayout(0, 16));
        painel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        painel.add(formulario, BorderLayout.NORTH);
        ComponentesCadastro.colorirStatus(tabela);
        JPanel listaMissoes = ComponentesCadastro.painelTabela("Missões cadastradas", tabela);
        JPanel equipe = ComponentesCadastro.painelTabela("Equipe da missão selecionada", new JTable(modeloEquipe));
        equipe.add(mensagemEquipe, BorderLayout.SOUTH);
        listaMissoes.setMinimumSize(new Dimension(0, 90));
        equipe.setMinimumSize(new Dimension(0, 90));
        JSplitPane listas = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listaMissoes, equipe);
        listas.setResizeWeight(0.6);
        listas.setBorder(null);
        painel.add(listas, BorderLayout.CENTER);
        JPanel rodape = new JPanel(new BorderLayout(12, 0));
        rodape.add(mensagem, BorderLayout.CENTER);
        JPanel acoesRegistro = new JPanel(new FlowLayout(FlowLayout.TRAILING, 12, 0));
        acoesRegistro.add(vincular);
        acoesRegistro.add(excluir);
        rodape.add(acoesRegistro, BorderLayout.EAST);
        painel.add(rodape, BorderLayout.SOUTH);
        setContentPane(painel);
        getRootPane().setDefaultButton(cadastrar);

        novo.addActionListener(evento -> {
            limparFormulario();
            mensagem.setText("Preencha os dados da nova missão.");
            titulo.requestFocusInWindow();
        });
        cadastrar.addActionListener(evento -> salvarMissao(null));
        editar.addActionListener(evento -> salvarMissao(missaoSelecionada()));
        excluir.addActionListener(evento -> excluirMissao());
        atualizar.addActionListener(evento -> carregarDados(null));
        vincular.addActionListener(evento -> {
            Missao selecionada = missaoSelecionada();
            if (selecionada != null) {
                abrirVinculos.accept(selecionada.getId());
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent evento) {
                Missao selecionada = dadosCarregados && !ocupado ? missaoSelecionada() : null;
                if (selecionada != null) {
                    carregarEquipe(selecionada);
                }
            }

            @Override
            public void windowClosed(WindowEvent evento) {
                cancelarConsultaEquipe();
            }
        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 680);
        setMinimumSize(new Dimension(650, 550));
        listas.setDividerLocation(185);
        carregarDados(null);
    }

    private void carregarDados(String mensagemAnterior) {
        if (usuario == null || usuario.isBlank() || senha == null) {
            mensagem.setText("Configure MYSQL_USER e MYSQL_PASSWORD no arquivo .env.");
            atualizarControles();
            return;
        }

        Missao selecionada = missaoSelecionada();
        limparEquipe();
        dadosCarregados = false;
        String falha = mensagemAnterior == null
            ? "Não foi possível carregar as missões. Verifique a conexão com o MySQL."
            : mensagemAnterior + " A lista não foi atualizada. Clique em Atualizar lista.";

        executarNoBanco("Carregando missões...", falha, () ->
            new DadosTela(dao.carregarOpcoes(usuario, senha), dao.listar(usuario, senha)),
            dados -> {
                preencherOpcoes(rank, dados.opcoes().ranks());
                preencherOpcoes(vila, dados.opcoes().vilas());
                preencherOpcoes(status, dados.opcoes().status());
                missoes = dados.missoes();
                modeloTabela.setRowCount(0);
                for (Missao missao : missoes) {
                    modeloTabela.addRow(new Object[] {
                        missao.getId(), missao.getTitulo(), missao.getDescricao(),
                        buscarOpcao(rank, missao.getRankMissaoId()),
                        buscarOpcao(vila, missao.getVilaOrigemId()),
                        buscarOpcao(status, missao.getStatusId())
                    });
                }
                limparFormulario();
                dadosCarregados = true;
                if (selecionada != null) {
                    for (int linha = 0; linha < missoes.size(); linha++) {
                        if (missoes.get(linha).getId() == selecionada.getId()) {
                            int linhaVisivel = tabela.convertRowIndexToView(linha);
                            tabela.setRowSelectionInterval(linhaVisivel, linhaVisivel);
                            break;
                        }
                    }
                }
                mensagem.setText(mensagemAnterior != null
                    ? mensagemAnterior : missoes.size() + " missão(ões) na lista.");
            }
        );
    }

    private void preencherOpcoes(JComboBox<Opcao> campo, List<Opcao> opcoes) {
        campo.removeAllItems();
        opcoes.forEach(campo::addItem);
        campo.setSelectedIndex(-1);
    }

    private Opcao buscarOpcao(JComboBox<Opcao> campo, int id) {
        for (int indice = 0; indice < campo.getItemCount(); indice++) {
            Opcao opcao = campo.getItemAt(indice);
            if (opcao.id() == id) {
                return opcao;
            }
        }
        return null;
    }

    private Missao missaoSelecionada() {
        int linha = tabela.getSelectedRow();
        return linha < 0 ? null : missoes.get(tabela.convertRowIndexToModel(linha));
    }

    private void mostrarMissaoSelecionada() {
        Missao missao = missaoSelecionada();
        if (missao != null) {
            titulo.setText(missao.getTitulo());
            descricao.setText(missao.getDescricao());
            rank.setSelectedItem(buscarOpcao(rank, missao.getRankMissaoId()));
            vila.setSelectedItem(buscarOpcao(vila, missao.getVilaOrigemId()));
            status.setSelectedItem(buscarOpcao(status, missao.getStatusId()));
            modoFormulario.setText("Editando missão #" + missao.getId() + " — " + missao.getTitulo());
            modoFormulario.setToolTipText(modoFormulario.getText());
            mensagem.setText(" ");
            carregarEquipe(missao);
        } else {
            limparEquipe();
        }
        atualizarControles();
    }

    private void limparFormulario() {
        tabela.clearSelection();
        limparEquipe();
        modoFormulario.setText("Nova missão");
        modoFormulario.setToolTipText(null);
        titulo.setText("");
        descricao.setText("");
        rank.setSelectedIndex(-1);
        vila.setSelectedIndex(-1);
        status.setSelectedIndex(-1);
        atualizarControles();
    }

    private void carregarEquipe(Missao missao) {
        cancelarConsultaEquipe();
        modeloEquipe.setRowCount(0);
        mensagemEquipe.setText("Consultando a equipe...");
        consultaEquipe = new SwingWorker<>() {
            @Override
            protected List<NinjaMissao> doInBackground() throws SQLException {
                return vinculosDAO.listarEquipe(missao.getId(), usuario, senha);
            }

            @Override
            protected void done() {
                if (consultaEquipe != this) {
                    return;
                }
                consultaEquipe = null;
                try {
                    List<NinjaMissao> integrantes = get();
                    for (NinjaMissao integrante : integrantes) {
                        modeloEquipe.addRow(new Object[] {integrante.ninja(), integrante.funcao(),
                            integrante.dataParticipacao().format(FORMATO_DATA)});
                    }
                    mensagemEquipe.setText(integrantes.isEmpty() ? "Esta missão está sem ninjas vinculados."
                        : integrantes.size() + (integrantes.size() == 1 ? " ninja vinculado." : " ninjas vinculados."));
                } catch (ExecutionException erro) {
                    mensagemEquipe.setText("Não foi possível consultar a equipe. Use Atualizar lista.");
                } catch (InterruptedException erro) {
                    Thread.currentThread().interrupt();
                    mensagemEquipe.setText("A consulta da equipe foi interrompida.");
                }
            }
        };
        consultaEquipe.execute();
    }

    private void limparEquipe() {
        cancelarConsultaEquipe();
        modeloEquipe.setRowCount(0);
        mensagemEquipe.setText("Selecione uma missão para ver a equipe.");
    }

    private void cancelarConsultaEquipe() {
        if (consultaEquipe != null) {
            var anterior = consultaEquipe;
            consultaEquipe = null;
            anterior.cancel(false);
        }
    }

    private void salvarMissao(Missao selecionada) {
        String tituloMissao = titulo.getText().strip();
        if (tituloMissao.isEmpty() || tituloMissao.codePointCount(0, tituloMissao.length()) > 100) {
            mensagem.setText("Digite um título com até 100 caracteres.");
            titulo.requestFocusInWindow();
            return;
        }
        String descricaoMissao = descricao.getText().strip();
        Opcao rankMissao = (Opcao) rank.getSelectedItem();
        Opcao vilaMissao = (Opcao) vila.getSelectedItem();
        Opcao statusMissao = (Opcao) status.getSelectedItem();
        if (rankMissao == null || vilaMissao == null || statusMissao == null) {
            mensagem.setText("Selecione o rank, a vila de origem e o status.");
            return;
        }

        if (selecionada == null) {
            executarNoBanco("Cadastrando missão...", "Não foi possível cadastrar a missão.", () -> {
                dao.cadastrar(tituloMissao, descricaoMissao, rankMissao.id(), vilaMissao.id(),
                    statusMissao.id(), usuario, senha);
                return null;
            }, resultado -> carregarDados("Missão cadastrada."));
        } else {
            Missao missao = new Missao(selecionada.getId(), tituloMissao, descricaoMissao,
                rankMissao.id(), vilaMissao.id(), statusMissao.id());
            executarNoBanco("Salvando alterações...", "Não foi possível alterar a missão.",
                () -> dao.alterar(missao, usuario, senha),
                alterada -> carregarDados(alterada ? "Alterações salvas." : "Esta missão já foi removida."));
        }
    }

    private void excluirMissao() {
        Missao missao = missaoSelecionada();
        if (missao == null) {
            return;
        }
        int confirmacao = JOptionPane.showConfirmDialog(this,
            "Excluir a missão de ID " + missao.getId() + "?",
            "Excluir missão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacao != JOptionPane.YES_OPTION) {
            return;
        }

        executarNoBanco("Excluindo missão...", "Não foi possível excluir a missão.",
            () -> dao.excluir(missao.getId(), usuario, senha),
            removida -> carregarDados(removida ? "Missão excluída." : "A missão já foi removida."));
    }

    private <T> void executarNoBanco(String andamento, String falha, OperacaoBanco<T> operacao,
                                   Consumer<T> sucesso) {
        ocupado = true;
        mensagem.setText(andamento);
        atualizarControles();

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws SQLException {
                return operacao.executar();
            }

            @Override
            protected void done() {
                ocupado = false;
                try {
                    sucesso.accept(get());
                } catch (ExecutionException erro) {
                    if (erro.getCause() instanceof SQLException sql && sql.getErrorCode() == 1451) {
                        mensagem.setText("A missão tem ninjas vinculados.");
                    } else {
                        mensagem.setText(falha);
                    }
                } catch (InterruptedException erro) {
                    Thread.currentThread().interrupt();
                    mensagem.setText(falha);
                } finally {
                    atualizarControles();
                }
            }
        }.execute();
    }

    private void atualizarControles() {
        boolean disponivel = dadosCarregados && !ocupado;
        boolean selecionada = tabela.getSelectedRow() >= 0;
        tabela.setEnabled(disponivel);
        titulo.setEditable(disponivel);
        descricao.setEditable(disponivel);
        rank.setEnabled(disponivel);
        vila.setEnabled(disponivel);
        status.setEnabled(disponivel);
        novo.setEnabled(disponivel);
        cadastrar.setEnabled(disponivel && !selecionada);
        editar.setEnabled(disponivel && selecionada);
        getRootPane().setDefaultButton(selecionada ? editar : cadastrar);
        excluir.setEnabled(disponivel && selecionada);
        Missao missao = disponivel ? missaoSelecionada() : null;
        Opcao statusMissao = missao == null ? null : buscarOpcao(status, missao.getStatusId());
        boolean concluida = statusMissao != null && NinjaMissaoService.missaoConcluida(statusMissao.nome());
        vincular.setEnabled(disponivel && selecionada && !concluida);
        vincular.setToolTipText(concluida ? "Missões concluídas não aceitam novos vínculos." : null);
        atualizar.setEnabled(!ocupado);
        setDefaultCloseOperation(ocupado ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE);
    }
}
