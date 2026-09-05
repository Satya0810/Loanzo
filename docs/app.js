/* ==========================================================================
   LOANZO DOCUMENTATION & SHOWCASE - INTERACTIVE LOGIC (app.js)
   LIGHT THEME DEFAULT WITH DYNAMIC CHARTS & THEME TOGGLE
   ========================================================================== */

let chartInstances = [];

document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  initCharts();
  initSimulator();
  initWorkflowStepper();
  initGalleryFilter();
  initLightbox();
  initSmoothScroll();
});

/* ==========================================================================
   0. Theme Initializer & Toggle
   ========================================================================== */
function initTheme() {
  const savedTheme = localStorage.getItem('loanzo_theme') || 'light';
  document.documentElement.setAttribute('data-theme', savedTheme);
  updateThemeButton(savedTheme);

  const toggleBtn = document.getElementById('themeToggleBtn');
  if (toggleBtn) {
    toggleBtn.addEventListener('click', () => {
      const current = document.documentElement.getAttribute('data-theme') || 'light';
      const nextTheme = current === 'light' ? 'dark' : 'light';
      document.documentElement.setAttribute('data-theme', nextTheme);
      localStorage.setItem('loanzo_theme', nextTheme);
      updateThemeButton(nextTheme);

      // Recreate charts with appropriate contrast palette
      rebuildCharts(nextTheme);
    });
  }
}

function updateThemeButton(theme) {
  const toggleBtn = document.getElementById('themeToggleBtn');
  if (!toggleBtn) return;
  if (theme === 'light') {
    toggleBtn.innerHTML = '<span>🌙 Dark Mode</span>';
  } else {
    toggleBtn.innerHTML = '<span>☀️ Light Mode</span>';
  }
}

function rebuildCharts(theme) {
  chartInstances.forEach(c => c.destroy());
  chartInstances = [];
  initCharts(theme);
}

/* ==========================================================================
   1. Dynamic Charts (Chart.js)
   ========================================================================== */
