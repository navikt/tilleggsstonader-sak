# language: no
# encoding: UTF-8

Egenskap: Beregning barnetilsyn - validering av stønadsperioder

  Scenario: Sender inn tomme stønadsperioder

    Gitt følgende støndsperioder
      | Fom | Tom |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 31.01.2023 | TILTAK    | 3               |

    Når beregner

    Så forvent følgende feil: Kan ikke innvilge når det ikke finnes noen vedtaksperioder
