package model;

import java.time.LocalDate;

public record NinjaMissao(int id, int idNinja, String ninja, String missao, String rankMissao,
                          String statusMissao, String funcao, LocalDate dataParticipacao) {}
