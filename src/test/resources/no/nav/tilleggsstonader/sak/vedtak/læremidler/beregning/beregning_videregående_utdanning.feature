# language: no
# encoding: UTF-8

Egenskap: Beregning - Videregående utdanning

  Scenario: Videregående utdanning og 100% studier
    Gitt følgende beregningsperiode for læremidler
      | Fom        | Tom        | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.03.2024 | VIDEREGÅENDE | 100           |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Måned   | Beløp | Studienivå   | Studieprosent | Sats |
      | 01.2024 | 438   | VIDEREGÅENDE | 100           | 438  |
      | 02.2024 | 438   | VIDEREGÅENDE | 100           | 438  |
      | 03.2024 | 438   | VIDEREGÅENDE | 100           | 438  |

  Scenario: Videregående utdanning og 50% studier
    Gitt følgende beregningsperiode for læremidler
      | Fom        | Tom        | Studienivå   | Studieprosent |
      | 01.01.2024 | 15.03.2024 | VIDEREGÅENDE | 50            |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Måned   | Beløp | Studienivå   | Studieprosent | Sats |
      | 01.2024 | 219   | VIDEREGÅENDE | 50            | 438  |
      | 02.2024 | 219   | VIDEREGÅENDE | 50            | 438  |
      | 03.2024 | 219   | VIDEREGÅENDE | 50            | 438  |

