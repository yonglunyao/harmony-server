import React, { useState, useEffect } from 'react';
import './DirectoryTree.css';

const API_BASE = 'http://47.251.185.82:8877';

function DirectoryTree({ currentPath, onPathChange, onRefresh }) {
  const [pathInput, setPathInput] = useState(currentPath);
  const [pathFiles, setPathFiles] = useState([]);

  useEffect(() => {
    setPathInput(currentPath);
    loadPathFiles(currentPath);
  }, [currentPath]);

  const loadPathFiles = async (path) => {
    try {
      const response = await fetch(`${API_BASE}/api/file/list/path?path=${encodeURIComponent(path)}`);
      const data = await response.json();

      if (data.success) {
        setPathFiles(data.files || []);
      }
    } catch (error) {
      console.error('Failed to load path:', error);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onPathChange(pathInput);
    loadPathFiles(pathInput);
  };

  return (
    <div className="directory-tree">
      <h3>📁 Directory Browser</h3>

      <form className="path-form" onSubmit={handleSubmit}>
        <label htmlFor="pathInput" className="visually-hidden">
          Directory path
        </label>
        <input
          id="pathInput"
          name="path"
          type="text"
          value={pathInput}
          onChange={(e) => setPathInput(e.target.value)}
          placeholder="Enter path (e.g., . or subfolder)"
          className="path-input"
          aria-label="Directory path to browse"
        />
        <button type="submit" className="btn btn-secondary" aria-label="Browse directory">
          Browse
        </button>
      </form>

      <div className="path-files">
        <h4>Contents ({pathFiles.length})</h4>
        {pathFiles.length === 0 ? (
          <p className="empty">Empty directory</p>
        ) : (
          <ul className="file-list-ul">
            {pathFiles.map((file, index) => (
              <li key={index} className={file.isDirectory ? 'dir-item' : 'file-item'}>
                <span className="icon">{file.isDirectory ? '📁' : '📄'}</span>
                <span className="name">{file.name}</span>
                {!file.isDirectory && (
                  <span className="size">({Math.round(file.size / 1024)} KB)</span>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

export default DirectoryTree;
