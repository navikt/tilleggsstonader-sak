# language: no
# encoding: UTF-8

Egenskap: Beregning
  Scenario: Flere stønadsperioder
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.01.2024 | 30.04.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.12.2024 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 10.02.2024 | AAP       | TILTAK    |
      | 11.02.2024 | 05.03.2024 | AAP       | TILTAK    |
      | 06.03.2024 | 30.04.2024 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsmåned |
      | 01.01.2024 | 31.01.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.2024          |
      | 01.02.2024 | 29.02.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.2024          |
      | 01.03.2024 | 31.03.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.2024          |
      | 01.04.2024 | 30.04.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.2024          |

  # Har: Flere slått sammen = ok
  # TODO: Ulike målgrupper/aktiviteter - kan ikke slås sammen - feil
  # Trigg begge feilmeldingene
  # to stønadsperioder med mellomrom + to vedtaksperioder med mellomrom - ok (ulike målgrupper eller aktivitet)
  # to ulike målgrupper, samme aktivitet, funker om månedskiftet er ok -> 2 tester