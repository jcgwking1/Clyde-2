import React, { useState, useRef, useEffect } from 'react';
import Icon from '../icons';

export interface InfoTooltipProps {
  content: React.ReactNode;
  label?: string;
  position?: 'top' | 'bottom';
  className?: string;
}

export default function InfoTooltip({
  content,
  label = 'More information',
  position = 'top',
  className = '',
}: InfoTooltipProps) {
  const [active, setActive] = useState(false);
  const containerRef = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setActive(false);
      }
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') setActive(false);
    }
    if (active) {
      document.addEventListener('mousedown', handleClickOutside);
      document.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [active]);

  return (
    <span
      ref={containerRef}
      className={`info-tooltip-container ${className} ${active ? 'is-active' : ''}`}
      onMouseEnter={() => setActive(true)}
      onMouseLeave={() => setActive(false)}
    >
      <button
        type="button"
        className="info-tooltip-trigger"
        aria-label={label}
        onClick={e => {
          e.preventDefault();
          e.stopPropagation();
          setActive(prev => !prev);
        }}
        onFocus={() => setActive(true)}
        onBlur={() => setActive(false)}
      >
        <Icon name="info" size={13} />
      </button>

      {active && (
        <span
          role="tooltip"
          className={`info-tooltip-bubble position-${position}`}
          onClick={e => e.stopPropagation()}
        >
          {content}
          <span className="info-tooltip-arrow" />
        </span>
      )}
    </span>
  );
}

