package model;

import java.util.List;

public class Ninja {
    public static final String SEM_NATUREZA = "Sem natureza";
    private final int id;
    private final String nome;
    private final String vila;
    private final String cla;
    private final String rank;
    private final List<String> naturezasChakra;
    private final String status;

    public Ninja(int id, String nome, String vila, String cla, String rank,
                 String naturezaChakra, String status) {
        this(id, nome, vila, cla, rank, List.of(naturezaChakra), status);
    }

    public Ninja(int id, String nome, String vila, String cla, String rank,
                 List<String> naturezasChakra, String status) {
        this.id = id;
        this.nome = nome;
        this.vila = vila;
        this.cla = cla;
        this.rank = rank;
        this.naturezasChakra = List.copyOf(naturezasChakra);
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getVila() {
        return vila;
    }

    public String getCla() {
        return cla;
    }

    public String getRank() {
        return rank;
    }

    public String getNaturezaChakra() {
        return naturezasChakra.isEmpty() ? SEM_NATUREZA : String.join(", ", naturezasChakra);
    }

    public List<String> getNaturezasChakra() {
        return naturezasChakra;
    }

    public String getStatus() {
        return status;
    }
}
