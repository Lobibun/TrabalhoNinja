package view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import dao.NinjaDAO;

public class JanelaPrincipal extends JFrame {
    private final NinjaDAO ninjaDAO = new NinjaDAO();
    private final CardLayout layoutConteudo = new CardLayout();
    private final JPanel conteudo = new JPanel(layoutConteudo);
    private final PainelConsultaNinja consultaNinja;
    private final String usuario;
    private final String senha;
    private TelaCadastroNinja telaCadastroNinja;
    private TelaCadastroMissao telaCadastroMissao;
    private TelaVincularMissao telaVincularMissao;
    private TelaTotalizadores telaTotalizadores;
    private TelaConsultaViews telaConsultaViews;

    public JanelaPrincipal(String usuario, String senha) {
        super("Trabalho Ninja");
        this.usuario = usuario;
        this.senha = senha;
        consultaNinja = new PainelConsultaNinja(ninjaDAO, usuario, senha);
        conteudo.add(criarInicio(this::abrirConsultaNinja, this::abrirCadastroNinja, this::abrirCadastroMissao,
            () -> abrirVinculos(null), this::abrirTotalizadores, this::abrirConsultaViews), "inicio");
        conteudo.add(consultaNinja, "consulta");

        JPanel navegacao = criarNavegacao(() -> layoutConteudo.show(conteudo, "inicio"), this::abrirConsultaNinja,
            this::abrirCadastroNinja, this::abrirCadastroMissao, () -> abrirVinculos(null),
            this::abrirTotalizadores, this::abrirConsultaViews);

        JPanel painel = new JPanel(new BorderLayout(0, 16));
        painel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        painel.add(navegacao, BorderLayout.NORTH);
        painel.add(conteudo, BorderLayout.CENTER);
        setContentPane(painel);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 490);
        setMinimumSize(new Dimension(670, 400));
        setLocationRelativeTo(null);
    }

    static JPanel criarInicio(Runnable consultarNinjas, Runnable cadastrarNinjas, Runnable gerenciarMissoes,
                               Runnable vincularNinja, Runnable abrirTotalizadores, Runnable consultarViews) {
        JLabel titulo = new JLabel("Sistema de Gerenciamento Ninja");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 26f));
        JLabel introducao = new JLabel("Consulte ninjas, gerencie missões e organize as equipes.");

        JPanel apresentacao = new JPanel(new GridLayout(0, 1, 0, 8));
        apresentacao.setBorder(BorderFactory.createEmptyBorder(16, 0, 8, 0));
        apresentacao.add(titulo);
        apresentacao.add(introducao);

        JPanel secoes = new JPanel(new GridLayout(1, 3, 12, 0));
        secoes.add(secaoInicio("Ninjas", "Consultar ninjas", consultarNinjas,
            "Cadastrar e listar", cadastrarNinjas));
        secoes.add(secaoInicio("Missões", "Gerenciar missões", gerenciarMissoes,
            "Vincular ninja à missão", vincularNinja));
        secoes.add(secaoInicio("Relatórios", "Totalizadores", abrirTotalizadores,
            "Consultar views", consultarViews));

        JPanel inicio = new JPanel(new BorderLayout(0, 14));
        inicio.add(apresentacao, BorderLayout.NORTH);
        inicio.add(secoes, BorderLayout.CENTER);

        JPanel painel = new JPanel(new BorderLayout());
        painel.add(inicio, BorderLayout.NORTH);
        return painel;
    }

    private static JPanel secaoInicio(String titulo, String primeiraAcao, Runnable executarPrimeira,
                                     String segundaAcao, Runnable executarSegunda) {
        JLabel rotulo = new JLabel(titulo);
        rotulo.setFont(rotulo.getFont().deriveFont(Font.BOLD, 16f));
        JButton primeiro = new JButton(primeiraAcao);
        primeiro.addActionListener(evento -> executarPrimeira.run());
        JButton segundo = new JButton(segundaAcao);
        segundo.addActionListener(evento -> executarSegunda.run());

        JPanel atalhos = new JPanel(new GridLayout(0, 1, 0, 6));
        atalhos.add(primeiro);
        atalhos.add(segundo);

        JPanel secao = new JPanel(new BorderLayout(0, 10));
        secao.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        secao.add(rotulo, BorderLayout.NORTH);
        secao.add(atalhos, BorderLayout.CENTER);
        return secao;
    }

    private void abrirConsultaNinja() {
        layoutConteudo.show(conteudo, "consulta");
        consultaNinja.carregarDados();
    }

    static JPanel criarNavegacao(Runnable mostrarInicio, Runnable abrirConsulta, Runnable abrirNinjas,
                                 Runnable abrirMissoes, Runnable abrirVinculos, Runnable abrirTotalizadores,
                                 Runnable abrirConsultaViews) {
        JButton inicio = new JButton("Início");
        JButton ninjas = new JButton("Ninjas");
        JButton missoes = new JButton("Missões");
        JButton relatorios = new JButton("Relatórios");
        JPanel secoes = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        secoes.add(inicio);
        secoes.add(ninjas);
        secoes.add(missoes);
        secoes.add(relatorios);

        JButton busca = new JButton("Consultar ninjas");
        JButton cadastro = new JButton("Cadastrar e listar");
        JPanel opcoesNinjas = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        opcoesNinjas.add(busca);
        opcoesNinjas.add(cadastro);

        JButton gerenciar = new JButton("Gerenciar missões");
        JButton vincular = new JButton("Vincular ninja à missão");
        JPanel opcoesMissoes = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        opcoesMissoes.add(gerenciar);
        opcoesMissoes.add(vincular);

        JButton totalizadores = new JButton("Totalizadores");
        JButton consultarViews = new JButton("Consultar views");
        JPanel opcoesRelatorios = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        opcoesRelatorios.add(totalizadores);
        opcoesRelatorios.add(consultarViews);

        CardLayout secoesLayout = new CardLayout();
        JPanel opcoes = new JPanel(secoesLayout);
        opcoes.add(opcoesNinjas, "Ninjas");
        opcoes.add(opcoesMissoes, "Missões");
        opcoes.add(opcoesRelatorios, "Relatórios");
        opcoes.setVisible(false);

        inicio.addActionListener(evento -> {
            opcoes.setVisible(false);
            mostrarInicio.run();
        });
        ninjas.addActionListener(evento -> {
            secoesLayout.show(opcoes, "Ninjas");
            opcoes.setVisible(true);
        });
        missoes.addActionListener(evento -> {
            secoesLayout.show(opcoes, "Missões");
            opcoes.setVisible(true);
        });
        relatorios.addActionListener(evento -> {
            secoesLayout.show(opcoes, "Relatórios");
            opcoes.setVisible(true);
        });
        totalizadores.addActionListener(evento -> {
            opcoes.setVisible(false);
            abrirTotalizadores.run();
        });
        consultarViews.addActionListener(evento -> {
            opcoes.setVisible(false);
            abrirConsultaViews.run();
        });
        busca.addActionListener(evento -> {
            opcoes.setVisible(false);
            abrirConsulta.run();
        });
        cadastro.addActionListener(evento -> {
            opcoes.setVisible(false);
            abrirNinjas.run();
        });
        gerenciar.addActionListener(evento -> {
            opcoes.setVisible(false);
            abrirMissoes.run();
        });
        vincular.addActionListener(evento -> {
            opcoes.setVisible(false);
            abrirVinculos.run();
        });

        JPanel navegacao = new JPanel(new BorderLayout());
        navegacao.add(secoes, BorderLayout.NORTH);
        navegacao.add(opcoes, BorderLayout.CENTER);
        return navegacao;
    }

    private void abrirCadastroNinja() {
        if (telaCadastroNinja == null || !telaCadastroNinja.isDisplayable()) {
            telaCadastroNinja = new TelaCadastroNinja(ninjaDAO, usuario, senha);
            telaCadastroNinja.setLocationRelativeTo(this);
        }
        telaCadastroNinja.setVisible(true);
        telaCadastroNinja.toFront();
    }

    private void abrirCadastroMissao() {
        if (telaCadastroMissao == null || !telaCadastroMissao.isDisplayable()) {
            telaCadastroMissao = new TelaCadastroMissao(usuario, senha, this::abrirVinculos);
            telaCadastroMissao.setLocationRelativeTo(this);
        }
        telaCadastroMissao.setVisible(true);
        telaCadastroMissao.toFront();
    }

    private void abrirVinculos(Integer idMissao) {
        if (telaVincularMissao == null || !telaVincularMissao.isDisplayable()) {
            telaVincularMissao = new TelaVincularMissao(usuario, senha, idMissao);
            telaVincularMissao.setLocationRelativeTo(idMissao == null ? this : telaCadastroMissao);
        } else if (idMissao != null) {
            telaVincularMissao.selecionarMissao(idMissao);
        }
        telaVincularMissao.setVisible(true);
        telaVincularMissao.toFront();
    }

    private void abrirTotalizadores() {
        if (telaTotalizadores == null || !telaTotalizadores.isDisplayable()) {
            telaTotalizadores = new TelaTotalizadores(usuario, senha);
            telaTotalizadores.setLocationRelativeTo(this);
        }
        telaTotalizadores.setVisible(true);
        telaTotalizadores.toFront();
    }

    private void abrirConsultaViews() {
        if (telaConsultaViews == null || !telaConsultaViews.isDisplayable()) {
            telaConsultaViews = new TelaConsultaViews(usuario, senha);
            telaConsultaViews.setLocationRelativeTo(this);
        }
        telaConsultaViews.setVisible(true);
        telaConsultaViews.toFront();
    }
}
