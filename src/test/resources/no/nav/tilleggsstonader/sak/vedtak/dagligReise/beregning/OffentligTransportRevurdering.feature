# language: no
# encoding: UTF-8

Egenskap: Innvilgelse av daglig reise - revurdering

  Bakgrunn:
    Gitt følgende aktiviteter for behandling=1
      | Fom        | Tom        | Aktivitet |
      | 01.10.2025 | 01.03.2026 | TILTAK    |

    Og følgende målgrupper for behandling=1
      | Fom        | Tom        | Målgruppe           |
      | 01.10.2025 | 01.03.2026 | NEDSATT_ARBEIDSEVNE |

  Scenario: Legger til lengre periode i en revurdering

    Gitt følgende daglig reise for behandling=1
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.10.2025 | 28.02.2026 | 44                 | 800                       | 5                         |

    Når vi innvilger daglig reise for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        |
      | 01.10.2025 | 15.01.2026 |

    Så kan vi forvente følgende daglig reise beregningsresultat for behandling=1
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall | Enkeltbillett-antall |
      | 01.10.2025 | 30.10.2025 | 800   | 1                          | 0                    |
      | 31.10.2025 | 29.11.2025 | 800   | 1                          | 0                    |
      | 30.11.2025 | 29.12.2025 | 800   | 1                          | 0                    |
      | 30.12.2025 | 15.01.2026 | 800   | 1                          | 0                    |

    Når vi kopierer perioder fra forrige daglig reise behandling for behandling=2

    Når vi innvilger daglig reise behandling=2 med tidligsteEndring=2026-01-13 med følgende vedtaksperioder
      | Fom        | Tom        |
      | 01.10.2025 | 15.01.2026 |
      | 16.01.2026 | 28.02.2026 |

    Så kan vi forvente følgende daglig reise beregningsresultat for behandling=2
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall | Enkeltbillett-antall |
      | 01.10.2025 | 30.10.2025 | 800   | 1                          | 0                    |
      | 31.10.2025 | 29.11.2025 | 800   | 1                          | 0                    |
      | 30.11.2025 | 29.12.2025 | 800   | 1                          | 0                    |
      | 30.12.2025 | 28.01.2026 | 800   | 1                          | 0                    |
      | 29.01.2026 | 27.02.2026 | 800   | 1                          | 0                    |

  Scenario: Legger til lengre periode i en revurdering som en vedtaksperiode

    Gitt følgende daglig reise for behandling=1
      | Fom        | Tom        | Pris enkeltbillett | Pris tretti-dagersbillett | Antall reisedager per uke |
      | 01.10.2025 | 28.02.2026 | 44                 | 800                       | 5                         |

    Når vi innvilger daglig reise for behandling=1 med følgende vedtaksperioder
      | Fom        | Tom        |
      | 01.10.2025 | 15.01.2026 |

    Så kan vi forvente følgende daglig reise beregningsresultat for behandling=1
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall | Enkeltbillett-antall |
      | 01.10.2025 | 30.10.2025 | 800   | 1                          | 0                    |
      | 31.10.2025 | 29.11.2025 | 800   | 1                          | 0                    |
      | 30.11.2025 | 29.12.2025 | 800   | 1                          | 0                    |
      | 30.12.2025 | 15.01.2026 | 800   | 1                          | 0                    |

    Når vi kopierer perioder fra forrige daglig reise behandling for behandling=2

    Når vi innvilger daglig reise behandling=2 med tidligsteEndring=2025-12-30 med følgende vedtaksperioder
      | Fom        | Tom        |
      | 01.10.2025 | 28.02.2026 |

    Så kan vi forvente følgende daglig reise beregningsresultat for behandling=2
      | Fom        | Tom        | Beløp | Trettidagersbillett-antall | Enkeltbillett-antall |
      | 01.10.2025 | 30.10.2025 | 800   | 1                          | 0                    |
      | 31.10.2025 | 29.11.2025 | 800   | 1                          | 0                    |
      | 30.11.2025 | 29.12.2025 | 800   | 1                          | 0                    |
      | 30.12.2025 | 28.01.2026 | 800   | 1                          | 0                    |
      | 29.01.2026 | 27.02.2026 | 800   | 1                          | 0                    |

