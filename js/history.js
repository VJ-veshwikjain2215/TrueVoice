/**
 * TRUEVOICE — Forensic History Manager
 * Manages client-side persistence of voice analysis sessions and forensic reports.
 */

const STORAGE_KEY = 'truevoice_analysis_history_v1';

export class HistoryManager {
  static getAll() {
    try {
      const data = localStorage.getItem(STORAGE_KEY);
      return data ? JSON.parse(data) : [];
    } catch (e) {
      console.warn("Could not read analysis history from localStorage", e);
      return [];
    }
  }

  static save(result, fileName = "Audio Sample") {
    try {
      const history = HistoryManager.getAll();
      const entry = {
        id: `tv_${Date.now()}_${Math.random().toString(36).substr(2, 6)}`,
        timestamp: new Date().toISOString(),
        formattedDate: new Date().toLocaleString(),
        fileName: fileName,
        durationSeconds: result.durationSeconds,
        classification: result.classification,
        level: result.level,
        reason: result.reason,
        explanation: result.explanation,
        syntheticProbability: result.syntheticProbability,
        authenticityConfidence: result.authenticityConfidence,
        averageRms: result.averageRms,
        measuredWindows: result.measuredWindows,
        totalWindows: result.windows ? result.windows.length : 0,
      };

      // Keep latest 50 analyses
      history.unshift(entry);
      if (history.length > 50) history.pop();

      localStorage.setItem(STORAGE_KEY, JSON.stringify(history));
      return entry;
    } catch (e) {
      console.warn("Could not save analysis history to localStorage", e);
      return null;
    }
  }

  static delete(id) {
    try {
      let history = HistoryManager.getAll();
      history = history.filter(item => item.id !== id);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(history));
      return true;
    } catch (e) {
      console.warn("Could not delete item from history", e);
      return false;
    }
  }

  static clear() {
    try {
      localStorage.removeItem(STORAGE_KEY);
      return true;
    } catch (e) {
      console.warn("Could not clear analysis history", e);
      return false;
    }
  }

  static exportReportJson(entry) {
    const jsonStr = JSON.stringify(entry, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `TrueVoice_Forensic_Report_${entry.id}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }
}
