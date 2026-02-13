# language: no
# encoding: UTF-8

Egenskap: Beregning av vedtaksperioder med typeAktivitet-flagg

  Regel: Når flagget er på skal aktiviteter med ulik typeAktivitet holdes adskilt

    Scenario: To aktiviteter med samme AktivitetType men ulik typeAktivitet som ikke overlapper skal gi to vedtaksperioder
      Gitt ta høyde for typeAktivitet er satt til true

      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type   | TypeAktivitet |
        | 01.01.2023 | 31.01.2023 | TILTAK | GRUPPEAMO     |
        | 01.02.2023 | 28.02.2023 | TILTAK | JOBBKLUBB     |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 28.02.2023 | AAP  |

      Gitt følgende vilkår for vedtaksforslag
        | Fom        | Tom        | Resultat |
        | 01.01.2023 | 28.02.2023 | OPPFYLT  |

      Når forslag til vedtaksperioder lages

      Så forvent følgende vedtaksperioder
        | Fom        | Tom        | aktivitet | målgruppe           | TypeAktivitet |
        | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE | GRUPPEAMO     |
        | 01.02.2023 | 28.02.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE | JOBBKLUBB     |

    Scenario: To aktiviteter med samme AktivitetType og samme typeAktivitet skal merges sammen
      Gitt ta høyde for typeAktivitet er satt til true

      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type   | TypeAktivitet |
        | 01.01.2023 | 31.01.2023 | TILTAK | GRUPPEAMO     |
        | 01.02.2023 | 28.02.2023 | TILTAK | GRUPPEAMO     |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 28.02.2023 | AAP  |

      Gitt følgende vilkår for vedtaksforslag
        | Fom        | Tom        | Resultat |
        | 01.01.2023 | 28.02.2023 | OPPFYLT  |

      Når forslag til vedtaksperioder lages

      Så forvent følgende vedtaksperioder
        | Fom        | Tom        | aktivitet | målgruppe           | TypeAktivitet |
        | 01.01.2023 | 28.02.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE | GRUPPEAMO     |

    Scenario: To aktiviteter med samme AktivitetType men ulik typeAktivitet som overlapper skal kaste feil
      Gitt ta høyde for typeAktivitet er satt til true

      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type   | TypeAktivitet |
        | 01.01.2023 | 31.01.2023 | TILTAK | GRUPPEAMO     |
        | 15.01.2023 | 28.02.2023 | TILTAK | JOBBKLUBB     |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 28.02.2023 | AAP  |

      Gitt følgende vilkår for vedtaksforslag
        | Fom        | Tom        | Resultat |
        | 01.01.2023 | 28.02.2023 | OPPFYLT  |

      Når forslag til vedtaksperioder lages

      Så forvent følgende feil for vedtaksforsalg: Aktiviteter med type=TILTAK kan ikke ha ulik typeAktivitet (GRUPPEAMO og JOBBKLUBB) som overlapper i tid

    Scenario: To aktiviteter med ulik AktivitetType og ulik typeAktivitet som ikke overlapper
      Gitt ta høyde for typeAktivitet er satt til true

      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type      | TypeAktivitet |
        | 01.01.2023 | 31.01.2023 | TILTAK    | GRUPPEAMO     |
        | 01.02.2023 | 28.02.2023 | UTDANNING |               |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 28.02.2023 | AAP  |

      Gitt følgende vilkår for vedtaksforslag
        | Fom        | Tom        | Resultat |
        | 01.01.2023 | 28.02.2023 | OPPFYLT  |

      Når forslag til vedtaksperioder lages

      Så forvent følgende vedtaksperioder
        | Fom        | Tom        | aktivitet | målgruppe           | TypeAktivitet |
        | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE | GRUPPEAMO     |
        | 01.02.2023 | 28.02.2023 | UTDANNING | NEDSATT_ARBEIDSEVNE |               |

    Scenario: Når flagget er av skal aktiviteter med ulik typeAktivitet merges som før
      Gitt ta høyde for typeAktivitet er satt til false

      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type   | TypeAktivitet |
        | 01.01.2023 | 31.01.2023 | TILTAK | GRUPPEAMO     |
        | 01.02.2023 | 28.02.2023 | TILTAK | JOBBKLUBB     |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 28.02.2023 | AAP  |

      Gitt følgende vilkår for vedtaksforslag
        | Fom        | Tom        | Resultat |
        | 01.01.2023 | 28.02.2023 | OPPFYLT  |

      Når forslag til vedtaksperioder lages

      Så forvent følgende vedtaksperioder
        | Fom        | Tom        | aktivitet | målgruppe           | TypeAktivitet |
        | 01.01.2023 | 28.02.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE | GRUPPEAMO     |

  Regel: Ved revurdering skal beholdId også ta høyde for typeAktivitet når flagget er på

    Scenario: Revurdering - perioder med ulik typeAktivitet skal ikke merges sammen når flagget er på
      Gitt ta høyde for typeAktivitet er satt til true

      Gitt følgende vilkårsperioder med aktiviteter for vedtaksforslag
        | Fom        | Tom        | type   | TypeAktivitet |
        | 01.01.2023 | 31.01.2023 | TILTAK | GRUPPEAMO     |
        | 01.02.2023 | 28.02.2023 | TILTAK | JOBBKLUBB     |

      Gitt følgende vilkårsperioder med målgrupper for vedtaksforslag
        | Fom        | Tom        | type |
        | 01.01.2023 | 28.02.2023 | AAP  |

      Gitt følgende vilkår for vedtaksforslag
        | Fom        | Tom        | Resultat |
        | 01.01.2023 | 28.02.2023 | OPPFYLT  |

      Gitt følgende tidligere vedtaksperioder for vedtaksforslag
        | Id | Fom        | Tom        | aktivitet | målgruppe           | TypeAktivitet |
        | 1  | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE | GRUPPEAMO     |

      Når forslag til vedtaksperioder behold id lages

      Så forvent følgende vedtaksperioder med riktig id
        | Id | Fom        | Tom        | aktivitet | målgruppe           | TypeAktivitet |
        | 1  | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE | GRUPPEAMO     |
        | -1 | 01.02.2023 | 28.02.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE | JOBBKLUBB     |
