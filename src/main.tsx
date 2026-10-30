import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './index.css'

class ErrorBoundary extends React.Component<
  { children: React.ReactNode },
  { hasError: boolean; error: Error | null }
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('CourseAdvisor caught unhandled error:', error, errorInfo);
  }

  handleReset = () => {
    localStorage.clear();
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: '#f8fafc',
          padding: '24px',
          fontFamily: 'system-ui, -apple-system, sans-serif'
        }}>
          <div style={{
            maxWidth: '560px',
            width: '100%',
            background: '#ffffff',
            borderRadius: '12px',
            padding: '28px',
            boxShadow: '0 20px 40px rgba(0,0,0,0.1)',
            border: '1px solid #fed7d7'
          }}>
            <h2 style={{ color: '#c53030', margin: '0 0 12px 0', fontSize: '20px' }}>
              Something interrupted Course Advisor
            </h2>
            <p style={{ color: '#4a5568', fontSize: '14px', lineHeight: '1.6', margin: '0 0 16px 0' }}>
              An error occurred while restoring your session. You can clear temporary local data to start fresh with the official prospectus.
            </p>
            <pre style={{
              background: '#fff5f5',
              padding: '12px',
              borderRadius: '6px',
              fontSize: '12px',
              color: '#9b2c2c',
              overflowX: 'auto',
              marginBottom: '20px'
            }}>
              {this.state.error?.message || 'Unknown error'}
            </pre>
            <div style={{ display: 'flex', gap: '12px' }}>
              <button
                onClick={this.handleReset}
                style={{
                  background: '#047857',
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '6px',
                  padding: '10px 18px',
                  fontWeight: 600,
                  fontSize: '14px',
                  cursor: 'pointer'
                }}
              >
                Reset & Open Fresh
              </button>
              <button
                onClick={() => window.location.reload()}
                style={{
                  background: '#f1f5f9',
                  color: '#334155',
                  border: '1px solid #cbd5e1',
                  borderRadius: '6px',
                  padding: '10px 18px',
                  fontWeight: 500,
                  fontSize: '14px',
                  cursor: 'pointer'
                }}
              >
                Reload Page
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </React.StrictMode>,
)
