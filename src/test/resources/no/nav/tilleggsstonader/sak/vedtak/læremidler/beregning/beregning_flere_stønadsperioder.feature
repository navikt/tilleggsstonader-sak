# language: no
# encoding: UTF-8

Egenskap: Beregning av læremidler - flere vedtaksperioder

  Scenario: Flere vedtalsperioder
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        |
      | 01.01.2024 | 30.04.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.12.2024 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2024 | 10.02.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 11.02.2024 | 05.03.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 06.03.2024 | 30.04.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.01.2024 | 31.01.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |
      | 01.02.2024 | 29.02.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.01.2024      |
      | 01.03.2024 | 31.03.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.03.2024      |
      | 01.04.2024 | 30.04.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.04.2024      |

  Scenario: Flere vedtalsperioder og vedtaksperioder med opphold
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe         |
      | 01.04.2024 | 31.05.2024 | AAP               |
      | 01.08.2024 | 30.09.2024 | OMSTILLINGSSTØNAD |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 01.01.2024 | 31.05.2024 | TILTAK    | VIDEREGÅENDE     | 100           |
      | 01.08.2024 | 31.10.2024 | UTDANNING | HØYERE_UTDANNING | 50            |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.04.2024 | 31.05.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 01.08.2024 | 30.09.2024 | GJENLEVENDE         | UTDANNING |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå       | Studieprosent | Sats | Målgruppe           | Aktivitet | Utbetalingsdato |
      | 01.04.2024 | 30.04.2024 | 438   | VIDEREGÅENDE     | 100           | 438  | NEDSATT_ARBEIDSEVNE | TILTAK    | 01.04.2024      |
      | 01.05.2024 | 31.05.2024 | 438   | VIDEREGÅENDE     | 100           | 438  | NEDSATT_ARBEIDSEVNE | TILTAK    | 01.04.2024      |
      | 01.08.2024 | 31.08.2024 | 438   | HØYERE_UTDANNING | 50            | 875  | GJENLEVENDE         | UTDANNING | 01.08.2024      |
      | 01.09.2024 | 30.09.2024 | 438   | HØYERE_UTDANNING | 50            | 875  | GJENLEVENDE         | UTDANNING | 01.08.2024      |

  Scenario: To ulike målgrupper av typen nedsatt arbeidsevne innenfor en vedtaksperiode men ulike løpende måneder
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe           |
      | 01.04.2024 | 31.04.2024 | AAP                 |
      | 01.05.2024 | 31.05.2024 | NEDSATT_ARBEIDSEVNE |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.05.2024 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.04.2024 | 31.05.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.04.2024 | 30.04.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.04.2024      |
      | 01.05.2024 | 31.05.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.04.2024      |

  Scenario: To ulike målgrupper der målgruppe 1 løper inn i måneden for nr 2, samme aktivitet skal bruke målgruppen som har høyest prioritet
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe       |
      | 01.04.2024 | 04.05.2024 | AAP             |
      | 05.05.2024 | 31.05.2024 | OVERGANGSSTØNAD |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.05.2024 | UTDANNING | VIDEREGÅENDE | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.04.2024 | 04.05.2024 | NEDSATT_ARBEIDSEVNE | UTDANNING |
      | 05.05.2024 | 31.05.2024 | ENSLIG_FORSØRGER    | UTDANNING |

    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Aktivitet | Utbetalingsdato |
      | 01.04.2024 | 30.04.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | UTDANNING | 01.04.2024      |
      | 01.05.2024 | 31.05.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | UTDANNING | 01.04.2024      |

