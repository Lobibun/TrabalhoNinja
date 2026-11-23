package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import connection.Conexao;
import model.NinjaMissao;

public class NinjaMissaoDAO {
    private static final String SELECIONAR_VINCULOS = """
        SELECT nm.id, nm.id_ninja, n.nome AS ninja, m.titulo AS missao, r.nome AS rank_missao,
               s.nome AS status, f.nome AS funcao, nm.data_participacao
        FROM tb_ninja_missao nm
        JOIN tb_ninja n ON n.id = nm.id_ninja
        JOIN tb_missao m ON m.id = nm.id_missao
        JOIN tb_rank_missao r ON r.id = m.rank_missao
        JOIN tb_status_missao s ON s.id = m.status
        JOIN tb_funcao_missao f ON f.id = nm.funcao
        """;

    public record Opcao(int id, String descricao) {
        @Override
        public String toString() {
            return descricao;
        }
    }

    public record DadosCadastro(List<Opcao> ninjas, List<Opcao> missoes, List<Opcao> funcoes,
                                List<NinjaMissao> vinculos) {}

    public record DadosVinculo(String rankNinja, String rankMissao, String statusMissao) {}

    public DadosCadastro carregarDados(String usuario, String senha) throws SQLException {
        try (var conexao = Conexao.abrirConexao(usuario, senha)) {
            return new DadosCadastro(
                listarOpcoes(conexao, """
                    SELECT n.id, CONCAT(n.nome, ' (#', n.id, ' · ', r.nome, ')') AS descricao
                    FROM tb_ninja n JOIN tb_rank_ninja r ON r.id = n.rank_ninja
                    ORDER BY n.nome, n.id
                    """),
                listarOpcoes(conexao, """
                    SELECT m.id, CONCAT(m.titulo, ' (#', m.id, ' · ', r.nome, ')') AS descricao
                    FROM tb_missao m JOIN tb_rank_missao r ON r.id = m.rank_missao
                    JOIN tb_status_missao s ON s.id = m.status
                    WHERE s.nome <> 'Concluída'
                    ORDER BY m.titulo, m.id
                    """),
                listarOpcoes(conexao, "SELECT id, nome AS descricao FROM tb_funcao_missao ORDER BY nome"),
                listarVinculos(conexao)
            );
        }
    }

    public DadosVinculo buscarDadosVinculo(Connection conexao, int idNinja, int idMissao)
            throws SQLException {
        String sql = """
            SELECT rn.nome AS rank_ninja, rm.nome AS rank_missao, sm.nome AS status_missao
            FROM tb_ninja n
            JOIN tb_rank_ninja rn ON rn.id = n.rank_ninja
            CROSS JOIN tb_missao m
            JOIN tb_rank_missao rm ON rm.id = m.rank_missao
            JOIN tb_status_missao sm ON sm.id = m.status
            WHERE n.id = ? AND m.id = ?
            FOR UPDATE
            """;
        try (var consulta = conexao.prepareStatement(sql)) {
            consulta.setInt(1, idNinja);
            consulta.setInt(2, idMissao);
            try (var resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    return new DadosVinculo(resultado.getString("rank_ninja"), resultado.getString("rank_missao"),
                        resultado.getString("status_missao"));
                }
                return null;
            }
        }
    }

    public void inserir(Connection conexao, int idNinja, int idMissao, int idFuncao, LocalDate data)
            throws SQLException {
        String sql = """
            INSERT INTO tb_ninja_missao (id_ninja, id_missao, funcao, data_participacao)
            VALUES (?, ?, ?, ?)
            """;
        try (var consulta = conexao.prepareStatement(sql)) {
            consulta.setInt(1, idNinja);
            consulta.setInt(2, idMissao);
            consulta.setInt(3, idFuncao);
            consulta.setDate(4, Date.valueOf(data));
            consulta.executeUpdate();
        }
    }

    public boolean alterarDetalhes(int id, int idFuncao, LocalDate data, String usuario, String senha)
            throws SQLException {
        String sql = "UPDATE tb_ninja_missao SET funcao = ?, data_participacao = ? WHERE id = ?";
        try (var conexao = Conexao.abrirConexao(usuario, senha);
             var consulta = conexao.prepareStatement(sql)) {
            consulta.setInt(1, idFuncao);
            consulta.setDate(2, Date.valueOf(data));
            consulta.setInt(3, id);
            return consulta.executeUpdate() > 0;
        }
    }

    public String buscarStatusParaDesvincular(Connection conexao, int id) throws SQLException {
        String sql = """
            SELECT s.nome
            FROM tb_ninja_missao nm
            JOIN tb_missao m ON m.id = nm.id_missao
            JOIN tb_status_missao s ON s.id = m.status
            WHERE nm.id = ?
            FOR UPDATE
            """;
        try (var consulta = conexao.prepareStatement(sql)) {
            consulta.setInt(1, id);
            try (var resultado = consulta.executeQuery()) {
                return resultado.next() ? resultado.getString("nome") : null;
            }
        }
    }

    public boolean desvincular(Connection conexao, int id) throws SQLException {
        String sql = "DELETE FROM tb_ninja_missao WHERE id = ?";
        try (var consulta = conexao.prepareStatement(sql)) {
            consulta.setInt(1, id);
            return consulta.executeUpdate() > 0;
        }
    }

    public List<NinjaMissao> listarEquipe(int idMissao, String usuario, String senha) throws SQLException {
        String sql = SELECIONAR_VINCULOS + "WHERE nm.id_missao = ? ORDER BY n.nome, nm.id";
        try (var conexao = Conexao.abrirConexao(usuario, senha);
             var consulta = conexao.prepareStatement(sql)) {
            consulta.setInt(1, idMissao);
            return lerVinculos(consulta.executeQuery());
        }
    }

    private List<Opcao> listarOpcoes(Connection conexao, String sql) throws SQLException {
        List<Opcao> opcoes = new ArrayList<>();
        try (var consulta = conexao.prepareStatement(sql);
             var resultado = consulta.executeQuery()) {
            while (resultado.next()) {
                opcoes.add(new Opcao(resultado.getInt("id"), resultado.getString("descricao")));
            }
        }
        return opcoes;
    }

    private List<NinjaMissao> listarVinculos(Connection conexao) throws SQLException {
        String sql = SELECIONAR_VINCULOS + "ORDER BY nm.data_participacao DESC, nm.id DESC";
        try (var consulta = conexao.prepareStatement(sql)) {
            return lerVinculos(consulta.executeQuery());
        }
    }

    private List<NinjaMissao> lerVinculos(ResultSet resultado) throws SQLException {
        List<NinjaMissao> vinculos = new ArrayList<>();
        try (resultado) {
            while (resultado.next()) {
                vinculos.add(new NinjaMissao(resultado.getInt("id"), resultado.getInt("id_ninja"),
                    resultado.getString("ninja"), resultado.getString("missao"), resultado.getString("rank_missao"),
                    resultado.getString("status"),
                    resultado.getString("funcao"), resultado.getDate("data_participacao").toLocalDate()));
            }
        }
        return vinculos;
    }
}
