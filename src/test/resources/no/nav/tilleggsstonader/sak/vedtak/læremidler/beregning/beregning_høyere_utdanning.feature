# language: no
# encoding: UTF-8

Egenskap: Beregning - Høyere utdanning

  Scenario: Høyere utdanning og 100% studier
    Gitt følgende beregningsperiode for læremidler
      | Fom        | Tom        | Studienivå       | Studieprosent |
      | 01.01.2024 | 31.03.2024 | HØYERE_UTDANNING | 100           |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Måned   | Beløp | Studienivå       | Studieprosent | Sats |
      | 01.2024 | 875   | HØYERE_UTDANNING | 100           | 875  |
      | 02.2024 | 875   | HØYERE_UTDANNING | 100           | 875  |
      | 03.2024 | 875   | HØYERE_UTDANNING | 100           | 875  |

  Scenario: Høyere utdanning og 50% studier
    Gitt følgende beregningsperiode for læremidler
      | Fom        | Tom        | Studienivå       | Studieprosent |
      | 01.01.2024 | 15.03.2024 | HØYERE_UTDANNING | 50            |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Måned   | Beløp | Studienivå       | Studieprosent | Sats |
      | 01.2024 | 438   | HØYERE_UTDANNING | 50            | 875  |
      | 02.2024 | 438   | HØYERE_UTDANNING | 50            | 875  |
      | 03.2024 | 438   | HØYERE_UTDANNING | 50            | 875  |

