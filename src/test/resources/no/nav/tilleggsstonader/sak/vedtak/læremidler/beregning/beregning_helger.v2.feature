# language: no
# encoding: UTF-8

Egenskap: Beregning av læremidler - helg v2

  Scenario: Vedtaksperiode er kun over helg
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 01.01.2025 | 31.03.2025 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 01.01.2025 | 31.03.2025 | TILTAK    | HØYERE_UTDANNING | 50            |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.02.2025 | 02.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom | Tom | Beløp | Studienivå | Studieprosent | Sats | Målgruppe | Utbetalingsdato |

  Scenario: Vedtaksperioder er kun over helger
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 01.01.2025 | 31.03.2025 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 01.01.2025 | 31.03.2025 | TILTAK    | HØYERE_UTDANNING | 50            |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.02.2025 | 02.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 08.02.2025 | 09.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom | Tom | Beløp | Studienivå | Studieprosent | Sats | Målgruppe | Utbetalingsdato |

  Scenario: Vedtaksperioder er over 2 helger og 1 ukesdag, men løpende måneden begynner første helgen
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 01.01.2025 | 31.03.2025 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå       | Studieprosent |
      | 01.01.2025 | 31.03.2025 | TILTAK    | HØYERE_UTDANNING | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.02.2025 | 02.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 08.02.2025 | 09.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 11.02.2025 | 11.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå       | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.02.2025 | 11.02.2025 | 901   | HØYERE_UTDANNING | 100           | 901  | NEDSATT_ARBEIDSEVNE | 03.02.2025      |
