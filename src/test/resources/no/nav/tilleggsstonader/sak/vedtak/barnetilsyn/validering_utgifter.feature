# language: no
# encoding: UTF-8

Egenskap: Beregning barnetilsyn - validering av utgifter

  Scenario: Sender inn tomme utgifter

    Gitt følgende vedtaksperioder
      | Fom     | Tom     | Målgruppe           | Aktivitet |
      | 01.2023 | 02.2023 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende aktiviteter
      | Fom     | Tom     | Aktivitet | Aktivitetsdager |
      | 01.2023 | 02.2023 | TILTAK    | 5               |

    Gitt følgende målgrupper
      | Fom     | Tom     | Målgruppe |
      | 01.2023 | 02.2023 | AAP       |

    Gitt følgende utgifter for barn med id: 1
      | Fom | Tom | Utgift |

    Når beregner

    Så forvent følgende feil: Vedtaksperioden 01.01.2023–28.02.2023 mangler oppfylt utgift hele eller deler av perioden.

  Scenario: Sender inn overlappende utgiftsperioder

    Gitt følgende vedtaksperioder
      | Fom     | Tom     | Målgruppe           | Aktivitet |
      | 01.2023 | 03.2023 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende aktiviteter
      | Fom     | Tom     | Aktivitet | Aktivitetsdager |
      | 01.2023 | 03.2023 | TILTAK    | 5               |

    Gitt følgende målgrupper
      | Fom     | Tom     | Målgruppe |
      | 01.2023 | 03.2023 | AAP       |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 02.2023 | 100    |
      | 02.2023 | 03.2023 | 100    |

    Når beregner

    Så forvent følgende feil: Utgiftsperioder overlapper

  Scenario: Sender inn negativ utgift

    Gitt følgende vedtaksperioder
      | Fom     | Tom     | Målgruppe           | Aktivitet |
      | 01.2023 | 03.2023 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende aktiviteter
      | Fom     | Tom     | Aktivitet | Aktivitetsdager |
      | 01.2023 | 03.2023 | TILTAK    | 5               |

    Gitt følgende målgrupper
      | Fom     | Tom     | Målgruppe |
      | 01.2023 | 03.2023 | AAP       |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 03.2023 | -100   |

    Når beregner

    Så forvent følgende feil: Utgiftsperioder inneholder ugyldig utgift: -100