function initCharts(theme = 'light') {
  if (typeof Chart === 'undefined') return;

  const isLight = (theme === 'light');
  const textColor = isLight ? '#334155' : '#94A3B8';
  const gridColor = isLight ? 'rgba(0, 0, 0, 0.06)' : 'rgba(255, 255, 255, 0.06)';
  const tooltipBg = isLight ? 'rgba(15, 23, 42, 0.95)' : 'rgba(7, 11, 18, 0.95)';

  Chart.defaults.color = textColor;
  Chart.defaults.font.family = "'Plus Jakarta Sans', sans-serif";
  Chart.defaults.plugins.tooltip.backgroundColor = tooltipBg;
  Chart.defaults.plugins.tooltip.borderColor = isLight ? '#CBD5E1' : 'rgba(212, 175, 55, 0.3)';
  Chart.defaults.plugins.tooltip.borderWidth = 1;
  Chart.defaults.plugins.tooltip.padding = 10;

  // 1. TAM & MSME Credit Deficit in India
  const ctxTam = document.getElementById('tamChart');
  if (ctxTam) {
    const c1 = new Chart(ctxTam, {
      type: 'bar',
      data: {
        labels: ['2020', '2021', '2022', '2023', '2024', '2025 (Est)'],
        datasets: [
          {
            label: 'Unmet MSME Credit Gap (₹ Lakh Cr)',
            data: [2.10, 2.55, 2.98, 3.45, 3.99, 4.60],
            backgroundColor: isLight ? 'rgba(217, 119, 6, 0.82)' : 'rgba(212, 175, 55, 0.75)',
            borderColor: isLight ? '#B45309' : '#D4AF37',
            borderWidth: 1.5,
            borderRadius: 6
          },
          {
            type: 'line',
            label: 'Formal Bank Reach %',
            data: [14, 16, 17.5, 18.2, 19.5, 21.0],
            borderColor: '#0284C7',
            backgroundColor: 'rgba(2, 132, 199, 0.12)',
            tension: 0.35,
            fill: true,
            yAxisID: 'y1'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            grid: { color: gridColor },
            title: { display: true, text: '₹ Lakh Crore', color: textColor }
          },
          y1: {
            position: 'right',
            grid: { drawOnChartArea: false },
            title: { display: true, text: 'Formal Reach %', color: '#0284C7' }
          },
          x: { grid: { color: gridColor } }
        }
      }
    });
    chartInstances.push(c1);
  }

  // 2. Delinquency Rates: Unsecured Apps vs Loanzo Milestone Escrow
  const ctxDelinq = document.getElementById('delinquencyChart');
  if (ctxDelinq) {
    const c2 = new Chart(ctxDelinq, {
      type: 'line',
      data: {
        labels: ['Month 1', 'Month 3', 'Month 6', 'Month 9', 'Month 12'],
        datasets: [
          {
            label: 'Unsecured Digital Apps (Default %)',
            data: [2.4, 5.8, 9.6, 12.8, 14.2],
            borderColor: '#DC2626',
            backgroundColor: 'rgba(220, 38, 38, 0.12)',
            borderWidth: 2.5,
            tension: 0.3,
            fill: true
          },
          {
            label: 'Loanzo Purpose-Bound Tranches (%)',
            data: [0.3, 0.6, 1.1, 1.4, 1.78],
            borderColor: '#059669',
            backgroundColor: 'rgba(5, 150, 105, 0.15)',
            borderWidth: 2.5,
            tension: 0.3,
            fill: true
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            grid: { color: gridColor },
            title: { display: true, text: 'NPA Default Rate %', color: textColor }
          },
          x: { grid: { color: gridColor } }
        }
      }
    });
    chartInstances.push(c2);
  }

  // 3. Multi-Asset Collateral Escrow Distribution (Doughnut)
  const ctxCollateral = document.getElementById('collateralChart');
  if (ctxCollateral) {
    const c3 = new Chart(ctxCollateral, {
      type: 'doughnut',
      data: {
        labels: ['Gold Bullion (38%)', 'Property Deeds (26%)', 'IT Equipment (18%)', 'Commercial Vehicles (11%)', 'Trade Invoices (7%)'],
        datasets: [{
          data: [38, 26, 18, 11, 7],
          backgroundColor: [
            '#D97706', // Amber Gold
            '#0284C7', // Sky Blue
            '#7C3AED', // Purple
            '#059669', // Emerald
            '#F59E0B'  // Yellow Amber
          ],
          borderColor: isLight ? '#FFFFFF' : '#070B12',
          borderWidth: 3
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { boxWidth: 12, padding: 14, color: textColor } }
        },
        cutout: '68%'
      }
    });
    chartInstances.push(c3);
  }

  // 4. Legal Recovery Speed (Days to Settlement)
  const ctxRecovery = document.getElementById('recoveryChart');
  if (ctxRecovery) {
    const c4 = new Chart(ctxRecovery, {
      type: 'bar',
      data: {
        labels: ['Civil Court Suit', 'DRT Tribunal', 'Arbitration', 'Loanzo (Order 37 + Escrow)'],
        datasets: [{
          label: 'Days to Recovery Resolution',
          data: [1095, 730, 365, 21],
          backgroundColor: [
            'rgba(220, 38, 38, 0.75)',
            'rgba(217, 119, 6, 0.75)',
            'rgba(2, 132, 199, 0.75)',
            'rgba(5, 150, 105, 0.85)'
          ],
          borderColor: ['#DC2626', '#D97706', '#0284C7', '#059669'],
          borderWidth: 1.5,
          borderRadius: 6
        }]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            grid: { color: gridColor },
            title: { display: true, text: 'Average Days', color: textColor }
          },
          y: { grid: { color: gridColor } }
        },
        plugins: { legend: { display: false } }
      }
    });
    chartInstances.push(c4);
  }
}

/* ==========================================================================
   2. Interactive Collateral & LTV Simulator
   ========================================================================== */
const ASSET_LTV_CAPS = {
  GOLD: 0.75,
  PROPERTY: 0.65,
  EQUIPMENT: 0.60,
  VEHICLE: 0.70,
  INVOICE: 0.80
};

let currentAssetType = 'GOLD';

