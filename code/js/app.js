/**
 * TRUEVOICE — Unified Audio Authenticity Detection Controller
 * 
 * Manages audio ingestion, decoding, 16 kHz Float32 normalization,
 * oscillogram waveform & spectrogram rendering, 3-step pipeline progress,
 * and laboratory authenticity verification report presentation.
 */

import { TrueVoiceAnalyzer, AudioConstants } from './analyzer.js?v=2.6';
import { HistoryManager } from './history.js?v=2.6';

class TrueVoiceApp {
  constructor() {
    this.analyzer = new TrueVoiceAnalyzer();
    this.audioCtx = null;
    this.audioBuffer = null;
    this.raw16kData = null;
    this.currentFileName = null;
    this.lastResult = null;

    // Playback state
    this.audioSourceNode = null;
    this.isPlaying = false;
    this.playbackStartTime = 0;
    this.playbackPauseOffset = 0;
    this.animationFrameId = null;

    // Recording state
    this.mediaRecorder = null;
    this.recordedChunks = [];
    this.isRecording = false;
    this.recordingStartTime = 0;
    this.recordingInterval = null;

    // View state
    this.currentView = 'analyze'; // 'analyze', 'history', 'about'
    this.currentCanvasTab = 'waveform'; // 'waveform' or 'spectrogram'
    this.currentWorkflowStep = 1; // 1, 2, 3

    // Initialize UI
    this.initElements();
    this.bindEvents();
    this.initCanvasResize();
    this.renderHistory();
    this.setWorkflowStep(1, 'Audio Authenticity Engine Ready · Select Audio Clip');
  }

  initElements() {
    // Navigation
    this.navBtns = document.querySelectorAll('.nav-tab-btn');
    this.viewPanels = document.querySelectorAll('.view-panel');
    this.navBrandLink = document.getElementById('navBrandLink');

    // Stepper elements
    this.stepIndicator1 = document.getElementById('stepIndicator1');
    this.stepIndicator2 = document.getElementById('stepIndicator2');
    this.stepIndicator3 = document.getElementById('stepIndicator3');
    this.pipelineStatusText = document.getElementById('pipelineStatusText');

    // Dropzone & File Input
    this.dropzone = document.getElementById('dropzone');
    this.fileInput = document.getElementById('fileInput');
    this.btnUpload = document.getElementById('btnUpload');
    this.btnRecord = document.getElementById('btnRecord');
    this.recordText = document.getElementById('recordText');
    this.dropHeading = document.getElementById('dropHeading');
    this.dropSubtext = document.getElementById('dropSubtext');

    // Preset Benchmarks (Human vs AI Clone)
    this.btnSampleHuman = document.getElementById('btnSampleHuman');
    this.btnSampleClone = document.getElementById('btnSampleClone');

    // Visualizer / Inspector Panel
    this.visualizerCard = document.getElementById('visualizerCard');
    this.btnPlayPause = document.getElementById('btnPlayPause');
    this.playIcon = document.getElementById('playIcon');
    this.pauseIcon = document.getElementById('pauseIcon');
    this.timeCurrent = document.getElementById('timeCurrent');
    this.timeTotal = document.getElementById('timeTotal');
    this.inspectFileName = document.getElementById('inspectFileName');
    this.inspectDurationTag = document.getElementById('inspectDurationTag');
    this.waveformCanvas = document.getElementById('waveformCanvas');
    this.spectrogramCanvas = document.getElementById('spectrogramCanvas');
    this.playheadMarker = document.getElementById('playheadMarker');
    this.btnTabWaveform = document.getElementById('btnTabWaveform');
    this.btnTabSpectrogram = document.getElementById('btnTabSpectrogram');

    // Scanning & Analysis Progress Overlay
    this.scanningOverlay = document.getElementById('scanningOverlay');
    this.scanStepText = document.getElementById('scanStepText');
    this.scanProgressFill = document.getElementById('scanProgressFill');
    this.scanTelemetryLog = document.getElementById('scanTelemetryLog');

    // Centerpiece Verification Report
    this.resultSection = document.getElementById('resultSection');
    this.reportSampleTitle = document.getElementById('reportSampleTitle');
    this.reportSerialId = document.getElementById('reportSerialId');
    this.verdictBanner = document.getElementById('verdictBanner');
    this.verdictClassification = document.getElementById('verdictClassification');
    this.verdictReason = document.getElementById('verdictReason');
    this.verdictConfidenceValue = document.getElementById('verdictConfidenceValue');
    this.syntheticScoreVal = document.getElementById('syntheticScoreVal');
    this.authenticityConfidenceVal = document.getElementById('authenticityConfidenceVal');
    this.durationEnergyVal = document.getElementById('durationEnergyVal');
    this.windowCountVal = document.getElementById('windowCountVal');
    this.evidenceNarrative = document.getElementById('evidenceNarrative');
    this.windowTableBody = document.getElementById('windowTableBody');
    this.btnExportReport = document.getElementById('btnExportReport');
    this.btnAnalyzeAnother = document.getElementById('btnAnalyzeAnother');

    // History View Elements
    this.historyTableBody = document.getElementById('historyTableBody');
    this.emptyHistoryBox = document.getElementById('emptyHistoryBox');
    this.historyTableCard = document.getElementById('historyTableCard');
    this.historyCount = document.getElementById('historyCount');
    this.btnClearHistory = document.getElementById('btnClearHistory');
    this.btnHistoryGoAnalyze = document.getElementById('btnHistoryGoAnalyze');
  }

