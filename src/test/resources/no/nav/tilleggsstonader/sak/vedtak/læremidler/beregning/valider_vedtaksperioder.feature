# language: no
# encoding: UTF-8

Egenskap: Validering av vedtaksperioder for læremidler
  Scenario: Første enkleste case - validering
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.01.2024 | 31.03.2024 |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 31.03.2024 | AAP       | UTDANNING |


    Når validerer vedtaksperiode for læremidler

    Så skal resultat fra validering være
      | Fom        | Tom        |
      | 01.01.2024 | 31.03.2024 |