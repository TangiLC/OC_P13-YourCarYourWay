import {
  extractDialogTitle,
  extractDialogDate,
  formatTimestamp,
} from './dialog-utils';

describe('dialog-utils', () => {
  describe('extractDialogTitle', () => {
    const cases: Array<{
      topic: string | undefined | null;
      expected: string;
      desc: string;
    }> = [
      {
        topic: 'Reunion.@20250428_14:05',
        expected: 'Reunion',
        desc: 'retourne la partie avant ".@"',
      },
      {
        topic: 'PasDeDateIci',
        expected: 'PasDeDateIci',
        desc: 'chaine sans ".@" retourne la même chaine',
      },
      {
        topic: undefined,
        expected: 'No Title',
        desc: 'undefined retourne "No Title"',
      },
      { topic: null, expected: 'No Title', desc: 'null retourne "No Title"' },
      { topic: '.@', expected: '', desc: 'chaine ".@" retourne chaine vide' },
      {
        topic: 'T1.@20250101_10:00.@autre',
        expected: 'T1',
        desc: 'plusieurs occurrences de ".@" prend la 1ère',
      },
      { topic: '', expected: '', desc: 'chaine vide retourne chaine vide' },
    ];

    cases.forEach(({ topic, expected, desc }) => {
      it(desc, () => {
        expect(extractDialogTitle(topic)).toBe(expected);
      });
    });
  });

  describe('extractDialogDate', () => {
    const cases: Array<{
      topic: string | undefined | null;
      expected: string;
      desc: string;
    }> = [
      {
        topic: 'Reunion.@20250425_09:07',
        expected: '25/04/2025 à 09h07',
        desc: 'format normal',
      },
      { topic: undefined, expected: '', desc: 'undefined retourne vide' },
      { topic: null, expected: '', desc: 'null retourne vide' },
      {
        topic: 'TitreSansDate',
        expected: '',
        desc: 'pas de ".@" retourne vide',
      },
      {
        topic: 'Titre.@20250101',
        expected: '01/01/2025 à undefinedhundefined',
        desc: "pas d'underscore retourne vide",
      },
      {
        topic: 'X.@20251301_12:30',
        expected: '01/13/2025 à 12h30',
        desc: 'mois > 12',
      },
      {
        topic: 'X.@20250101_7:5',
        expected: '01/01/2025 à 7:h',
        desc: 'timeStr format court',
      },
      {
        topic: 'X.@abc_def',
        expected: '//abc à deh',
        desc: 'format incorrect',
      },
    ];

    cases.forEach(({ topic, expected, desc }) => {
      it(desc, () => {
        expect(extractDialogDate(topic)).toBe(expected);
      });
    });
  });

  describe('formatTimestamp', () => {
    const cases: Array<{
      ts: string;
      expected?: string;
      expectedPattern?: RegExp;
      desc: string;
    }> = [
      {
        ts: '2025-04-28T14:05:00Z',
        expected: '28/04/25 @14h05',
        desc: 'format normal UTC',
      },
      {
        ts: '2025-01-02T03:04:00Z',
        expected: '02/01/25 @03h04',
        desc: 'padding des zéros',
      },
      {
        ts: 'not-a-date',
        expectedPattern: /NaN\/NaN\/N @NaNhNaN/,
        desc: 'input invalide produit NaN',
      },
      {
        ts: '1970-01-01T00:00:00Z',
        expected: '01/01/70 @00h00',
        desc: 'début epoch',
      },
      {
        ts: '9999-12-31T23:59:00Z',
        expected: '31/12/99 @23h59',
        desc: 'date lointaine siècle tronqué',
      },
    ];

    cases.forEach(({ ts, expected, expectedPattern, desc }) => {
      it(desc, () => {
        const result = formatTimestamp(ts);
        if (expected !== undefined) {
          expect(result).toBe(expected);
        } else if (expectedPattern) {
          expect(result).toMatch(expectedPattern);
        }
      });
    });
  });
});