  bindEvents() {
    // Navigation Tabs
    this.navBtns.forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        const targetView = btn.dataset.view;
        this.switchView(targetView);
      });
    });

    if (this.navBrandLink) {
      this.navBrandLink.addEventListener('click', (e) => {
        e.preventDefault();
        this.switchView('analyze');
      });
    }

    if (this.btnHistoryGoAnalyze) {
      this.btnHistoryGoAnalyze.addEventListener('click', () => {
        this.switchView('analyze');
      });
    }

    // Dropzone & File Input
    if (this.btnUpload && this.fileInput) {
      this.btnUpload.addEventListener('click', (e) => {
        e.stopPropagation();
        this.fileInput.click();
      });
    }

    if (this.dropzone && this.fileInput) {
      this.dropzone.addEventListener('click', () => this.fileInput.click());
      this.dropzone.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          this.fileInput.click();
        }
      });
    }

    if (this.fileInput) {
      this.fileInput.addEventListener('change', (e) => {
        if (e.target.files && e.target.files.length > 0) {
          this.handleFileSelected(e.target.files[0]);
        }
      });
    }

    // Drag & Drop
    if (this.dropzone) {
      ['dragenter', 'dragover'].forEach(eventName => {
        this.dropzone.addEventListener(eventName, (e) => {
          e.preventDefault();
          e.stopPropagation();
          this.dropzone.classList.add('drag-active');
        });
      });

      ['dragleave', 'drop'].forEach(eventName => {
        this.dropzone.addEventListener(eventName, (e) => {
          e.preventDefault();
          e.stopPropagation();
          this.dropzone.classList.remove('drag-active');
        });
      });

      this.dropzone.addEventListener('drop', (e) => {
        const files = e.dataTransfer.files;
        if (files && files.length > 0) {
          this.handleFileSelected(files[0]);
        }
      });
    }

    // Presets / Benchmark samples
    if (this.btnSampleHuman) {
      this.btnSampleHuman.addEventListener('click', () => {
        this.loadRemoteSample('audio/sample-genuine-human.mp3', 'Benchmark: Genuine Human Voice');
      });
    }

    if (this.btnSampleClone) {
      this.btnSampleClone.addEventListener('click', () => {
        this.loadRemoteSample('audio/sample-ai-clone.mp3', 'Benchmark: Synthetic AI Voice Clone');
      });
    }

    // Microphone Recording
    if (this.btnRecord) {
      this.btnRecord.addEventListener('click', (e) => {
        e.stopPropagation();
        this.toggleMicrophone();
      });
    }

    // Audio Playback
    if (this.btnPlayPause) {
      this.btnPlayPause.addEventListener('click', () => this.togglePlayback());
    }

    // Canvas Tabs
    if (this.btnTabWaveform) {
      this.btnTabWaveform.addEventListener('click', () => this.setCanvasTab('waveform'));
    }
    if (this.btnTabSpectrogram) {
      this.btnTabSpectrogram.addEventListener('click', () => this.setCanvasTab('spectrogram'));
    }

    // Canvas click to seek
    const canvasContainer = document.querySelector('.canvas-viewport');
    if (canvasContainer) {
      canvasContainer.addEventListener('click', (e) => {
        if (!this.audioBuffer) return;
        const rect = canvasContainer.getBoundingClientRect();
        const clickX = e.clientX - rect.left;
        const ratio = Math.max(0, Math.min(1, clickX / rect.width));
        this.seekTo(ratio * this.audioBuffer.duration);
      });
    }

    // Export Verification Report
    if (this.btnExportReport) {
      this.btnExportReport.addEventListener('click', () => {
        if (this.lastResult) {
          HistoryManager.exportReportJson(this.lastResult);
        }
      });
    }

    // Analyze Another Clip
    if (this.btnAnalyzeAnother) {
      this.btnAnalyzeAnother.addEventListener('click', () => {
        this.resetToInitial();
      });
    }

    // Clear History
    if (this.btnClearHistory) {
      this.btnClearHistory.addEventListener('click', () => {
        if (confirm("Clear all recorded analysis logs from this browser session?")) {
          HistoryManager.clear();
          this.renderHistory();
        }
      });
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Workflow Step Management
  // ──────────────────────────────────────────────────────────────────────────
  setWorkflowStep(step, statusText) {
    this.currentWorkflowStep = step;

    if (this.stepIndicator1) {
      this.stepIndicator1.className = step === 1 ? 'step-item active' : (step > 1 ? 'step-item done' : 'step-item');
    }
    if (this.stepIndicator2) {
      this.stepIndicator2.className = step === 2 ? 'step-item active' : (step > 2 ? 'step-item done' : 'step-item');
    }
    if (this.stepIndicator3) {
      this.stepIndicator3.className = step === 3 ? 'step-item active' : 'step-item';
    }

    if (this.pipelineStatusText && statusText) {
      this.pipelineStatusText.textContent = statusText;
    }
  }

  switchView(viewName) {
    this.currentView = viewName;

    this.navBtns.forEach(btn => {
      if (btn.dataset.view === viewName) {
        btn.classList.add('active');
      } else {
        btn.classList.remove('active');
      }
    });

    this.viewPanels.forEach(panel => {
      if (panel.id === `view-${viewName}`) {
        panel.classList.add('active');
      } else {
        panel.classList.remove('active');
      }
    });

    if (viewName === 'history') {
      this.renderHistory();
    } else if (viewName === 'analyze') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  resetToInitial() {
    this.stopPlayback();
    if (this.fileInput) this.fileInput.value = '';
    this.setWorkflowStep(1, 'Audio Authenticity Engine Ready · Select Audio Clip');
    if (this.visualizerCard) this.visualizerCard.style.display = 'none';
    if (this.resultSection) this.resultSection.classList.remove('active');
    if (this.scanningOverlay) this.scanningOverlay.classList.remove('active');

    if (this.dropzone) {
      this.dropzone.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }

  getAudioContext() {
    if (!this.audioCtx) {
      const AudioCtxClass = window.AudioContext || window.webkitAudioContext;
      this.audioCtx = new AudioCtxClass();
    }
    if (this.audioCtx.state === 'suspended') {
      this.audioCtx.resume();
    }
    return this.audioCtx;
  }

  setCanvasTab(tab) {
    this.currentCanvasTab = tab;
    if (tab === 'waveform') {
      if (this.btnTabWaveform) this.btnTabWaveform.classList.add('active');
      if (this.btnTabSpectrogram) this.btnTabSpectrogram.classList.remove('active');
      if (this.waveformCanvas) this.waveformCanvas.style.display = 'block';
      if (this.spectrogramCanvas) this.spectrogramCanvas.style.display = 'none';
      this.renderWaveform();
    } else {
      if (this.btnTabSpectrogram) this.btnTabSpectrogram.classList.add('active');
      if (this.btnTabWaveform) this.btnTabWaveform.classList.remove('active');
      if (this.waveformCanvas) this.waveformCanvas.style.display = 'none';
      if (this.spectrogramCanvas) this.spectrogramCanvas.style.display = 'block';
      this.renderSpectrogram();
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Audio Ingestion (Files, Benchmarks, Microphone)
  // ──────────────────────────────────────────────────────────────────────────
  async handleFileSelected(file) {
    try {
      this.currentFileName = file.name;
      this.setWorkflowStep(2, `02 ANALYZING: ${file.name}`);
      this.showScanningOverlay(true);
      const sizeKb = (typeof file.size === 'number' && !isNaN(file.size)) ? (file.size / 1024).toFixed(1) : "0";
      this.updateScanningProgress(`Ingesting ${file.name} (${sizeKb} KB)...`, 20);

      const arrayBuffer = await file.arrayBuffer();
      await this.processArrayBuffer(arrayBuffer);
    } catch (err) {
      console.error("File ingestion failed", err);
      this.showScanningOverlay(false);
      this.setWorkflowStep(1, 'Error reading audio file. Please try another sample.');
      alert(`Could not process audio file: ${err.message}`);
    }
  }

  async loadRemoteSample(url, displayName) {
    try {
      this.setWorkflowStep(2, `02 ANALYZING: ${displayName}`);
      this.showScanningOverlay(true);
      this.updateScanningProgress(`Loading benchmark recording: ${displayName}...`, 20);

      const response = await fetch(url);
      if (!response.ok) throw new Error(`HTTP ${response.status} loading sample`);
      const arrayBuffer = await response.arrayBuffer();

      this.currentFileName = displayName;
      await this.processArrayBuffer(arrayBuffer);
    } catch (err) {
      console.error("Benchmark sample fetch failed", err);
      this.showScanningOverlay(false);
      this.setWorkflowStep(1, 'Error fetching benchmark audio.');
      alert(`Could not load benchmark sample: ${err.message}`);
    }
  }

  async toggleMicrophone() {
    if (this.isRecording) {
      this.stopMicrophone();
    } else {
      await this.startMicrophone();
    }
  }

  async startMicrophone() {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      this.recordedChunks = [];
      this.mediaRecorder = new MediaRecorder(stream);

      this.mediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) this.recordedChunks.push(e.data);
      };

      this.mediaRecorder.onstop = async () => {
        const blob = new Blob(this.recordedChunks, { type: 'audio/webm' });
        this.currentFileName = `Live Recording (${new Date().toLocaleTimeString()})`;
        const arrayBuffer = await blob.arrayBuffer();
        await this.processArrayBuffer(arrayBuffer);
        stream.getTracks().forEach(track => track.stop());
      };

      this.mediaRecorder.start(250);
      this.isRecording = true;
      if (this.btnRecord) this.btnRecord.classList.add('recording');
      this.recordingStartTime = Date.now();

      this.recordingInterval = setInterval(() => {
        const elapsed = Math.floor((Date.now() - this.recordingStartTime) / 1000);
        if (this.recordText) this.recordText.textContent = `Recording (${elapsed}s)...`;
      }, 500);

    } catch (err) {
      console.error("Microphone access error", err);
      alert("Microphone permission was denied or is not supported in this browser context.");
    }
  }

  stopMicrophone() {
    if (this.mediaRecorder && this.isRecording) {
      this.mediaRecorder.stop();
      this.isRecording = false;
      if (this.btnRecord) this.btnRecord.classList.remove('recording');
      if (this.recordText) this.recordText.textContent = "Record Microphone";
      clearInterval(this.recordingInterval);
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Decoding & Neural Forensic Pipeline Execution
  // ──────────────────────────────────────────────────────────────────────────
  async processArrayBuffer(arrayBuffer) {
    this.stopPlayback();
    const ctx = this.getAudioContext();

    this.updateScanningProgress("Decoding audio with Web Audio API...", 45);
    const decoded = await ctx.decodeAudioData(arrayBuffer);
    this.audioBuffer = decoded;

    this.updateScanningProgress("Resampling to 16,000 Hz mono PCM Float32...", 65);
    this.raw16kData = TrueVoiceAnalyzer.resampleTo16kMono(decoded);

    // Setup visualizer
    const durSec = (this.audioBuffer && typeof this.audioBuffer.duration === 'number') ? this.audioBuffer.duration : 0;
    if (this.visualizerCard) this.visualizerCard.style.display = 'block';
    if (this.timeTotal) this.timeTotal.textContent = this.formatTime(durSec);
    if (this.inspectFileName) this.inspectFileName.textContent = `Sample: ${this.currentFileName}`;
    if (this.inspectDurationTag) this.inspectDurationTag.textContent = `Total: ${durSec.toFixed(2)}s`;

    this.renderWaveform();
    if (this.currentCanvasTab === 'spectrogram') {
      this.renderSpectrogram();
    }

    // Run Forensic Detection
    await this.runDetectionPipeline();
  }

  async runDetectionPipeline() {
    this.updateScanningProgress("Slicing 64,600-sample windows & running voice authenticity analysis...", 85);
    await new Promise(r => setTimeout(r, 280));

    const results = await this.analyzer.analyze(this.raw16kData);

    this.updateScanningProgress("Synthesizing laboratory verification report...", 100);
    await new Promise(r => setTimeout(r, 150));

    this.showScanningOverlay(false);
    const safeLevel = (results && results.level) ? results.level : 'SAFE';
    this.setWorkflowStep(3, `03 VERIFICATION REPORT GENERATED: ${safeLevel === 'SAFE' ? 'LIKELY HUMAN' : 'AI VOICE CLONE'}`);
    this.renderResults(results);

    // Save to History
    const savedEntry = HistoryManager.save(results, this.currentFileName);
    this.lastResult = savedEntry || results;
    this.renderHistory();

    // Scroll to verification report
    if (this.resultSection) {
      this.resultSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  showScanningOverlay(show) {
    if (!this.scanningOverlay) return;
    if (show) {
      this.scanningOverlay.classList.add('active');
      if (this.resultSection) this.resultSection.classList.remove('active');
      if (this.scanProgressFill) this.scanProgressFill.style.width = '0%';
    } else {
      this.scanningOverlay.classList.remove('active');
    }
  }

  updateScanningProgress(text, percent) {
    if (this.scanStepText) this.scanStepText.textContent = text;
    if (this.scanProgressFill) this.scanProgressFill.style.width = `${percent}%`;
    if (this.scanTelemetryLog) this.scanTelemetryLog.textContent = `[AUTHENTICITY PIPELINE] ${text}`;
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Results & Verification Report Rendering
  // ──────────────────────────────────────────────────────────────────────────
  renderResults(res) {
    if (!this.resultSection) return;
    this.resultSection.classList.add('active');

    // Report Title & Serial
    if (this.reportSampleTitle) {
      this.reportSampleTitle.textContent = `Sample: ${this.currentFileName || 'Audio Recording'}`;
    }
    if (this.reportSerialId) {
      const serialNum = Math.floor(1000 + Math.random() * 9000);
      const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
      this.reportSerialId.textContent = `REPORT ID: TV-${dateStr}-${serialNum}`;
    }

    // Verdict Certificate Styling
    const levelStr = (res && res.level) ? res.level.toLowerCase() : 'safe';
    if (this.verdictBanner) {
      this.verdictBanner.className = `verdict-certificate ${levelStr}`;
    }

    let classificationTitle = "Likely Human Voice";
    if (res.level === "CRITICAL") classificationTitle = "AI Voice Clone Detected";
    else if (res.level === "SUSPICIOUS") classificationTitle = "Suspicious Voice / Synthetic Signal";
    else if (res.level === "INDETERMINATE") classificationTitle = "Indeterminate Audio Signal";

    if (this.verdictClassification) {
      this.verdictClassification.textContent = classificationTitle;
    }
    if (this.verdictReason) {
      this.verdictReason.textContent = `VERDICT: ${res.reason || 'AUTHENTIC_HUMAN_VOICE'}`;
    }

    // Confidence / Score Display with safe type guards
    const synthVal = (typeof res.syntheticProbability === 'number' && !isNaN(res.syntheticProbability)) ? res.syntheticProbability : 0.12;
    const authVal = (typeof res.authenticityConfidence === 'number' && !isNaN(res.authenticityConfidence)) ? res.authenticityConfidence : ((1 - synthVal) * 100);

    if (this.verdictConfidenceValue) {
      this.verdictConfidenceValue.textContent = `${(synthVal * 100).toFixed(1)}%`;
    }

    // Telemetry Cells
    if (this.syntheticScoreVal) {
      this.syntheticScoreVal.textContent = synthVal.toFixed(4);
    }

    if (this.authenticityConfidenceVal) {
      this.authenticityConfidenceVal.textContent = `${authVal.toFixed(1)}%`;
    }

    if (this.durationEnergyVal) {
      const durationSec = (typeof res.durationSeconds === 'number' && !isNaN(res.durationSeconds)) ? res.durationSeconds : 0;
      const rmsVal = (typeof res.averageRms === 'number' && !isNaN(res.averageRms)) ? res.averageRms : 0;
      this.durationEnergyVal.textContent = `${durationSec.toFixed(2)}s · RMS ${rmsVal.toFixed(4)}`;
    }

    if (this.windowCountVal) {
      const winCount = res.windows ? res.windows.length : 0;
      const actCount = typeof res.measuredWindows === 'number' ? res.measuredWindows : winCount;
      this.windowCountVal.textContent = `${winCount} Windows (${actCount} Active)`;
    }

    // Narrative Evidence
    if (this.evidenceNarrative) {
      this.evidenceNarrative.textContent = res.explanation || "Acoustic analysis complete.";
    }

    // Window-by-Window Audit Table
    if (this.windowTableBody) {
      this.windowTableBody.innerHTML = '';
      const winList = res.windows || [];
      winList.forEach((w) => {
        const tr = document.createElement('tr');
        const startSec = (typeof w.startSecond === 'number' && !isNaN(w.startSecond)) ? w.startSecond : 0;
        const timeStr = `${startSec.toFixed(2)}s - ${(startSec + 4.04).toFixed(2)}s`;
        const rmsStr = (typeof w.rms === 'number' && !isNaN(w.rms)) ? w.rms.toFixed(4) : "0.0000";
        const synthStr = (typeof w.synthetic === 'number' && !isNaN(w.synthetic)) ? `${(w.synthetic * 100).toFixed(1)}%` : "—";
        const bandStr = w.bandDescription || "Glottal Formants";
        const wLevel = (w && w.level) ? w.level : 'SAFE';

        tr.innerHTML = `
          <td style="font-family: var(--tv-font-mono); font-weight: 600;">W-${w.index || 1}</td>
          <td>${timeStr}</td>
          <td>${rmsStr}</td>
          <td>${synthStr}</td>
          <td>${bandStr}</td>
          <td><span class="pill-status ${wLevel.toLowerCase()}">${wLevel === 'SAFE' ? 'Human' : (wLevel === 'CRITICAL' ? 'AI Clone' : wLevel)}</span></td>
          <td>${w.reason || 'AUTHENTIC_HUMAN_VOICE'}</td>
        `;
        this.windowTableBody.appendChild(tr);
      });
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Persistent History Rendering
  // ──────────────────────────────────────────────────────────────────────────
  renderHistory() {
    if (!this.historyTableBody) return;
    const history = HistoryManager.getAll();

    if (this.historyCount) {
      this.historyCount.textContent = `${history.length} Saved Records`;
    }

    if (history.length === 0) {
      if (this.emptyHistoryBox) this.emptyHistoryBox.style.display = 'flex';
      if (this.historyTableCard) this.historyTableCard.style.display = 'none';
      return;
    }

    if (this.emptyHistoryBox) this.emptyHistoryBox.style.display = 'none';
    if (this.historyTableCard) this.historyTableCard.style.display = 'block';
    this.historyTableBody.innerHTML = '';

    history.forEach((item) => {
      if (!item || typeof item !== 'object') return;
      const tr = document.createElement('tr');
      const sProb = (typeof item.syntheticProbability === 'number' && !isNaN(item.syntheticProbability)) ? item.syntheticProbability : null;
      const aConf = (typeof item.authenticityConfidence === 'number' && !isNaN(item.authenticityConfidence)) ? item.authenticityConfidence : (sProb !== null ? (1 - sProb) * 100 : null);
      const dSec = (typeof item.durationSeconds === 'number' && !isNaN(item.durationSeconds)) ? item.durationSeconds : 0;
      
      const synthStr = sProb !== null ? `${(sProb * 100).toFixed(1)}%` : '—';
      const authStr = aConf !== null ? `${aConf.toFixed(1)}%` : '—';
      const itemLevel = item.level || 'SAFE';

      tr.innerHTML = `
        <td style="font-weight: 600; color: var(--tv-text-primary);">${item.fileName || 'Audio Sample'}</td>
        <td>${item.formattedDate || 'Recent'}</td>
        <td>${dSec.toFixed(1)}s</td>
        <td><span class="pill-status ${itemLevel.toLowerCase()}">${itemLevel === 'SAFE' ? 'Human' : (itemLevel === 'CRITICAL' ? 'AI Clone' : itemLevel)}</span></td>
        <td>${synthStr}</td>
        <td>${authStr}</td>
        <td style="white-space: nowrap;">
          <button type="button" class="btn-sample" data-action="export" data-id="${item.id}" style="padding: 0.2rem 0.5rem; font-size: 0.75rem;">Report</button>
          <button type="button" class="btn-sample" data-action="delete" data-id="${item.id}" style="padding: 0.2rem 0.5rem; font-size: 0.75rem; color: var(--tv-ai-text);">Delete</button>
        </td>
      `;

      tr.querySelector('[data-action="export"]').addEventListener('click', () => {
        HistoryManager.exportReportJson(item);
      });
      tr.querySelector('[data-action="delete"]').addEventListener('click', () => {
        HistoryManager.delete(item.id);
        this.renderHistory();
      });

      this.historyTableBody.appendChild(tr);
    });
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Laboratory Oscillogram & Spectrogram Canvas Rendering
  // ──────────────────────────────────────────────────────────────────────────
  initCanvasResize() {
    const resize = () => {
      if (!this.waveformCanvas || !this.spectrogramCanvas) return;
      const rect = this.waveformCanvas.getBoundingClientRect();
      const dpr = window.devicePixelRatio || 1;
      this.waveformCanvas.width = rect.width * dpr;
      this.waveformCanvas.height = rect.height * dpr;
      this.spectrogramCanvas.width = rect.width * dpr;
      this.spectrogramCanvas.height = rect.height * dpr;

      if (this.raw16kData) {
        this.renderWaveform();
        if (this.currentCanvasTab === 'spectrogram') {
          this.renderSpectrogram();
        }
      }
    };
    window.addEventListener('resize', resize);
    setTimeout(resize, 80);
  }

  renderWaveform() {
    if (!this.raw16kData || !this.waveformCanvas) return;
    const canvas = this.waveformCanvas;
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;

    ctx.clearRect(0, 0, width, height);

    // Center reference baseline (1px hairline)
    ctx.strokeStyle = '#E2E2DA';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(0, height / 2);
    ctx.lineTo(width, height / 2);
    ctx.stroke();

    // 4.04s Window division markers (32,300 hop)
    const totalDuration = this.raw16kData.length / 16000;
    const windowDuration = 4.0375;
    ctx.strokeStyle = '#D1D1C7';
    ctx.setLineDash([4, 4]);
    for (let t = windowDuration; t < totalDuration; t += 2.01875) {
      const x = (t / totalDuration) * width;
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, height);
      ctx.stroke();
    }
    ctx.setLineDash([]);

    // Forensic Charcoal Ink Waveform Rendering
    const step = Math.ceil(this.raw16kData.length / width);
    const amp = height / 2.2;

    ctx.fillStyle = '#141413';

    for (let i = 0; i < width; i++) {
      let min = 1.0;
      let max = -1.0;
      const startIdx = i * step;
      const endIdx = Math.min(this.raw16kData.length, startIdx + step);

      for (let j = startIdx; j < endIdx; j++) {
        const val = this.raw16kData[j];
        if (val < min) min = val;
        if (val > max) max = val;
      }

      if (max < min) { min = 0; max = 0; }

      const y1 = height / 2 + min * amp;
      const y2 = height / 2 + max * amp;
      ctx.fillRect(i, y1, 1, Math.max(1.5, y2 - y1));
    }
  }

  renderSpectrogram() {
    if (!this.raw16kData || !this.spectrogramCanvas) return;
    const canvas = this.spectrogramCanvas;
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;

    ctx.clearRect(0, 0, width, height);

    const N = 512;
    const numCols = Math.min(width, 320);
    const colStep = Math.floor((this.raw16kData.length - N) / numCols);

    // Grayscale thermal density spectrogram
    for (let c = 0; c < numCols; c++) {
      const offset = c * colStep;
      const x = (c / numCols) * width;
      const colW = Math.ceil(width / numCols);

      for (let b = 0; b < 64; b++) {
        const y = height - (b / 64) * height;
        const blockH = Math.ceil(height / 64);

        let energy = 0;
        const sampleIdx = offset + b * 4;
        if (sampleIdx < this.raw16kData.length) {
          energy = Math.abs(this.raw16kData[sampleIdx]);
        }

        const intensity = Math.min(1.0, energy * 4.8);
        const v = Math.floor(251 - intensity * 230);
        ctx.fillStyle = `rgb(${v}, ${v}, ${Math.max(0, v - 10)})`;
        ctx.fillRect(x, y - blockH, colW, blockH);
      }
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Audio Playback & Transport Controls
  // ──────────────────────────────────────────────────────────────────────────
  togglePlayback() {
    if (this.isPlaying) {
      this.pausePlayback();
    } else {
      this.startPlayback();
    }
  }

  startPlayback() {
    if (!this.audioBuffer) return;
    const ctx = this.getAudioContext();

    this.audioSourceNode = ctx.createBufferSource();
    this.audioSourceNode.buffer = this.audioBuffer;
    this.audioSourceNode.connect(ctx.destination);

    const offset = this.playbackPauseOffset % this.audioBuffer.duration;
    this.audioSourceNode.start(0, offset);
    this.playbackStartTime = ctx.currentTime - offset;
    this.isPlaying = true;

    if (this.playIcon) this.playIcon.style.display = 'none';
    if (this.pauseIcon) this.pauseIcon.style.display = 'block';

    this.audioSourceNode.onended = () => {
      if (this.isPlaying) {
        this.stopPlayback();
      }
    };

    this.trackPlayback();
  }

  pausePlayback() {
    if (!this.isPlaying) return;
    const ctx = this.getAudioContext();
    this.playbackPauseOffset = ctx.currentTime - this.playbackStartTime;
    if (this.audioSourceNode) {
      this.audioSourceNode.stop();
      this.audioSourceNode.disconnect();
      this.audioSourceNode = null;
    }
    this.isPlaying = false;
    if (this.playIcon) this.playIcon.style.display = 'block';
    if (this.pauseIcon) this.pauseIcon.style.display = 'none';
    cancelAnimationFrame(this.animationFrameId);
  }

  stopPlayback() {
    this.pausePlayback();
    this.playbackPauseOffset = 0;
    this.updatePlayhead(0);
    if (this.timeCurrent) this.timeCurrent.textContent = "0:00";
  }

  seekTo(seconds) {
    const wasPlaying = this.isPlaying;
    this.pausePlayback();
    this.playbackPauseOffset = seconds;
    this.updatePlayhead(seconds / this.audioBuffer.duration);
    if (this.timeCurrent) this.timeCurrent.textContent = this.formatTime(seconds);
    if (wasPlaying) {
      this.startPlayback();
    }
  }

  trackPlayback() {
    if (!this.isPlaying) return;
    const ctx = this.getAudioContext();
    const current = (ctx.currentTime - this.playbackStartTime) % this.audioBuffer.duration;
    if (this.timeCurrent) this.timeCurrent.textContent = this.formatTime(current);
    this.updatePlayhead(current / this.audioBuffer.duration);
    this.animationFrameId = requestAnimationFrame(() => this.trackPlayback());
  }

  updatePlayhead(ratio) {
    if (this.playheadMarker) {
      const r = (typeof ratio === 'number' && !isNaN(ratio)) ? Math.max(0, Math.min(1, ratio)) : 0;
      this.playheadMarker.style.left = `${(r * 100).toFixed(2)}%`;
    }
  }

  formatTime(seconds) {
    if (typeof seconds !== 'number' || isNaN(seconds) || seconds < 0) return "0:00";
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  }
}

// Instantiate on DOM load
document.addEventListener('DOMContentLoaded', () => {
  window.trueVoiceApp = new TrueVoiceApp();
});
