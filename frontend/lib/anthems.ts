// Anthems (Phase 5B): ten tiny generative songs, synthesized in the browser with the Web Audio API.
// No audio files exist: everything is derived at runtime from the data below.
//
// buildEvents(song) is a PURE function: same song, same events, every time (seeded PRNG).
// lib/anthemPlayer.ts turns those events into scheduled oscillators.

export type AnthemWave = "sine" | "triangle" | "square" | "sawtooth";

export type AnthemEvent = {
  /** Seconds from the start of the anthem. */
  t: number;
  /** MIDI note number. */
  midi: number;
  /** Duration in seconds. */
  dur: number;
  /** Relative loudness 0..1 (the player scales this globally). */
  gain: number;
  wave: AnthemWave;
};

export type AnthemSong = {
  /** Matches the backend catalog key (and lib/cosmetics.ts ANTHEM_TITLES). */
  key: string;
  bpm: number;
  bars: number;
  /** MIDI note of the root (D2 = 38, A2 = 45, ...). */
  rootMidi: number;
  /** Semitone offsets of the scale used for melodies. */
  scale: number[];
  /** Per-song loudness trim, multiplied into the master gain. */
  trim: number;
  /** Style drives the arrangement (bass pattern, waves, layering). */
  style: "pulse" | "campfire" | "chiptune" | "folk" | "march" | "industrial" | "airy" | "synth" | "shimmer" | "ascension";
};

// scales (semitone offsets)
const MINOR_PENT = [0, 3, 5, 7, 10];
const MAJOR_PENT = [0, 2, 4, 7, 9];
const DORIAN = [0, 2, 3, 5, 7, 9, 10];
const LYDIAN = [0, 2, 4, 6, 7, 9, 11];
const AEOLIAN = [0, 2, 3, 5, 7, 8, 10];

// The ten anthems, in rank order (index = rank index 0..9). Lengths: bars * 4 beats * 60 / bpm.
export const ANTHEM_SONGS: AnthemSong[] = [
  { key: "anthem.soft-pulse", bpm: 60, bars: 8, rootMidi: 38, scale: MINOR_PENT, trim: 1, style: "pulse" },        // 32.0s, D2
  { key: "anthem.campfire", bpm: 72, bars: 8, rootMidi: 45, scale: MAJOR_PENT, trim: 0.75, style: "campfire" },    // 26.7s, A2
  { key: "anthem.chiptune", bpm: 132, bars: 16, rootMidi: 57, scale: MAJOR_PENT, trim: 1.2, style: "chiptune" },   // 29.1s, A3
  { key: "anthem.folk", bpm: 96, bars: 12, rootMidi: 50, scale: DORIAN, trim: 0.7, style: "folk" },                // 30.0s, D3
  { key: "anthem.march", bpm: 108, bars: 14, rootMidi: 43, scale: DORIAN, trim: 1.65, style: "march" },            // 31.1s, G2
  { key: "anthem.industrial", bpm: 120, bars: 16, rootMidi: 41, scale: AEOLIAN, trim: 1.7, style: "industrial" },  // 32.0s, F2
  { key: "anthem.airy", bpm: 70, bars: 10, rootMidi: 62, scale: LYDIAN, trim: 1.1, style: "airy" },                // 34.3s, D4
  { key: "anthem.synth", bpm: 124, bars: 16, rootMidi: 45, scale: DORIAN, trim: 1, style: "synth" },               // 31.0s, A2
  { key: "anthem.shimmer", bpm: 90, bars: 12, rootMidi: 57, scale: LYDIAN, trim: 1.75, style: "shimmer" },         // 32.0s, A3
  { key: "anthem.ascension", bpm: 66, bars: 10, rootMidi: 36, scale: MINOR_PENT, trim: 1, style: "ascension" }     // 36.4s, C2
];

export function anthemSong(key: string | null | undefined): AnthemSong | null {
  return ANTHEM_SONGS.find((song) => song.key === key) ?? null;
}

/** Length of one anthem in seconds (bar = 4 beats). */
export function anthemLength(song: AnthemSong): number {
  return (song.bars * 4 * 60) / song.bpm;
}

export function midiToFreq(midi: number): number {
  return 440 * Math.pow(2, (midi - 69) / 12);
}

// ------------------------------------------------------------------ deterministic PRNG

/** Small deterministic PRNG (mulberry32); seeded from the song key so every render is identical. */
function prng(seedText: string): () => number {
  let seed = 2166136261;
  for (let i = 0; i < seedText.length; i++) {
    seed ^= seedText.charCodeAt(i);
    seed = Math.imul(seed, 16777619);
  }
  let state = seed >>> 0;
  return () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

// ------------------------------------------------------------------ the composer

/**
 * Builds every note of an anthem as plain data. Each style has its own arrangement, but all of them are:
 * a slow bass on the root, a chord pad per bar, and a melody walking the scale. Ascension starts empty and
 * adds one layer per bar until everything plays at once.
 */
export function buildEvents(song: AnthemSong): AnthemEvent[] {
  const random = prng(song.key);
  const events: AnthemEvent[] = [];
  const beat = 60 / song.bpm;                       // one beat in seconds
  const barSeconds = 4 * beat;
  const scale = song.scale;

  const scaleNote = (degree: number, octave: number): number => {
    const index = ((degree % scale.length) + scale.length) % scale.length;
    const octaveJump = Math.floor(degree / scale.length) + octave;
    return song.rootMidi + 12 * octaveJump + scale[index];
  };

  const push = (t: number, midi: number, dur: number, gain: number, wave: AnthemWave) => {
    events.push({ t: Math.round(t * 1000) / 1000, midi, dur, gain, wave });
  };

  for (let bar = 0; bar < song.bars; bar++) {
    const barStart = bar * barSeconds;
    const layer = (n: number) => song.style !== "ascension" || bar >= n;   // ascension: one layer per 2 bars

    // --- bass: root and fifth, on beats 0 and 2 (styles differ in wave and gain)
    if (layer(0)) {
      const wave: AnthemWave = song.style === "chiptune" || song.style === "synth" || song.style === "industrial" ? "sawtooth" : "triangle";
      const bassGain = song.style === "airy" || song.style === "shimmer" ? 0.16 : 0.3;
      push(barStart, song.rootMidi, beat * 1.8, bassGain, wave);
      push(barStart + 2 * beat, song.rootMidi + 7, beat * 1.8, bassGain * 0.8, wave);
      if (song.style === "industrial" || song.style === "march") {
        push(barStart + beat, song.rootMidi, beat * 0.9, bassGain * 0.6, wave);
        push(barStart + 3 * beat, song.rootMidi + (bar % 2 === 0 ? 3 : 5), beat * 0.9, bassGain * 0.6, wave);
      }
    }

    // --- pad: a chord held for the whole bar, quiet
    if (layer(2)) {
      const degrees = [0, 2, 4];
      for (const degree of degrees) {
        const wave: AnthemWave = song.style === "shimmer" || song.style === "airy" ? "sine" : "triangle";
        push(barStart, scaleNote(degree, song.style === "airy" || song.style === "shimmer" ? 1 : 0), barSeconds * 0.95, 0.1, wave);
      }
    }

    // --- melody: an eighth-note walk that always returns toward the root
    if (layer(4)) {
      const wave: AnthemWave =
        song.style === "chiptune" ? "square" : song.style === "synth" ? "square" : song.style === "industrial" ? "sawtooth" : "sine";
      const steps = 8;                                   // eight eighths per bar
      let degree = Math.floor(random() * scale.length);
      const octave = song.style === "airy" ? 2 : song.style === "shimmer" || song.style === "chiptune" ? 1 : 1;
      const melGain = song.style === "march" || song.style === "industrial" ? 0.22 : 0.17;
      for (let step = 0; step < steps; step++) {
        // rests keep it musical: roughly one eighth in four
        if (random() < 0.22 && step % 4 !== 0) continue;
        degree += random() < 0.5 ? 1 : -1;
        if (random() < 0.12) degree += random() < 0.5 ? 2 : -2;
        // pull back toward the root at the end of every second bar
        if (step === steps - 1 && bar % 2 === 1) degree = 0;
        const midi = scaleNote(degree, octave);
        const dur = step % 4 === 3 ? beat * 0.9 : beat * 0.45;
        push(barStart + step * (beat / 2), midi, dur, melGain, wave);
      }
    }

    // --- shimmer/airy sparkle: one high bell note per bar
    if (layer(6) && (song.style === "shimmer" || song.style === "airy")) {
      const degree = Math.floor(random() * scale.length);
      push(barStart + random() * barSeconds * 0.5, scaleNote(degree, 2), beat * 2, 0.12, "sine");
    }
  }

  // events must be in time order for the scheduler
  events.sort((a, b) => a.t - b.t);
  return events;
}
