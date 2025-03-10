# language: no
# encoding: UTF-8

Egenskap: Beregning barnetilsyn - validering av vedtaksperioder

  Scenario: Sender inn tomme vedtaksperioder

    Gitt følgende vedtaksperioder
      | Fom | Tom |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 31.01.2023 | TILTAK    | 3               |

    Når beregner

    Så forvent følgende feil: Kan ikke innvilge når det ikke finnes noen vedtaksperioder
