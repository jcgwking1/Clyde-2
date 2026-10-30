import { createServer } from 'vite';
import React from 'react';
import ReactDOMServer from 'react-dom/server';

// Global browser mocks
const storage = {};
globalThis.localStorage = {
  getItem: (k) => storage[k] ?? null,
  setItem: (k, v) => { storage[k] = String(v); },
  removeItem: (k) => { delete storage[k]; },
  clear: () => { Object.keys(storage).forEach(k => delete storage[k]); }
};
globalThis.window = {
  scrollTo: () => {},
  clearTimeout: () => {},
  setTimeout: (fn) => setTimeout(fn, 0),
  location: { href: 'http://localhost:8443' },
  matchMedia: () => ({ matches: false, addListener: () => {}, removeListener: () => {} }),
};
globalThis.document = {
  body: { style: {} },
  getElementById: () => null,
};

async function test() {
  const server = await createServer({
    server: { port: 8446 },
    logLevel: 'error'
  });

  const AppMod = await server.ssrLoadModule('./src/App.tsx');
  const App = AppMod.default;

  console.log('--- TEST 1: Default / Clean localStorage ---');
  try {
    const html1 = ReactDOMServer.renderToString(React.createElement(App));
    console.log('Test 1 SUCCESS! Rendered HTML length:', html1.length);
  } catch (err) {
    console.error('Test 1 ERROR:', err.message, '\nStack:', err.stack);
  }

  console.log('\n--- TEST 2: Stale/corrupted profile { "confidence": 7 } ---');
  storage['ca-profile'] = '{"confidence":7}';
  try {
    const html2 = ReactDOMServer.renderToString(React.createElement(App));
    console.log('Test 2 SUCCESS! Rendered HTML length:', html2.length);
  } catch (err) {
    console.error('Test 2 ERROR:', err.message, '\nStack:', err.stack);
  }

  console.log('\n--- TEST 3: Stale profile { "isConfigured": true } ---');
  storage['ca-profile'] = '{"isConfigured":true}';
  try {
    const html3 = ReactDOMServer.renderToString(React.createElement(App));
    console.log('Test 3 SUCCESS! Rendered HTML length:', html3.length);
  } catch (err) {
    console.error('Test 3 ERROR:', err.message, '\nStack:', err.stack);
  }

  console.log('\n--- TEST 4: Null / empty ca-profile ---');
  storage['ca-profile'] = '';
  try {
    const html4 = ReactDOMServer.renderToString(React.createElement(App));
    console.log('Test 4 SUCCESS! Rendered HTML length:', html4.length);
  } catch (err) {
    console.error('Test 4 ERROR:', err.message, '\nStack:', err.stack);
  }

  await server.close();
}

test().catch(err => console.error('Fatal test error:', err));
