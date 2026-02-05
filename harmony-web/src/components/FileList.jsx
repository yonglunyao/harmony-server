import React from 'react';
import './FileList.css';

const API_BASE = 'http://47.251.185.82:8877';

function FileList({ files, currentPath, onRefresh }) {
  const handleDownload = (filename) => {
    window.open(`${API_BASE}/api/file/download/${filename}`, '_blank');
  };

  const handleDelete = async (filename) => {
    if (!confirm(`Delete "${filename}"?`)) return;

    try {
      const response = await fetch(`${API_BASE}/api/file/delete/${filename}`, {
        method: 'POST'
      });
      const data = await response.json();

      if (data.success) {
        alert('Deleted successfully');
        onRefresh();
      } else {
        alert('Delete failed: ' + data.message);
      }
    } catch (error) {
      console.error('Delete error:', error);
      alert('Delete error');
    }
  };

  const handleCleanAll = async () => {
    if (!confirm('Delete all files? This action cannot be undone!')) return;

    try {
      const response = await fetch(`${API_BASE}/api/file/clean`, {
        method: 'POST'
      });
      const data = await response.json();

      if (data.success) {
        alert(`Cleared ${data.deletedFiles} files, ${data.deletedDirectories} directories`);
        onRefresh();
      } else {
        alert('Clean failed: ' + data.message);
      }
    } catch (error) {
      console.error('Clean error:', error);
      alert('Clean error');
    }
  };

  const formatSize = (bytes) => {
    if (!bytes) return '-';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  };

  return (
    <div className="file-list">
      <div className="file-list-header">
        <h3>📋 Files ({files.length})</h3>
        <button className="btn btn-danger" onClick={handleCleanAll}>
          🧹 Clean All
        </button>
      </div>

      {files.length === 0 ? (
        <div className="empty-state">
          <p>No files found</p>
        </div>
      ) : (
        <div className="file-list-content">
          <table className="file-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Size</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {files.map((file, index) => (
                <tr key={index}>
                  <td className="file-name">📄 {file}</td>
                  <td className="file-size">{formatSize(0)}</td>
                  <td className="file-actions">
                    <button
                      className="btn-icon btn-download"
                      onClick={() => handleDownload(file)}
                      title="Download"
                    >
                      ⬇️
                    </button>
                    <button
                      className="btn-icon btn-delete"
                      onClick={() => handleDelete(file)}
                      title="Delete"
                    >
                      🗑️
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default FileList;
