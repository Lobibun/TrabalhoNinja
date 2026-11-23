package service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import connection.Conexao;
import dao.NinjaMissaoDAO;

public class NinjaMissaoService {
    private static final Map<String, Set<String>> RANKS_PERMITIDOS = Map.of(
        "Genin", Set.of("D", "C"),
        "Chunin", Set.of("D", "C", "B"),
        "Jounin", Set.of("D", "C", "B", "A", "S"),
        "Kage", Set.of("A", "S")
    );

    private final NinjaMissaoDAO dao;

    public NinjaMissaoService(NinjaMissaoDAO dao) {
        this.dao = dao;
    }

    public void vincular(int idNinja, int idMissao, int idFuncao, LocalDate data,
                         String usuario, String senha) throws SQLException {
        try (var conexao = Conexao.abrirConexao(usuario, senha)) {
            conexao.setAutoCommit(false);
            try {
                NinjaMissaoDAO.DadosVinculo dados = dao.buscarDadosVinculo(conexao, idNinja, idMissao);
                if (dados == null) {
                    throw new IllegalArgumentException("O ninja ou a missão deixou de existir. Atualize a lista.");
                }
                validarStatusMissao(dados.statusMissao());
                validarRanks(dados.rankNinja(), dados.rankMissao());
                dao.inserir(conexao, idNinja, idMissao, idFuncao, data);
                conexao.commit();
            } catch (SQLException | IllegalArgumentException erro) {
                cancelarTransacao(conexao, erro);
                if (erro instanceof SQLException sql && sql.getErrorCode() == 1062) {
                    throw new IllegalArgumentException("Este ninja já está vinculado a essa missão.", erro);
                }
                throw erro;
            }
        }
    }

    public boolean desvincular(int id, String usuario, String senha) throws SQLException {
        try (var conexao = Conexao.abrirConexao(usuario, senha)) {
            conexao.setAutoCommit(false);
            try {
                String status = dao.buscarStatusParaDesvincular(conexao, id);
                if (missaoConcluida(status)) {
                    throw new IllegalArgumentException("A equipe de uma missão concluída faz parte do histórico.");
                }
                boolean removido = status != null && dao.desvincular(conexao, id);
                conexao.commit();
                return removido;
            } catch (SQLException | IllegalArgumentException erro) {
                cancelarTransacao(conexao, erro);
                throw erro;
            }
        }
    }

    private void cancelarTransacao(Connection conexao, Exception erro) {
        try {
            conexao.rollback();
        } catch (SQLException falhaRollback) {
            erro.addSuppressed(falhaRollback);
        }
    }

    public static boolean missaoConcluida(String status) {
        return "Concluída".equalsIgnoreCase(status);
    }

    static void validarStatusMissao(String status) {
        if (missaoConcluida(status)) {
            throw new IllegalArgumentException("A missão já foi concluída e não aceita novos ninjas.");
        }
    }

    static void validarRanks(String rankNinja, String rankMissao) {
        if (!RANKS_PERMITIDOS.getOrDefault(rankNinja, Set.of()).contains(rankMissao)) {
            throw new IllegalArgumentException(rankNinja + " não pode participar de uma missão de rank "
                + rankMissao + ".");
        }
    }
}
