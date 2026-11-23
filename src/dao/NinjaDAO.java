package dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import connection.Conexao;
import model.Ninja;

public class NinjaDAO {
    private static final String SELECIONAR_NINJAS = "SELECT n.id, n.nome, v.nome AS vila, c.nome AS cla, "
        + "r.nome AS rank_ninja, ch.nome AS natureza_chakra, s.nome AS status "
        + "FROM tb_ninja n "
        + "JOIN tb_vila v ON v.id = n.vila "
        + "JOIN tb_cla c ON c.id = n.cla "
        + "JOIN tb_rank_ninja r ON r.id = n.rank_ninja "
        + "LEFT JOIN tb_ninja_natureza_chakra nn ON nn.id_ninja = n.id "
        + "LEFT JOIN tb_natureza_chakra ch ON ch.id = COALESCE(nn.id_natureza, n.natureza_chakra) "
        + "JOIN tb_status_ninja s ON s.id = n.status";

    public record Opcao(int id, String nome) {
        @Override
        public String toString() {
            return nome;
        }
    }

    public record OpcoesCadastro(List<Opcao> vilas, List<Opcao> clas, List<Opcao> ranks,
                                 List<Opcao> naturezas, List<Opcao> status) {}

    public enum TipoOpcao {
        VILA("tb_vila", 60),
        CLA("tb_cla", 50),
        NATUREZA_CHAKRA("tb_natureza_chakra", 30);

        private final String tabela;
        private final int limiteNome;

        TipoOpcao(String tabela, int limiteNome) {
            this.tabela = tabela;
            this.limiteNome = limiteNome;
        }

        public int limiteNome() {
            return limiteNome;
        }
    }

    public Ninja buscarPorId(int id, String usuario, String senha) throws SQLException {
        String sql = SELECIONAR_NINJAS + " WHERE n.id = ?";

        try (var conexao = Conexao.abrirConexao(usuario, senha);
             var consulta = conexao.prepareStatement(sql)) {
            consulta.setInt(1, id);

            return lerNinjas(consulta.executeQuery()).stream().findFirst().orElse(null);
        }
    }

    public List<Ninja> listar(String usuario, String senha) throws SQLException {
        try (var conexao = Conexao.abrirConexao(usuario, senha);
             var consulta = conexao.prepareStatement(SELECIONAR_NINJAS + " ORDER BY n.id, ch.nome")) {
            return lerNinjas(consulta.executeQuery());
        }
    }

    public OpcoesCadastro carregarOpcoes(String usuario, String senha) throws SQLException {
        try (var conexao = Conexao.abrirConexao(usuario, senha)) {
            return new OpcoesCadastro(
                listarOpcoes(conexao, "SELECT id, nome FROM tb_vila ORDER BY nome"),
                listarOpcoes(conexao, "SELECT id, nome FROM tb_cla ORDER BY nome"),
                listarOpcoes(conexao, "SELECT id, nome FROM tb_rank_ninja ORDER BY CASE nome "
                    + "WHEN 'Genin' THEN 1 WHEN 'Chunin' THEN 2 WHEN 'Jounin' THEN 3 "
                    + "WHEN 'Kage' THEN 4 ELSE 5 END, nome"),
                listarOpcoes(conexao, "SELECT id, nome FROM tb_natureza_chakra ORDER BY nome"),
                listarOpcoes(conexao, "SELECT id, nome FROM tb_status_ninja ORDER BY nome")
            );
        }
    }

    public Opcao cadastrarOpcao(TipoOpcao tipo, String nome, String usuario, String senha) throws SQLException {
        String sql = "INSERT INTO " + tipo.tabela + " (nome) VALUES (?)";
        try (var conexao = Conexao.abrirConexao(usuario, senha)) {
            conexao.setAutoCommit(false);
            try {
                int id;
                try (var consulta = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    consulta.setString(1, nome);
                    consulta.executeUpdate();
                    id = lerIdGerado(consulta);
                }
                conexao.commit();
                return new Opcao(id, nome);
            } catch (SQLException erro) {
                cancelarTransacao(conexao, erro);
                throw erro;
            }
        }
    }

    public void renomearOpcao(TipoOpcao tipo, int id, String nome, String usuario, String senha)
            throws SQLException {
        String sql = "UPDATE " + tipo.tabela + " SET nome = ? WHERE id = ?";
        try (var conexao = Conexao.abrirConexao(usuario, senha);
             var consulta = conexao.prepareStatement(sql)) {
            consulta.setString(1, nome);
            consulta.setInt(2, id);
            consulta.executeUpdate();
        }
    }

