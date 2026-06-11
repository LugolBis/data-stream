//  State 
var allData = { labels: [], values: [] };
var exclusionSet = new Set();
var inclusionSet = new Set();
var msgCount = 0;

//  Chart 
var chart = new Chart(
    document.getElementById('chart').getContext('2d'),
    {
        type: 'bar',
        data: {
            labels: [],
            datasets: [{
                label: 'Value',
                data: [],
                backgroundColor: 'rgba(129,140,248,.72)',
                borderColor: 'rgba(129,140,248,1)',
                borderWidth: 1, borderRadius: 5, borderSkipped: false
            }]
        },
        options: {
            indexAxis: 'y',
            responsive: true,
            animation: { duration: 200 },
            plugins: {
                legend: { display: false },
                tooltip: { callbacks: { label: function (c) { return '  ' + c.parsed.x; } } }
            },
            scales: {
                x: {
                    beginAtZero: true,
                    grid: { color: 'rgba(255,255,255,.06)' },
                    ticks: { color: '#94a3b8' }
                },
                y: {
                    grid: { display: false },
                    ticks: { color: '#e2e8f0', font: { size: 13 } }
                }
            }
        }
    }
);

//  Filter logic 
function applyFilters(labels, values) {
    var outL = [], outV = [];
    for (var i = 0; i < labels.length; i++) {
        var cat = labels[i], val = values[i];
        if (inclusionSet.size > 0 && !inclusionSet.has(cat)) continue;
        if (exclusionSet.has(cat)) continue;
        outL.push(cat);
        outV.push(val);
    }
    return { labels: outL, values: outV };
}

function updateChart() {
    var f = applyFilters(allData.labels, allData.values);
    chart.data.labels = f.labels;
    chart.data.datasets[0].data = f.values;
    chart.update('active');
    var total = allData.labels.length, shown = f.labels.length;
    document.getElementById('chart-info').textContent =
        total > 0 ? (shown < total ? shown + ' / ' + total + ' domains' : total + ' domains') : '';
}

//  Data refresh (called by WS) 
function refresh(payload) {
    allData = payload;
    msgCount++;
    document.getElementById('n-cats').textContent = payload.labels.length;
    document.getElementById('n-msgs').textContent = msgCount;
    document.getElementById('max-v').textContent =
        payload.values.length ? Math.max.apply(null, payload.values) : '—';
    renderCategoryList();
    refreshSearchResults('excl');
    refreshSearchResults('incl');
    updateChart();
}

//  Panel collapse 
function togglePanel(id) {
    var panel = document.getElementById(id);
    var body = document.getElementById('body-' + id);
    var hidden = body.style.display === 'none';
    body.style.display = hidden ? '' : 'none';
    if (hidden) {
        panel.classList.remove('collapsed');
        if (id === 'panel-chart') { setTimeout(function () { chart.resize(); }, 10); }
        if (id === 'panel-cats') { renderCategoryList(); }
    } else {
        panel.classList.add('collapsed');
    }
}

//  Category list 
function renderCategoryList() {
    var body = document.getElementById('body-panel-cats');
    if (body.style.display === 'none') return;
    var container = document.getElementById('cat-list');
    container.innerHTML = '';
    var q = document.getElementById('cats-search').value.toLowerCase();
    for (var i = 0; i < allData.labels.length; i++) {
        var label = allData.labels[i], value = allData.values[i];
        if (q && label.toLowerCase().indexOf(q) === -1) continue;
        var row = document.createElement('div'); row.className = 'cat-row';
        var nm = document.createElement('span'); nm.className = 'cat-name'; nm.textContent = label; nm.title = label;
        var vl = document.createElement('span'); vl.className = 'cat-val'; vl.textContent = value;
        var bE = document.createElement('button'); bE.className = 'btn-xs btn-excl-xs'; bE.textContent = '− Exclude';
        bE.dataset.cat = label; bE.onclick = function () { addToFilter('excl', this.dataset.cat); };
        var bI = document.createElement('button'); bI.className = 'btn-xs btn-incl-xs'; bI.textContent = '+ Include';
        bI.dataset.cat = label; bI.onclick = function () { addToFilter('incl', this.dataset.cat); };
        row.appendChild(nm); row.appendChild(vl); row.appendChild(bE); row.appendChild(bI);
        container.appendChild(row);
    }
    var badge = document.getElementById('badge-cats');
    badge.textContent = allData.labels.length > 0 ? allData.labels.length : '';
    badge.style.display = allData.labels.length > 0 ? '' : 'none';
}

