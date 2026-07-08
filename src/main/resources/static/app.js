const $ = (id) => document.getElementById(id);

async function loadAccounts() {
  try {
    const res = await fetch('/api/accounts');
    const data = await res.json();
    const sel = $('account');
    sel.innerHTML = '';
    (data.accounts || []).forEach(a => {
      const opt = document.createElement('option');
      opt.value = a.id;
      opt.textContent = `${a.name} (${a.id})`;
      sel.appendChild(opt);
    });
    if (!data.accounts || data.accounts.length === 0) {
      const opt = document.createElement('option');
      opt.textContent = 'No accounts found';
      opt.value = '';
      sel.appendChild(opt);
    }
    updateHint(data);
  } catch (e) {
    $('hint').textContent = 'Could not load accounts: ' + e;
  }
}

function updateHint(data) {
  const mode = $('mode').value;
  if (data && data.error) {
    $('hint').textContent = data.error;
    return;
  }
  if (mode === 'english' && data && data.translationAvailable === false) {
    $('hint').textContent = 'English mode needs a running model (Ollama) or an API key. ' +
      'Switch Mode to "NRQL" to type a query directly.';
  } else if (mode === 'nrql') {
    $('hint').textContent = 'Type a NRQL query, e.g. SELECT * FROM Log SINCE 1 hour ago LIMIT 20';
  } else {
    $('hint').textContent = '';
  }
}

let lastAccountsData = null;
$('mode').addEventListener('change', () => updateHint(lastAccountsData));

function renderTable(result) {
  const box = $('results');
  box.innerHTML = '';
  const cols = result.columns || [];
  const rows = result.rows || [];
  if (rows.length === 0) {
    box.innerHTML = '<p class="count">No results.</p>';
    return;
  }
  const table = document.createElement('table');
  const thead = document.createElement('thead');
  const htr = document.createElement('tr');
  cols.forEach(c => { const th = document.createElement('th'); th.textContent = c; htr.appendChild(th); });
  thead.appendChild(htr);
  table.appendChild(thead);
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
  box.appendChild(count);
  box.appendChild(table);
}

$('searchForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const text = $('query').value.trim();
  const accountId = $('account').value;
  if (!text) return;
  if (!accountId) { $('status').innerHTML = '<span class="error">Pick an account first.</span>'; return; }

  const raw = $('mode').value === 'nrql';
  $('go').disabled = true;
  $('status').innerHTML = '<span class="loading">Searching…</span>';
  $('nrqlBox').hidden = true;
  $('results').innerHTML = '';

  try {
    const res = await fetch('/api/query', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ accountId: Number(accountId), text, raw })
    });
    const data = await res.json();
    if (!res.ok || data.error) {
      $('status').innerHTML = '<span class="error">' + (data.error || 'Query failed') + '</span>';
    } else {
      $('status').innerHTML = '';
      if (data.nrql) { $('nrqlText').textContent = data.nrql; $('nrqlBox').hidden = false; }
      renderTable(data);
    }
  } catch (err) {
    $('status').innerHTML = '<span class="error">' + err + '</span>';
  } finally {
    $('go').disabled = false;
  }
});

loadAccounts().then(() => {}).catch(() => {});
fetch('/api/accounts').then(r => r.json()).then(d => { lastAccountsData = d; updateHint(d); }).catch(() => {});