function initSimulator() {
  const assetChips = document.querySelectorAll('.sim-chip');
  const valSlider = document.getElementById('simValSlider');
  const loanSlider = document.getElementById('simLoanSlider');

  const valDisplay = document.getElementById('simValText');
  const loanDisplay = document.getElementById('simLoanText');
  const ltvPercentage = document.getElementById('simLtvValue');
  const healthBadge = document.getElementById('simHealthBadge');
  const maxBorrowDisplay = document.getElementById('simMaxBorrow');
  const emiDisplay = document.getElementById('simEmiValue');

  const tranche1Val = document.getElementById('tranche1Val');
  const tranche2Val = document.getElementById('tranche2Val');
  const tranche3Val = document.getElementById('tranche3Val');

  function updateCalculations() {
    if (!valSlider || !loanSlider) return;
    const assetVal = parseInt(valSlider.value, 10);
    const loanReq = parseInt(loanSlider.value, 10);

    valDisplay.textContent = '₹' + assetVal.toLocaleString('en-IN');
    loanDisplay.textContent = '₹' + loanReq.toLocaleString('en-IN');

    const ltvRatio = ((loanReq / assetVal) * 100);
    ltvPercentage.textContent = ltvRatio.toFixed(1) + '%';

    const maxBorrow = assetVal * ASSET_LTV_CAPS[currentAssetType];
    maxBorrowDisplay.textContent = '₹' + Math.round(maxBorrow).toLocaleString('en-IN');

    // Monthly EMI estimation (12 months @ 12% p.a. reducing balance)
    const annualRate = 0.12;
    const monthlyRate = annualRate / 12;
    const months = 12;
    const emi = (loanReq * monthlyRate * Math.pow(1 + monthlyRate, months)) / (Math.pow(1 + monthlyRate, months) - 1);
    emiDisplay.textContent = '₹' + Math.round(emi).toLocaleString('en-IN') + '/mo';

    // Milestone Tranches
    tranche1Val.textContent = '₹' + Math.round(loanReq * 0.40).toLocaleString('en-IN');
    tranche2Val.textContent = '₹' + Math.round(loanReq * 0.35).toLocaleString('en-IN');
    tranche3Val.textContent = '₹' + Math.round(loanReq * 0.25).toLocaleString('en-IN');

    // Health badge styling
    const maxLtvPct = ASSET_LTV_CAPS[currentAssetType] * 100;
    if (ltvRatio <= maxLtvPct * 0.85) {
      healthBadge.textContent = '● LTV HEALTHY (SECURED)';
      healthBadge.className = 'sim-health-indicator health-healthy';
      ltvPercentage.style.color = 'var(--emerald-accent)';
    } else if (ltvRatio <= maxLtvPct) {
      healthBadge.textContent = '▲ MODERATE RISK (AT CAP)';
      healthBadge.className = 'sim-health-indicator health-warning';
      ltvPercentage.style.color = 'var(--gold-primary)';
    } else {
      healthBadge.textContent = '✕ LTV EXCEEDED (INELIGIBLE)';
      healthBadge.className = 'sim-health-indicator health-danger';
      ltvPercentage.style.color = 'var(--crimson-accent)';
    }
  }

  assetChips.forEach(chip => {
    chip.addEventListener('click', () => {
      assetChips.forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      currentAssetType = chip.getAttribute('data-asset');
      updateCalculations();
    });
  });

  if (valSlider && loanSlider) {
    valSlider.addEventListener('input', updateCalculations);
    loanSlider.addEventListener('input', updateCalculations);
    updateCalculations();
  }
}

/* ==========================================================================
   3. Interactive Workflow Stepper
   ========================================================================== */
