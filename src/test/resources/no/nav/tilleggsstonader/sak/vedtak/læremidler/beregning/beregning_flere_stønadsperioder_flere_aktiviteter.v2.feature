# language: no
# encoding: UTF-8

Egenskap: Beregning læremidler - flere målgrupper - flere aktiviteter v2

  Scenario: 2 ulike målgrupper og ulike aktiviteter innenfor en løpende måned
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe       |
      | 07.01.2025 | 07.01.2025 | OVERGANGSSTØNAD |
      | 08.01.2025 | 08.01.2025 | AAP             |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      # Overlapper ikke med stønadsperiode, med med vedtaksperiode
      | 01.01.2025 | 07.01.2025 | TILTAK    | HØYERE_UTDANNING | 100           |
      | 01.01.2025 | 07.01.2025 | UTDANNING | VIDEREGÅENDE     | 50            |
      | 08.01.2025 | 08.01.2025 | TILTAK    | VIDEREGÅENDE     | 50            |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 07.01.2025 | 07.01.2025 | ENSLIG_FORSØRGER    | UTDANNING |
      | 08.01.2025 | 08.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 07.01.2025 | 08.01.2025 | 226   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 07.01.2025      |

  Scenario: Flere aktiviteter med ulike datoer innenfor løpende måned men utenfor vedtaksperiode
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 15.08.2024 | 14.09.2024 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 15.08.2024 | 15.08.2024 | TILTAK    | VIDEREGÅENDE     | 50            |
      # aktivitet 2 er innenfor løpende måned, men ikke overlapp med vedtaksperiode, men overlapp med stønadsperiode
      | 20.08.2024 | 25.08.2024 | TILTAK    | HØYERE_UTDANNING | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 15.08.2024 | 15.08.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 15.08.2024 | 15.08.2024 | 219   | VIDEREGÅENDE | 50            | 438  | NEDSATT_ARBEIDSEVNE | 15.08.2024      |

  Scenario: Flere målgrupper, skal prioritere målgruppe med høyest prioritet
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe       |
      | 01.01.2024 | 04.01.2024 | OVERGANGSSTØNAD |
      | 05.01.2024 | 07.01.2024 | AAP             |
      | 08.01.2024 | 15.01.2024 | OVERGANGSSTØNAD |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 03.01.2024 | UTDANNING | VIDEREGÅENDE | 100           |
      | 04.01.2024 | 07.01.2024 | TILTAK    | VIDEREGÅENDE | 100           |
      | 08.01.2024 | 15.01.2024 | UTDANNING | VIDEREGÅENDE | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2024 | 03.01.2024 | ENSLIG_FORSØRGER    | UTDANNING |
      | 05.01.2024 | 07.01.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 08.01.2024 | 15.01.2024 | ENSLIG_FORSØRGER    | UTDANNING |

    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.01.2024 | 15.01.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |