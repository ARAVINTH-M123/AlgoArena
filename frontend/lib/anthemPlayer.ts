// AnthemPlayer (Phase 5B): schedules the note events of lib/anthems.ts with the Web Audio API.
//
// Rules this code follows (they matter):
// - NEVER auto-plays: an AudioContext is created only inside play(), which is only called from a click.
// - One player for the whole app (anthemController), so only one anthem can sound at a time.
// - Lookahead scheduling: a 250 ms timer keeps ~2 s of notes queued; the AudioContext clock, not setTimeout,
//   decides when each note sounds.
// - Gentle fades: 0.15 s in, 0.3 s out; the context is closed after the fade so nothing lingers.
// - The mute switch is remembered in localStorage under "anthemMuted".

import { anthemLength, anthemSong, buildEvents, midiToFreq, type AnthemEvent, type AnthemSong } from "@/lib/anthems";

const MASTER_LEVEL = 0.3;          // overall loudness before the per-song trim
const FADE_IN_SECONDS = 0.15;
const FADE_OUT_SECONDS = 0.3;
const LOOKAHEAD_SECONDS = 2;       // keep this much audio queued
const TICK_MS = 250;               // scheduler poll interval

export const ANTHEM_MUTED_STORAGE_KEY = "anthemMuted";

type Voice = { osc: OscillatorNode; gain: GainNode; endsAt: number };

class AnthemPlayer {
  private ctx: AudioContext | null = null;
  private master: GainNode | null = null;
  private timer: ReturnType<typeof setInterval> | null = null;
  private voices: Voice[] = [];
  private song: AnthemSong | null = null;
  private events: AnthemEvent[] = [];
  private loopLength = 0;
  private eventIndex = 0;
  private loopStartCtxTime = 0;
  private stopRequested = false;

  private currentKey: string | null = null;     // the anthem currently sounding (the store snapshot)
  private listeners = new Set<() => void>();
  private muted = false;

  constructor() {
    if (typeof window !== "undefined") {
      try {
        this.muted = window.localStorage.getItem(ANTHEM_MUTED_STORAGE_KEY) === "true";
      } catch {
        // private mode etc.: just start unmuted
      }
    }
  }

  // ---- useSyncExternalStore surface ----

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  getSnapshot = (): string | null => {
    return this.currentKey;
  };

  isMuted(): boolean {
    return this.muted;
  }

  setMuted(muted: boolean): void {
    this.muted = muted;
    try {
      window.localStorage.setItem(ANTHEM_MUTED_STORAGE_KEY, String(muted));
    } catch {
      // best effort only
    }
    if (this.master && this.ctx) {
      const target = muted ? 0 : MASTER_LEVEL * (this.song?.trim ?? 1);
      const now = this.ctx.currentTime;
      this.master.gain.cancelScheduledValues(now);
      this.master.gain.setValueAtTime(this.master.gain.value, now);
      this.master.gain.linearRampToValueAtTime(target, now + 0.1);
    }
    this.notify();
  }

  // ---- commands ----

  /** Starts an anthem by catalog key. Does nothing when muted, unknown, or already playing this key. */
  play(key: string): void {
    if (this.muted || this.currentKey === key) {
      return;
    }
    const song = anthemSong(key);
    if (!song) {
      return;
    }
    this.stopImmediately();                      // switch anthems cleanly
    try {
      // The AudioContext exists only from a user gesture (this method is called from onClick handlers).
      const Ctor = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
      if (!Ctor) {
        return;
      }
      this.ctx = new Ctor();
    } catch {
      this.ctx = null;
      return;
    }

    const ctx = this.ctx;
    void ctx.resume();

    this.master = ctx.createGain();
    this.master.gain.setValueAtTime(0, ctx.currentTime);
    this.master.gain.linearRampToValueAtTime(MASTER_LEVEL * song.trim, ctx.currentTime + FADE_IN_SECONDS);

    const compressor = ctx.createDynamicsCompressor();
    compressor.threshold.value = -18;
    compressor.ratio.value = 6;
    this.master.connect(compressor);
    compressor.connect(ctx.destination);

    this.song = song;
    this.events = buildEvents(song);
    this.loopLength = anthemLength(song);
    this.eventIndex = 0;
    this.stopRequested = false;
    this.loopStartCtxTime = ctx.currentTime + 0.08;   // tiny breath before the first note
    this.currentKey = key;
    this.notify();

    this.tick();                                       // queue the first notes right away
    this.timer = setInterval(() => this.tick(), TICK_MS);
  }

