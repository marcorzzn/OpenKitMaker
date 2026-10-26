// Implements: SCR-HOME / FUN-OFFLINE-MODE
import { mount } from 'svelte';
import App from './App.svelte';
import './index.css';

const target = document.getElementById('app');
if (target) {
  mount(App, { target });
}

// Register Workbox Service Worker for 100% Offline Autonomy (FUN-OFFLINE-MODE)
if (typeof window !== 'undefined' && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {
      // Service worker registration handled gracefully in preview sandbox
    });
  });
}
