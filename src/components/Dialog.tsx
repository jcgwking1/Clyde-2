import { useEffect, useId, useRef, type ReactNode } from 'react';
import Icon from '../icons';
import { cn } from './cn';

export type DialogProps = {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: ReactNode;
  wide?: boolean;
  alert?: boolean;
  closable?: boolean;
};

/** Native dialogs provide focus containment, background inertness, and focus restoration. */
export default function Dialog({
  open,
  onClose,
  title,
  description,
  children,
  wide = false,
  alert = false,
  closable = true,
}: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  const descriptionId = useId();

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => { document.body.style.overflow = previousOverflow; };
  }, [open]);

  return (
    <dialog
      ref={ref}
      className={cn('advisor-dialog', wide && 'advisor-dialog-wide')}
      role={alert ? 'alertdialog' : 'dialog'}
      aria-labelledby={titleId}
      aria-describedby={description ? descriptionId : undefined}
      onCancel={event => {
        event.preventDefault();
        if (closable) onClose();
      }}
      onClick={event => {
        if (!closable || alert || event.target !== event.currentTarget) return;
        const rect = event.currentTarget.getBoundingClientRect();
        if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) {
          onClose();
        }
      }}
    >
      <div className="advisor-dialog-heading">
        <div>
          <h2 id={titleId}>{title}</h2>
          {description && <p id={descriptionId}>{description}</p>}
        </div>
        {closable && (
          <button type="button" className="icon-button" onClick={onClose} aria-label="Close dialog">
            <Icon name="close" size={20} />
          </button>
        )}
      </div>
      {children}
    </dialog>
  );
}
