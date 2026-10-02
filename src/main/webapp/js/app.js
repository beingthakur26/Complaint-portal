// Shared helper functions used by all pages

// Send a GET request and return {status, data}
async function apiGet(url) {
  try {
    const response = await fetch(url);
    const data = await response.json().catch(() => ({}));
    return { status: response.status, data: data };
  } catch (err) {
    console.error("Network error on GET " + url, err);
    return { status: 500, data: { success: false, message: "Network connection error." } };
  }
}

// Send a POST request with form fields and return {status, data}
async function apiPost(url, fields) {
  try {
    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams(fields)
    });
    const data = await response.json().catch(() => ({}));
    return { status: response.status, data: data };
  } catch (err) {
    console.error("Network error on POST " + url, err);
    return { status: 500, data: { success: false, message: "Network connection error." } };
  }
}

// Format timestamp to user-friendly readable date string
function formatDate(dateStr) {
  if (!dateStr) return "-";
  try {
    // Handle both ISO strings and SQL strings (e.g. "2026-10-02 14:03:01")
    const d = new Date(dateStr.replace(" ", "T"));
    if (isNaN(d.getTime())) return dateStr;
    return d.toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit",
      hour12: true
    });
  } catch (e) {
    return dateStr;
  }
}

// Extract 1-2 letter initials from a full name
function getInitials(name) {
  if (!name) return "U";
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

// Create a <td> with plain text. textContent prevents XSS.
function cell(text, className) {
  const td = document.createElement("td");
  td.textContent = text === null || text === undefined ? "" : text;
  if (className) td.className = className;
  return td;
}

// Create a modern status badge with status dot indicator
function statusBadge(status) {
  const safeStatus = (status || "PENDING").toUpperCase().trim();
  const span = document.createElement("span");
  span.className = "badge " + safeStatus.replace(/\s+/g, "-");
  
  const dot = document.createElement("span");
  dot.className = "badge-dot";
  span.appendChild(dot);

  const textNode = document.createTextNode(safeStatus);
  span.appendChild(textNode);
  return span;
}

// Inline message banner under forms
function showMessage(elementId, text, type) {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.textContent = text;
  el.className = "msg " + type;
}

// Modern floating toast notification
function showToast(text, type = "ok", duration = 3500) {
  let container = document.getElementById("toastContainer");
  if (!container) {
    container = document.createElement("div");
    container.id = "toastContainer";
    container.className = "toast-container";
    document.body.appendChild(container);
  }

  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;

  const iconSvg = type === "ok" 
    ? `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>`
    : `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>`;

  toast.innerHTML = `
    <div style="display:flex;align-items:center;">${iconSvg}</div>
    <div class="toast-message">${escapeHtml(text)}</div>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transform = "translateY(10px) scale(0.95)";
    setTimeout(() => toast.remove(), 250);
  }, duration);
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str;
  return div.innerHTML;
}

// Global Logout
async function logout() {
  await apiPost("api/logout", {});
  window.location.href = "login.html";
}
