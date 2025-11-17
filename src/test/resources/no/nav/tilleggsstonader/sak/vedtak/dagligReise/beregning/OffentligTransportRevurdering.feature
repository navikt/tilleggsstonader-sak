# language: no
# encoding: UTF-8

Egenskap: Innvilgelse av daglig reise - revurdering

  Bakgrunn:
    Gitt følgende aktiviteter for behandling=1
      | Fom        | Tom        | Aktivitet |
      | 01.01.2025 | 31.12.2025 | TILTAK    |

    Og følgende målgrupper for behandling=1
      | Fom        | Tom        | Målgruppe           |
      | 01.01.2025 | 31.12.2025 | NEDSATT_ARBEIDSEVNE |

  Scenario: Legger til lengre periode i en revurdering

    Gitt følgende daglig reise for behandling=1
      | Fom        | Tom        | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.01.2025 | 30.01.2025 | 778                       | 3                         |

    Når vi innvilger daglig reise for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        |
      | 01.01.2025 | 30.01.2025 |

    Så kan vi forvente følgende daglig reise beregningsresultat for behandling=1
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall |
      | 01.01.2025 | 30.01.2025 | 778   | 1                          |

    Når vi kopierer perioder fra forrige daglig reise behandling for behandling=2

    Og vi legger inn følgende daglig reise endringer for behandling=2
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall |
      | 31.01.2025 | 01.03.2025 | 778   | 1                          |

    Og vi innvilger daglig reise behandling=2 med tidligsteEndring=2025-01-30 med følgende vedtaksperioder
      | Fom        | Tom        |
      | 01.01.2025 | 30.01.2025 |
      | 31.02.2025 | 01.03.2025 |


    Så kan vi forvente følgende daglig reise beregningsresultat for behandling=2
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall |
      | 01.01.2025 | 30.01.2025 | 778   | 1                          |
      | 31.01.2025 | 01.03.2025 | 778   | 1                          |

    Og følgende daglig reise vedtaksperioder for behandling=2
      | Fom        | Tom        |
      | 01.01.2025 | 30.01.2025 |
      | 31.01.2025 | 01.03.2025 |
