import React, { useState } from 'react';
import './FileUpload.css';

const API_BASE = 'http://47.251.185.82:8877';

function FileUpload({ onUploadSuccess }) {
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [selectedFile, setSelectedFile] = useState(null);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    setSelectedFile(file);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    const file = e.dataTransfer.files[0];
    setSelectedFile(file);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    setUploading(true);
    setProgress(0);

    const formData = new FormData();
    formData.append('file', selectedFile);
    formData.append('category', 'upload');

    try {
      const xhr = new XMLHttpRequest();

      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable) {
          const percentComplete = Math.round((e.loaded / e.total) * 100);
          setProgress(percentComplete);
        }
      });

      xhr.addEventListener('load', () => {
        if (xhr.status === 200) {
          const response = JSON.parse(xhr.responseText);
          if (response.success) {
            alert('Upload successful!');
            setSelectedFile(null);
            setProgress(0);
            onUploadSuccess();
          } else {
            alert('Upload failed: ' + response.message);
          }
        } else {
          alert('Upload failed');
        }
        setUploading(false);
      });

      xhr.addEventListener('error', () => {
        alert('Upload error');
        setUploading(false);
      });

      xhr.open('POST', `${API_BASE}/api/file/upload`);
      xhr.send(formData);
    } catch (error) {
      console.error('Upload error:', error);
      alert('Upload error');
      setUploading(false);
    }
  };

  const formatSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  };

  return (
    <div className="file-upload">
      <h3>📤 Upload File</h3>

      <div
        className={`drop-zone ${uploading ? 'uploading' : ''}`}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onClick={() => document.getElementById('fileInput').click()}
        role="button"
        tabIndex={0}
        aria-label="Select file to upload"
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            document.getElementById('fileInput').click();
          }
        }}
      >
        <input
          id="fileInput"
          name="file"
          type="file"
          onChange={handleFileChange}
          style={{ display: 'none' }}
          aria-label="File input for upload"
        />

        {selectedFile ? (
          <div className="file-info">
            <p className="file-name">{selectedFile.name}</p>
            <p className="file-size">{formatSize(selectedFile.size)}</p>
          </div>
        ) : (
          <div className="drop-hint">
            <p>Drag & drop file here</p>
            <p>or click to select</p>
          </div>
        )}
      </div>

      {uploading && (
        <div className="progress-bar">
          <div className="progress-fill" style={{ width: `${progress}%` }}></div>
          <span className="progress-text">{progress}%</span>
        </div>
      )}

      <button
        className="btn btn-primary"
        onClick={handleUpload}
        disabled={!selectedFile || uploading}
      >
        {uploading ? 'Uploading...' : 'Upload'}
      </button>
    </div>
  );
}

export default FileUpload;
