/* LinkVault — Main JavaScript */

'use strict';

// ─── Settings Panel Toggle ────────────────────────────────────────────────────

function toggleSettings() {
    const panel = document.getElementById('settingsPanel');
    const arrow = document.getElementById('settingsArrow');
    if (!panel) return;
    const isOpen = panel.classList.contains('open');
    panel.classList.toggle('open', !isOpen);
    if (arrow) arrow.classList.toggle('open', !isOpen);
}

// ─── Password Field Toggle ────────────────────────────────────────────────────

function togglePasswordField(enabled) {
    const row = document.getElementById('passwordRow');
    const input = document.getElementById('linkPassword');
    if (!row) return;
    row.style.display = enabled ? 'block' : 'none';
    if (!enabled && input) input.value = '';
    // Auto-open settings panel if toggling on
    if (enabled) {
        const panel = document.getElementById('settingsPanel');
        if (panel && !panel.classList.contains('open')) toggleSettings();
    }
}

// ─── Password Visibility Toggle ───────────────────────────────────────────────

function togglePwdVisibility() {
    const input = document.getElementById('linkPassword');
    if (input) input.type = input.type === 'password' ? 'text' : 'password';
}

function togglePwdVisibilityById(inputId, btn) {
    const input = document.getElementById(inputId);
    if (!input) return;
    input.type = input.type === 'password' ? 'text' : 'password';
    btn.textContent = input.type === 'password' ? '👁' : '🙈';
}

// ─── Copy to Clipboard ────────────────────────────────────────────────────────

function copyToClipboard(btn) {
    const text = btn.dataset.copy;
    if (!text) return;
    navigator.clipboard.writeText(text).then(() => {
        const original = btn.textContent;
        btn.textContent = '✓ Copied!';
        btn.style.color = '#10b981';
        setTimeout(() => {
            btn.textContent = original;
            btn.style.color = '';
        }, 2000);
    }).catch(() => {
        // Fallback for older browsers
        const ta = document.createElement('textarea');
        ta.value = text;
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
    });
}

// ─── Short Code Availability Check ───────────────────────────────────────────

let codeCheckTimeout = null;

function checkCodeAvailability(value) {
    const statusEl = document.getElementById('codeStatus');
    if (!statusEl) return;

    clearTimeout(codeCheckTimeout);

    if (!value || value.length < 3) {
        statusEl.textContent = '';
        statusEl.className = 'code-status';
        return;
    }

    statusEl.textContent = 'Checking...';
    statusEl.className = 'code-status';

    codeCheckTimeout = setTimeout(async () => {
        try {
            const res = await fetch(`/check-code?code=${encodeURIComponent(value)}`);
            const data = await res.json();
            statusEl.textContent = data.message;
            statusEl.className = `code-status ${data.available ? 'available' : 'taken'}`;
        } catch (err) {
            statusEl.textContent = '';
        }
    }, 400);
}

// ─── Form Submission Loading State ───────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('shortenForm');
    const btn  = document.getElementById('submitBtn');

    if (form && btn) {
        form.addEventListener('submit', () => {
            btn.disabled = true;
            btn.querySelector('.btn-text').textContent = 'Shortening...';
            btn.querySelector('.btn-icon').textContent = '⟳';
        });
    }

    // Auto-open settings panel if there were validation errors for settings fields
    const customCode = document.getElementById('customCode');
    if (customCode && customCode.value.trim().length > 0) {
        const panel = document.getElementById('settingsPanel');
        if (panel) panel.classList.add('open');
        const arrow = document.getElementById('settingsArrow');
        if (arrow) arrow.classList.add('open');
    }

    // Auto-dismiss success banner after 8 seconds
    const banner = document.getElementById('successBanner');
    if (banner) {
        setTimeout(() => {
            banner.style.transition = 'opacity 0.5s ease';
            banner.style.opacity = '0';
            setTimeout(() => banner.remove(), 500);
        }, 8000);
    }

    // Restore password toggle if form was submitted with password protection checked
    const pwdToggle = document.getElementById('passwordToggle');
    if (pwdToggle && pwdToggle.checked) {
        togglePasswordField(true);
    }
});
