import React, { useState } from 'react';
import './LogReport.css';

const API_BASE = 'http://47.251.185.82:8877';

function LogReport() {
  const [logData, setLogData] = useState({
    level: 'INFO',
    tag: '',
    message: ''
  });
  const [reportResult, setReportResult] = useState(null);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setLogData(prev => ({ ...prev, [name]: value }));
  };

  const handleReportLog = async () => {
    try {
      const response = await fetch(`${API_BASE}/api/data/log`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          ...logData,
          timestamp: Date.now()
        })
      });
      const data = await response.json();

      setReportResult(data);
      setTimeout(() => setReportResult(null), 3000);
    } catch (error) {
      setReportResult({ success: false, message: 'Report failed' });
    }
  };

  return (
    <div className="log-report">
      <h3>📝 Log Report</h3>

      <div className="form-group">
        <label htmlFor="logLevel">Level</label>
        <select
          id="logLevel"
          value={logData.level}
          onChange={handleInputChange}
          name="level"
        >
          <option value="DEBUG">DEBUG</option>
          <option value="INFO">INFO</option>
          <option value="WARN">WARN</option>
          <option value="ERROR">ERROR</option>
        </select>
      </div>

      <div className="form-group">
        <label htmlFor="logTag">Tag</label>
        <input
          id="logTag"
          type="text"
          value={logData.tag}
          onChange={handleInputChange}
          name="tag"
          placeholder="e.g., MainActivity"
        />
      </div>

      <div className="form-group">
        <label htmlFor="logMessage">Message</label>
        <textarea
          id="logMessage"
          value={logData.message}
          onChange={handleInputChange}
          name="message"
          placeholder="Enter log message..."
          rows="3"
        ></textarea>
      </div>

      <button className="btn btn-primary" onClick={handleReportLog}>
        Report Log
      </button>

      {reportResult && (
        <div className={`result ${reportResult.success ? 'success' : 'error'}`}>
          {reportResult.success ? '✓ ' : '✗ '}
          {reportResult.message}
        </div>
      )}
    </div>
  );
}

export default LogReport;
