import React, { useState, useEffect } from 'react';
import './App.css';
import FileUpload from './components/FileUpload';
import LogReport from './components/LogReport';
import DirectoryTree from './components/DirectoryTree';
import FileList from './components/FileList';

const API_BASE = 'http://47.251.185.82:8877';

function App() {
  const [files, setFiles] = useState([]);
  const [currentPath, setCurrentPath] = useState('.');
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const refreshFiles = () => {
    setRefreshTrigger(prev => prev + 1);
  };

  useEffect(() => {
    fetch(`${API_BASE}/api/file/list`)
      .then(res => res.json())
      .then(data => {
        if (data.success) {
          setFiles(data.files || []);
        }
      })
      .catch(err => console.error('Failed to load files:', err));
  }, [refreshTrigger]);

  return (
    <div className="app">
      <header className="app-header">
        <h1>🗂️ Harmony File Server</h1>
        <p className="subtitle">File Management Service</p>
      </header>

      <main className="app-main">
        <section className="sidebar">
          <FileUpload onUploadSuccess={refreshFiles} />
          <LogReport />
        </section>

        <section className="content">
          <DirectoryTree
            currentPath={currentPath}
            onPathChange={setCurrentPath}
            onRefresh={refreshFiles}
          />
          <FileList
            files={files}
            currentPath={currentPath}
            onRefresh={refreshFiles}
          />
        </section>
      </main>
    </div>
  );
}

export default App;
