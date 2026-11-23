package dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import connection.Conexao;
import model.Missao;

public class MissaoDAO {

    public record Opcao(int id, String nome) {
        @Override
        public String toString() {
            return nome;
        }
    }

    public record OpcoesCadastro(
        List<Opcao> ranks,
        List<Opcao> vilas,
        List<Opcao> status
    ) {}

    public List<Missao> listar(String usuario, String senha) throws SQLException {
        String sql = "SELECT id, titulo, descricao, rank_missao, vila_origem, status "
            + "FROM tb_missao ORDER BY id";

        List<Missao> missoes = new ArrayList<>();

        try (var conexao = Conexao.abrirConexao(usuario, senha);
             var consulta = conexao.prepareStatement(sql);
             var resultado = consulta.executeQuery()) {
            while (resultado.next()) {
                Missao missao = new Missao(
                    resultado.getInt("id"),
                    resultado.getString("titulo"),
                    resultado.getString("descricao"),
                    resultado.getInt("rank_missao"),
                    resultado.getInt("vila_origem"),
                    resultado.getInt("status")
                );

                missoes.add(missao);
            }
        }

        return missoes;
    }

    public void cadastrar(String titulo, String descricao, int rankMissaoId,
                          int vilaOrigemId, int statusId, String usuario,
                          String senha) throws SQLException {
        String sql = "INSERT INTO tb_missao "
            + "(titulo, descricao, rank_missao, vila_origem, status) "
            + "VALUES (?, ?, ?, ?, ?)";

        try (var conexao = Conexao.abrirConexao(usuario, senha);
             var consulta = conexao.prepareStatement(sql)) {
            consulta.setString(1, titulo);
            consulta.setString(2, descricao);
            consulta.setInt(3, rankMissaoId);
            consulta.setInt(4, vilaOrigemId);
            consulta.setInt(5, statusId);
            consulta.executeUpdate();
        }
    }

    public boolean alterar(Missao missao, String usuario, String senha) throws SQLException {
        String sql = "UPDATE tb_missao "
            + "SET titulo = ?, descricao = ?, rank_missao = ?, vila_origem = ?, status = ? "
            + "WHERE id = ?";

        try (var conexao = Conexao.abrirConexao(usuario, senha);
             var consulta = conexao.prepareStatement(sql)) {
            consulta.setString(1, missao.getTitulo());
            consulta.setString(2, missao.getDescricao());
            consulta.setInt(3, missao.getRankMissaoId());
            consulta.setInt(4, missao.getVilaOrigemId());
            consulta.setInt(5, missao.getStatusId());
            consulta.setInt(6, missao.getId());
            return consulta.executeUpdate() > 0;
        }
    }

    public boolean excluir(int id, String usuario, String senha) throws SQLException {
        String sql = "DELETE FROM tb_missao WHERE id = ?";

        try (var conexao = Conexao.abrirConexao(usuario, senha);
             var consulta = conexao.prepareStatement(sql)) {
            consulta.setInt(1, id);
            return consulta.executeUpdate() > 0;
        }
    }

    public OpcoesCadastro carregarOpcoes(String usuario, String senha) throws SQLException {
        try (var conexao = Conexao.abrirConexao(usuario, senha)) {
            return new OpcoesCadastro(
                listarOpcoes(conexao, "SELECT id, nome FROM tb_rank_missao ORDER BY id"),
                listarOpcoes(conexao, "SELECT id, nome FROM tb_vila ORDER BY nome"),
                listarOpcoes(conexao, "SELECT id, nome FROM tb_status_missao ORDER BY nome")
            );
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
}
