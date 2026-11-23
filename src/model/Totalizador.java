package model;

import java.time.LocalDateTime;

public record Totalizador(String descricao, int quantidade, LocalDateTime dataGeracao) {}
