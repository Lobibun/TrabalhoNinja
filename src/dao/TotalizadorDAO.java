package dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import connection.Conexao;
import model.Totalizador;

public class TotalizadorDAO {
    private static final String CALCULAR = """
        WITH naturezas_ninja AS (
            SELECT n.id AS id_ninja, COALESCE(nn.id_natureza, n.natureza_chakra) AS id_natureza
            FROM tb_ninja n
            LEFT JOIN tb_ninja_natureza_chakra nn ON nn.id_ninja = n.id
        )
        SELECT descricao, quantidade, CURRENT_TIMESTAMP AS data_geracao
        FROM (
            SELECT 1 AS grupo, v.nome AS categoria, CONCAT('Ninjas por vila: ', v.nome) AS descricao,
                   COUNT(n.id) AS quantidade
            FROM tb_vila v
            LEFT JOIN tb_ninja n ON n.vila = v.id
            GROUP BY v.id, v.nome

            UNION ALL
            SELECT 2, r.nome, CONCAT('Ninjas por rank: ', r.nome), COUNT(n.id)
            FROM tb_rank_ninja r
            LEFT JOIN tb_ninja n ON n.rank_ninja = r.id
            GROUP BY r.id, r.nome

            UNION ALL
            SELECT 3, ch.nome, CONCAT('Ninjas por natureza: ', ch.nome), COUNT(DISTINCT nn.id_ninja)
            FROM tb_natureza_chakra ch
            LEFT JOIN naturezas_ninja nn ON nn.id_natureza = ch.id
            GROUP BY ch.id, ch.nome

            UNION ALL
            SELECT 3, 'Sem natureza', 'Ninjas sem natureza de chakra', COUNT(*)
            FROM naturezas_ninja
            WHERE id_natureza IS NULL

            UNION ALL
            SELECT 4, CONVERT(r.nome USING utf8mb4),
                   CONCAT('Missões por rank: ', CONVERT(r.nome USING utf8mb4)), COUNT(m.id)
            FROM tb_rank_missao r
            LEFT JOIN tb_missao m ON m.rank_missao = r.id
            GROUP BY r.id, r.nome

            UNION ALL
            SELECT 5, s.nome, CONCAT('Missões por status: ', s.nome), COUNT(m.id)
            FROM tb_status_missao s
            LEFT JOIN tb_missao m ON m.status = s.id
            GROUP BY s.id, s.nome

            UNION ALL
            SELECT 6, '', 'Ninjas vinculados a missões', COUNT(DISTINCT id_ninja)
            FROM tb_ninja_missao

            UNION ALL
            SELECT 7, '', 'Missões sem ninjas vinculados', COUNT(*)
            FROM tb_missao m
            WHERE NOT EXISTS (SELECT 1 FROM tb_ninja_missao nm WHERE nm.id_missao = m.id)
        ) AS totais
        ORDER BY grupo, categoria
        """;

    public List<Totalizador> gerarESalvar(String usuario, String senha) throws SQLException {
        try (var conexao = Conexao.abrirConexao(usuario, senha)) {
            conexao.setAutoCommit(false);
            try {
                List<Totalizador> totais = calcular(conexao);
                salvar(conexao, totais);
                conexao.commit();
                return totais;
            } catch (SQLException erro) {
                try {
                    conexao.rollback();
                } catch (SQLException falhaRollback) {
                    erro.addSuppressed(falhaRollback);
                }
                throw erro;
            }
        }
    }

    private List<Totalizador> calcular(Connection conexao) throws SQLException {
        List<Totalizador> totais = new ArrayList<>();
        try (var consulta = conexao.prepareStatement(CALCULAR);
             var resultado = consulta.executeQuery()) {
            while (resultado.next()) {
                totais.add(new Totalizador(resultado.getString("descricao"), resultado.getInt("quantidade"),
                    resultado.getTimestamp("data_geracao").toLocalDateTime()));
            }
        }
        return totais;
    }

    private void salvar(Connection conexao, List<Totalizador> totais) throws SQLException {
        String sql = "INSERT INTO tb_totalizador_ninja (descricao, quantidade, data_geracao) VALUES (?, ?, ?)";
        try (var insercao = conexao.prepareStatement(sql)) {
            for (Totalizador total : totais) {
                insercao.setString(1, total.descricao());
                insercao.setInt(2, total.quantidade());
                insercao.setTimestamp(3, Timestamp.valueOf(total.dataGeracao()));
                insercao.addBatch();
            }
            insercao.executeBatch();
        }
    }
}
