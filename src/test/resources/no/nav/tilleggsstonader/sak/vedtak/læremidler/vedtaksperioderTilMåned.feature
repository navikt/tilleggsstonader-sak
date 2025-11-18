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
      | 01.08.2024 | 31.08.2024 | 01.08.2024      |
      | 01.09.2024 | 30.09.2024 | 01.08.2024      |

  Scenario: En vedtaksperiode innenfor et år, start i midten av måneden
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 15.08.2024 | 30.09.2024 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 15.08.2024 | 14.09.2024 | 15.08.2024      |
      | 15.09.2024 | 14.10.2024 | 15.08.2024      |

  Scenario: En vedtaksperiode som treffer nytt år
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 15.11.2024 | 19.01.2025 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 15.11.2024 | 14.12.2024 | 15.11.2024      |
      | 15.12.2024 | 14.01.2025 | 15.11.2024      |
      | 15.01.2025 | 14.02.2025 | 15.01.2025      |

  Scenario: Flere vedtaksperioder
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 17.04.2024 | 20.05.2024 |
      | 18.08.2024 | 04.10.2024 |
      | 13.12.2024 | 31.01.2025 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 17.04.2024 | 16.05.2024 | 17.04.2024      |
      | 17.05.2024 | 16.06.2024 | 17.04.2024      |
      | 18.08.2024 | 17.09.2024 | 19.08.2024      |
      | 18.09.2024 | 17.10.2024 | 19.08.2024      |
      | 13.12.2024 | 12.01.2025 | 13.12.2024      |
      | 13.01.2025 | 12.02.2025 | 13.01.2025      |

  Scenario: Flere vedtaksperioder krysser flere år
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 13.11.2024 | 05.03.2025 |
      | 10.11.2025 | 02.03.2026 |


    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 13.11.2024 | 12.12.2024 | 13.11.2024      |
      | 13.12.2024 | 12.01.2025 | 13.11.2024      |
      | 13.01.2025 | 12.02.2025 | 13.01.2025      |
      | 13.02.2025 | 12.03.2025 | 13.01.2025      |

      | 10.11.2025 | 09.12.2025 | 10.11.2025      |
      | 10.12.2025 | 09.01.2026 | 10.11.2025      |
      | 10.01.2026 | 09.02.2026 | 12.01.2026      |
      | 10.02.2026 | 09.03.2026 | 12.01.2026      |

  Scenario: Vedtaksperiode starter lørdag 31. desember. Flytter da utbetalingsdato til nytt år
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 31.12.2022 | 15.01.2023 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 31.12.2022 | 30.01.2023 | 02.01.2023      |

  Scenario: Treffer rundt månedsskifte februar-mars - håndter spesialtilfelle
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 31.01.2024 | 31.08.2024 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 31.01.2024 | 28.02.2024 | 31.01.2024      |
      | 29.02.2024 | 28.03.2024 | 31.01.2024      |
      | 29.03.2024 | 28.04.2024 | 31.01.2024      |
      | 29.04.2024 | 28.05.2024 | 31.01.2024      |
      | 29.05.2024 | 28.06.2024 | 31.01.2024      |
      | 29.06.2024 | 28.07.2024 | 31.01.2024      |
      | 29.07.2024 | 28.08.2024 | 31.01.2024      |
      | 29.08.2024 | 28.09.2024 | 31.01.2024      |

  Scenario: Treffer rundt månedsskifte februar-mars - ingen spesialtilfelle
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 28.01.2024 | 27.04.2024 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 28.01.2024 | 27.02.2024 | 29.01.2024      |
      | 28.02.2024 | 27.03.2024 | 29.01.2024      |
      | 28.03.2024 | 27.04.2024 | 29.01.2024      |


  Scenario: Fra lenger til kortere måned
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 31.03.2024 | 31.07.2024 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 31.03.2024 | 29.04.2024 | 01.04.2024      |
      | 30.04.2024 | 29.05.2024 | 01.04.2024      |
      | 30.05.2024 | 29.06.2024 | 01.04.2024      |
      | 30.06.2024 | 29.07.2024 | 01.04.2024      |
      | 30.07.2024 | 29.08.2024 | 01.04.2024      |

  Scenario: Flere vedtaksperioder innenfor en måned
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 03.01.2025 | 03.01.2025 |
      | 07.01.2025 | 07.01.2025 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 03.01.2025 | 02.02.2025 | 03.01.2025      |

  Scenario: Flere vedtaksperioder, der vedtaksperiode 2 løper i første og andre måned
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 03.01.2025 | 03.01.2025 |
      | 07.01.2025 | 07.02.2025 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 03.01.2025 | 02.02.2025 | 03.01.2025      |
      | 03.02.2025 | 02.03.2025 | 03.02.2025      |

  Scenario: Flere vedtaksperioder, der vedtaksperiode 2 løper fra første til tredje måned
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 03.01.2025 | 03.01.2025 |
      | 07.01.2025 | 07.03.2025 |

    Når splitter vedtaksperioder for læremidler

    Så forvent følgende utbetalingsperioder
      | Fom        | Tom        | Utbetalingsdato |
      | 03.01.2025 | 02.02.2025 | 03.01.2025      |
      | 03.02.2025 | 02.03.2025 | 03.02.2025      |
      | 03.03.2025 | 02.04.2025 | 03.02.2025      |
