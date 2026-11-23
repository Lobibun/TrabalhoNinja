package dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import connection.Conexao;

public class ConsultaViewsDAO {
    public enum Consulta {
        NINJAS_E_MISSOES("Ninjas e missões", """
            SELECT nome_ninja, vila, cla, rank_ninja, titulo_missao, rank_missao, funcao, status_missao
            FROM vw_ninja_missoes
            ORDER BY nome_ninja, titulo_missao
            """, "Ninja", "Vila", "Clã", "Rank do ninja", "Missão", "Rank da missão", "Função", "Status"),
        NINJAS_POR_VILA("Ninjas por vila", """
            SELECT vila, quantidade_ninjas
            FROM vw_total_ninjas_por_vila
            ORDER BY vila
            """, "Vila", "Quantidade"),
        MISSOES_POR_RANK("Missões por rank", """
            SELECT rank_missao, quantidade_missoes
            FROM vw_total_missoes_por_rank
            ORDER BY CASE rank_missao
                WHEN 'D' THEN 1 WHEN 'C' THEN 2 WHEN 'B' THEN 3 WHEN 'A' THEN 4 WHEN 'S' THEN 5
                ELSE 6 END
            """, "Rank", "Quantidade"),
        MISSOES_SEM_EQUIPE("Missões sem equipe", """
            SELECT id_missao, titulo, rank_missao, vila_origem, status
            FROM vw_missoes_sem_ninjas
            ORDER BY titulo, id_missao
            """, "ID", "Missão", "Rank da missão", "Vila de origem", "Status");

        private final String titulo;
        private final String sql;
        private final List<String> colunas;

        Consulta(String titulo, String sql, String... colunas) {
            this.titulo = titulo;
            this.sql = sql;
            this.colunas = List.of(colunas);
        }

        public String titulo() {
            return titulo;
        }

        public List<String> colunas() {
            return colunas;
        }
    }

    public List<Object[]> consultar(Consulta consulta, String usuario, String senha) throws SQLException {
        List<Object[]> linhas = new ArrayList<>();
        try (var conexao = Conexao.abrirConexao(usuario, senha);
             var comando = conexao.prepareStatement(consulta.sql);
             var resultado = comando.executeQuery()) {
            while (resultado.next()) {
                Object[] linha = new Object[consulta.colunas().size()];
                for (int coluna = 0; coluna < linha.length; coluna++) {
                    linha[coluna] = resultado.getObject(coluna + 1);
                }
                linhas.add(linha);
            }
        }
        return linhas;
    }
}