//  Search results (add chips) 
function refreshSearchResults(type) {
    var input = document.getElementById(type + '-search');
    if (input) renderSearchResults(type, input.value.toLowerCase());
}

function renderSearchResults(type, query) {
    var container = document.getElementById(type + '-results');
    container.innerHTML = '';
    if (!query) return;
    var filterSet = type === 'excl' ? exclusionSet : inclusionSet;
    var matches = allData.labels.filter(function (l) {
        return l.toLowerCase().indexOf(query) !== -1 && !filterSet.has(l);
    });
    if (matches.length === 0) {
        var noRes = document.createElement('span'); noRes.className = 'no-results';
        noRes.textContent = 'No result'; container.appendChild(noRes); return;
    }
    matches.slice(0, 20).forEach(function (cat) {
        var chip = document.createElement('div'); chip.className = 'result-chip';
        var sp = document.createElement('span'); sp.textContent = cat; sp.title = cat;
        var btn = document.createElement('button'); btn.textContent = '+'; btn.title = 'Add a filter';
        btn.dataset.cat = cat; btn.dataset.type = type;
        btn.onclick = function () { addToFilter(this.dataset.type, this.dataset.cat); };
        chip.appendChild(sp); chip.appendChild(btn);
        container.appendChild(chip);
    });
}

//  Filter management 
function addToFilter(type, cat) {
    (type === 'excl' ? exclusionSet : inclusionSet).add(cat);
    renderChips(type); refreshSearchResults(type); updateChart();
}
function removeFromFilter(type, cat) {
    (type === 'excl' ? exclusionSet : inclusionSet).delete(cat);
    renderChips(type); refreshSearchResults(type); updateChart();
}
function clearFilter(type) {
    (type === 'excl' ? exclusionSet : inclusionSet).clear();
    renderChips(type); refreshSearchResults(type); updateChart();
}

function renderChips(type) {
    var set = type === 'excl' ? exclusionSet : inclusionSet;
    var container = document.getElementById(type + '-chips');
    var clearBtn = document.getElementById(type + '-clear');
    var badge = document.getElementById('badge-' + type);
    container.innerHTML = '';
    if (set.size === 0) {
        var hint = document.createElement('span'); hint.className = 'empty-hint';
        hint.textContent = type === 'excl'
            ? 'There is not domains excluded'
            : 'There is not domains included — all of them are displayed';
        container.appendChild(hint);
        clearBtn.style.display = 'none'; badge.style.display = 'none'; return;
    }
    set.forEach(function (cat) {
        var chip = document.createElement('div'); chip.className = 'chip chip-' + type;
        var sp = document.createElement('span'); sp.textContent = cat; sp.title = cat;
        var btn = document.createElement('button'); btn.className = 'chip-rm'; btn.textContent = '×'; btn.title = 'Delete';
        btn.dataset.cat = cat; btn.dataset.type = type;
        btn.onclick = function () { removeFromFilter(this.dataset.type, this.dataset.cat); };
        chip.appendChild(sp); chip.appendChild(btn);
        container.appendChild(chip);
    });
    clearBtn.style.display = ''; badge.textContent = set.size; badge.style.display = '';
}

//  Search input listeners 
document.getElementById('excl-search').addEventListener('input', function () {
    renderSearchResults('excl', this.value.toLowerCase());
});
document.getElementById('incl-search').addEventListener('input', function () {
    renderSearchResults('incl', this.value.toLowerCase());
});

//  WebSocket connection
function connect() {
    var dot = document.getElementById('dot');
    var txt = document.getElementById('status-text');
    var ws = new WebSocket('ws://' + location.host + '/ws');

    ws.onopen = function () {
        dot.className = 'live';
        txt.textContent = 'Live';
    };

    ws.onmessage = function (event) {
        refresh(JSON.parse(event.data));
    };

    ws.onclose = function () {
        dot.className = '';
        txt.textContent = 'Reconnecting…';
        setTimeout(connect, 2000);
    };
}

connect();