const WORKFLOW_STEPS = [
  {
    title: 'Biometric KYC & Aadhaar e-Sign',
    desc: 'The borrower or agent logs in securely via hardware-backed Biometrics and DigiLocker integration. Automated SHA-256 fingerprint hash ensures identity verification with zero forged accounts.',
    specs: [
      'DigiLocker Aadhaar e-KYC Verification',
      'BiometricPrompt API with StrongBox KeyStore',
      'Automated Sanction List & Anti-Money Laundering (AML) checks'
    ]
  },
  {
    title: 'Multi-Asset Collateral Escrow Pledge',
    desc: 'Borrower pledges certified physical or digital assets (Gold, Property, IT Equipment, Vehicles, or Trade Invoices). Real-time LTV engine enforces safety ceilings before loan creation.',
    specs: [
      'Deterministic Room v12 Collateral Vault Schema',
      'Automated LTV Ceiling Validation (60% - 80%)',
      'Real-time Market Valuation Re-indexing'
    ]
  },
  {
    title: 'Social Marketplace Bidding & Syndication',
    desc: 'The loan request is published to the decentralized community feed where verified peer lenders bid transparently on portions or the entirety of the purpose-bound loan.',
    specs: [
      'Unidirectional Data Flow (UDF) Jetpack Compose Marketplace',
      'Atomic Firestore Cloud Sync for Multi-Lender Syndication',
      'Zero predatory middleman markups'
    ]
  },
  {
    title: 'Certified Field Agent Physical Inspection',
    desc: 'A certified local community agent visits the borrower site to physically verify the asset, machine equipment, and vendor invoices before milestone activation.',
    specs: [
      'Offline-First Inspection Form with Geo-tagging',
      'Agent Inspection Checklist saved to Room v12',
      'Cryptographic agent signature timestamped on audit log'
    ]
  },
  {
    title: 'Purpose-Bound Milestone Tranche Disbursement',
    desc: 'Capital is released in verifiable tranches directly to suppliers rather than lump sums: Phase 1 Raw Materials (40%), Phase 2 Machining (35%), Phase 3 Distribution (25%).',
    specs: [
      'Direct vendor escrow settlement (Anti-Ghost Lending)',
      'Proof-of-work receipt upload per milestone',
      'Automated Telegram Alert Desk notification to all parties'
    ]
  },
  {
    title: 'Automated UPI Repayments & Escrow Release',
    desc: 'Borrower pays monthly EMIs via instant UPI deep links. Upon full loan liquidation, the collateral is automatically released with a digital certificate of discharge.',
    specs: [
      'Dynamic UPI Intent QR Generation (GPay, PhonePe, Paytm)',
      'Section 4 NI Act digital discharge certificate',
      'Collateral Vault unlocks instantly in Room & Firestore'
    ]
  }
];

function initWorkflowStepper() {
  const tabs = document.querySelectorAll('.step-tab');
  const titleEl = document.getElementById('stepTitle');
  const descEl = document.getElementById('stepDesc');
  const specsEl = document.getElementById('stepSpecs');

  if (!tabs.length || !titleEl) return;

  tabs.forEach((tab, index) => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');

      const data = WORKFLOW_STEPS[index];
      titleEl.textContent = data.title;
      descEl.textContent = data.desc;

      specsEl.innerHTML = data.specs.map(s => `
        <div class="tech-spec-item">
          <span style="color: var(--emerald-accent); font-weight: bold;">✔</span>
          <span>${s}</span>
        </div>
      `).join('');
    });
  });
}

/* ==========================================================================
   4. Screenshots Gallery Filter & Lightbox
   ========================================================================== */
function initGalleryFilter() {
  const filterBtns = document.querySelectorAll('.filter-btn');
  const cards = document.querySelectorAll('.screen-card');

  filterBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      filterBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');

      const filter = btn.getAttribute('data-filter');
      cards.forEach(card => {
        if (filter === 'all' || card.getAttribute('data-category').includes(filter)) {
          card.style.display = 'block';
        } else {
          card.style.display = 'none';
        }
      });
    });
  });
}

