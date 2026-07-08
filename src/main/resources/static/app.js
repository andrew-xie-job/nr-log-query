const $ = (id) => document.getElementById(id);
let accountsData = null;

async function loadAccounts() {
  try {
    const res = await fetch('/api/accounts');
    accountsData = await res.json();
    const sel = $('account');
    sel.innerHTML = '';
    const accounts = accountsData.accounts || [];
    accounts.forEach(a => {
      const opt = document.createElement('option');
      opt.value = a.id;
      opt.textContent = `${a.name} (${a.id})`;
      sel.appendChild(opt);
    });
    if (accounts.length === 0) {
      const opt = document.createElement('option');
      opt.value = ''; opt.textContent = 'No accounts found';
      sel.appendChild(opt);
    }
  } catch (e) {
    accountsData = { error: String(e) };
  }
  applyMode();
}

function applyMode() {
  const mode = $('mode').value;
  const english = mode === 'english';
  $('englishRow').hidden = !english;
  if (english) {
    // NRQL box appears only after a translate
    $('nrqlRow').hidden = true;
  } else {
    // NRQL mode: show the editor immediately for direct typing
    $('nrqlRow').hidden = false;
    $('explanation').textContent = '';
  }
  updateHint();
}

function updateHint() {
  const mode = $('mode').value;
  const hint = $('hint');
  if (accountsData && accountsData.error) { hint.textContent = accountsData.error; return; }
  if (mode === 'english' && accountsData && accountsData.translationAvailable === false) {
    hint.textContent = 'English mode needs a running model (Ollama) or an API key. ' +
      'Or switch Mode to "NRQL" to type a query directly.';
  } else if (mode === 'nrql') {
    hint.textContent = 'Type NRQL and press Run.';
  } else {
    hint.textContent = 'Type a question and press Translate. You can edit the NRQL before running.';
  }
}

function currentAccountId() {
  const v = $('account').value;
  return v ? Number(v) : null;
}

function setBusy(busy) {
  $('translateBtn').disabled = busy;
  $('runBtn').disabled = busy;
}

function showError(msg) { $('status').innerHTML = '<span class="error">' + msg + '</span>'; }
function clearStatus() { $('status').innerHTML = ''; }

// English -> NRQL (translate only, then let the user edit)
$('searchForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  if ($('mode').value !== 'english') return;
  const text = $('question').value.trim();
  if (!text) return;
  setBusy(true);
  $('status').innerHTML = '<span class="loading">Translating…</span>';
  $('results').innerHTML = '';
  try {
    const res = await fetch('/api/translate', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text })
    });
    const data = await res.json();
    if (!res.ok || data.error) { showError(data.error || 'Translation failed'); return; }
    clearStatus();
    $('nrql').value = data.nrql || '';
    $('explanation').textContent = data.explanation || '';
    $('nrqlRow').hidden = false;
    $('nrql').focus();
  } catch (err) {
    showError(String(err));
  } finally {
    setBusy(false);
  }
});

// Run the (possibly edited) NRQL
$('runBtn').addEventListener('click', async () => {
  const accountId = currentAccountId();
  if (accountId === null) { showError('Pick an account first.'); return; }
  const nrql = $('nrql').value.trim();
  if (!nrql) { showError('NRQL is empty.'); return; }
  setBusy(true);
  $('status').innerHTML = '<span class="loading">Running…</span>';
  $('results').innerHTML = '';
  try {
    const res = await fetch('/api/query', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ accountId, text: nrql, raw: true })
    });
    const data = await res.json();
    if (!res.ok || data.error) { showError(data.error || 'Query failed'); return; }
    clearStatus();
    renderTable(data);
  } catch (err) {
    showError(String(err));
  } finally {
    setBusy(false);
  }
});

$('mode').addEventListener('change', () => { $('results').innerHTML = ''; clearStatus(); applyMode(); });

function renderTable(result) {
  const box = $('results');
  box.innerHTML = '';
  const cols = result.columns || [];
  const rows = result.rows || [];
  if (rows.length === 0) { box.innerHTML = '<p class="count">No results.</p>'; return; }
  const table = document.createElement('table');
  const thead = document.createElement('thead');
  const htr = document.createElement('tr');
  cols.forEach(c => { const th = document.createElement('th'); th.textContent = c; htr.appendChild(th); });
  thead.appendChild(htr); table.appendChild(thead);
  const tbody = document.createElement('tbody');
  rows.forEach(r => {
    const tr = document.createElement('tr');
    cols.forEach(c => {
      const td = document.createElement('td');
      const v = r[c];
      td.textContent = (v === null || v === undefined) ? '' : (typeof v === 'object' ? JSON.stringify(v) : v);
      tr.appendChild(td);
    });
    tbody.appendChild(tr);
  });
  table.appendChild(tbody);
  const count = document.createElement('p');
  count.className = 'count';
  count.textContent = `${rows.length} row${rows.length === 1 ? '' : 's'}`;
  box.appendChild(count); box.appendChild(table);
}

loadAccounts();