    public Ninja salvar(Integer idNinja, String nome, Opcao vila, Opcao cla, Opcao rank,
                        List<Opcao> naturezas, Opcao status, String usuario, String senha) throws SQLException {
        Opcao principal = naturezas.isEmpty() ? null : naturezas.getFirst();
        try (var conexao = Conexao.abrirConexao(usuario, senha)) {
            conexao.setAutoCommit(false);
            try {
                int id = gravarNinja(conexao, idNinja, nome, vila, cla, rank, principal, status);
                gravarNaturezas(conexao, id, naturezas);
                conexao.commit();
                return new Ninja(id, nome, vila.nome(), cla.nome(), rank.nome(),
                    naturezas.stream().map(Opcao::nome).sorted().toList(), status.nome());
            } catch (SQLException erro) {
                cancelarTransacao(conexao, erro);
                throw erro;
            }
        }
    }

    private int gravarNinja(Connection conexao, Integer id, String nome, Opcao vila, Opcao cla,
                            Opcao rank, Opcao natureza, Opcao status) throws SQLException {
        String sql = id == null
            ? "INSERT INTO tb_ninja (nome, vila, cla, rank_ninja, natureza_chakra, status) VALUES (?, ?, ?, ?, ?, ?)"
            : "UPDATE tb_ninja SET nome = ?, vila = ?, cla = ?, rank_ninja = ?, natureza_chakra = ?, status = ? WHERE id = ?";
        try (var consulta = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            consulta.setString(1, nome);
            consulta.setInt(2, vila.id());
            consulta.setInt(3, cla.id());
            consulta.setInt(4, rank.id());
            if (natureza == null) {
                consulta.setNull(5, Types.INTEGER);
            } else {
                consulta.setInt(5, natureza.id());
            }
            consulta.setInt(6, status.id());
            if (id == null) {
                consulta.executeUpdate();
                return lerIdGerado(consulta);
            }
            consulta.setInt(7, id);
            if (consulta.executeUpdate() == 0) {
                throw new SQLException("O ninja selecionado não existe mais.", "02000");
            }
            return id;
        }
    }

    private void gravarNaturezas(Connection conexao, int id, List<Opcao> naturezas) throws SQLException {
        try (var apagar = conexao.prepareStatement("DELETE FROM tb_ninja_natureza_chakra WHERE id_ninja = ?");
             var inserir = conexao.prepareStatement(
                 "INSERT INTO tb_ninja_natureza_chakra (id_ninja, id_natureza) VALUES (?, ?)")) {
            apagar.setInt(1, id);
            apagar.executeUpdate();
            for (Opcao natureza : naturezas) {
                inserir.setInt(1, id);
                inserir.setInt(2, natureza.id());
                inserir.addBatch();
            }
            inserir.executeBatch();
        }
    }

    private int lerIdGerado(Statement consulta) throws SQLException {
        try (var chaves = consulta.getGeneratedKeys()) {
            if (chaves.next()) {
                return chaves.getInt(1);
            }
            throw new SQLException("O MySQL não retornou o ID do registro cadastrado.");
        }
    }

    private void cancelarTransacao(Connection conexao, SQLException erro) {
        try {
            conexao.rollback();
        } catch (SQLException erroRollback) {
            erro.addSuppressed(erroRollback);
        }
    }

    private List<Opcao> listarOpcoes(Connection conexao, String sql) throws SQLException {
        List<Opcao> opcoes = new ArrayList<>();
        try (var consulta = conexao.prepareStatement(sql);
             var resultado = consulta.executeQuery()) {
            while (resultado.next()) {
                opcoes.add(new Opcao(resultado.getInt("id"), resultado.getString("nome")));
            }
        }
        return opcoes;
    }

    private record LinhaNinja(int id, String nome, String vila, String cla, String rank,
                              List<String> naturezas, String status) {}

    private List<Ninja> lerNinjas(ResultSet linhas) throws SQLException {
        var ninjas = new LinkedHashMap<Integer, LinhaNinja>();
        try (linhas) {
            while (linhas.next()) {
                int id = linhas.getInt("id");
                LinhaNinja ninja = ninjas.get(id);
                if (ninja == null) {
                    ninja = new LinhaNinja(id, linhas.getString("nome"), linhas.getString("vila"),
                        linhas.getString("cla"), linhas.getString("rank_ninja"), new ArrayList<>(),
                        linhas.getString("status"));
                    ninjas.put(id, ninja);
                }
                String natureza = linhas.getString("natureza_chakra");
                if (natureza != null) {
                    ninja.naturezas().add(natureza);
                }
            }
        }
        return ninjas.values().stream().map(ninja -> new Ninja(ninja.id(), ninja.nome(), ninja.vila(),
            ninja.cla(), ninja.rank(), ninja.naturezas(), ninja.status())).toList();
    }
}