const SCREEN_DETAILS = {
  'screen_login_auth.png': {
    title: 'Biometric & Role-Based Authentication',
    role: 'Security & Access Layer',
    desc: 'Multi-role switcher (Member, Agent, Admin) integrated with hardware-backed Android BiometricPrompt and SHA-256 password hashing. Built with Jetpack Compose Material 3.',
    file: 'ui/auth/LoginScreen.kt'
  },
  'screen_home_dashboard.png': {
    title: 'Consumer Portfolio Dashboard',
    role: 'Borrower & Lender Experience',
    desc: 'Shows active borrowed capital, circular repayment progress, upcoming milestone EMIs, and quick actions for capital requests.',
    file: 'ui/dashboard/DashboardScreen.kt'
  },
  'screen_radial_menu.png': {
    title: 'Radial Action Command Wheel',
    role: 'Ergonomic Navigation',
    desc: 'Gesture-driven floating radial menu providing instantaneous access to Escrow Vault, Tranche Requests, and Community Wall.',
    file: 'ui/dashboard/HomeActionSheets.kt'
  },
  'screen_vault_modal.png': {
    title: 'Multi-Asset Collateral Escrow Vault',
    role: 'Risk Management Engine',
    desc: 'Appraisal sheet displaying real-time LTV valuation, safety thresholds, and legal lock status across 5 supported collateral classes.',
    file: 'ui/loan/CollateralVaultModal.kt'
  },
  'screen_loan_agreement.png': {
    title: 'Section 4 NI Act Promissory Agreement',
    role: 'Legal Compliance Architecture',
    desc: 'Legally admissible digital promissory note binding borrower and lender under Section 4 Negotiable Instruments Act 1881, backed by DigiLocker Aadhaar e-Sign.',
    file: 'ui/loan/LoanAgreementSheet.kt'
  },
  'screen_upi_qr.png': {
    title: 'Dynamic Intent UPI QR Payment Gateway',
    role: 'Instant Settlement Engine',
    desc: 'Native UPI intent invocation supporting Google Pay, PhonePe, and BHIM with dynamic QR code rendering and offline transaction fallback.',
    file: 'ui/loan/UpiPaymentSheet.kt'
  },
  'screen_bank_accounts.png': {
    title: 'Multi-Bank & Tokenized Virtual Card',
    role: 'Banking Integration',
    desc: 'Direct account-to-account settlement compliant with RBI circular RBI/2023-24/53. Prevents pooled intermediary fund risks.',
    file: 'ui/dashboard/BankAccountsSheet.kt'
  },
  'screen_activity_alerts.png': {
    title: 'Audit Feed & Telegram Alert Desk',
    role: 'Transparency & Field Recovery',
    desc: 'Immutable chronological event ledger tracking milestone unlocks, agent visits, and automated Telegram bot dispatches.',
    file: 'ui/dashboard/ActivityAlertsSheet.kt'
  }
};

function initLightbox() {
  const modal = document.getElementById('lightboxModal');
  const modalImg = document.getElementById('lightboxImg');
  const modalTitle = document.getElementById('lightboxTitle');
  const modalRole = document.getElementById('lightboxRole');
  const modalDesc = document.getElementById('lightboxDesc');
  const modalFile = document.getElementById('lightboxFile');
  const closeBtn = document.getElementById('lightboxClose');

  if (!modal) return;

  document.querySelectorAll('.screen-card').forEach(card => {
    card.addEventListener('click', (e) => {
      e.stopPropagation();
      const filename = card.getAttribute('data-img');
      const info = SCREEN_DETAILS[filename] || {
        title: 'Loanzo Interface',
        role: 'Android UI',
        desc: 'Production Jetpack Compose screen.',
        file: 'app/src/main/java/com/loanzo/app'
      };

      modalImg.src = 'screenshots/' + filename;
      modalTitle.textContent = info.title;
      modalRole.textContent = info.role;
      modalDesc.textContent = info.desc;
      modalFile.textContent = 'Source: com.loanzo.app.' + info.file;

      modal.classList.add('open');
    });
  });

  if (closeBtn) {
    closeBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      modal.classList.remove('open');
    });
  }
  
  modal.addEventListener('click', (e) => {
    if (e.target === modal) modal.classList.remove('open');
  });
  
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') modal.classList.remove('open');
  });
}

/* ==========================================================================
   5. Smooth Scroll & Navbar Spy
   ========================================================================== */
function initSmoothScroll() {
  const navLinks = document.querySelectorAll('.nav-links a');
  window.addEventListener('scroll', () => {
    const scrollPos = window.scrollY + 100;
    navLinks.forEach(link => {
      const href = link.getAttribute('href');
      if (href && href.startsWith('#')) {
        const section = document.querySelector(href);
        if (section) {
          if (section.offsetTop <= scrollPos && section.offsetTop + section.offsetHeight > scrollPos) {
            link.classList.add('active');
          } else {
            link.classList.remove('active');
          }
        }
      }
    });
  });
}
