# language: no
# encoding: UTF-8

Egenskap: Validering av vedtaksperioder for læremidler

  Scenario: Vedtaksperioder overlapper
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.01.2024 | 31.03.2024 |
      | 31.03.2024 | 31.04.2024 |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 31.04.2024 | AAP       | UTDANNING |

    Når validerer vedtaksperiode for læremidler

    Så forvent følgende feil fra vedtaksperiode validering: Periode=01.01.2024 - 31.03.2024 og 31.03.2024 - 30.04.2024 overlapper.

  Scenario: Skal kunne lage en vedtaksperiode som stekker seg over flere stønadsperioder
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.01.2024 | 03.02.2024 |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe       | Aktivitet |
      | 01.01.2024 | 15.01.2024 | AAP             | UTDANNING |
      | 16.01.2024 | 01.02.2024 | OVERGANGSSTØNAD | UTDANNING |
      | 02.02.2024 | 03.02.2024 | AAP             | UTDANNING |

    Når validerer vedtaksperiode for læremidler

    Så forvent ingen feil fra vedtaksperiode validering