  /** Toggles an anthem: play it, or stop it when it is the one sounding. */
  toggle(key: string): void {
    if (this.currentKey === key) {
      this.stop();
    } else {
      this.play(key);
    }
  }

  /** Fades out over 0.3 s and then closes the AudioContext. */
  stop(): void {
    if (!this.ctx || !this.master) {
      this.stopImmediately();
      return;
    }
    this.stopRequested = true;
    const ctx = this.ctx;
    const now = ctx.currentTime;
    try {
      this.master.gain.cancelScheduledValues(now);
      this.master.gain.setValueAtTime(this.master.gain.value, now);
      this.master.gain.linearRampToValueAtTime(0, now + FADE_OUT_SECONDS);
    } catch {
      // the context may already be closed
    }
    const key = this.currentKey;
    this.currentKey = null;
    this.notify();
    window.setTimeout(() => {
      if (key !== null && this.currentKey === key) {
        return;                          // the user started the same anthem again during the fade
      }
      this.stopImmediately();
    }, FADE_OUT_SECONDS * 1000 + 60);
  }

  // ---- internals ----

  private tick(): void {
    const ctx = this.ctx;
    if (!ctx || this.stopRequested) {
      return;
    }
    const horizon = ctx.currentTime + LOOKAHEAD_SECONDS;
    // schedule every event whose absolute time falls inside the horizon, wrapping across loop borders
    let guard = 0;
    while (guard++ < 500) {
      const event = this.events[this.eventIndex];
      const eventTime = this.loopStartCtxTime + event.t;
      if (eventTime >= horizon) {
        break;
      }
      this.scheduleVoice(event, Math.max(eventTime, ctx.currentTime + 0.005));
      this.eventIndex++;
      if (this.eventIndex >= this.events.length) {
        this.eventIndex = 0;
        this.loopStartCtxTime += this.loopLength;    // loop the anthem
      }
    }
  }

  private scheduleVoice(event: AnthemEvent, startTime: number): void {
    const ctx = this.ctx;
    const master = this.master;
    if (!ctx || !master) {
      return;
    }
    try {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = event.wave;
      osc.frequency.value = midiToFreq(event.midi);
      // a tiny attack/release envelope per note keeps voices from clicking
      gain.gain.setValueAtTime(0, startTime);
      gain.gain.linearRampToValueAtTime(event.gain, startTime + Math.min(0.02, event.dur / 3));
      gain.gain.setValueAtTime(event.gain, startTime + event.dur * 0.7);
      gain.gain.linearRampToValueAtTime(0, startTime + event.dur);
      osc.connect(gain);
      gain.connect(master);
      osc.start(startTime);
      osc.stop(startTime + event.dur + 0.02);
      this.voices.push({ osc, gain, endsAt: startTime + event.dur + 0.02 });
      if (this.voices.length > 256) {
        this.voices = this.voices.filter((voice) => voice.endsAt > ctx.currentTime);
      }
    } catch {
      // one lost note must never break the anthem
    }
  }

  private stopImmediately(): void {
    if (this.timer !== null) {
      clearInterval(this.timer);
      this.timer = null;
    }
    for (const voice of this.voices) {
      try {
        voice.osc.stop();
        voice.osc.disconnect();
        voice.gain.disconnect();
      } catch {
        // already stopped
      }
    }
    this.voices = [];
    if (this.ctx) {
      const ctx = this.ctx;
      this.ctx = null;
      this.master = null;
      window.setTimeout(() => {
        void ctx.close().catch(() => undefined);
      }, 0);
    }
    if (this.currentKey !== null) {
      this.currentKey = null;
      this.notify();
    }
    this.song = null;
    this.events = [];
  }

  private notify(): void {
    for (const listener of this.listeners) {
      listener();
    }
  }
}

/** The one anthem player for the whole app. */
export const anthemController = new AnthemPlayer();
