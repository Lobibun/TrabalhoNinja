package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import dao.NinjaMissaoDAO;
import dao.NinjaMissaoDAO.DadosCadastro;
import dao.NinjaMissaoDAO.Opcao;
import model.NinjaMissao;
import service.NinjaMissaoService;

public final class TelaVincularMissao extends JFrame {
    private static final DateTimeFormatter FORMATO_DATA =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final NinjaMissaoDAO dao = new NinjaMissaoDAO();
    private final NinjaMissaoService service = new NinjaMissaoService(dao);
    private final String usuario;
    private final String senha;
    private Integer missaoSolicitada;
    private boolean ocupado;
    private boolean dadosCarregados;

    private final JComboBox<Opcao> ninja = new JComboBox<>();
    private final JComboBox<Opcao> missao = new JComboBox<>();
    private final JComboBox<Opcao> funcao = new JComboBox<>();
    private final JTextField data = new JTextField(LocalDate.now().format(FORMATO_DATA));
    private final JButton vincular = new JButton("Vincular ninja");
    private final JButton atualizar = new JButton("Atualizar lista");
    private final JButton editar = new JButton("Editar função e data");
    private final JButton desvincular = new JButton("Desvincular");
    private final JLabel mensagem = new JLabel(" ");
    private final DefaultTableModel tabela = new DefaultTableModel(
        new String[] {"ID", "Ninja", "Missão", "Rank da missão", "Status", "Função", "Data"}, 0
    ) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }
    };
    private final JTable listaVinculos = new JTable(tabela);
    private List<NinjaMissao> vinculos = List.of();

    @FunctionalInterface
    private interface Gravacao {
        String executar() throws SQLException;
    }

    public TelaVincularMissao(String usuario, String senha, Integer idMissao) {
        super("Vincular Ninja à Missão");
        this.usuario = usuario;
        this.senha = senha;
        missaoSolicitada = idMissao;

        JPanel campos = new JPanel(new GridLayout(0, 2, 16, 12));
        ComponentesCadastro.adicionarCampo(campos, "Ninja", ninja);
        ComponentesCadastro.adicionarCampo(campos, "Missão", missao);
        ComponentesCadastro.adicionarCampo(campos, "Função", funcao);
        ComponentesCadastro.adicionarCampo(campos, "Data da participação (DD/MM/AAAA)", data);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        acoes.add(vincular);
        acoes.add(editar);
        acoes.add(atualizar);

        JPanel formulario = new JPanel(new BorderLayout(0, 12));
        formulario.add(campos, BorderLayout.CENTER);
        formulario.add(acoes, BorderLayout.SOUTH);

        JPanel painel = new JPanel(new BorderLayout(0, 16));
        painel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        painel.add(formulario, BorderLayout.NORTH);
        ComponentesCadastro.colorirStatus(listaVinculos);
        painel.add(ComponentesCadastro.painelTabela("Vínculos cadastrados", listaVinculos),
            BorderLayout.CENTER);
        JPanel rodape = new JPanel(new BorderLayout(12, 0));
        rodape.add(mensagem, BorderLayout.CENTER);
        rodape.add(desvincular, BorderLayout.EAST);
        painel.add(rodape, BorderLayout.SOUTH);
        setContentPane(painel);

        vincular.addActionListener(evento -> vincularNinja());
        editar.addActionListener(evento -> editarVinculo());
        desvincular.addActionListener(evento -> desvincularNinja());
        listaVinculos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaVinculos.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                if (dadosCarregados && !ocupado) {
                    NinjaMissao registro = vinculoSelecionado();
                    if (registro != null && selecionarOpcao(ninja, registro.idNinja())) {
                        mensagem.setText("Ninja selecionado: " + registro.ninja());
                    }
                }
                atualizarControles();
            }
        });
        atualizar.addActionListener(evento -> carregarDados(null));
        setSize(860, 500);
        setMinimumSize(new Dimension(650, 410));
        carregarDados(null);
    }

    public void selecionarMissao(int idMissao) {
        missaoSolicitada = idMissao;
        if (!ocupado && !aplicarMissaoSolicitada()) {
            carregarDados(null);
        }
    }

    private boolean aplicarMissaoSolicitada() {
        if (missaoSolicitada == null) {
            return true;
        }
        if (selecionarOpcao(missao, missaoSolicitada)) {
            missaoSolicitada = null;
            return true;
        }
        return false;
    }

    static boolean selecionarOpcao(JComboBox<Opcao> campo, int id) {
        for (int indice = 0; indice < campo.getItemCount(); indice++) {
            if (campo.getItemAt(indice).id() == id) {
                campo.setSelectedIndex(indice);
                return true;
            }
        }
        return false;
    }

    private void carregarDados(String retornoGravacao) {
        dadosCarregados = false;
        if (usuario == null || usuario.isBlank() || senha == null) {
            mensagem.setText("Configure MYSQL_USER e MYSQL_PASSWORD no arquivo .env.");
            atualizarControles();
            return;
        }

        ocupado = true;
        atualizarControles();
        mensagem.setText(retornoGravacao == null
            ? "Carregando os vínculos..." : retornoGravacao + " Atualizando a lista...");
        new SwingWorker<DadosCadastro, Void>() {
            @Override
            protected DadosCadastro doInBackground() throws SQLException {
                return dao.carregarDados(usuario, senha);
            }

            @Override
            protected void done() {
                try {
                    DadosCadastro dados = get();
                    preencher(ninja, dados.ninjas());
                    preencher(missao, dados.missoes());
                    preencher(funcao, dados.funcoes());
                    vinculos = dados.vinculos();
                    tabela.setRowCount(0);
                    for (NinjaMissao registro : vinculos) {
                        tabela.addRow(new Object[] {registro.id(), registro.ninja(), registro.missao(),
                            registro.rankMissao(), registro.statusMissao(), registro.funcao(),
                            registro.dataParticipacao().format(FORMATO_DATA)});
                    }
                    if (aplicarMissaoSolicitada()) {
                        mensagem.setText(retornoGravacao == null
                            ? "Selecione o ninja, a missão e a função." : retornoGravacao);
                    } else {
                        mensagem.setText("A missão escolhida já não está disponível.");
                    }
                    missaoSolicitada = null;
                    dadosCarregados = true;
                } catch (ExecutionException erro) {
                    mensagem.setText(retornoGravacao == null ? "Não foi possível carregar os dados do MySQL."
                        : retornoGravacao + " Clique em Atualizar lista para recarregar os dados.");
                } catch (InterruptedException erro) {
                    Thread.currentThread().interrupt();
                    mensagem.setText("O carregamento foi interrompido. Clique em Atualizar lista.");
                } finally {
                    ocupado = false;
                    atualizarControles();
                }
            }
        }.execute();
    }

    private void preencher(JComboBox<Opcao> campo, List<Opcao> opcoes) {
        campo.removeAllItems();
        opcoes.forEach(campo::addItem);
        campo.setSelectedIndex(-1);
    }

    private void vincularNinja() {
        Opcao ninjaSelecionado = (Opcao) ninja.getSelectedItem();
        Opcao missaoSelecionada = (Opcao) missao.getSelectedItem();
        Opcao funcaoSelecionada = (Opcao) funcao.getSelectedItem();
        if (ninjaSelecionado == null || missaoSelecionada == null || funcaoSelecionada == null) {
            mensagem.setText("Selecione o ninja, a missão e a função.");
            return;
        }

        LocalDate participacao = lerData(data.getText());
        if (participacao == null) {
            return;
        }
        gravar(() -> {
            service.vincular(ninjaSelecionado.id(), missaoSelecionada.id(), funcaoSelecionada.id(),
                participacao, usuario, senha);
            return "Vínculo salvo.";
        }, missaoSelecionada.id());
    }

    private void editarVinculo() {
        NinjaMissao registro = vinculoSelecionado();
        if (registro == null) {
            return;
        }
        JComboBox<Opcao> editarFuncao = new JComboBox<>();
        for (int indice = 0; indice < funcao.getItemCount(); indice++) {
            Opcao opcao = funcao.getItemAt(indice);
            editarFuncao.addItem(opcao);
            if (opcao.descricao().equals(registro.funcao())) {
                editarFuncao.setSelectedItem(opcao);
            }
        }
        JTextField editarData = new JTextField(registro.dataParticipacao().format(FORMATO_DATA));
        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 8));
        campos.add(ComponentesCadastro.rotuloFormulario("Editando vínculo #" + registro.id()));
        campos.add(new JLabel(registro.ninja() + " — " + registro.missao()));
        ComponentesCadastro.adicionarCampo(campos, "Função", editarFuncao);
        ComponentesCadastro.adicionarCampo(campos, "Data da participação (DD/MM/AAAA)", editarData);
        if (JOptionPane.showConfirmDialog(this, campos, "Editar vínculo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        Opcao funcaoEscolhida = (Opcao) editarFuncao.getSelectedItem();
        LocalDate participacao = lerData(editarData.getText());
        if (participacao != null && funcaoEscolhida != null) {
            gravar(() -> dao.alterarDetalhes(registro.id(), funcaoEscolhida.id(), participacao, usuario, senha)
                ? "Vínculo atualizado." : "O vínculo já havia sido removido.", null);
        }
    }

    private NinjaMissao vinculoSelecionado() {
        int linha = listaVinculos.getSelectedRow();
        return linha < 0 ? null : vinculos.get(listaVinculos.convertRowIndexToModel(linha));
    }

    private void desvincularNinja() {
        NinjaMissao registro = vinculoSelecionado();
        if (registro == null) {
            return;
        }
        int confirmacao = JOptionPane.showConfirmDialog(this,
            "Remover o vínculo de " + registro.ninja() + " com a missão “" + registro.missao() + "”?",
            "Desvincular ninja", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacao == JOptionPane.YES_OPTION) {
            gravar(() -> service.desvincular(registro.id(), usuario, senha)
                ? "Vínculo removido." : "O vínculo já havia sido removido.", null);
        }
    }

    private LocalDate lerData(String texto) {
        try {
            LocalDate participacao = LocalDate.parse(texto.strip(), FORMATO_DATA);
            if (participacao.getYear() >= 1000 && participacao.getYear() <= 9999) {
                return participacao;
            }
            mensagem.setText("Informe uma data com ano entre 1000 e 9999.");
        } catch (DateTimeParseException erro) {
            mensagem.setText("Informe uma data válida no formato DD/MM/AAAA.");
        }
        return null;
    }

    private void gravar(Gravacao operacao, Integer idMissao) {
        ocupado = true;
        atualizarControles();
        mensagem.setText("Atualizando vínculo...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws SQLException {
                return operacao.executar();
            }

            @Override
            protected void done() {
                ocupado = false;
                try {
                    String retorno = get();
                    if (missaoSolicitada == null) {
                        missaoSolicitada = idMissao;
                    }
                    carregarDados(retorno);
                } catch (ExecutionException erro) {
                    if (erro.getCause() instanceof IllegalArgumentException validacao) {
                        mensagem.setText("Vínculo recusado.");
                        JOptionPane.showMessageDialog(TelaVincularMissao.this, validacao.getMessage(),
                            "Vínculo recusado", JOptionPane.WARNING_MESSAGE);
                    } else {
                        mensagem.setText("Não foi possível gravar o vínculo no MySQL.");
                    }
                } catch (InterruptedException erro) {
                    Thread.currentThread().interrupt();
                    mensagem.setText("A operação foi interrompida. Atualize a lista para conferir o vínculo.");
                } finally {
                    atualizarControles();
                }
            }
        }.execute();
    }

    private void atualizarControles() {
        boolean disponivel = dadosCarregados && !ocupado;
        ninja.setEnabled(disponivel);
        missao.setEnabled(disponivel);
        funcao.setEnabled(disponivel);
        data.setEditable(disponivel);
        vincular.setEnabled(disponivel);
        listaVinculos.setEnabled(disponivel);
        editar.setEnabled(disponivel && listaVinculos.getSelectedRow() >= 0);
        NinjaMissao registro = disponivel ? vinculoSelecionado() : null;
        boolean concluida = registro != null && NinjaMissaoService.missaoConcluida(registro.statusMissao());
        desvincular.setEnabled(registro != null && !concluida);
        desvincular.setToolTipText(concluida ? "A equipe de uma missão concluída faz parte do histórico." : null);
        atualizar.setEnabled(!ocupado);
        setDefaultCloseOperation(ocupado ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE);
    }
}
