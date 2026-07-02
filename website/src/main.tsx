import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import AppLayout from './layouts/AppLayout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import GalleryPage from './pages/GalleryPage'
import LastMessagePage from './pages/LastMessagePage'
import LocationPage from './pages/LocationPage'
import SocialPage from './pages/SocialPage'
import AiPage from './pages/AiPage'
import SyncPage from './pages/SyncPage'
import AboutPage from './pages/AboutPage'
import BackupPage from './pages/BackupPage'
import './index.css'

// Protected Route Shield
const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const token = localStorage.getItem('token');
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return <AppLayout>{children}</AppLayout>;
};

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        
        {/* Protected routes */}
        <Route path="/" element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        } />
        
        <Route path="/gallery" element={
          <ProtectedRoute>
            <GalleryPage />
          </ProtectedRoute>
        } />

        <Route path="/last-message" element={
          <ProtectedRoute>
            <LastMessagePage />
          </ProtectedRoute>
        } />

        <Route path="/location" element={
          <ProtectedRoute>
            <LocationPage />
          </ProtectedRoute>
        } />

        <Route path="/social" element={
          <ProtectedRoute>
            <SocialPage />
          </ProtectedRoute>
        } />

        <Route path="/ai" element={
          <ProtectedRoute>
            <AiPage />
          </ProtectedRoute>
        } />

        <Route path="/sync" element={
          <ProtectedRoute>
            <SyncPage />
          </ProtectedRoute>
        } />

        <Route path="/about" element={
          <ProtectedRoute>
            <AboutPage />
          </ProtectedRoute>
        } />

        <Route path="/security" element={
          <ProtectedRoute>
            <BackupPage />
          </ProtectedRoute>
        } />

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>,
)
