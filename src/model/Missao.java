package model;

public class Missao {
    private final int id;
    private final String titulo;
    private final String descricao;
    private final int rankMissaoId;
    private final int vilaOrigemId;
    private final int statusId;

    public Missao(int id, String titulo, String descricao, int rankMissaoId, int vilaOrigemId, int statusId) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.rankMissaoId = rankMissaoId;
        this.vilaOrigemId = vilaOrigemId;
        this.statusId = statusId;
    }

    public int getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getRankMissaoId() {
        return rankMissaoId;
    }

    public int getVilaOrigemId() {
        return vilaOrigemId;
    }

    public int getStatusId() {
        return statusId;
    }
}
