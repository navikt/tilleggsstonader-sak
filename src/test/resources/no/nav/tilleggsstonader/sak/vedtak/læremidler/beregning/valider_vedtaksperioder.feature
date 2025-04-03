# language: no
# encoding: UTF-8

Egenskap: Validering av vedtaksperioder for læremidler

  Scenario: Vedtaksperioder overlapper

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2024 | 31.03.2024 | NEDSATT_ARBEIDSEVNE | UTDANNING |
      | 31.03.2024 | 31.04.2024 | NEDSATT_ARBEIDSEVNE | UTDANNING |

    Når validerer vedtaksperiode for læremidler

    Så forvent følgende feil fra vedtaksperiode validering: Periode=01.01.2024 - 31.03.2024 og 31.03.2024 - 30.04.2024 overlapper.

  Scenario: Vedtaksperioder overlapper med målgruppe som ikke gir rett på stønad skal kaste feil

    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe       |
      | 01.01.2024 | 31.01.2024 | AAP             |
      | 20.01.2024 | 25.01.2024 | INGEN_MÅLGRUPPE |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.01.2024 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2024 | 31.01.2024 | NEDSATT_ARBEIDSEVNE | UTDANNING |

    Når validerer vedtaksperiode for læremidler

    Så forvent følgende feil fra vedtaksperiode validering: Vedtaksperiode 01.01.2024 - 31.01.2024 overlapper med INGEN_MÅLGRUPPE(20.01.2024 - 25.01.2024) som ikke gir rett på stønad

  Scenario: Skal kunne lage en vedtaksperiode som stekker seg over flere vedtaksperioder

    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe       |
      | 01.01.2024 | 15.01.2024 | AAP             |
      | 16.01.2024 | 01.02.2024 | OVERGANGSSTØNAD |
      | 02.02.2024 | 03.02.2024 | AAP             |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 03.02.2024 | UTDANNING | VIDEREGÅENDE | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2024 | 15.01.2024 | NEDSATT_ARBEIDSEVNE | UTDANNING |
      | 16.01.2024 | 01.02.2024 | ENSLIG_FORSØRGER    | UTDANNING |
      | 02.02.2024 | 03.02.2024 | NEDSATT_ARBEIDSEVNE | UTDANNING |

    Når validerer vedtaksperiode for læremidler

    Så forvent ingen feil fra vedtaksperiode validering
