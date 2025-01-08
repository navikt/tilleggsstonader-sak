# language: no
# encoding: UTF-8

Egenskap: Splitt vedtaksperioder til utbetalingsperioder

  Scenario: En vedtaksperiode innenfor et år, start i starten av måneden
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.08.2024 | 30.09.2024 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 01.08.2024 | 31.08.2024 | 01.08.2024          |
      | 01.09.2024 | 30.09.2024 | 01.08.2024          |

  Scenario: En vedtaksperiode innenfor et år, start i midten av måneden
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 15.08.2024 | 30.09.2024 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 15.08.2024 | 14.09.2024 | 15.08.2024          |
      | 15.09.2024 | 30.09.2024 | 15.08.2024          |

  Scenario: En vedtaksperiode som treffer nytt år
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 15.11.2024 | 14.01.2025 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 15.11.2024 | 14.12.2024 | 15.11.2024          |
      | 15.12.2024 | 31.12.2024 | 15.11.2024          |
      | 01.01.2025 | 14.01.2025 | 01.01.2025          |

  Scenario: Flere vedtaksperioder
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 17.04.2024 | 20.05.2024 |
      | 18.08.2024 | 04.10.2024 |
      | 13.12.2024 | 31.01.2025 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 17.04.2024 | 16.05.2024 | 17.04.2024          |
      | 17.05.2024 | 20.05.2024 | 17.04.2024          |
      | 18.08.2024 | 17.09.2024 | 19.08.2024          |
      | 18.09.2024 | 04.10.2024 | 19.08.2024          |
      | 13.12.2024 | 31.12.2024 | 13.12.2024          |
      | 01.01.2025 | 31.01.2025 | 01.01.2025          |

  Scenario: Treffer rundt månedsskifte februar-mars - håndter spesialtilfelle
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 31.01.2024 | 31.08.2024 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 31.01.2024 | 28.02.2024 | 31.01.2024          |
      | 29.02.2024 | 28.03.2024 | 31.01.2024          |
      | 29.03.2024 | 28.04.2024 | 31.01.2024          |
      | 29.04.2024 | 28.05.2024 | 31.01.2024          |
      | 29.05.2024 | 28.06.2024 | 31.01.2024          |
      | 29.06.2024 | 28.07.2024 | 31.01.2024          |
      | 29.07.2024 | 28.08.2024 | 31.01.2024          |
      | 29.08.2024 | 31.08.2024 | 31.01.2024          |

  Scenario: Treffer rundt månedsskifte februar-mars - ingen spesialtilfelle
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 28.01.2024 | 27.04.2024 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 28.01.2024 | 27.02.2024 | 29.01.2024          |
      | 28.02.2024 | 27.03.2024 | 29.01.2024          |
      | 28.03.2024 | 27.04.2024 | 29.01.2024          |


  Scenario: Fra lenger til kortere måned
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 31.03.2024 | 31.07.2024 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 31.03.2024 | 29.04.2024 | 01.04.2024          |
      | 30.04.2024 | 29.05.2024 | 01.04.2024          |
      | 30.05.2024 | 29.06.2024 | 01.04.2024          |
      | 30.06.2024 | 29.07.2024 | 01.04.2024          |
      | 30.07.2024 | 31.07.2024 | 01.04.2024          |
